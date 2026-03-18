package com.topviec.topviec_be.enums.jobs;

public enum WorkType {
    FULL_TIME("full_time"),
    PART_TIME("part_time"),
    REMOTE("remote"),
    HYBRID("hybrid"),
    FREELANCE("freelance");

    private final String value;

    WorkType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static WorkType fromValue(String value) {
        for (WorkType type : WorkType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown WorkType: " + value);
    }
}