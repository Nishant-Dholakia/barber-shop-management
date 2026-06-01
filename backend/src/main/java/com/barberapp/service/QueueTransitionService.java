package com.barberapp.service;

import com.barberapp.domain.QueueStatus;
import com.barberapp.entity.QueueEntry;
import com.barberapp.repository.QueueEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueTransitionService {

    private final Clock clock;
    private final QueueEntryRepository queueEntryRepository;

    public void advanceQueueIfNeeded() {
        if (queueEntryRepository.countByStatus(QueueStatus.IN_PROGRESS) > 0) {
            return;
        }

        queueEntryRepository.findFirstByStatusOrderByJoinedAtAscIdAsc(QueueStatus.WAITING)
                .ifPresent(this::startEntry);
    }

    private void startEntry(QueueEntry queueEntry) {
        queueEntry.setStatus(QueueStatus.IN_PROGRESS);
        queueEntry.setStartedAt(LocalDateTime.now(clock));
        queueEntryRepository.save(queueEntry);
        log.info("Queue advanced: queueId={} moved to IN_PROGRESS", queueEntry.getId());
    }
}
