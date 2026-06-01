package com.barberapp.service;

import com.barberapp.domain.QueueStatus;
import com.barberapp.domain.ServiceType;
import com.barberapp.dto.BarberDashboardResponse;
import com.barberapp.dto.BarberQueueItemResponse;
import com.barberapp.dto.CompleteCustomerRequest;
import com.barberapp.dto.MessageResponse;
import com.barberapp.dto.WalkInRequest;
import com.barberapp.entity.QueueEntry;
import com.barberapp.entity.RevenueEntry;
import com.barberapp.exception.InvalidQueueStateException;
import com.barberapp.exception.ResourceNotFoundException;
import com.barberapp.repository.QueueEntryRepository;
import com.barberapp.repository.RevenueEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarberServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-31T11:05:00Z"), ZoneId.of("UTC"));

    @Mock
    private QueueEntryRepository queueEntryRepository;

    @Mock
    private RevenueEntryRepository revenueEntryRepository;

    @Mock
    private ServiceCatalog serviceCatalog;

    @Mock
    private QueueTransitionService queueTransitionService;

    private BarberService barberService;

    @BeforeEach
    void setUp() {
        barberService = new BarberService(
                clock,
                queueEntryRepository,
                revenueEntryRepository,
                serviceCatalog,
                queueTransitionService
        );
    }

    @Test
    void returnsActiveBarberQueueInQueueOrder() {
        QueueEntry first = queueEntry(1L, "Ravi", QueueStatus.IN_PROGRESS, ServiceType.HAIRCUT, 20);
        QueueEntry second = queueEntry(2L, "Amit", QueueStatus.WAITING, ServiceType.BEARD, 10);
        when(queueEntryRepository.findByStatusInOrderByJoinedAtAscIdAsc(QueueStatus.activeStatuses()))
                .thenReturn(List.of(first, second));

        List<BarberQueueItemResponse> response = barberService.getQueue();

        assertThat(response).containsExactly(
                new BarberQueueItemResponse(1L, "Ravi", ServiceType.HAIRCUT, QueueStatus.IN_PROGRESS),
                new BarberQueueItemResponse(2L, "Amit", ServiceType.BEARD, QueueStatus.WAITING)
        );
    }

    @Test
    void addsWalkInAtEndOfQueueAndAdvancesIfNeeded() {
        WalkInRequest request = new WalkInRequest(" Walkin Customer ", ServiceType.BEARD);
        when(serviceCatalog.getDurationMinutes(ServiceType.BEARD)).thenReturn(10);

        MessageResponse response = barberService.addWalkIn(request);

        assertThat(response.message()).isEqualTo("Customer added");

        ArgumentCaptor<QueueEntry> entryCaptor = ArgumentCaptor.forClass(QueueEntry.class);
        verify(queueEntryRepository).save(entryCaptor.capture());
        QueueEntry savedEntry = entryCaptor.getValue();
        assertThat(savedEntry.getName()).isEqualTo("Walkin Customer");
        assertThat(savedEntry.getService()).isEqualTo(ServiceType.BEARD);
        assertThat(savedEntry.getDurationMinutes()).isEqualTo(10);
        assertThat(savedEntry.getStatus()).isEqualTo(QueueStatus.WAITING);
        assertThat(savedEntry.getJoinedAt()).isEqualTo(LocalDateTime.parse("2026-05-31T11:05:00"));
        verify(queueTransitionService).advanceQueueIfNeeded();
    }

    @Test
    void completesCustomerRecordsRevenueAndAdvancesQueue() {
        QueueEntry entry = queueEntry(1L, "Ravi", QueueStatus.IN_PROGRESS, ServiceType.HAIRCUT, 20);
        CompleteCustomerRequest request = new CompleteCustomerRequest(new BigDecimal("150.00"));
        when(queueEntryRepository.findById(1L)).thenReturn(Optional.of(entry));

        MessageResponse response = barberService.completeCustomer(1L, request);

        assertThat(response.message()).isEqualTo("Customer completed");

        ArgumentCaptor<QueueEntry> queueCaptor = ArgumentCaptor.forClass(QueueEntry.class);
        verify(queueEntryRepository).save(queueCaptor.capture());
        QueueEntry savedQueueEntry = queueCaptor.getValue();
        assertThat(savedQueueEntry.getStatus()).isEqualTo(QueueStatus.COMPLETED);
        assertThat(savedQueueEntry.getCompletedAt()).isEqualTo(LocalDateTime.parse("2026-05-31T11:05:00"));

        ArgumentCaptor<RevenueEntry> revenueCaptor = ArgumentCaptor.forClass(RevenueEntry.class);
        verify(revenueEntryRepository).save(revenueCaptor.capture());
        RevenueEntry savedRevenue = revenueCaptor.getValue();
        assertThat(savedRevenue.getCustomerName()).isEqualTo("Ravi");
        assertThat(savedRevenue.getService()).isEqualTo(ServiceType.HAIRCUT);
        assertThat(savedRevenue.getAmount()).isEqualByComparingTo("150.00");
        assertThat(savedRevenue.getCreatedAt()).isEqualTo(LocalDateTime.parse("2026-05-31T11:05:00"));
        verify(queueTransitionService).advanceQueueIfNeeded();
    }

    @Test
    void doesNotCompleteInactiveCustomer() {
        QueueEntry entry = queueEntry(1L, "Ravi", QueueStatus.COMPLETED, ServiceType.HAIRCUT, 20);
        when(queueEntryRepository.findById(1L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> barberService.completeCustomer(1L, new CompleteCustomerRequest(new BigDecimal("150.00"))))
                .isInstanceOf(InvalidQueueStateException.class)
                .hasMessage("Only active queue entries can be completed");

        verify(revenueEntryRepository, never()).save(any(RevenueEntry.class));
        verify(queueTransitionService, never()).advanceQueueIfNeeded();
    }

    @Test
    void marksActiveCustomerAsNoShowAndAdvancesQueue() {
        QueueEntry entry = queueEntry(1L, "Ravi", QueueStatus.WAITING, ServiceType.HAIRCUT, 20);
        when(queueEntryRepository.findById(1L)).thenReturn(Optional.of(entry));

        MessageResponse response = barberService.markNoShow(1L);

        assertThat(response.message()).isEqualTo("Customer marked as no show");

        ArgumentCaptor<QueueEntry> queueCaptor = ArgumentCaptor.forClass(QueueEntry.class);
        verify(queueEntryRepository).save(queueCaptor.capture());
        assertThat(queueCaptor.getValue().getStatus()).isEqualTo(QueueStatus.NO_SHOW);
        verify(queueTransitionService).advanceQueueIfNeeded();
    }

    @Test
    void returnsDashboardForToday() {
        LocalDateTime startOfDay = LocalDateTime.parse("2026-05-31T00:00:00");
        LocalDateTime startOfTomorrow = LocalDateTime.parse("2026-06-01T00:00:00");
        when(revenueEntryRepository.sumAmountByCreatedAtBetween(startOfDay, startOfTomorrow))
                .thenReturn(new BigDecimal("1500.00"));
        when(queueEntryRepository.countByStatusAndCompletedAtBetween(
                QueueStatus.COMPLETED,
                startOfDay,
                startOfTomorrow
        )).thenReturn(12L);
        when(queueEntryRepository.countByStatus(QueueStatus.WAITING)).thenReturn(4L);

        BarberDashboardResponse response = barberService.getDashboard();

        assertThat(response.todayRevenue()).isEqualByComparingTo("1500.00");
        assertThat(response.customersServed()).isEqualTo(12);
        assertThat(response.queueLength()).isEqualTo(4);
    }

    @Test
    void returnsZeroRevenueWhenNoRevenueExistsToday() {
        when(revenueEntryRepository.sumAmountByCreatedAtBetween(
                LocalDateTime.parse("2026-05-31T00:00:00"),
                LocalDateTime.parse("2026-06-01T00:00:00")
        )).thenReturn(null);
        when(queueEntryRepository.countByStatusAndCompletedAtBetween(
                QueueStatus.COMPLETED,
                LocalDateTime.parse("2026-05-31T00:00:00"),
                LocalDateTime.parse("2026-06-01T00:00:00")
        )).thenReturn(0L);
        when(queueEntryRepository.countByStatus(QueueStatus.WAITING)).thenReturn(0L);

        BarberDashboardResponse response = barberService.getDashboard();

        assertThat(response.todayRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void throwsWhenQueueEntryDoesNotExist() {
        when(queueEntryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> barberService.markNoShow(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Queue entry not found: 99");
    }

    private QueueEntry queueEntry(
            Long id,
            String name,
            QueueStatus status,
            ServiceType service,
            int durationMinutes
    ) {
        QueueEntry queueEntry = new QueueEntry();
        queueEntry.setId(id);
        queueEntry.setName(name);
        queueEntry.setService(service);
        queueEntry.setDurationMinutes(durationMinutes);
        queueEntry.setStatus(status);
        queueEntry.setJoinedAt(LocalDateTime.parse("2026-05-31T10:00:00"));
        return queueEntry;
    }
}
