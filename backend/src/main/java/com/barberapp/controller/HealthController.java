package com.barberapp.controller;

import com.barberapp.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Application health checks")
public class HealthController {

    private final Clock clock;
    private final Optional<BuildProperties> buildProperties;
    private final Optional<GitProperties> gitProperties;

    @Value("${spring.application.name}")
    private String applicationName;

    @GetMapping
    @Operation(summary = "Get application health")
    public HealthResponse getHealth() {
        return new HealthResponse(
                "UP",
                applicationName,
                readVersion(),
                readCommit(),
                LocalDateTime.now(clock)
        );
    }

    private String readVersion() {
        return buildProperties.map(BuildProperties::getVersion).orElse("dev");
    }

    private String readCommit() {
        return gitProperties
                .map(properties -> properties.get("commit.id.abbrev"))
                .orElse("unknown");
    }
}
