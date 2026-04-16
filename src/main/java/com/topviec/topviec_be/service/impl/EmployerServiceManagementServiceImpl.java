package com.topviec.topviec_be.service.impl;

import com.topviec.topviec_be.dto.request.ReqApplyAddonDTO;
import com.topviec.topviec_be.dto.response.ResCompanyAddonDTO;
import com.topviec.topviec_be.dto.response.ResCompanySubscriptionDTO;
import com.topviec.topviec_be.dto.response.ResCompanySubscriptionDTO.ResSubscriptionUsageDTO;
import com.topviec.topviec_be.dto.response.ResJobPostAddonDTO;
import com.topviec.topviec_be.entity.AddonService;
import com.topviec.topviec_be.entity.CompanyAddon;
import com.topviec.topviec_be.entity.CompanySubscription;
import com.topviec.topviec_be.entity.JobPostAddon;
import com.topviec.topviec_be.entity.JobPosting;
import com.topviec.topviec_be.entity.Services;
import com.topviec.topviec_be.entity.ServicePackage;
import com.topviec.topviec_be.entity.SubscriptionUsage;
import com.topviec.topviec_be.enums.services.JobPostAddonStatus;
import com.topviec.topviec_be.enums.services.SubscriptionStatus;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.AddonServiceRepository;
import com.topviec.topviec_be.repository.CompanyAddonRepository;
import com.topviec.topviec_be.repository.CompanySubscriptionRepository;
import com.topviec.topviec_be.repository.JobPostAddonRepository;
import com.topviec.topviec_be.repository.JobPostingRepository;
import com.topviec.topviec_be.repository.ServicePackageRepository;
import com.topviec.topviec_be.repository.ServiceRepository;
import com.topviec.topviec_be.repository.SubscriptionUsageRepository;
import com.topviec.topviec_be.service.CompanyService;
import com.topviec.topviec_be.service.EmployerServiceManagementService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployerServiceManagementServiceImpl implements EmployerServiceManagementService {

        private final CompanyService companyService;
        private final CompanySubscriptionRepository companySubscriptionRepository;
        private final SubscriptionUsageRepository subscriptionUsageRepository;
        private final CompanyAddonRepository companyAddonRepository;
        private final AddonServiceRepository addonServiceRepository;
        private final ServiceRepository serviceRepository;
        private final ServicePackageRepository servicePackageRepository;
        private final JobPostingRepository jobPostingRepository;
        private final JobPostAddonRepository jobPostAddonRepository;

        @Override
        @Transactional(readOnly = true)
        public ResCompanySubscriptionDTO getMySubscription(Long userId) {
                Long companyId = getCompanyId(userId);

                CompanySubscription subscription = companySubscriptionRepository
                                .findFirstByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, SubscriptionStatus.ACTIVE)
                                .orElseThrow(() -> AppException.notFound("Công ty chưa đăng ký gói dịch vụ nào."));

                ServicePackage servicePackage = servicePackageRepository.findById(subscription.getServicePackageId())
                                .orElse(null);

                List<SubscriptionUsage> usages = subscriptionUsageRepository
                                .findByCompanySubscriptionId(subscription.getId());

                List<ResSubscriptionUsageDTO> usageDTOs = usages.stream()
                                .map(u -> {
                                        Services svc = serviceRepository.findByCode(u.getFeatureCode()).orElse(null);
                                        return ResSubscriptionUsageDTO.builder()
                                                        .id(u.getId())
                                                        .featureCode(u.getFeatureCode())
                                                        .featureName(svc != null ? svc.getName() : null)
                                                        .quantityTotal(u.getQuantityTotal())
                                                        .quantityRemaining(u.getQuantityRemaining())
                                                        .resetAt(u.getResetAt())
                                                        .build();
                                })
                                .collect(Collectors.toList());

                return ResCompanySubscriptionDTO.builder()
                                .id(subscription.getId())
                                .servicePackageId(subscription.getServicePackageId())
                                .packageName(servicePackage != null ? servicePackage.getName() : null)
                                .packageCode(servicePackage != null ? servicePackage.getCode() : null)
                                .billingCycle(subscription.getBillingCycle())
                                .status(subscription.getStatus())
                                .startedAt(subscription.getStartedAt())
                                .expiredAt(subscription.getExpiredAt())
                                .createdAt(subscription.getCreatedAt())
                                .usages(usageDTOs)
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public List<ResCompanyAddonDTO> getMyAddons(Long userId) {
                Long companyId = getCompanyId(userId);

                List<CompanyAddon> addons = companyAddonRepository
                                .findByCompanyIdOrderByCreatedAtDesc(companyId);

                return addons.stream().map(addon -> {
                        AddonService addonSvc = addonServiceRepository.findById(addon.getAddonServiceId()).orElse(null);
                        Services svc = addonSvc != null
                                        ? serviceRepository.findById(addonSvc.getServiceId()).orElse(null)
                                        : null;

                        return ResCompanyAddonDTO.builder()
                                        .id(addon.getId())
                                        .addonServiceId(addon.getAddonServiceId())
                                        .addonName(addonSvc != null ? addonSvc.getName() : null)
                                        .addonCode(addonSvc != null ? addonSvc.getCode() : null)
                                        .addonQuantity(addonSvc != null ? addonSvc.getQuantity() : null)
                                        .serviceId(svc != null ? svc.getId() : null)
                                        .serviceCode(svc != null ? svc.getCode() : null)
                                        .serviceName(svc != null ? svc.getName() : null)
                                        .serviceCategory(svc != null ? svc.getCategory() : null)
                                        .serviceCategoryName(svc != null && svc.getCategory() != null
                                                        ? svc.getCategory().getValue()
                                                        : null)
                                        .status(addon.getStatus())
                                        .quantityTotal(addon.getQuantityTotal())
                                        .quantityRemaining(addon.getQuantityRemaining())
                                        .startedAt(addon.getStartedAt())
                                        .expiredAt(addon.getExpiredAt())
                                        .createdAt(addon.getCreatedAt())
                                        .build();
                }).collect(Collectors.toList());
        }

        @Override
        @Transactional
        public ResJobPostAddonDTO applyAddonToJobPost(Long userId, Long jobPostingId, ReqApplyAddonDTO request) {
                Long companyId = getCompanyId(userId);

                JobPosting jobPosting = jobPostingRepository.findByIdAndDeletedAtIsNull(jobPostingId)
                                .orElseThrow(() -> AppException.notFound("Không tìm thấy tin tuyển dụng."));

                if (!jobPosting.getCompanyId().equals(companyId)) {
                        throw AppException.forbidden("Bạn không có quyền thao tác trên tin tuyển dụng này.");
                }

                CompanyAddon companyAddon = companyAddonRepository.findById(request.getCompanyAddonId())
                                .orElseThrow(() -> AppException.notFound("Không tìm thấy dịch vụ lẻ."));

                if (!companyAddon.getCompanyId().equals(companyId)) {
                        throw AppException.forbidden("Dịch vụ lẻ này không thuộc công ty của bạn.");
                }

                if (companyAddon.getStatus() != SubscriptionStatus.ACTIVE) {
                        throw AppException.badRequest("Dịch vụ lẻ này đã hết hiệu lực.");
                }

                if (companyAddon.getExpiredAt() != null && companyAddon.getExpiredAt().isBefore(LocalDateTime.now())) {
                        throw AppException.badRequest("Dịch vụ lẻ này đã hết hạn sử dụng.");
                }

                if (companyAddon.getQuantityRemaining() <= 0) {
                        throw AppException.badRequest("Dịch vụ lẻ này đã hết số lượng sử dụng.");
                }

                AddonService addonService = addonServiceRepository.findById(companyAddon.getAddonServiceId())
                                .orElseThrow(() -> AppException.notFound("Không tìm thấy thông tin dịch vụ lẻ."));

                LocalDateTime now = LocalDateTime.now();
                LocalDateTime expiredAt = addonService.getDurationDays() != null
                                ? now.plusDays(addonService.getDurationDays())
                                : null;

                JobPostAddon jobPostAddon = JobPostAddon.builder()
                                .jobPostingId(jobPostingId)
                                .companyAddonId(companyAddon.getId())
                                .addonServiceId(addonService.getId())
                                .startedAt(now)
                                .expiredAt(expiredAt)
                                .status(JobPostAddonStatus.ACTIVE)
                                .build();

                JobPostAddon saved = jobPostAddonRepository.save(jobPostAddon);

                companyAddon.setQuantityRemaining(companyAddon.getQuantityRemaining() - 1);
                companyAddonRepository.save(companyAddon);

                return ResJobPostAddonDTO.builder()
                                .id(saved.getId())
                                .jobPostingId(saved.getJobPostingId())
                                .companyAddonId(saved.getCompanyAddonId())
                                .addonServiceId(saved.getAddonServiceId())
                                .addonName(addonService.getName())
                                .status(saved.getStatus())
                                .startedAt(saved.getStartedAt())
                                .expiredAt(saved.getExpiredAt())
                                .createdAt(saved.getCreatedAt())
                                .build();
        }

        private Long getCompanyId(Long userId) {
                Long companyId = companyService.getCompanyIdByUserId(userId);
                if (companyId == null) {
                        throw AppException.badRequest("Chưa có hồ sơ công ty.");
                }
                return companyId;
        }
}
