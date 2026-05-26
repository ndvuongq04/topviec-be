package com.topviec.topviec_be.service.impl;

import com.topviec.topviec_be.dto.internal.ServiceQuotaAllocation;
import com.topviec.topviec_be.dto.request.ReqApplyAddonDTO;
import com.topviec.topviec_be.dto.request.ReqRenewSubscriptionDTO;
import com.topviec.topviec_be.dto.response.ResCompanyAddonDTO;
import com.topviec.topviec_be.dto.response.ResCompanyBrandingDTO;
import com.topviec.topviec_be.dto.response.ResCompanySubscriptionDTO;
import com.topviec.topviec_be.dto.response.ResCompanySubscriptionDTO.ResSubscriptionUsageDTO;
import com.topviec.topviec_be.dto.response.ResJobPostAddonDTO;
import com.topviec.topviec_be.dto.response.ResSubscriptionRenewalDTO;
import com.topviec.topviec_be.entity.AddonService;
import com.topviec.topviec_be.entity.CompanyAddon;
import com.topviec.topviec_be.entity.CompanySubscription;
import com.topviec.topviec_be.entity.JobPosting;
import com.topviec.topviec_be.entity.Order;
import com.topviec.topviec_be.entity.OrderItem;
import com.topviec.topviec_be.entity.ServicePackage;
import com.topviec.topviec_be.entity.ServicePackageDetail;
import com.topviec.topviec_be.entity.Services;
import com.topviec.topviec_be.entity.SubscriptionRenewalLog;
import com.topviec.topviec_be.entity.SubscriptionUsage;
import com.topviec.topviec_be.enums.services.BillingCycle;
import com.topviec.topviec_be.enums.services.OrderItemType;
import com.topviec.topviec_be.enums.services.OrderStatus;
import com.topviec.topviec_be.enums.services.OrderType;
import com.topviec.topviec_be.enums.services.ServiceCategory;
import com.topviec.topviec_be.enums.services.SubscriptionStatus;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.AddonServiceRepository;
import com.topviec.topviec_be.repository.CompanyAddonRepository;
import com.topviec.topviec_be.repository.CompanySubscriptionRepository;
import com.topviec.topviec_be.repository.JobPostingRepository;
import com.topviec.topviec_be.repository.OrderRepository;
import com.topviec.topviec_be.repository.ServicePackageDetailRepository;
import com.topviec.topviec_be.repository.ServicePackageRepository;
import com.topviec.topviec_be.repository.ServiceRepository;
import com.topviec.topviec_be.repository.SubscriptionRenewalLogRepository;
import com.topviec.topviec_be.repository.SubscriptionUsageRepository;
import com.topviec.topviec_be.service.CompanyService;
import com.topviec.topviec_be.service.CompanyServiceQuotaService;
import com.topviec.topviec_be.service.EmployerServiceManagementService;
import com.topviec.topviec_be.service.activation.BrandingActivationService;
import com.topviec.topviec_be.service.activation.JobPostingActivationService;
import com.topviec.topviec_be.service.activation.ServiceActivationRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployerServiceManagementServiceImpl implements EmployerServiceManagementService {

        private final CompanyService companyService;
        private final CompanySubscriptionRepository companySubscriptionRepository;
        private final SubscriptionUsageRepository subscriptionUsageRepository;
        private final CompanyAddonRepository companyAddonRepository;
        private final AddonServiceRepository addonServiceRepository;
        private final ServiceRepository serviceRepository;
        private final ServicePackageRepository servicePackageRepository;
        private final ServicePackageDetailRepository servicePackageDetailRepository;
        private final JobPostingRepository jobPostingRepository;
        private final CompanyServiceQuotaService quotaService;
        private final ServiceActivationRouter serviceActivationRouter;
        private final JobPostingActivationService jobPostingActivationService;
        private final BrandingActivationService brandingActivationService;
        private final OrderRepository orderRepository;
        private final SubscriptionRenewalLogRepository subscriptionRenewalLogRepository;

        @Override
        @Transactional(readOnly = true)
        public ResCompanySubscriptionDTO getMySubscription(Long userId) {
                Long companyId = getCompanyId(userId);

                CompanySubscription subscription = companySubscriptionRepository
                                .findFirstByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, SubscriptionStatus.ACTIVE)
                                .orElseThrow(() -> AppException.notFound("Cong ty chua dang ky goi dich vu nao."));

                if (subscription.getExpiredAt() != null
                                && subscription.getExpiredAt().isBefore(LocalDateTime.now())) {
                        throw AppException.notFound("Goi dich vu da het han. Vui long gia han hoac mua goi moi.");
                }

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
                LocalDateTime now = LocalDateTime.now();

                List<CompanyAddon> addons = companyAddonRepository
                                .findByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, SubscriptionStatus.ACTIVE)
                                .stream()
                                .filter(addon -> addon.getExpiredAt() == null || addon.getExpiredAt().isAfter(now))
                                .collect(Collectors.toList());

                Map<String, List<CompanyAddon>> grouped = new LinkedHashMap<>();
                Map<String, AddonService> addonSvcMap = new LinkedHashMap<>();

                for (CompanyAddon addon : addons) {
                        AddonService addonSvc = addonServiceRepository.findById(addon.getAddonServiceId())
                                        .orElse(null);
                        if (addonSvc == null) continue;

                        String addonCode = addonSvc.getCode();
                        grouped.computeIfAbsent(addonCode, k -> new ArrayList<>()).add(addon);
                        addonSvcMap.putIfAbsent(addonCode, addonSvc);
                }

                return grouped.entrySet().stream().map(entry -> {
                        String addonCode = entry.getKey();
                        List<CompanyAddon> group = entry.getValue();
                        AddonService addonSvc = addonSvcMap.get(addonCode);
                        Services svc = serviceRepository.findById(addonSvc.getServiceId()).orElse(null);

                        int totalQty = group.stream().mapToInt(addon -> safeInt(addon.getQuantityTotal())).sum();
                        int remainingQty = group.stream().mapToInt(addon -> safeInt(addon.getQuantityRemaining())).sum();
                        List<Long> ids = group.stream().map(CompanyAddon::getId).collect(Collectors.toList());
                        CompanyAddon representative = group.get(0);

                        LocalDateTime latestExpiry = group.stream()
                                        .map(CompanyAddon::getExpiredAt)
                                        .filter(e -> e != null)
                                        .max(LocalDateTime::compareTo)
                                        .orElse(null);

                        LocalDateTime earliestStart = group.stream()
                                        .map(CompanyAddon::getStartedAt)
                                        .filter(s -> s != null)
                                        .min(LocalDateTime::compareTo)
                                        .orElse(null);

                        return ResCompanyAddonDTO.builder()
                                        .id(representative.getId())
                                        .addonServiceId(representative.getAddonServiceId())
                                        .companyAddonIds(ids)
                                        .addonName(addonSvc.getName())
                                        .addonCode(addonCode)
                                        .addonQuantity(addonSvc.getQuantity())
                                        .serviceId(svc != null ? svc.getId() : null)
                                        .serviceCode(svc != null ? svc.getCode() : null)
                                        .serviceName(svc != null ? svc.getName() : null)
                                        .serviceCategory(svc != null ? svc.getCategory() : null)
                                        .serviceCategoryName(svc != null && svc.getCategory() != null
                                                        ? svc.getCategory().getValue()
                                                        : null)
                                        .status(SubscriptionStatus.ACTIVE)
                                        .quantityTotal(totalQty)
                                        .quantityRemaining(remainingQty)
                                        .startedAt(earliestStart)
                                        .expiredAt(latestExpiry)
                                        .createdAt(representative.getCreatedAt())
                                        .build();
                }).collect(Collectors.toList());
        }

        @Override
        @Transactional
        public ResJobPostAddonDTO applyAddonToJobPost(Long userId, Long jobPostingId, ReqApplyAddonDTO request) {
                Long companyId = getCompanyId(userId);

                JobPosting jobPosting = jobPostingRepository.findByIdAndDeletedAtIsNull(jobPostingId)
                                .orElseThrow(() -> AppException.notFound("Khong tim thay tin tuyen dung."));

                if (!jobPosting.getCompanyId().equals(companyId)) {
                        throw AppException.forbidden("Ban khong co quyen thao tac tren tin tuyen dung nay.");
                }

                ServiceQuotaAllocation quota = resolveQuotaForUpdate(companyId, request);
                Services service = quota.getService();
                ServiceCategory serviceCategory = service != null ? service.getCategory() : null;
                String serviceCode = quota.getServiceCode();

                if (serviceCategory != ServiceCategory.JOB_POSTING) {
                        throw AppException.badRequest("Dich vu nay khong thuoc nhom tin tuyen dung.");
                }

                if (serviceActivationRouter.isSupported(serviceCategory, serviceCode)) {
                        log.info("[applyAddon] Routing to handler: {} for job #{}", serviceCode, jobPostingId);
                        return serviceActivationRouter.activate(serviceCategory, serviceCode, jobPosting, quota);
                }

                log.info("[applyAddon] Generic fallback for service: {} on job #{}", serviceCode, jobPostingId);
                return jobPostingActivationService.applyGenericAddon(jobPostingId, quota);
        }

        @Override
        @Transactional
        public ResCompanyBrandingDTO applyBrandingToCompany(Long userId, ReqApplyAddonDTO request) {
                Long companyId = getCompanyId(userId);
                ServiceQuotaAllocation quota = resolveQuotaForUpdate(companyId, request);

                Services service = quota.getService();
                if (service == null || service.getCategory() != ServiceCategory.BRANDING) {
                        throw AppException.badRequest("Dich vu nay khong thuoc nhom branding.");
                }

                String serviceCode = quota.getServiceCode();
                if (!BrandingActivationService.isSupported(serviceCode)) {
                        throw AppException.badRequest("Dich vu branding nay chua duoc ho tro.");
                }

                return brandingActivationService.activate(serviceCode, companyId, quota);
        }

        @Override
        @Transactional
        public ResSubscriptionRenewalDTO renewSubscription(Long userId, ReqRenewSubscriptionDTO request) {
                Long companyId = getCompanyId(userId);

                CompanySubscription subscription = companySubscriptionRepository
                                .findFirstByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, SubscriptionStatus.ACTIVE)
                                .orElseThrow(() -> AppException.badRequest(
                                                "Khong tim thay goi dich vu dang hoat dong. Vui long mua goi moi."));

                ServicePackage servicePackage = servicePackageRepository.findById(subscription.getServicePackageId())
                                .orElseThrow(() -> AppException.notFound("Khong tim thay thong tin goi dich vu."));

                if (servicePackage.getIsActive() == null || !servicePackage.getIsActive()) {
                        throw AppException.badRequest("Goi dich vu nay khong con hoat dong. Khong the gia han.");
                }

                if (subscription.getExpiredAt() == null) {
                        throw AppException.badRequest("Goi dich vu khong co ngay het han, khong the gia han.");
                }

                Order order = Order.builder()
                                .companyId(companyId)
                                .orderCode("ORD-RN-" + System.currentTimeMillis())
                                .type(OrderType.SUBSCRIPTION)
                                .totalAmount(servicePackage.getPrice())
                                .status(OrderStatus.PAID)
                                .paymentMethod(request.getPaymentMethod())
                                .paidAt(LocalDateTime.now())
                                .createdBy(userId)
                                .build();
                Order savedOrder = orderRepository.save(order);

                OrderItem item = OrderItem.builder()
                                .orderId(savedOrder.getId())
                                .itemType(OrderItemType.SUBSCRIPTION)
                                .servicePackageId(servicePackage.getId())
                                .quantity(1)
                                .unitPrice(servicePackage.getPrice())
                                .totalPrice(servicePackage.getPrice())
                                .billingCycle(servicePackage.getBillingCycle())
                                .build();
                savedOrder.setOrderItems(new ArrayList<>(List.of(item)));

                LocalDateTime oldExpiredAt = subscription.getExpiredAt();
                LocalDateTime newExpiredAt = subscription.getBillingCycle() == BillingCycle.MONTHLY
                                ? oldExpiredAt.plusMonths(1)
                                : oldExpiredAt.plusYears(1);
                subscription.setExpiredAt(newExpiredAt);
                subscription.setReminderSentAt(null);
                companySubscriptionRepository.save(subscription);

                List<ServicePackageDetail> details = servicePackageDetailRepository
                                .findByServicePackageId(servicePackage.getId());
                List<SubscriptionUsage> usages = subscriptionUsageRepository
                                .findByCompanySubscriptionId(subscription.getId());

                int totalQuotaAdded = 0;
                for (ServicePackageDetail detail : details) {
                        Services svc = serviceRepository.findById(detail.getServiceId()).orElse(null);
                        if (svc == null) continue;

                        SubscriptionUsage usage = usages.stream()
                                        .filter(u -> u.getFeatureCode().equals(svc.getCode()))
                                        .findFirst()
                                        .orElse(null);

                        if (usage != null) {
                                usage.setQuantityTotal(usage.getQuantityTotal() + detail.getQuantity());
                                usage.setQuantityRemaining(usage.getQuantityRemaining() + detail.getQuantity());
                                usage.setResetAt(newExpiredAt);
                                subscriptionUsageRepository.save(usage);
                        } else {
                                SubscriptionUsage newUsage = SubscriptionUsage.builder()
                                                .companySubscriptionId(subscription.getId())
                                                .companyId(companyId)
                                                .featureCode(svc.getCode())
                                                .quantityTotal(detail.getQuantity())
                                                .quantityRemaining(detail.getQuantity())
                                                .resetAt(newExpiredAt)
                                                .build();
                                subscriptionUsageRepository.save(newUsage);
                        }
                        totalQuotaAdded += detail.getQuantity();
                }

                SubscriptionRenewalLog renewalLog = SubscriptionRenewalLog.builder()
                                .companySubscriptionId(subscription.getId())
                                .orderId(savedOrder.getId())
                                .oldExpiredAt(oldExpiredAt)
                                .newExpiredAt(newExpiredAt)
                                .quotaAdded(totalQuotaAdded)
                                .renewedBy(userId)
                                .build();
                SubscriptionRenewalLog savedLog = subscriptionRenewalLogRepository.save(renewalLog);

                List<SubscriptionUsage> updatedUsages = subscriptionUsageRepository
                                .findByCompanySubscriptionId(subscription.getId());

                List<ResSubscriptionRenewalDTO.UsageInfo> usageInfos = updatedUsages.stream()
                                .map(u -> {
                                        Services svc = serviceRepository.findByCode(u.getFeatureCode()).orElse(null);
                                        return ResSubscriptionRenewalDTO.UsageInfo.builder()
                                                        .featureCode(u.getFeatureCode())
                                                        .featureName(svc != null ? svc.getName() : null)
                                                        .quantityTotal(u.getQuantityTotal())
                                                        .quantityRemaining(u.getQuantityRemaining())
                                                        .build();
                                })
                                .collect(Collectors.toList());

                return ResSubscriptionRenewalDTO.builder()
                                .renewalLogId(savedLog.getId())
                                .orderId(savedOrder.getId())
                                .orderCode(savedOrder.getOrderCode())
                                .totalAmount(savedOrder.getTotalAmount())
                                .subscription(ResSubscriptionRenewalDTO.SubscriptionInfo.builder()
                                                .id(subscription.getId())
                                                .packageName(servicePackage.getName())
                                                .packageCode(servicePackage.getCode())
                                                .billingCycle(subscription.getBillingCycle())
                                                .status(subscription.getStatus())
                                                .oldExpiredAt(oldExpiredAt)
                                                .newExpiredAt(newExpiredAt)
                                                .usages(usageInfos)
                                                .build())
                                .build();
        }

        private ServiceQuotaAllocation resolveQuotaForUpdate(Long companyId, ReqApplyAddonDTO request) {
                if (request.getCompanyAddonId() != null) {
                        ServiceQuotaAllocation quota = quotaService.findAddonQuotaForUpdate(companyId, request.getCompanyAddonId());
                        if (request.getServiceCode() != null && !request.getServiceCode().isBlank()) {
                                String requestedCode = request.getServiceCode().trim().toUpperCase();
                                if (!requestedCode.equals(quota.getServiceCode())) {
                                        throw AppException.badRequest("companyAddonId khong khop voi serviceCode.");
                                }
                        }
                        return quota;
                }
                return quotaService.findAvailableQuotaForUpdate(companyId, request.getServiceCode());
        }

        private Long getCompanyId(Long userId) {
                Long companyId = companyService.getCompanyIdByUserId(userId);
                if (companyId == null) {
                        throw AppException.badRequest("Chua co ho so cong ty.");
                }
                return companyId;
        }

        private int safeInt(Integer value) {
                return value != null ? value : 0;
        }
}
