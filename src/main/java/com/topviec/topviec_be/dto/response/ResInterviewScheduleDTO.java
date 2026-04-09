package com.topviec.topviec_be.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResInterviewScheduleDTO {

    private Long id;
    private Long applicationId;
    private Long roundId;
    private Integer roundNumber;
    private String roundName;

    // Thông tin UV
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;

    // Thông tin buổi PV
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private String interviewType;
    private String location;
    private String meetingLink;
    private String status;
    private Boolean confirmedByCandidate;
    private Boolean isDefault;
    private String interviewerNote;

    // Trạng thái đơn ứng tuyển (dùng để FE kiểm tra ứng viên đã OFFERED chưa)
    private String applicationStatus;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
