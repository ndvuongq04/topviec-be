package com.topviec.topviec_be.service.impl;

import com.topviec.topviec_be.dto.request.ReqApplyAddonDTO;
import com.topviec.topviec_be.dto.response.ResCompanyAddonDTO;
import com.topviec.topviec_be.dto.response.ResCompanySubscriptionDTO;
import com.topviec.topviec_be.dto.response.ResCompanySubscriptionDTO.ResSubscriptionUsageDTO;
import com.topviec.topviec_be.dto.response.ResJobPostAddonDTO;
import com.topviec.topviec_be.entity.AddonPackage;
import com.topviec.topviec_be.entity.CompanyAddon;
import com.topviec.topviec_be.entity.CompanySubscription;
import com.topviec.topviec_be.entity.JobPostAddon;
import com.topviec.topviec_be.entity.JobPosting;
import com.topviec.topviec_be.entity.ServicePackage;
import com.topviec.topviec_be.entity.SubscriptionUsage;
import com.topviec.topviec_be.enums.services.JobPostAddonStatus;
import com.topviec.topviec_be.enums.services.SubscriptionStatus;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.AddonPackageRepository;
import com.topviec.topviec_be.repository.CompanyAddonRepository;
import com.topviec.topviec_be.repository.CompanySubscriptionRepository;
import com.topviec.topviec_be.repository.JobPostAddonRepository;
import com.topviec.topviec_be.repository.JobPostingRepository;
import com.topviec.topviec_be.repository.ServicePackageRepository;
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
    private final AddonPackageRepository addonPackageRepository;
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

        // Lấy thông tin gói dịch vụ
        ServicePackage servicePackage = servicePackageRepository.findById(subscription.getServicePackageId())
                .orElse(null);

        // Lấy hạn mức sử dụng
        List<SubscriptionUsage> usages = subscriptionUsageRepository
                .findByCompanySubscriptionId(subscription.getId());

        List<ResSubscriptionUsageDTO> usageDTOs = usages.stream()
                .map(u -> ResSubscriptionUsageDTO.builder()
                        .id(u.getId())
                        .featureCode(u.getFeatureCode())
                        .quantityTotal(u.getQuantityTotal())
                        .quantityRemaining(u.getQuantityRemaining())
                        .resetAt(u.getResetAt())
                        .build())
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
            AddonPackage pkg = addonPackageRepository.findById(addon.getAddonPackageId()).orElse(null);

            return ResCompanyAddonDTO.builder()
                    .id(addon.getId())
                    .addonPackageId(addon.getAddonPackageId())
                    .addonName(pkg != null ? pkg.getName() : null)
                    .addonCode(pkg != null ? pkg.getCode() : null)
                    .groupCode(pkg != null ? pkg.getGroupCode() : null)
                    .groupName(pkg != null ? pkg.getGroupName() : null)
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

        // Kiểm tra tin tuyển dụng tồn tại và thuộc về công ty
        JobPosting jobPosting = jobPostingRepository.findByIdAndDeletedAtIsNull(jobPostingId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy tin tuyển dụng."));

        if (!jobPosting.getCompanyId().equals(companyId)) {
            throw AppException.forbidden("Bạn không có quyền thao tác trên tin tuyển dụng này.");
        }

        // Kiểm tra dịch vụ lẻ tồn tại và thuộc về công ty
        CompanyAddon companyAddon = companyAddonRepository.findById(request.getCompanyAddonId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy dịch vụ lẻ."));

        if (!companyAddon.getCompanyId().equals(companyId)) {
            throw AppException.forbidden("Dịch vụ lẻ này không thuộc công ty của bạn.");
        }

        // Kiểm tra dịch vụ lẻ còn hiệu lực
        if (companyAddon.getStatus() != SubscriptionStatus.ACTIVE) {
            throw AppException.badRequest("Dịch vụ lẻ này đã hết hiệu lực.");
        }

        // Kiểm tra hạn sử dụng
        if (companyAddon.getExpiredAt() != null && companyAddon.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw AppException.badRequest("Dịch vụ lẻ này đã hết hạn sử dụng.");
        }

        // Action 1: Kiểm tra quantity_remaining
        if (companyAddon.getQuantityRemaining() <= 0) {
            throw AppException.badRequest("Dịch vụ lẻ này đã hết số lượng sử dụng.");
        }

        // Lấy thông tin gói addon để tính thời hạn
        AddonPackage addonPackage = addonPackageRepository.findById(companyAddon.getAddonPackageId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy gói dịch vụ lẻ."));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = addonPackage.getDurationDays() != null
                ? now.plusDays(addonPackage.getDurationDays())
                : null;

        // Action 2: Lưu vào bảng job_post_addons
        JobPostAddon jobPostAddon = JobPostAddon.builder()
                .jobPostingId(jobPostingId)
                .companyAddonId(companyAddon.getId())
                .addonPackageId(addonPackage.getId())
                .startedAt(now)
                .expiredAt(expiredAt)
                .status(JobPostAddonStatus.ACTIVE)
                .build();

        JobPostAddon saved = jobPostAddonRepository.save(jobPostAddon);

        // Action 3: Trừ quantity_remaining đi 1
        companyAddon.setQuantityRemaining(companyAddon.getQuantityRemaining() - 1);
        companyAddonRepository.save(companyAddon);

        return ResJobPostAddonDTO.builder()
                .id(saved.getId())
                .jobPostingId(saved.getJobPostingId())
                .companyAddonId(saved.getCompanyAddonId())
                .addonPackageId(saved.getAddonPackageId())
                .addonName(addonPackage.getName())
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
