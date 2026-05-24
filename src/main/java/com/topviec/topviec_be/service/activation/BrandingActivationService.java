package com.topviec.topviec_be.service.activation;

import com.topviec.topviec_be.dto.internal.ServiceQuotaAllocation;
import com.topviec.topviec_be.dto.response.ResCompanyBrandingDTO;
import com.topviec.topviec_be.entity.Company;
import com.topviec.topviec_be.entity.CompanyBranding;
import com.topviec.topviec_be.enums.services.BrandingAddonStatus;
import com.topviec.topviec_be.enums.services.ServiceCategory;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.CompanyBrandingRepository;
import com.topviec.topviec_be.repository.CompanyRepository;
import com.topviec.topviec_be.service.CompanyServiceQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrandingActivationService {

    public static final ServiceCategory CATEGORY = ServiceCategory.BRANDING;
    public static final String CODE_BANNER_HOME = "BRANDING_BANNER_HOME";
    public static final String CODE_TOP_EMPLOYER = "BRANDING_TOP_EMPLOYER";
    public static final String CODE_VERIFIED = "BRANDING_VERIFIED";

    private final CompanyRepository companyRepository;
    private final CompanyBrandingRepository companyBrandingRepository;
    private final CompanyServiceQuotaService quotaService;

    @Transactional
    public ResCompanyBrandingDTO activate(String serviceCode, Long companyId, ServiceQuotaAllocation quota) {
        return switch (serviceCode) {
            case CODE_BANNER_HOME -> applyBrandingService(
                    companyId, quota, CODE_BANNER_HOME,
                    "Cong ty dang co Banner trang chu dang hoat dong. Khong can ap dung them.",
                    30, company -> company.setIsBanner(true));
            case CODE_TOP_EMPLOYER -> applyBrandingService(
                    companyId, quota, CODE_TOP_EMPLOYER,
                    "Cong ty dang co nhan Top Employer dang hoat dong. Khong can ap dung them.",
                    30, company -> company.setIsTopEmployer(true));
            case CODE_VERIFIED -> applyBrandingService(
                    companyId, quota, CODE_VERIFIED,
                    "Cong ty dang co nhan Doanh Nghiep Xac Thuc dang hoat dong. Khong can ap dung them.",
                    365, company -> company.setIsBrandVerified(true));
            default -> throw AppException.badRequest(
                    "Ma dich vu BRANDING khong hop le hoac chua duoc ho tro: " + serviceCode);
        };
    }

    public static boolean isSupported(String serviceCode) {
        return CODE_BANNER_HOME.equals(serviceCode)
                || CODE_TOP_EMPLOYER.equals(serviceCode)
                || CODE_VERIFIED.equals(serviceCode);
    }

    private ResCompanyBrandingDTO applyBrandingService(Long companyId, ServiceQuotaAllocation quota,
            String serviceCode, String duplicateMessage, int defaultDurationDays, Consumer<Company> flagSetter) {

        LocalDateTime now = LocalDateTime.now();
        log.info("[BrandingActivationService] Activate {} for company #{}", serviceCode, companyId);

        long active = companyBrandingRepository.countActiveForCompany(companyId, serviceCode, now);
        if (active > 0) {
            throw AppException.badRequest(duplicateMessage);
        }

        int durationDays = quota.getDurationDays() != null ? quota.getDurationDays() : defaultDurationDays;
        LocalDateTime expiredAt = now.plusDays(durationDays);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> AppException.notFound("Khong tim thay cong ty."));
        flagSetter.accept(company);
        companyRepository.save(company);

        CompanyBranding branding = CompanyBranding.builder()
                .companyId(companyId)
                .companyAddonId(quota.getCompanyAddonId())
                .addonServiceId(quota.getAddonServiceId())
                .subscriptionUsageId(quota.getSubscriptionUsageId())
                .serviceCode(serviceCode)
                .usageSourceType(quota.getSourceType())
                .status(BrandingAddonStatus.ACTIVE)
                .startedAt(now)
                .expiredAt(expiredAt)
                .build();
        CompanyBranding saved = companyBrandingRepository.save(branding);

        quotaService.consume(quota);

        return ResCompanyBrandingDTO.builder()
                .id(saved.getId())
                .companyId(saved.getCompanyId())
                .companyAddonId(saved.getCompanyAddonId())
                .addonServiceId(saved.getAddonServiceId())
                .subscriptionUsageId(saved.getSubscriptionUsageId())
                .addonName(quota.getDisplayName())
                .serviceCode(serviceCode)
                .usageSourceType(saved.getUsageSourceType())
                .status(saved.getStatus())
                .startedAt(saved.getStartedAt())
                .expiredAt(saved.getExpiredAt())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
