package com.barberapp.dto;

import com.barberapp.domain.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WalkInRequest(
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[\\p{L} .'-]+$", message = "must contain only letters, spaces, periods, apostrophes, or hyphens")
        @Schema(example = "Walkin Customer")
        String name,

        @NotNull
        @Schema(example = "BEARD")
        ServiceType service
) {
}
