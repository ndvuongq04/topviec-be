package com.topviec.topviec_be.enums.jobs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum JobPostStatus {
    DRAFT("draft"),
    PENDING_APPROVAL("pending_approval"),
    REJECTED("rejected"),
    SCHEDULED("scheduled"),
    PUBLISHED("published"),
    PAUSED("paused"),
    CLOSED("closed"),
    EXPIRED("expired"),
    RENEWED("renewed"),
    INTERVIEWING("interviewing"),
    COMPLETED("completed"),
    DELETED("deleted");

    private final String value;

    JobPostStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static JobPostStatus fromValue(String value) {
        for (JobPostStatus status : JobPostStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown JobPostStatus: " + value);
    }
}