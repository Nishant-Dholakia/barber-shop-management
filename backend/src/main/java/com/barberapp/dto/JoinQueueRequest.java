package com.barberapp.dto;

import com.barberapp.domain.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JoinQueueRequest(
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[\\p{L} .'-]+$", message = "must contain only letters, spaces, periods, apostrophes, or hyphens")
        @Schema(example = "Ravi")
        String name,

        @Size(max = 20)
        @Pattern(regexp = "^[0-9+\\- ]*$", message = "must contain only digits, spaces, +, or -")
        @Schema(example = "9876543210")
        String phone,

        @NotNull
        @Schema(example = "HAIRCUT")
        ServiceType service
) {
}
