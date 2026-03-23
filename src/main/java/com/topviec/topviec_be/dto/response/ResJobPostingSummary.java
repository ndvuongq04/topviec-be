package com.topviec.topviec_be.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResJobPostingSummary {

    private Long id;
    private String title;
    private String slug;
    private Long companyId;
    private Long industryId;
    private Long levelId;
    private String workType;
    private String status;
    private Long salaryMin;
    private Long salaryMax;
    private Boolean salaryNegotiable;
    private Boolean isFeatured;
    private Boolean isUrgent;
    private Integer viewCount;
    private LocalDateTime deadline;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
}