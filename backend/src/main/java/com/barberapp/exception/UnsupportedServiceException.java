package com.barberapp.exception;

import com.barberapp.domain.ServiceType;

public class UnsupportedServiceException extends BusinessRuleException {

    public UnsupportedServiceException(ServiceType service) {
        super("Unsupported service: " + service);
    }
}
