package com.topviec.topviec_be.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResJobPostingDetail {

    private Long id;
    private String title;
    private String slug;
    private String description;
    private String requirements;
    private String benefits;

    private Long companyId;
    private Long industryId;
    private Long levelId;

    private Integer experienceYearsMin;
    private Integer experienceYearsMax;

    private Long salaryMin;
    private Long salaryMax;
    private Boolean salaryNegotiable;

    private String workType;
    private Integer headcount;
    private LocalDateTime deadline;
    private String status;

    private Boolean isFeatured;
    private Boolean isUrgent;

    private Integer viewCount;
    private Integer editCount;

    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<ResJobPostLocationDTO> locations;
    private List<ResJobPostSkillDTO> skills;
}