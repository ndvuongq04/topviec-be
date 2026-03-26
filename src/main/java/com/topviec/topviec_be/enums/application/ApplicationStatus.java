package com.topviec.topviec_be.enums.application;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ApplicationStatus {

    PENDING("pending"), // Vừa nộp, NTD chưa xem
    INVITED("invited"), // NTD mời từ talent pool
    SEEN("seen"), // NTD đã mở xem CV
    CONSIDERING("considering"), // NTD lưu để xem lại
    CV_PASSED("cv_passed"), // Pass vòng CV, chờ setup lịch PV
    INTERVIEWING("interviewing"), // Đang phỏng vấn (auto khi NTT bắt đầu PV)
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

    public boolean isTerminal() {
        return this == HIRED || this == REJECTED || this == WITHDRAWN || this == EXPIRED;
    }

    public boolean isWithdrawable() {
        return this == PENDING || this == SEEN || this == CV_PASSED;
    }

    public boolean canTransitionTo(ApplicationStatus next) {
        return switch (this) {
            case PENDING -> next == SEEN || next == WITHDRAWN || next == EXPIRED;
            case INVITED -> next == PENDING || next == EXPIRED;
            case SEEN -> next == CONSIDERING || next == CV_PASSED
                    || next == REJECTED || next == EXPIRED || next == WITHDRAWN;
            case CONSIDERING -> next == CV_PASSED || next == REJECTED
                    || next == EXPIRED || next == WITHDRAWN;
            case CV_PASSED -> next == INTERVIEWING || next == REJECTED
                    || next == WITHDRAWN || next == EXPIRED;
            case INTERVIEWING -> next == OFFERED || next == REJECTED;
            case OFFERED -> next == HIRED || next == REJECTED;
            default -> false;
        };
    }
}