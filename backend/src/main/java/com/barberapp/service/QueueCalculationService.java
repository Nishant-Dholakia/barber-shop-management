package com.barberapp.service;

import com.barberapp.domain.QueueStatus;
import com.barberapp.entity.QueueEntry;
import com.barberapp.exception.InvalidQueueStateException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Service
public class QueueCalculationService {

    private static final DateTimeFormatter TURN_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<QueueStatus> ACTIVE_STATUSES = Set.of(QueueStatus.WAITING, QueueStatus.IN_PROGRESS);

    private final Clock clock;

    public QueueCalculationService(Clock clock) {
        this.clock = clock;
    }

    public QueueCalculation calculate(QueueEntry currentEntry, List<QueueEntry> activeQueue) {
        if (!ACTIVE_STATUSES.contains(currentEntry.getStatus())) {
            return new QueueCalculation(0, 0, null);
        }

        int position = 0;
        int estimatedWaitMinutes = 0;

        for (QueueEntry queueEntry : activeQueue) {
            position++;
            if (queueEntry.getId().equals(currentEntry.getId())) {
                return new QueueCalculation(position, estimatedWaitMinutes, formatExpectedTurnTime(estimatedWaitMinutes));
            }
            estimatedWaitMinutes += queueEntry.getDurationMinutes();
        }

        throw new InvalidQueueStateException("Queue entry is not present in active queue");
    }

    public int calculateEstimatedWaitForNewCustomer(List<QueueEntry> activeQueue) {
        return activeQueue.stream()
                .mapToInt(QueueEntry::getDurationMinutes)
                .sum();
    }

    public String formatExpectedTurnTime(int estimatedWaitMinutes) {
        return LocalTime.now(clock)
                .plusMinutes(estimatedWaitMinutes)
                .format(TURN_TIME_FORMATTER);
    }

    public record QueueCalculation(
            int position,
            int estimatedWaitMinutes,
            String estimatedTurnTime
    ) {
    }
}
