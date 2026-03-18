package com.topviec.topviec_be.enums.jobs;

public enum JobPostStatus {
    DRAFT("draft"),
    PENDING_APPROVAL("pending_approval"),
    REJECTED("rejected"),
    PUBLISHED("published"),
    EDITING("editing"),
    PAUSED("paused"),
    CLOSED("closed"),
    EXPIRED("expired"),
    COMPLETED("completed"),
    INTERVIEWING("interviewing");

    private final String value;

    JobPostStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static JobPostStatus fromValue(String value) {
        for (JobPostStatus status : JobPostStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown JobPostStatus: " + value);
    }
}