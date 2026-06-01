package com.barberapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI barberQueueOpenApi() {
        return new OpenAPI()
                .servers(List.of(new Server().url("/").description("Current server")))
                .info(new Info()
                        .title("Barber Queue API")
                        .version("v1")
                        .description("Backend API for a single-shop barber queue management system."));
    }
}
