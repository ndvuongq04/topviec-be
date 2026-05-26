package com.topviec.topviec_be.dto.internal;

import com.topviec.topviec_be.entity.AddonService;
import com.topviec.topviec_be.entity.CompanyAddon;
import com.topviec.topviec_be.entity.Services;
import com.topviec.topviec_be.entity.SubscriptionUsage;
import com.topviec.topviec_be.enums.services.ServiceUsageSourceType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ServiceQuotaAllocation {

    private Long companyId;
    private String serviceCode;
    private Services service;
    private ServiceUsageSourceType sourceType;
    private SubscriptionUsage subscriptionUsage;
    private CompanyAddon companyAddon;
    private AddonService addonService;
    private Integer durationDays;
    private LocalDateTime quotaExpiredAt;

    public Long getSubscriptionUsageId() {
        return subscriptionUsage != null ? subscriptionUsage.getId() : null;
    }

    public Long getCompanyAddonId() {
        return companyAddon != null ? companyAddon.getId() : null;
    }

    public Long getAddonServiceId() {
        return addonService != null ? addonService.getId() : null;
    }

    public String getDisplayName() {
        if (addonService != null && addonService.getName() != null) {
            return addonService.getName();
        }
        return service != null ? service.getName() : serviceCode;
    }
}
