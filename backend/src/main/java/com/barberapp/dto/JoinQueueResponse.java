package com.barberapp.dto;

public record JoinQueueResponse(
        Long queueId,
        int position,
        int estimatedWaitMinutes,
        String estimatedTurnTime
) {
}
