package com.topviec.topviec_be.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReqCreateInterviewSlotsDTO {

    @NotEmpty(message = "Danh sách ứng viên không được trống")
    private List<Long> applicationIds;

    @NotNull(message = "Hạn chót chọn lịch không được trống")
    private LocalDateTime deadline;

    @Valid
    @NotEmpty(message = "Cần ít nhất 1 slot thời gian")
    @Size(min = 3, max = 5, message = "Số lượng slot phải từ 3 đến 5")
    private List<SlotDTO> slots;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SlotDTO {
        @NotNull(message = "Thời gian slot không được trống")
        private LocalDateTime proposedAt;

        @NotBlank(message = "Loại phỏng vấn không được trống")
        private String interviewType;

        private String location;

        private String meetingLink;
    }
}
