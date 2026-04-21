package com.topviec.topviec_be.service.activation;

import com.topviec.topviec_be.dto.response.ResCompanyBrandingDTO;
import com.topviec.topviec_be.entity.AddonService;
import com.topviec.topviec_be.entity.Company;
import com.topviec.topviec_be.entity.CompanyAddon;
import com.topviec.topviec_be.entity.CompanyBranding;
import com.topviec.topviec_be.enums.services.BrandingAddonStatus;
import com.topviec.topviec_be.enums.services.ServiceCategory;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.CompanyAddonRepository;
import com.topviec.topviec_be.repository.CompanyBrandingRepository;
import com.topviec.topviec_be.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrandingActivationService {

    public static final ServiceCategory CATEGORY = ServiceCategory.BRANDING;
    public static final String CODE_BANNER_HOME = "BRANDING_BANNER_HOME";

    private final CompanyRepository companyRepository;
    private final CompanyBrandingRepository companyBrandingRepository;
    private final CompanyAddonRepository companyAddonRepository;

    @Transactional
    public ResCompanyBrandingDTO applyBannerHomeService(Long companyId, CompanyAddon companyAddon,
            AddonService addonService) {
        LocalDateTime now = LocalDateTime.now();

        log.info("[BrandingActivationService] Kích hoạt {} cho công ty #{}", CODE_BANNER_HOME, companyId);

        // 1. Kiểm tra công ty đang có banner active chưa
        long active = companyBrandingRepository.countActiveForCompany(companyId, CODE_BANNER_HOME, now);
        if (active > 0) {
            throw AppException.badRequest("Công ty đang có Banner trang chủ đang hoạt động. Không cần áp dụng thêm.");
        }

        // 2. Tính thời hạn (mặc định 30 ngày)
        int durationDays = addonService.getDurationDays() != null ? addonService.getDurationDays() : 30;
        LocalDateTime expiredAt = now.plusDays(durationDays);

        // 3. Bật cờ isBanner trên Company
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy công ty."));
        company.setIsBanner(true);
        companyRepository.save(company);

        // 4. Tạo record CompanyBranding để track expiry
        CompanyBranding branding = CompanyBranding.builder()
                .companyId(companyId)
                .companyAddonId(companyAddon.getId())
                .addonServiceId(addonService.getId())
                .status(BrandingAddonStatus.ACTIVE)
                .startedAt(now)
                .expiredAt(expiredAt)
                .build();
        CompanyBranding saved = companyBrandingRepository.save(branding);

        // 5. Trừ quota CompanyAddon
        companyAddon.setQuantityRemaining(companyAddon.getQuantityRemaining() - 1);
        companyAddonRepository.save(companyAddon);

        return ResCompanyBrandingDTO.builder()
                .id(saved.getId())
                .companyId(saved.getCompanyId())
                .companyAddonId(saved.getCompanyAddonId())
                .addonServiceId(saved.getAddonServiceId())
                .addonName(addonService.getName())
                .serviceCode(CODE_BANNER_HOME)
                .status(saved.getStatus())
                .startedAt(saved.getStartedAt())
                .expiredAt(saved.getExpiredAt())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
