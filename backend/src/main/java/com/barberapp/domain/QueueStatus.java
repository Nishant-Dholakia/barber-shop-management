package com.barberapp.domain;

import java.util.List;

public enum QueueStatus {
    WAITING,
    IN_PROGRESS,
    COMPLETED,
    NO_SHOW,
    CANCELLED;

    public static List<QueueStatus> activeStatuses() {
        return List.of(WAITING, IN_PROGRESS);
    }

    public static List<QueueStatus> cleanupEligibleStatuses() {
        return List.of(COMPLETED, NO_SHOW, CANCELLED);
    }
}
