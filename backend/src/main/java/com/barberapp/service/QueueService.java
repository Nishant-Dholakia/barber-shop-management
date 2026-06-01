package com.barberapp.service;

import com.barberapp.domain.QueueStatus;
import com.barberapp.dto.JoinQueueRequest;
import com.barberapp.dto.JoinQueueResponse;
import com.barberapp.dto.QueueStatusResponse;
import com.barberapp.entity.QueueEntry;
import com.barberapp.exception.QueueEntryNotFoundException;
import com.barberapp.repository.QueueEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {

    private final Clock clock;
    private final QueueEntryRepository queueEntryRepository;
    private final QueueCalculationService queueCalculationService;
    private final ServiceCatalog serviceCatalog;

    @Transactional
    public JoinQueueResponse joinQueue(JoinQueueRequest request) {
        QueueEntry queueEntry = new QueueEntry();
        queueEntry.setName(request.name().trim());
        queueEntry.setPhone(normalizePhone(request.phone()));
        queueEntry.setService(request.service());
        queueEntry.setDurationMinutes(serviceCatalog.getDurationMinutes(request.service()));
        queueEntry.setStatus(QueueStatus.WAITING);
        queueEntry.setJoinedAt(LocalDateTime.now(clock));

        QueueEntry savedEntry = queueEntryRepository.save(queueEntry);
        log.info("Customer joined queue: queueId={}, service={}", savedEntry.getId(), savedEntry.getService());

        List<QueueEntry> activeQueue = queueEntryRepository.findByStatusInOrderByJoinedAtAscIdAsc(QueueStatus.activeStatuses());
        QueueCalculationService.QueueCalculation calculation = queueCalculationService.calculate(savedEntry, activeQueue);

        return new JoinQueueResponse(
                savedEntry.getId(),
                calculation.position(),
                calculation.estimatedWaitMinutes(),
                calculation.estimatedTurnTime()
        );
    }

    @Transactional(readOnly = true)
    public QueueStatusResponse getStatus(Long queueId) {
        QueueEntry queueEntry = queueEntryRepository.findById(queueId)
                .orElseThrow(() -> new QueueEntryNotFoundException(queueId));

        List<QueueEntry> activeQueue = queueEntryRepository.findByStatusInOrderByJoinedAtAscIdAsc(QueueStatus.activeStatuses());
        QueueCalculationService.QueueCalculation calculation = queueCalculationService.calculate(queueEntry, activeQueue);

        return new QueueStatusResponse(
                queueEntry.getId(),
                calculation.position(),
                calculation.estimatedWaitMinutes(),
                calculation.estimatedTurnTime(),
                queueEntry.getStatus()
        );
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return phone.trim();
    }
}
