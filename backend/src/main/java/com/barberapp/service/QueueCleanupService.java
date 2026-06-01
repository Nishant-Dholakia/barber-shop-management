package com.barberapp.service;

import com.barberapp.config.CleanupProperties;
import com.barberapp.domain.QueueStatus;
import com.barberapp.repository.QueueEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueCleanupService {

    private final Clock clock;
    private final CleanupProperties cleanupProperties;
    private final QueueEntryRepository queueEntryRepository;

    @Scheduled(cron = "${barber.cleanup.cron}")
    @Transactional
    public void cleanupOldTerminalQueueEntries() {
        if (!cleanupProperties.isEnabled()) {
            log.debug("Queue cleanup skipped because it is disabled");
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(cleanupProperties.getRetentionDays());
        long deletedCount = queueEntryRepository.deleteByStatusInAndJoinedAtBefore(
                QueueStatus.cleanupEligibleStatuses(),
                cutoff
        );

        log.info("Queue cleanup deleted {} terminal entries older than {}", deletedCount, cutoff);
    }
}
