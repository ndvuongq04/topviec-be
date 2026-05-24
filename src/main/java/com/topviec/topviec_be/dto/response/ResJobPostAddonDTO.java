package com.topviec.topviec_be.dto.response;

import com.topviec.topviec_be.enums.services.JobPostAddonStatus;
import com.topviec.topviec_be.enums.services.ServiceUsageSourceType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ResJobPostAddonDTO {
    private Long id;
    private Long jobPostingId;
    private Long companyAddonId;
    private Long addonServiceId;
    private Long subscriptionUsageId;
    private String serviceCode;
    private ServiceUsageSourceType usageSourceType;
    private String addonName;
    private JobPostAddonStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
}
