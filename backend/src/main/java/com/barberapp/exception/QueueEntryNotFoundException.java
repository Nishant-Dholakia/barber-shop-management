package com.barberapp.exception;

public class QueueEntryNotFoundException extends ResourceNotFoundException {

    public QueueEntryNotFoundException(Long queueId) {
        super("Queue entry not found: " + queueId);
    }
}
