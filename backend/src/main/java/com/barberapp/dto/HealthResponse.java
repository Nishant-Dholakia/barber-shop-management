package com.barberapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record HealthResponse(
        @Schema(example = "UP")
        String status,

        @Schema(example = "barber-app-backend")
        String application,

        @Schema(example = "0.0.1-SNAPSHOT")
        String version,

        @Schema(example = "abc1234")
        String commit,

        LocalDateTime timestamp
) {
}
