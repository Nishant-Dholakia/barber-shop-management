package com.barberapp.service;

import com.barberapp.domain.QueueStatus;
import com.barberapp.domain.ServiceType;
import com.barberapp.entity.QueueEntry;
import com.barberapp.exception.InvalidQueueStateException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueueCalculationServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-31T11:05:00Z"), ZoneId.of("UTC"));
    private final QueueCalculationService queueCalculationService = new QueueCalculationService(clock);

    @Test
    void calculatesPositionWaitAndExpectedTurnTime() {
        QueueEntry first = queueEntry(1L, QueueStatus.IN_PROGRESS, 20, LocalDateTime.parse("2026-05-31T10:00:00"));
        QueueEntry second = queueEntry(2L, QueueStatus.WAITING, 25, LocalDateTime.parse("2026-05-31T10:05:00"));
        QueueEntry current = queueEntry(3L, QueueStatus.WAITING, 10, LocalDateTime.parse("2026-05-31T10:10:00"));

        QueueCalculationService.QueueCalculation calculation = queueCalculationService.calculate(
                current,
                List.of(first, second, current)
        );

        assertThat(calculation.position()).isEqualTo(3);
        assertThat(calculation.estimatedWaitMinutes()).isEqualTo(45);
        assertThat(calculation.estimatedTurnTime()).isEqualTo("11:50");
    }

    @Test
    void inactiveEntryHasNoActiveQueueCalculation() {
        QueueEntry completed = queueEntry(1L, QueueStatus.COMPLETED, 20, LocalDateTime.parse("2026-05-31T10:00:00"));

        QueueCalculationService.QueueCalculation calculation = queueCalculationService.calculate(completed, List.of());

        assertThat(calculation.position()).isZero();
        assertThat(calculation.estimatedWaitMinutes()).isZero();
        assertThat(calculation.estimatedTurnTime()).isNull();
    }

    @Test
    void calculatesEstimatedWaitForNewCustomerFromActiveQueueDurations() {
        List<QueueEntry> activeQueue = List.of(
                queueEntry(1L, QueueStatus.IN_PROGRESS, 20, LocalDateTime.parse("2026-05-31T10:00:00")),
                queueEntry(2L, QueueStatus.WAITING, 25, LocalDateTime.parse("2026-05-31T10:05:00")),
                queueEntry(3L, QueueStatus.WAITING, 10, LocalDateTime.parse("2026-05-31T10:10:00"))
        );

        int estimatedWaitMinutes = queueCalculationService.calculateEstimatedWaitForNewCustomer(activeQueue);

        assertThat(estimatedWaitMinutes).isEqualTo(55);
    }

    @Test
    void throwsWhenActiveEntryIsMissingFromActiveQueue() {
        QueueEntry current = queueEntry(2L, QueueStatus.WAITING, 10, LocalDateTime.parse("2026-05-31T10:10:00"));

        assertThatThrownBy(() -> queueCalculationService.calculate(current, List.of()))
                .isInstanceOf(InvalidQueueStateException.class)
                .hasMessage("Queue entry is not present in active queue");
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
