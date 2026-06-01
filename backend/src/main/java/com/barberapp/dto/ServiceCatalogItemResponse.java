package com.barberapp.dto;

import com.barberapp.domain.ServiceType;

public record ServiceCatalogItemResponse(
        ServiceType service,
        int durationMinutes
) {
}
