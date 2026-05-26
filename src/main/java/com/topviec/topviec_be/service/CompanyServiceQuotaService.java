package com.topviec.topviec_be.service;

import com.topviec.topviec_be.dto.internal.ServiceQuotaAllocation;
import com.topviec.topviec_be.entity.AddonService;
import com.topviec.topviec_be.entity.CompanyAddon;
import com.topviec.topviec_be.entity.CompanySubscription;
import com.topviec.topviec_be.entity.ServicePackageDetail;
import com.topviec.topviec_be.entity.Services;
import com.topviec.topviec_be.entity.SubscriptionUsage;
import com.topviec.topviec_be.enums.services.ServiceUsageSourceType;
import com.topviec.topviec_be.enums.services.SubscriptionStatus;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.CompanyAddonRepository;
import com.topviec.topviec_be.repository.ServicePackageDetailRepository;
import com.topviec.topviec_be.repository.ServiceRepository;
import com.topviec.topviec_be.repository.SubscriptionUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyServiceQuotaService {

    private static final PageRequest FIRST_RESULT = PageRequest.of(0, 1);

    private final SubscriptionUsageRepository subscriptionUsageRepository;
    private final CompanyAddonRepository companyAddonRepository;
    private final ServiceRepository serviceRepository;
    private final ServicePackageDetailRepository servicePackageDetailRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public ServiceQuotaAllocation findAvailableQuotaForUpdate(Long companyId, String rawServiceCode) {
        String serviceCode = normalizeServiceCode(rawServiceCode);
        Services service = getActiveService(serviceCode);
        LocalDateTime now = LocalDateTime.now();

        SubscriptionUsage subscriptionUsage = first(subscriptionUsageRepository.findAvailableForUpdate(
                companyId, serviceCode, SubscriptionStatus.ACTIVE, now, FIRST_RESULT));
        CompanyAddon companyAddon = first(companyAddonRepository.findAvailableByServiceCodeForUpdate(
                companyId, serviceCode, SubscriptionStatus.ACTIVE, now, FIRST_RESULT));

        if (subscriptionUsage == null && companyAddon == null) {
            throw AppException.forbidden("Cong ty chua co dich vu " + serviceCode + " hoac da het so luong su dung.");
        }

        if (shouldUseSubscription(subscriptionUsage, companyAddon)) {
            return buildSubscriptionAllocation(companyId, serviceCode, service, subscriptionUsage);
        }
        return buildAddonAllocation(companyId, serviceCode, service, companyAddon);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ServiceQuotaAllocation findAddonQuotaForUpdate(Long companyId, Long companyAddonId) {
        CompanyAddon companyAddon = companyAddonRepository.findByIdForUpdate(companyAddonId)
                .orElseThrow(() -> AppException.notFound("Khong tim thay dich vu le."));

        if (!companyAddon.getCompanyId().equals(companyId)) {
            throw AppException.forbidden("Dich vu le nay khong thuoc cong ty cua ban.");
        }
        validateAvailableAddon(companyAddon);

        AddonService addonService = companyAddon.getAddonService();
        Services service = addonService != null ? addonService.getService() : null;
        if (service == null) {
            throw AppException.notFound("Khong tim thay dich vu goc cua dich vu le.");
        }
        validateActiveService(service);

        return buildAddonAllocation(companyId, service.getCode(), service, companyAddon);
    }

    @Transactional(readOnly = true)
    public boolean hasAvailableQuota(Long companyId, String rawServiceCode) {
        String serviceCode = normalizeServiceCode(rawServiceCode);
        getActiveService(serviceCode);
        LocalDateTime now = LocalDateTime.now();

        return subscriptionUsageRepository.countAvailable(
                companyId, serviceCode, SubscriptionStatus.ACTIVE, now) > 0
                || companyAddonRepository.countAvailableByServiceCode(
                        companyId, serviceCode, SubscriptionStatus.ACTIVE, now) > 0;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void consume(ServiceQuotaAllocation allocation) {
        if (allocation.getSourceType() == ServiceUsageSourceType.SUBSCRIPTION) {
            SubscriptionUsage usage = allocation.getSubscriptionUsage();
            if (usage == null || usage.getQuantityRemaining() == null || usage.getQuantityRemaining() <= 0) {
                throw AppException.forbidden("Dich vu trong goi da het so luong su dung.");
            }
            usage.setQuantityRemaining(usage.getQuantityRemaining() - 1);
            subscriptionUsageRepository.save(usage);
            return;
        }

        CompanyAddon addon = allocation.getCompanyAddon();
        if (addon == null || addon.getQuantityRemaining() == null || addon.getQuantityRemaining() <= 0) {
            throw AppException.forbidden("Dich vu le da het so luong su dung.");
        }
        addon.setQuantityRemaining(addon.getQuantityRemaining() - 1);
        companyAddonRepository.save(addon);
    }

    private ServiceQuotaAllocation buildSubscriptionAllocation(
            Long companyId, String serviceCode, Services service, SubscriptionUsage usage) {
        CompanySubscription subscription = usage.getCompanySubscription();
        Integer durationDays = null;
        LocalDateTime expiredAt = null;

        if (subscription != null) {
            expiredAt = subscription.getExpiredAt();
            durationDays = servicePackageDetailRepository
                    .findByServicePackageIdAndServiceId(subscription.getServicePackageId(), service.getId())
                    .map(ServicePackageDetail::getDurationDays)
                    .orElse(null);
        }

        return ServiceQuotaAllocation.builder()
                .companyId(companyId)
                .serviceCode(serviceCode)
                .service(service)
                .sourceType(ServiceUsageSourceType.SUBSCRIPTION)
                .subscriptionUsage(usage)
                .durationDays(durationDays)
                .quotaExpiredAt(expiredAt)
                .build();
    }

    private ServiceQuotaAllocation buildAddonAllocation(
            Long companyId, String serviceCode, Services service, CompanyAddon addon) {
        AddonService addonService = addon.getAddonService();

        return ServiceQuotaAllocation.builder()
                .companyId(companyId)
                .serviceCode(serviceCode)
                .service(service)
                .sourceType(ServiceUsageSourceType.ADDON)
                .companyAddon(addon)
                .addonService(addonService)
                .durationDays(addonService != null ? addonService.getDurationDays() : null)
                .quotaExpiredAt(addon.getExpiredAt())
                .build();
    }

    private Services getActiveService(String serviceCode) {
        Services service = serviceRepository.findByCode(serviceCode)
                .orElseThrow(() -> AppException.notFound("Khong tim thay dich vu: " + serviceCode));
        validateActiveService(service);
        return service;
    }

    private void validateActiveService(Services service) {
        if (service.getIsActive() != null && !service.getIsActive()) {
            throw AppException.badRequest("Dich vu nay khong con hoat dong.");
        }
    }

    private void validateAvailableAddon(CompanyAddon addon) {
        if (addon.getStatus() != SubscriptionStatus.ACTIVE) {
            throw AppException.badRequest("Dich vu le nay da het hieu luc.");
        }
        if (addon.getExpiredAt() != null && !addon.getExpiredAt().isAfter(LocalDateTime.now())) {
            throw AppException.badRequest("Dich vu le nay da het han su dung.");
        }
        if (addon.getQuantityRemaining() == null || addon.getQuantityRemaining() <= 0) {
            throw AppException.badRequest("Dich vu le nay da het so luong su dung.");
        }
        AddonService addonService = addon.getAddonService();
        if (addonService == null || (addonService.getIsActive() != null && !addonService.getIsActive())) {
            throw AppException.badRequest("Dich vu le nay khong con hoat dong.");
        }
    }

    private boolean shouldUseSubscription(SubscriptionUsage subscriptionUsage, CompanyAddon companyAddon) {
        if (subscriptionUsage == null) {
            return false;
        }
        if (companyAddon == null) {
            return true;
        }

        LocalDateTime subscriptionExpiredAt = subscriptionUsage.getCompanySubscription() != null
                ? subscriptionUsage.getCompanySubscription().getExpiredAt()
                : null;
        LocalDateTime addonExpiredAt = companyAddon.getExpiredAt();

        int expiryComparison = compareExpiry(subscriptionExpiredAt, addonExpiredAt);
        if (expiryComparison != 0) {
            return expiryComparison < 0;
        }
        return true;
    }

    private int compareExpiry(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    private String normalizeServiceCode(String serviceCode) {
        if (serviceCode == null || serviceCode.isBlank()) {
            throw AppException.badRequest("Ma dich vu khong duoc de trong.");
        }
        return serviceCode.trim().toUpperCase();
    }

    private <T> T first(List<T> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }
}
