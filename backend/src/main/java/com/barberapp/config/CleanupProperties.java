package com.barberapp.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "barber.cleanup")
public class CleanupProperties {

    private boolean enabled = true;

    @Min(1)
    private int retentionDays = 2;

    @NotBlank
    private String cron = "0 0 3 * * *";
}
