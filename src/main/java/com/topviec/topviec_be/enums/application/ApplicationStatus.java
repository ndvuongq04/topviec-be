package com.topviec.topviec_be.enums.application;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ApplicationStatus {

    PENDING("pending"), // Vừa nộp, NTD chưa xem
    INVITED("invited"), // NTD mời từ talent pool
    SEEN("seen"), // NTD đã mở xem CV
    CONSIDERING("considering"), // NTD lưu để xem lại sau
    INTERVIEWING("interviewing"), // Đang phỏng vấn
    OFFERED("offered"), // NTD gửi offer
    HIRED("hired"), // UV chấp nhận offer
    REJECTED("rejected"), // NTD từ chối
    WITHDRAWN("withdrawn"), // UV tự rút đơn
    EXPIRED("expired"); // Job đóng/hết hạn

    private final String value;

    ApplicationStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ApplicationStatus fromValue(String value) {
        for (ApplicationStatus status : ApplicationStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ApplicationStatus: " + value);
    }

    // Kiểm tra trạng thái kết thúc (terminal state)
    public boolean isTerminal() {
        return this == HIRED || this == REJECTED || this == WITHDRAWN || this == EXPIRED;
    }

    // Kiểm tra UV có thể rút đơn không
    public boolean isWithdrawable() {
        return this == PENDING || this == SEEN;
    }

    // Kiểm tra chuyển trạng thái hợp lệ
    public boolean canTransitionTo(ApplicationStatus next) {
        return switch (this) {
            case PENDING -> next == SEEN || next == WITHDRAWN || next == EXPIRED;
            case INVITED -> next == PENDING || next == EXPIRED;
            case SEEN -> next == CONSIDERING || next == INTERVIEWING || next == REJECTED || next == EXPIRED || next == WITHDRAWN;
            case CONSIDERING -> next == INTERVIEWING || next == REJECTED || next == EXPIRED || next == WITHDRAWN;
            case INTERVIEWING -> next == OFFERED || next == REJECTED;
            case OFFERED -> next == HIRED || next == REJECTED;
            default -> false; // terminal states
        };
    }
}