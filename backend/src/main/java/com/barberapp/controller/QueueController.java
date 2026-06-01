package com.barberapp.controller;

import com.barberapp.dto.JoinQueueRequest;
import com.barberapp.dto.JoinQueueResponse;
import com.barberapp.dto.QueueStatusResponse;
import com.barberapp.dto.QueueSummaryResponse;
import com.barberapp.service.QueueService;
import com.barberapp.service.QueueSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/queue")
@Tag(name = "Queue", description = "Customer queue APIs")
public class QueueController {

    private final QueueService queueService;
    private final QueueSummaryService queueSummaryService;

    @PostMapping("/join")
    @Operation(summary = "Join the customer queue")
    public JoinQueueResponse joinQueue(@Valid @RequestBody JoinQueueRequest request) {
        return queueService.joinQueue(request);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get current queue summary")
    public QueueSummaryResponse getQueueSummary() {
        return queueSummaryService.getSummary();
    }

    @GetMapping("/status/{id}")
    @Operation(summary = "Get queue status for a customer")
    public QueueStatusResponse getQueueStatus(@Positive @PathVariable Long id) {
        return queueService.getStatus(id);
    }
}
