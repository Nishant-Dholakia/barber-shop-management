package com.barberapp.service;

import com.barberapp.config.CleanupProperties;
import com.barberapp.domain.QueueStatus;
import com.barberapp.repository.QueueEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueueCleanupServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneId.of("UTC"));

    @Mock
    private QueueEntryRepository queueEntryRepository;

    private CleanupProperties cleanupProperties;
    private QueueCleanupService queueCleanupService;

    @BeforeEach
    void setUp() {
        cleanupProperties = new CleanupProperties();
        queueCleanupService = new QueueCleanupService(clock, cleanupProperties, queueEntryRepository);
    }

    @Test
    void deletesTerminalQueueEntriesOlderThanRetention() {
        cleanupProperties.setRetentionDays(2);
        when(queueEntryRepository.deleteByStatusInAndJoinedAtBefore(
                QueueStatus.cleanupEligibleStatuses(),
                LocalDateTime.parse("2026-05-30T00:00:00")
        )).thenReturn(3L);

        queueCleanupService.cleanupOldTerminalQueueEntries();

        verify(queueEntryRepository).deleteByStatusInAndJoinedAtBefore(
                QueueStatus.cleanupEligibleStatuses(),
                LocalDateTime.parse("2026-05-30T00:00:00")
        );
    }

    @Test
    void skipsCleanupWhenDisabled() {
        cleanupProperties.setEnabled(false);

        queueCleanupService.cleanupOldTerminalQueueEntries();

        verify(queueEntryRepository, never()).deleteByStatusInAndJoinedAtBefore(
                org.mockito.ArgumentMatchers.anyCollection(),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class)
        );
    }
}
