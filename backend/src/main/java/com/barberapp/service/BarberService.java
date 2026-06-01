package com.barberapp.service;

import com.barberapp.domain.QueueStatus;
import com.barberapp.dto.BarberDashboardResponse;
import com.barberapp.dto.BarberQueueItemResponse;
import com.barberapp.dto.CompleteCustomerRequest;
import com.barberapp.dto.MessageResponse;
import com.barberapp.dto.WalkInRequest;
import com.barberapp.entity.QueueEntry;
import com.barberapp.entity.RevenueEntry;
import com.barberapp.exception.InvalidQueueStateException;
import com.barberapp.exception.QueueEntryNotFoundException;
import com.barberapp.repository.QueueEntryRepository;
import com.barberapp.repository.RevenueEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BarberService {

    private final Clock clock;
    private final QueueEntryRepository queueEntryRepository;
    private final RevenueEntryRepository revenueEntryRepository;
    private final ServiceCatalog serviceCatalog;
    private final QueueTransitionService queueTransitionService;

    @Transactional(readOnly = true)
    public List<BarberQueueItemResponse> getQueue() {
        return queueEntryRepository.findByStatusInOrderByJoinedAtAscIdAsc(QueueStatus.activeStatuses())
                .stream()
                .map(this::toQueueItemResponse)
                .toList();
    }

    @Transactional
    public MessageResponse addWalkIn(WalkInRequest request) {
        QueueEntry queueEntry = new QueueEntry();
        queueEntry.setName(request.name().trim());
        queueEntry.setService(request.service());
        queueEntry.setDurationMinutes(serviceCatalog.getDurationMinutes(request.service()));
        queueEntry.setStatus(QueueStatus.WAITING);
        queueEntry.setJoinedAt(LocalDateTime.now(clock));

        queueEntryRepository.save(queueEntry);
        log.info("Walk-in customer added: service={}", queueEntry.getService());
        queueTransitionService.advanceQueueIfNeeded();

        return new MessageResponse("Customer added");
    }

    @Transactional
    public MessageResponse completeCustomer(Long queueId, CompleteCustomerRequest request) {
        QueueEntry queueEntry = getQueueEntry(queueId);
        if (!QueueStatus.activeStatuses().contains(queueEntry.getStatus())) {
            throw new InvalidQueueStateException("Only active queue entries can be completed");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        queueEntry.setStatus(QueueStatus.COMPLETED);
        queueEntry.setCompletedAt(now);
        queueEntryRepository.save(queueEntry);

        RevenueEntry revenueEntry = new RevenueEntry();
        revenueEntry.setCustomerName(queueEntry.getName());
        revenueEntry.setService(queueEntry.getService());
        revenueEntry.setAmount(request.amount());
        revenueEntry.setCreatedAt(now);
        revenueEntryRepository.save(revenueEntry);
        log.info("Customer completed: queueId={}, amount={}", queueId, request.amount());

        queueTransitionService.advanceQueueIfNeeded();

        return new MessageResponse("Customer completed");
    }

    @Transactional
    public MessageResponse markNoShow(Long queueId) {
        QueueEntry queueEntry = getQueueEntry(queueId);
        if (!QueueStatus.activeStatuses().contains(queueEntry.getStatus())) {
            throw new InvalidQueueStateException("Only active queue entries can be marked as no-show");
        }

        queueEntry.setStatus(QueueStatus.NO_SHOW);
        queueEntryRepository.save(queueEntry);
        log.info("Customer marked no-show: queueId={}", queueId);
        queueTransitionService.advanceQueueIfNeeded();

        return new MessageResponse("Customer marked as no show");
    }

    @Transactional(readOnly = true)
    public BarberDashboardResponse getDashboard() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay();

        BigDecimal todayRevenue = revenueEntryRepository.sumAmountByCreatedAtBetween(startOfDay, startOfTomorrow);
        long customersServed = queueEntryRepository.countByStatusAndCompletedAtBetween(
                QueueStatus.COMPLETED,
                startOfDay,
                startOfTomorrow
        );
        long queueLength = queueEntryRepository.countByStatus(QueueStatus.WAITING);

        return new BarberDashboardResponse(
                todayRevenue == null ? BigDecimal.ZERO : todayRevenue,
                customersServed,
                queueLength
        );
    }

    private QueueEntry getQueueEntry(Long queueId) {
        return queueEntryRepository.findById(queueId)
                .orElseThrow(() -> new QueueEntryNotFoundException(queueId));
    }

    private BarberQueueItemResponse toQueueItemResponse(QueueEntry queueEntry) {
        return new BarberQueueItemResponse(
                queueEntry.getId(),
                queueEntry.getName(),
                queueEntry.getService(),
                queueEntry.getStatus()
        );
    }
}
