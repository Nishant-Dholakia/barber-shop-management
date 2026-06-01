package com.barberapp.dto;

import com.barberapp.domain.QueueStatus;
import com.barberapp.domain.ServiceType;

public record BarberQueueItemResponse(
        Long id,
        String name,
        ServiceType service,
        QueueStatus status
) {
}
