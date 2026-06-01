package com.barberapp.exception;

public class InvalidQueueStateException extends BusinessRuleException {

    public InvalidQueueStateException(String message) {
        super(message);
    }
}
