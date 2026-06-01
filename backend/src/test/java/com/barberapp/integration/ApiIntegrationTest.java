package com.barberapp.integration;

import com.barberapp.repository.QueueEntryRepository;
import com.barberapp.repository.RevenueEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private RevenueEntryRepository revenueEntryRepository;

    @BeforeEach
    void setUp() {
        revenueEntryRepository.deleteAll();
        queueEntryRepository.deleteAll();
    }

    @Test
    void customerAndBarberQueueFlowWorksThroughApi() throws Exception {
        mockMvc.perform(post("/api/queue/join")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Ravi",
                                  "phone": "9876543210",
                                  "service": "HAIRCUT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueId").isNumber())
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.estimatedWaitMinutes").value(0))
                .andExpect(jsonPath("$.estimatedTurnTime").isString());

        mockMvc.perform(post("/api/barber/walkin")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Walkin Customer",
                                  "service": "BEARD"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Customer added"));

        mockMvc.perform(get("/api/barber/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Ravi"))
                .andExpect(jsonPath("$[1].name").value("Walkin Customer"));

        Long inProgressId = queueEntryRepository.findAll()
                .stream()
                .filter(entry -> entry.getName().equals("Ravi"))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/barber/complete/{id}", inProgressId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "amount": 150
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Customer completed"));

        mockMvc.perform(get("/api/barber/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayRevenue").value(150.0))
                .andExpect(jsonPath("$.customersServed").value(1))
                .andExpect(jsonPath("$.queueLength").value(0));
    }

    @Test
    void validationErrorsUseStandardErrorResponse() throws Exception {
        mockMvc.perform(post("/api/queue/join")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "",
                                  "phone": "abc",
                                  "service": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("barber-app-backend"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void exportsOpenApiJson() throws Exception {
        String openApiJson = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$['paths']['/api/queue/join']").exists())
                .andExpect(jsonPath("$['paths']['/api/barber/dashboard']").exists())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        Path outputPath = Path.of("target", "openapi.json");
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, openApiJson, StandardCharsets.UTF_8);
    }
}
