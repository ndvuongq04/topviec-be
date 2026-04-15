package com.topviec.topviec_be.dto.response;

import com.topviec.topviec_be.enums.services.AddonPackageGroup;
import com.topviec.topviec_be.enums.services.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ResCompanyAddonDTO {
    private Long id;
    private Long addonPackageId;
    private String addonName;
    private String addonCode;
    private AddonPackageGroup groupCode;
    private SubscriptionStatus status;
    private Integer quantityTotal;
    private Integer quantityRemaining;
    private LocalDateTime startedAt;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
}
