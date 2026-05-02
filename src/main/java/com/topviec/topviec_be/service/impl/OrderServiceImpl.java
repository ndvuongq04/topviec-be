package com.topviec.topviec_be.service.impl;

import com.topviec.topviec_be.dto.request.ReqCreateOrderDTO;
import com.topviec.topviec_be.dto.request.ReqUpdateOrderStatusDTO;
import com.topviec.topviec_be.dto.response.ResAdminOrderStatisticsDTO;
import com.topviec.topviec_be.dto.response.ResCompanyDTO;
import com.topviec.topviec_be.dto.response.ResOrderDTO;
import com.topviec.topviec_be.dto.response.ResOrderItemDTO;
import com.topviec.topviec_be.dto.response.ResServicePackageDetailDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import com.topviec.topviec_be.entity.AddonService;
import com.topviec.topviec_be.entity.Order;
import com.topviec.topviec_be.entity.OrderItem;
import com.topviec.topviec_be.entity.Services;
import com.topviec.topviec_be.entity.ServicePackage;
import com.topviec.topviec_be.entity.ServicePackageDetail;
import com.topviec.topviec_be.entity.CompanySubscription;
import com.topviec.topviec_be.entity.SubscriptionUsage;
import com.topviec.topviec_be.entity.CompanyAddon;
import com.topviec.topviec_be.enums.services.BillingCycle;
import com.topviec.topviec_be.enums.services.SubscriptionStatus;
import com.topviec.topviec_be.enums.services.OrderItemType;
import com.topviec.topviec_be.enums.services.OrderStatus;
import com.topviec.topviec_be.enums.services.OrderType;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.AddonServiceRepository;
import com.topviec.topviec_be.repository.OrderRepository;
import com.topviec.topviec_be.repository.ServicePackageRepository;
import com.topviec.topviec_be.repository.ServicePackageDetailRepository;
import com.topviec.topviec_be.repository.ServiceRepository;
import com.topviec.topviec_be.repository.CompanySubscriptionRepository;
import com.topviec.topviec_be.repository.SubscriptionUsageRepository;
import com.topviec.topviec_be.repository.CompanyAddonRepository;
import com.topviec.topviec_be.service.CompanyService;
import com.topviec.topviec_be.service.OrderService;
import com.topviec.topviec_be.specification.OrderSpecification;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final ServicePackageDetailRepository servicePackageDetailRepository;
    private final ServiceRepository serviceRepository;
    private final AddonServiceRepository addonServiceRepository;
    private final CompanyService companyService;
    private final CompanySubscriptionRepository companySubscriptionRepository;
    private final SubscriptionUsageRepository subscriptionUsageRepository;
    private final CompanyAddonRepository companyAddonRepository;

    @Override
    @Transactional
    public ResOrderDTO createOrder(Long userId, ReqCreateOrderDTO request) {
        Long companyId = companyService.getCompanyIdByUserId(userId);
        if (companyId == null) {
            throw AppException.badRequest("Chưa có hồ sơ công ty. Không thể thực hiện mua hàng.");
        }

        BigDecimal unitPrice = BigDecimal.ZERO;
        ServicePackage servicePackage = null;
        AddonService addonService = null;
        OrderItemType itemType;

        if (request.getType() == OrderType.SUBSCRIPTION) {
            itemType = OrderItemType.SUBSCRIPTION;
            servicePackage = servicePackageRepository.findById(request.getPackageId())
                    .orElseThrow(() -> AppException.notFound("Không tìm thấy gói dịch vụ."));
            if (servicePackage.getIsActive() == null || !servicePackage.getIsActive()) {
                throw AppException.badRequest("Gói dịch vụ này không còn hoạt động.");
            }
            unitPrice = servicePackage.getPrice();
        } else {
            itemType = OrderItemType.ADDON;
            addonService = addonServiceRepository.findById(request.getPackageId())
                    .orElseThrow(() -> AppException.notFound("Không tìm thấy dịch vụ lẻ."));
            if (addonService.getIsActive() == null || !addonService.getIsActive()) {
                throw AppException.badRequest("Dịch vụ lẻ này không còn hoạt động.");
            }
            unitPrice = addonService.getPrice();
        }

        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        Order order = Order.builder()
                .companyId(companyId)
                .orderCode("ORD-" + System.currentTimeMillis())
                .type(request.getType())
                .totalAmount(totalAmount)
                .status(OrderStatus.PAID)
                .paymentMethod(request.getPaymentMethod())
                .paidAt(LocalDateTime.now())
                .createdBy(userId)
                .build();

        Order savedOrder = orderRepository.save(order);

        OrderItem item = OrderItem.builder()
                .orderId(savedOrder.getId())
                .itemType(itemType)
                .servicePackageId(servicePackage != null ? servicePackage.getId() : null)
                .addonServiceId(addonService != null ? addonService.getId() : null)
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .totalPrice(totalAmount)
                .billingCycle(servicePackage != null ? servicePackage.getBillingCycle() : null)
                .durationDays(addonService != null ? addonService.getDurationDays() : null)
                .build();

        List<OrderItem> items = new ArrayList<>();
        items.add(item);
        savedOrder.setOrderItems(items);

        // TODO: Sẽ có phần Gateway thanh toán (VNPAY/MOMO) ở đây để nhận callback
        // Tạm thời giả lập thanh toán thành công và kích hoạt ngay
        if (itemType == OrderItemType.SUBSCRIPTION && servicePackage != null) {
            activateSubscription(companyId, savedOrder.getId(), servicePackage);
        } else if (itemType == OrderItemType.ADDON && addonService != null) {
            activateAddon(companyId, savedOrder.getId(), addonService, request.getQuantity());
        }

        return mapToDTO(savedOrder);
    }

    private void activateSubscription(Long companyId, Long orderId, ServicePackage servicePackage) {
        CompanySubscription sub = CompanySubscription.builder()
                .companyId(companyId)
                .servicePackageId(servicePackage.getId())
                .orderId(orderId)
                .status(SubscriptionStatus.ACTIVE)
                .billingCycle(servicePackage.getBillingCycle())
                .startedAt(LocalDateTime.now())
                .expiredAt(servicePackage.getBillingCycle() == BillingCycle.MONTHLY
                        ? LocalDateTime.now().plusMonths(1)
                        : LocalDateTime.now().plusYears(1))
                .build();

        CompanySubscription savedSub = companySubscriptionRepository.save(sub);

        // Tạo SubscriptionUsage từ ServicePackageDetails (thay thế JSON features cũ)
        List<ServicePackageDetail> details = servicePackageDetailRepository
                .findByServicePackageId(servicePackage.getId());

        for (ServicePackageDetail detail : details) {
            Services svc = serviceRepository.findById(detail.getServiceId()).orElse(null);
            if (svc == null) continue;

            SubscriptionUsage usage = SubscriptionUsage.builder()
                    .companySubscriptionId(savedSub.getId())
                    .companyId(companyId)
                    .featureCode(svc.getCode())
                    .quantityTotal(detail.getQuantity())
                    .quantityRemaining(detail.getQuantity())
                    .resetAt(savedSub.getExpiredAt())
                    .build();

            subscriptionUsageRepository.save(usage);
        }
    }

    private void activateAddon(Long companyId, Long orderId, AddonService addonService, int quantity) {
        CompanyAddon companyAddon = CompanyAddon.builder()
                .companyId(companyId)
                .addonServiceId(addonService.getId())
                .orderId(orderId)
                .status(SubscriptionStatus.ACTIVE)
                .quantityTotal(quantity * addonService.getQuantity())
                .quantityRemaining(quantity * addonService.getQuantity())
                .startedAt(LocalDateTime.now())
                .expiredAt(addonService.getDurationDays() != null
                        ? LocalDateTime.now().plusDays(addonService.getDurationDays())
                        : null)
                .build();
        companyAddonRepository.save(companyAddon);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationDTO getMyOrders(
            Long userId,
            String keyword, OrderType type, OrderStatus status,
            String dateFilter, String startDate, String endDate,
            Pageable pageable) {
        Long companyId = companyService.getCompanyIdByUserId(userId);
        if (companyId == null) {
            throw AppException.badRequest("Chưa có hồ sơ công ty.");
        }

        LocalDateTime startDt = parseDateFilter(dateFilter, startDate, true);
        LocalDateTime endDt = parseDateFilter(dateFilter, endDate, false);

        Specification<Order> spec = OrderSpecification.withFilter(keyword, type, status, startDt, endDt)
                .and(OrderSpecification.hasCompanyId(companyId));

        Page<Order> page = orderRepository.findAll(spec, pageable);
        return buildPaginationResult(page, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ResOrderDTO getMyOrderById(Long userId, Long orderId) {
        Long companyId = companyService.getCompanyIdByUserId(userId);
        if (companyId == null) {
            throw AppException.badRequest("Chưa có hồ sơ công ty.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn hàng."));

        if (!order.getCompanyId().equals(companyId)) {
            throw AppException.badRequest("Bạn không có quyền truy cập đơn hàng này.");
        }

        return mapToDTO(order);
    }

    @Override
    @Transactional
    public ResOrderDTO cancelOrder(Long userId, Long orderId) {
        Long companyId = companyService.getCompanyIdByUserId(userId);
        if (companyId == null) {
            throw AppException.badRequest("Chưa có hồ sơ công ty.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn hàng."));

        if (!order.getCompanyId().equals(companyId)) {
            throw AppException.badRequest("Bạn không có quyền thao tác trên đơn hàng này.");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw AppException.badRequest("Chỉ có thể hủy đơn hàng đang trong trạng thái chờ thanh toán (PENDING).");
        }

        order.setStatus(OrderStatus.CANCELLED);
        return mapToDTO(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationDTO getAllOrders(
            String keyword, OrderType type, OrderStatus status,
            String dateFilter, String startDate, String endDate,
            Pageable pageable) {

        LocalDateTime startDt = parseDateFilter(dateFilter, startDate, true);
        LocalDateTime endDt = parseDateFilter(dateFilter, endDate, false);

        Specification<Order> spec = OrderSpecification.withFilter(keyword, type, status, startDt, endDt);
        Page<Order> page = orderRepository.findAll(spec, pageable);
        return buildPaginationResult(page, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ResOrderDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn hàng."));
        return mapToDTO(order);
    }

    @Override
    @Transactional
    public ResOrderDTO updateOrderStatus(Long adminId, Long orderId, ReqUpdateOrderStatusDTO request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn hàng."));

        if (request.getStatus() == OrderStatus.PAID && order.getStatus() != OrderStatus.PAID) {
            order.setPaidAt(LocalDateTime.now());
        }

        order.setStatus(request.getStatus());
        return mapToDTO(orderRepository.save(order));
    }

    // ── Admin statistics ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ResAdminOrderStatisticsDTO getOrderStatistics() {
        // 1. Tổng đơn hàng
        long totalOrders = orderRepository.count();

        // 2. Tổng đơn hàng đã thanh toán
        long paidOrders = orderRepository.countByStatus(OrderStatus.PAID);

        // 3. Tổng đơn hàng đang chờ xử lý
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);

        // 4. Tổng giá trị (tất cả đơn đã thanh toán)
        BigDecimal totalRevenue = orderRepository.sumTotalAmountByStatus(OrderStatus.PAID);

        return ResAdminOrderStatisticsDTO.builder()
                .totalOrders(totalOrders)
                .paidOrders(paidOrders)
                .pendingOrders(pendingOrders)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .build();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private LocalDateTime parseDateFilter(String dateFilter, String rawDate, boolean isStart) {
        if (dateFilter != null && !dateFilter.isBlank()) {
            LocalDateTime now = LocalDateTime.now();
            switch (dateFilter.toLowerCase()) {
                case "today":
                    return isStart ? now.toLocalDate().atStartOfDay() : now.toLocalDate().atTime(23, 59, 59);
                case "last7days":
                    return isStart ? now.minusDays(7).toLocalDate().atStartOfDay() : now.toLocalDate().atTime(23, 59, 59);
                case "thismonth":
                    return isStart ? now.withDayOfMonth(1).toLocalDate().atStartOfDay() : now.toLocalDate().atTime(23, 59, 59);
            }
        }
        if (rawDate != null && !rawDate.isBlank()) {
            try {
                return LocalDateTime.parse(rawDate);
            } catch (Exception e) {
                try {
                    return isStart
                            ? java.time.LocalDate.parse(rawDate).atStartOfDay()
                            : java.time.LocalDate.parse(rawDate).atTime(23, 59, 59);
                } catch (Exception ex) {}
            }
        }
        return null;
    }

    private ResultPaginationDTO buildPaginationResult(Page<Order> page, Pageable pageable) {
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotals(page.getTotalElements());

        List<ResOrderDTO> results = page.getContent().stream()
                .map(this::mapToDTO).collect(Collectors.toList());

        ResultPaginationDTO response = new ResultPaginationDTO();
        response.setMeta(meta);
        response.setResult(results);
        return response;
    }

    private ResOrderDTO mapToDTO(Order entity) {
        List<ResOrderItemDTO> itemDTOs = new ArrayList<>();
        if (entity.getOrderItems() != null) {
            itemDTOs = entity.getOrderItems().stream().map(item -> {
                String packageName = null;
                List<ResServicePackageDetailDTO> detailDTOs = new ArrayList<>();

                if (item.getServicePackageId() != null) {
                    ServicePackage sp = item.getServicePackage();
                    if (sp == null) {
                        sp = servicePackageRepository.findById(item.getServicePackageId()).orElse(null);
                    }
                    if (sp != null) {
                        packageName = sp.getName();
                        final ServicePackage finalSp = sp;
                        List<ServicePackageDetail> details = finalSp.getDetails();
                        if (details == null || details.isEmpty()) {
                            details = servicePackageDetailRepository.findByServicePackageId(finalSp.getId());
                        }
                        detailDTOs = details.stream().map(d -> {
                            Services svc = serviceRepository.findById(d.getServiceId()).orElse(null);
                            return ResServicePackageDetailDTO.builder()
                                    .id(d.getId())
                                    .serviceId(d.getServiceId())
                                    .serviceCode(svc != null ? svc.getCode() : null)
                                    .serviceName(svc != null ? svc.getName() : null)
                                    .serviceCategory(svc != null ? svc.getCategory() : null)
                                    .serviceCategoryName(svc != null && svc.getCategory() != null ? svc.getCategory().getValue() : null)
                                    .serviceUnit(svc != null ? svc.getUnit() : null)
                                    .quantity(d.getQuantity())
                                    .build();
                        }).collect(Collectors.toList());
                    }
                }

                if (packageName == null && item.getAddonServiceId() != null) {
                    AddonService addon = item.getAddonService();
                    if (addon == null) {
                        addon = addonServiceRepository.findById(item.getAddonServiceId()).orElse(null);
                    }
                    if (addon != null) {
                        packageName = addon.getName();
                    }
                }

                return ResOrderItemDTO.builder()
                        .id(item.getId())
                        .itemType(item.getItemType())
                        .servicePackageId(item.getServicePackageId())
                        .addonServiceId(item.getAddonServiceId())
                        .packageName(packageName)
                        .details(detailDTOs)
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .billingCycle(item.getBillingCycle())
                        .durationDays(item.getDurationDays())
                        .build();
            }).collect(Collectors.toList());
        }

        ResOrderDTO.CompanyInfo companyInfo = null;
        if (entity.getCompany() != null) {
            companyInfo = ResOrderDTO.CompanyInfo.builder()
                    .name(entity.getCompany().getName())
                    .logoUrl(entity.getCompany().getLogoUrl())
                    .email(entity.getCompany().getEmail())
                    .phone(entity.getCompany().getPhone())
                    .build();
        } else if (entity.getCompanyId() != null) {
            try {
                ResCompanyDTO dto = companyService.getById(entity.getCompanyId());
                companyInfo = ResOrderDTO.CompanyInfo.builder()
                        .name(dto.getName())
                        .logoUrl(dto.getLogoUrl())
                        .email(dto.getEmail())
                        .phone(dto.getPhone())
                        .build();
            } catch (Exception e) {
                // Ignore if not found
            }
        }

        return ResOrderDTO.builder()
                .id(entity.getId())
                .orderCode(entity.getOrderCode())
                .type(entity.getType())
                .totalAmount(entity.getTotalAmount())
                .status(entity.getStatus())
                .paymentMethod(entity.getPaymentMethod())
                .paymentTransactionId(entity.getPaymentTransactionId())
                .paidAt(entity.getPaidAt())
                .note(entity.getNote())
                .createdAt(entity.getCreatedAt())
                .items(itemDTOs)
                .company(companyInfo)
                .build();
    }
}
