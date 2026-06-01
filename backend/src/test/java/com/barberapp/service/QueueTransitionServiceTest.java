package com.barberapp.service;

import com.barberapp.domain.QueueStatus;
import com.barberapp.domain.ServiceType;
import com.barberapp.entity.QueueEntry;
import com.barberapp.repository.QueueEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueueTransitionServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-31T11:05:00Z"), ZoneId.of("UTC"));

    @Mock
    private QueueEntryRepository queueEntryRepository;

    private QueueTransitionService queueTransitionService;

    @BeforeEach
    void setUp() {
        queueTransitionService = new QueueTransitionService(clock, queueEntryRepository);
    }

    @Test
    void doesNotAdvanceWhenCustomerIsAlreadyInProgress() {
        when(queueEntryRepository.countByStatus(QueueStatus.IN_PROGRESS)).thenReturn(1L);

        queueTransitionService.advanceQueueIfNeeded();

        verify(queueEntryRepository, never()).findFirstByStatusOrderByJoinedAtAscIdAsc(QueueStatus.WAITING);
        verify(queueEntryRepository, never()).save(org.mockito.ArgumentMatchers.any(QueueEntry.class));
    }

    @Test
    void advancesFirstWaitingCustomerWhenNoCustomerIsInProgress() {
        QueueEntry waiting = queueEntry(2L, QueueStatus.WAITING);
        when(queueEntryRepository.countByStatus(QueueStatus.IN_PROGRESS)).thenReturn(0L);
        when(queueEntryRepository.findFirstByStatusOrderByJoinedAtAscIdAsc(QueueStatus.WAITING))
                .thenReturn(Optional.of(waiting));

        queueTransitionService.advanceQueueIfNeeded();

        ArgumentCaptor<QueueEntry> entryCaptor = ArgumentCaptor.forClass(QueueEntry.class);
        verify(queueEntryRepository).save(entryCaptor.capture());

        QueueEntry savedEntry = entryCaptor.getValue();
        assertThat(savedEntry.getStatus()).isEqualTo(QueueStatus.IN_PROGRESS);
        assertThat(savedEntry.getStartedAt()).isEqualTo(LocalDateTime.parse("2026-05-31T11:05:00"));
    }

    @Test
    void doesNothingWhenNoWaitingCustomerExists() {
        when(queueEntryRepository.countByStatus(QueueStatus.IN_PROGRESS)).thenReturn(0L);
        when(queueEntryRepository.findFirstByStatusOrderByJoinedAtAscIdAsc(QueueStatus.WAITING))
                .thenReturn(Optional.empty());

        queueTransitionService.advanceQueueIfNeeded();

        verify(queueEntryRepository, never()).save(org.mockito.ArgumentMatchers.any(QueueEntry.class));
    }

    private QueueEntry queueEntry(Long id, QueueStatus status) {
        QueueEntry queueEntry = new QueueEntry();
        queueEntry.setId(id);
        queueEntry.setName("Customer " + id);
        queueEntry.setService(ServiceType.HAIRCUT);
        queueEntry.setDurationMinutes(20);
        queueEntry.setStatus(status);
        queueEntry.setJoinedAt(LocalDateTime.parse("2026-05-31T10:00:00"));
        return queueEntry;
    }
}
