package com.barberapp.service;

import com.barberapp.domain.QueueStatus;
import com.barberapp.domain.ServiceType;
import com.barberapp.dto.JoinQueueRequest;
import com.barberapp.dto.JoinQueueResponse;
import com.barberapp.dto.QueueStatusResponse;
import com.barberapp.entity.QueueEntry;
import com.barberapp.exception.ResourceNotFoundException;
import com.barberapp.repository.QueueEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-31T11:05:00Z"), ZoneId.of("UTC"));

    @Mock
    private QueueEntryRepository queueEntryRepository;

    @Mock
    private ServiceCatalog serviceCatalog;

    private final QueueCalculationService queueCalculationService = new QueueCalculationService(clock);

    private QueueService queueService;

    @BeforeEach
    void setUp() {
        queueService = new QueueService(clock, queueEntryRepository, queueCalculationService, serviceCatalog);
    }

    @Test
    void joinsQueueAndReturnsCalculatedPositionWaitAndTurnTime() {
        QueueEntry first = queueEntry(1L, QueueStatus.IN_PROGRESS, 20, LocalDateTime.parse("2026-05-31T10:00:00"));
        QueueEntry second = queueEntry(2L, QueueStatus.WAITING, 25, LocalDateTime.parse("2026-05-31T10:05:00"));
        QueueEntry saved = queueEntry(3L, QueueStatus.WAITING, 20, LocalDateTime.parse("2026-05-31T11:05:00"));
        JoinQueueRequest request = new JoinQueueRequest(" Ravi ", " 9876543210 ", ServiceType.HAIRCUT);

        when(serviceCatalog.getDurationMinutes(ServiceType.HAIRCUT)).thenReturn(20);
        when(queueEntryRepository.save(any(QueueEntry.class))).thenReturn(saved);
        when(queueEntryRepository.findByStatusInOrderByJoinedAtAscIdAsc(QueueStatus.activeStatuses()))
                .thenReturn(List.of(first, second, saved));

        JoinQueueResponse response = queueService.joinQueue(request);

        assertThat(response.queueId()).isEqualTo(3L);
        assertThat(response.position()).isEqualTo(3);
        assertThat(response.estimatedWaitMinutes()).isEqualTo(45);
        assertThat(response.estimatedTurnTime()).isEqualTo("11:50");

        ArgumentCaptor<QueueEntry> entryCaptor = ArgumentCaptor.forClass(QueueEntry.class);
        verify(queueEntryRepository).save(entryCaptor.capture());
        QueueEntry entryToSave = entryCaptor.getValue();
        assertThat(entryToSave.getName()).isEqualTo("Ravi");
        assertThat(entryToSave.getPhone()).isEqualTo("9876543210");
        assertThat(entryToSave.getService()).isEqualTo(ServiceType.HAIRCUT);
        assertThat(entryToSave.getDurationMinutes()).isEqualTo(20);
        assertThat(entryToSave.getStatus()).isEqualTo(QueueStatus.WAITING);
        assertThat(entryToSave.getJoinedAt()).isEqualTo(LocalDateTime.parse("2026-05-31T11:05:00"));
    }

    @Test
    void returnsQueueStatusWithCalculatedValues() {
        QueueEntry first = queueEntry(1L, QueueStatus.IN_PROGRESS, 20, LocalDateTime.parse("2026-05-31T10:00:00"));
        QueueEntry current = queueEntry(2L, QueueStatus.WAITING, 25, LocalDateTime.parse("2026-05-31T10:05:00"));

        when(queueEntryRepository.findById(2L)).thenReturn(Optional.of(current));
        when(queueEntryRepository.findByStatusInOrderByJoinedAtAscIdAsc(QueueStatus.activeStatuses()))
                .thenReturn(List.of(first, current));

        QueueStatusResponse response = queueService.getStatus(2L);

        assertThat(response.queueId()).isEqualTo(2L);
        assertThat(response.position()).isEqualTo(2);
        assertThat(response.estimatedWaitMinutes()).isEqualTo(20);
        assertThat(response.estimatedTurnTime()).isEqualTo("11:25");
        assertThat(response.status()).isEqualTo(QueueStatus.WAITING);
    }

    @Test
    void throwsWhenQueueEntryDoesNotExist() {
        when(queueEntryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queueService.getStatus(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Queue entry not found: 99");
    }

    private QueueEntry queueEntry(Long id, QueueStatus status, int durationMinutes, LocalDateTime joinedAt) {
        QueueEntry queueEntry = new QueueEntry();
        queueEntry.setId(id);
        queueEntry.setName("Customer " + id);
        queueEntry.setService(ServiceType.HAIRCUT);
        queueEntry.setDurationMinutes(durationMinutes);
        queueEntry.setStatus(status);
        queueEntry.setJoinedAt(joinedAt);
        return queueEntry;
    }
}
