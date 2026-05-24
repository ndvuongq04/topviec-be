package com.topviec.topviec_be.service.activation;

import com.topviec.topviec_be.dto.internal.ServiceQuotaAllocation;
import com.topviec.topviec_be.dto.response.ResJobPostAddonDTO;
import com.topviec.topviec_be.entity.JobPostAddon;
import com.topviec.topviec_be.entity.JobPosting;
import com.topviec.topviec_be.enums.services.JobPostAddonStatus;
import com.topviec.topviec_be.enums.services.ServiceCategory;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.JobPostAddonRepository;
import com.topviec.topviec_be.repository.JobPostingRepository;
import com.topviec.topviec_be.service.CompanyServiceQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobPostingActivationService {

    public static final ServiceCategory CATEGORY = ServiceCategory.JOB_POSTING;

    public static final String CODE_HOT = "JOB_POSTING_HOT";
    public static final String CODE_URGENT = "JOB_POSTING_URGENT";
    public static final String CODE_REFRESH = "JOB_POSTING_REFRESH";

    private final JobPostingRepository jobPostingRepository;
    private final JobPostAddonRepository jobPostAddonRepository;
    private final CompanyServiceQuotaService quotaService;

    @Transactional
    public ResJobPostAddonDTO activate(String serviceCode, JobPosting jobPosting, ServiceQuotaAllocation quota) {
        log.info("[JobPostingActivationService] Activate service {} for job #{}", serviceCode, jobPosting.getId());

        return switch (serviceCode) {
            case CODE_HOT -> applyHotService(jobPosting, quota);
            case CODE_URGENT -> applyUrgentService(jobPosting, quota);
            case CODE_REFRESH -> applyRefreshService(jobPosting, quota);
            default -> throw AppException
                    .badRequest("Ma dich vu nhom tuyen dung khong hop le hoac chua duoc ho tro: " + serviceCode);
        };
    }

    public ResJobPostAddonDTO applyGenericAddon(Long jobPostingId, ServiceQuotaAllocation quota) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = quota.getDurationDays() != null
                ? now.plusDays(quota.getDurationDays())
                : null;

        return createJobPostAddonRecord(jobPostingId, quota, now, expiredAt);
    }

    private ResJobPostAddonDTO applyHotService(JobPosting jobPosting, ServiceQuotaAllocation quota) {
        LocalDateTime now = LocalDateTime.now();

        long activeHotCountForJob = jobPostAddonRepository.countActiveAddonForJob(jobPosting.getId(), CODE_HOT, now);
        if (activeHotCountForJob > 0) {
            throw AppException.badRequest("Tin tuyen dung nay dang o trang thai HOT. Khong can ap dung them.");
        }

        int durationDays = quota.getDurationDays() != null ? quota.getDurationDays() : 30;
        LocalDateTime hotExpiredAt = now.plusDays(durationDays);

        jobPosting.setIsHot(true);
        jobPostingRepository.save(jobPosting);

        return createJobPostAddonRecord(jobPosting.getId(), quota, now, hotExpiredAt);
    }

    private ResJobPostAddonDTO applyUrgentService(JobPosting jobPosting, ServiceQuotaAllocation quota) {
        LocalDateTime now = LocalDateTime.now();

        long activeUrgentCountForJob = jobPostAddonRepository.countActiveAddonForJob(jobPosting.getId(), CODE_URGENT, now);
        if (activeUrgentCountForJob > 0) {
            throw AppException.badRequest("Tin tuyen dung nay dang o trang thai TUYEN GAP. Khong can ap dung them.");
        }

        int durationDays = quota.getDurationDays() != null ? quota.getDurationDays() : 14;
        LocalDateTime urgentExpiredAt = now.plusDays(durationDays);

        jobPosting.setIsUrgent(true);
        jobPostingRepository.save(jobPosting);

        return createJobPostAddonRecord(jobPosting.getId(), quota, now, urgentExpiredAt);
    }

    private ResJobPostAddonDTO applyRefreshService(JobPosting jobPosting, ServiceQuotaAllocation quota) {
        throw AppException.badRequest("Tinh nang ap dung Dich vu Lam Moi Tin dang duoc xay dung.");
    }

    private ResJobPostAddonDTO createJobPostAddonRecord(
            Long jobPostingId, ServiceQuotaAllocation quota, LocalDateTime startedAt, LocalDateTime expiredAt) {
        JobPostAddon jobPostAddon = JobPostAddon.builder()
                .jobPostingId(jobPostingId)
                .companyAddonId(quota.getCompanyAddonId())
                .addonServiceId(quota.getAddonServiceId())
                .subscriptionUsageId(quota.getSubscriptionUsageId())
                .serviceCode(quota.getServiceCode())
                .usageSourceType(quota.getSourceType())
                .startedAt(startedAt)
                .expiredAt(expiredAt)
                .status(JobPostAddonStatus.ACTIVE)
                .build();
        JobPostAddon saved = jobPostAddonRepository.save(jobPostAddon);

        quotaService.consume(quota);

        return ResJobPostAddonDTO.builder()
                .id(saved.getId())
                .jobPostingId(saved.getJobPostingId())
                .companyAddonId(saved.getCompanyAddonId())
                .addonServiceId(saved.getAddonServiceId())
                .subscriptionUsageId(saved.getSubscriptionUsageId())
                .serviceCode(saved.getServiceCode())
                .usageSourceType(saved.getUsageSourceType())
                .addonName(quota.getDisplayName())
                .status(saved.getStatus())
                .startedAt(saved.getStartedAt())
                .expiredAt(saved.getExpiredAt())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
