package com.barberapp.controller;

import com.barberapp.dto.ServiceCatalogItemResponse;
import com.barberapp.service.ServiceCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/services")
@Tag(name = "Services", description = "Configured barber service catalog")
public class ServiceController {

    private final ServiceCatalog serviceCatalog;

    @GetMapping
    @Operation(summary = "List configured barber services")
    public List<ServiceCatalogItemResponse> getServices() {
        return serviceCatalog.getServices();
    }
}
