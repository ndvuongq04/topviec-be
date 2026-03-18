package com.topviec.topviec_be.enums.jobs;

public enum EditType {
    DRAFT_EDIT("draft_edit"),
    POST_PUBLISH_EDIT("post_publish_edit"),
    ADMIN_EDIT("admin_edit");

    private final String value;

    EditType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static EditType fromValue(String value) {
        for (EditType type : EditType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown EditType: " + value);
    }
}