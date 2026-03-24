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
    private CompanyDTO company;
    private IndustryDTO industry;
    private LevelDTO level;
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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompanyDTO {
        private Long id;
        private String name;
        private String logoUrl;
        private String address;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IndustryDTO {
        private Long id;
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LevelDTO {
        private Long id;
        private String name;
    }
}