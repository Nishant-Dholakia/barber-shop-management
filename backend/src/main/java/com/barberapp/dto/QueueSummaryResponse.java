package com.barberapp.dto;

public record QueueSummaryResponse(
        long queueLength,
        int estimatedWaitMinutes
) {
}
