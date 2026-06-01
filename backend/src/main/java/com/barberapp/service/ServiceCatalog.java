package com.barberapp.service;

import com.barberapp.config.ServiceCatalogProperties;
import com.barberapp.domain.ServiceType;
import com.barberapp.dto.ServiceCatalogItemResponse;
import com.barberapp.exception.UnsupportedServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceCatalog {

    private final ServiceCatalogProperties properties;

    public List<ServiceCatalogItemResponse> getServices() {
        return properties.getServices()
                .entrySet()
                .stream()
                .sorted(Comparator.comparingInt(entry -> entry.getKey().ordinal()))
                .map(entry -> new ServiceCatalogItemResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    public int getDurationMinutes(ServiceType service) {
        Integer durationMinutes = properties.getServices().get(service);
        if (durationMinutes == null) {
            throw new UnsupportedServiceException(service);
        }
        return durationMinutes;
    }
}
