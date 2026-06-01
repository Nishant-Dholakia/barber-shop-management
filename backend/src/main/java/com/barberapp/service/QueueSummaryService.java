package com.barberapp.service;

import com.barberapp.domain.QueueStatus;
import com.barberapp.dto.QueueSummaryResponse;
import com.barberapp.entity.QueueEntry;
import com.barberapp.repository.QueueEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QueueSummaryService {

    private final QueueEntryRepository queueEntryRepository;
    private final QueueCalculationService queueCalculationService;

    @Transactional(readOnly = true)
    public QueueSummaryResponse getSummary() {
        long queueLength = queueEntryRepository.countByStatus(QueueStatus.WAITING);
        List<QueueEntry> activeQueue = queueEntryRepository.findByStatusInOrderByJoinedAtAscIdAsc(QueueStatus.activeStatuses());
        int estimatedWaitMinutes = queueCalculationService.calculateEstimatedWaitForNewCustomer(activeQueue);

        return new QueueSummaryResponse(queueLength, estimatedWaitMinutes);
    }
}
