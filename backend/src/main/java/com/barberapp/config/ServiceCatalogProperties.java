package com.barberapp.config;

import com.barberapp.domain.ServiceType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.EnumMap;
import java.util.Map;

@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "barber")
public class ServiceCatalogProperties {

    @NotEmpty
    private Map<ServiceType, @Min(1) Integer> services = new EnumMap<>(ServiceType.class);
}
