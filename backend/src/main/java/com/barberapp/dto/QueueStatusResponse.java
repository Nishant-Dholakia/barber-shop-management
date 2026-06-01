package com.barberapp.dto;

import com.barberapp.domain.QueueStatus;

public record QueueStatusResponse(
        Long queueId,
        int position,
        int estimatedWaitMinutes,
        String estimatedTurnTime,
        QueueStatus status
) {
}
