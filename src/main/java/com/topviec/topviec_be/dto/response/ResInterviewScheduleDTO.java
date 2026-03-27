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
    private String interviewerNote;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
