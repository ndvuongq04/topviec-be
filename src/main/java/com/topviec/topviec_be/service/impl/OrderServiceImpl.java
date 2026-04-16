package com.topviec.topviec_be.service.impl;

import com.topviec.topviec_be.dto.request.ReqCreateOrderDTO;
import com.topviec.topviec_be.dto.request.ReqUpdateOrderStatusDTO;
import com.topviec.topviec_be.dto.response.ResCompanyDTO;
import com.topviec.topviec_be.dto.response.ResOrderDTO;
import com.topviec.topviec_be.dto.response.ResOrderItemDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import com.topviec.topviec_be.entity.AddonPackage;
import com.topviec.topviec_be.entity.Order;
import com.topviec.topviec_be.entity.OrderItem;
import com.topviec.topviec_be.entity.ServicePackage;
import com.topviec.topviec_be.entity.CompanySubscription;
import com.topviec.topviec_be.entity.SubscriptionUsage;
import com.topviec.topviec_be.entity.CompanyAddon;
import com.topviec.topviec_be.enums.services.BillingCycle;
import com.topviec.topviec_be.enums.services.SubscriptionStatus;
import com.topviec.topviec_be.enums.services.OrderItemType;
import com.topviec.topviec_be.enums.services.OrderStatus;
import com.topviec.topviec_be.enums.services.OrderType;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.AddonPackageRepository;
import com.topviec.topviec_be.repository.OrderRepository;
import com.topviec.topviec_be.repository.ServicePackageRepository;
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
    private final AddonPackageRepository addonPackageRepository;
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
        AddonPackage addonPackage = null;
        OrderItemType itemType;

        if (request.getType() == OrderType.SUBSCRIPTION) {
            itemType = OrderItemType.SUBSCRIPTION;
            servicePackage = servicePackageRepository.findById(request.getPackageId())
                    .orElseThrow(() -> AppException.notFound("Không tìm thấy gói dịch vụ (Subscription)."));
            if (servicePackage.getIsActive() == null || !servicePackage.getIsActive()) {
                throw AppException.badRequest("Gói dịch vụ này không còn hoạt động.");
            }
            unitPrice = servicePackage.getPrice();
        } else {
            itemType = OrderItemType.ADDON;
            addonPackage = addonPackageRepository.findById(request.getPackageId())
                    .orElseThrow(() -> AppException.notFound("Không tìm thấy gói dịch vụ phụ (Addon)."));
            if (addonPackage.getIsActive() == null || !addonPackage.getIsActive()) {
                throw AppException.badRequest("Gói dịch vụ phụ này không còn hoạt động.");
            }
            unitPrice = addonPackage.getPrice();
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
                .addonPackageId(addonPackage != null ? addonPackage.getId() : null)
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .totalPrice(totalAmount)
                .billingCycle(servicePackage != null ? servicePackage.getBillingCycle() : null)
                .durationDays(addonPackage != null ? addonPackage.getDurationDays() : null)
                .build();

        List<OrderItem> items = new ArrayList<>();
        items.add(item);
        savedOrder.setOrderItems(items);

        // TODO: Sẽ có phần Gateway thanh toán (VNPAY/MOMO) ở đây để nhận callback
        // Tạm thời giả lập thanh toán thành công và kích hoạt ngay
        if (itemType == OrderItemType.SUBSCRIPTION && servicePackage != null) {
            CompanySubscription sub = CompanySubscription.builder()
                    .companyId(companyId)
                    .servicePackageId(servicePackage.getId())
                    .orderId(savedOrder.getId())
                    .status(SubscriptionStatus.ACTIVE)
                    .billingCycle(servicePackage.getBillingCycle())
                    .startedAt(LocalDateTime.now())
                    .expiredAt(servicePackage.getBillingCycle() == BillingCycle.MONTHLY
                            ? LocalDateTime.now().plusMonths(1)
                            : LocalDateTime.now().plusYears(1))
                    .build();

            CompanySubscription savedSub = companySubscriptionRepository.save(sub);

            if (servicePackage.getFeatures() instanceof java.util.Map) {
                java.util.Map<String, Object> featureMap = (java.util.Map<String, Object>) servicePackage.getFeatures();
                for (java.util.Map.Entry<String, Object> entry : featureMap.entrySet()) {
                    int total = 0;
                    if (entry.getValue() instanceof Number) {
                        total = ((Number) entry.getValue()).intValue();
                    } else if (entry.getValue() instanceof Boolean) {
                        total = ((Boolean) entry.getValue()) ? 999999 : 0;
                    }

                    SubscriptionUsage usage = SubscriptionUsage.builder()
                            .companySubscriptionId(savedSub.getId())
                            .companyId(companyId)
                            .featureCode(entry.getKey())
                            .quantityTotal(total)
                            .quantityRemaining(total)
                            .resetAt(savedSub.getExpiredAt())
                            .build();
                    subscriptionUsageRepository.save(usage);
                }
            }
        } else if (itemType == OrderItemType.ADDON && addonPackage != null) {
            CompanyAddon companyAddon = CompanyAddon.builder()
                    .companyId(companyId)
                    .addonPackageId(addonPackage.getId())
                    .orderId(savedOrder.getId())
                    .status(SubscriptionStatus.ACTIVE)
                    .quantityTotal(request.getQuantity())
                    .quantityRemaining(request.getQuantity())
                    .startedAt(LocalDateTime.now())
                    .expiredAt(addonPackage.getDurationDays() != null
                            ? LocalDateTime.now().plusDays(addonPackage.getDurationDays())
                            : null)
                    .build();
            companyAddonRepository.save(companyAddon);
        }

        return mapToDTO(savedOrder);
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

        LocalDateTime startDt = null;
        LocalDateTime endDt = null;

        if (dateFilter != null && !dateFilter.isBlank()) {
            LocalDateTime now = LocalDateTime.now();
            switch (dateFilter.toLowerCase()) {
                case "today":
                    startDt = now.toLocalDate().atStartOfDay();
                    endDt = now.toLocalDate().atTime(23, 59, 59);
                    break;
                case "last7days":
                    startDt = now.minusDays(7).toLocalDate().atStartOfDay();
                    endDt = now.toLocalDate().atTime(23, 59, 59);
                    break;
                case "thismonth":
                    startDt = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
                    endDt = now.toLocalDate().atTime(23, 59, 59);
                    break;
            }
        } else {
            if (startDate != null && !startDate.isBlank()) {
                try {
                    startDt = LocalDateTime.parse(startDate);
                } catch (Exception e) {
                    try {
                        startDt = java.time.LocalDate.parse(startDate).atStartOfDay();
                    } catch (Exception ex) {
                    }
                }
            }
            if (endDate != null && !endDate.isBlank()) {
                try {
                    endDt = LocalDateTime.parse(endDate);
                } catch (Exception e) {
                    try {
                        endDt = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);
                    } catch (Exception ex) {
                    }
                }
            }
        }

        Specification<Order> spec = OrderSpecification.withFilter(
                keyword, type, status, startDt, endDt)
                .and(OrderSpecification.hasCompanyId(companyId));

        Page<Order> page = orderRepository.findAll(spec, pageable);

        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotals(page.getTotalElements());

        List<ResOrderDTO> results = page.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        ResultPaginationDTO response = new ResultPaginationDTO();
        response.setMeta(meta);
        response.setResult(results);

        return response;
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
            throw AppException.badRequest("Chỉ có thể hủy hóa đơn đang trong trạng thái chờ thanh toán (PENDING).");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updatedOrder = orderRepository.save(order);

        return mapToDTO(updatedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationDTO getAllOrders(
            String keyword, OrderType type, OrderStatus status,
            String dateFilter, String startDate, String endDate,
            Pageable pageable) {

        LocalDateTime startDt = null;
        LocalDateTime endDt = null;

        if (dateFilter != null && !dateFilter.isBlank()) {
            LocalDateTime now = LocalDateTime.now();
            switch (dateFilter.toLowerCase()) {
                case "today":
                    startDt = now.toLocalDate().atStartOfDay();
                    endDt = now.toLocalDate().atTime(23, 59, 59);
                    break;
                case "last7days":
                    startDt = now.minusDays(7).toLocalDate().atStartOfDay();
                    endDt = now.toLocalDate().atTime(23, 59, 59);
                    break;
                case "thismonth":
                    startDt = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
                    endDt = now.toLocalDate().atTime(23, 59, 59);
                    break;
            }
        } else {
            if (startDate != null && !startDate.isBlank()) {
                try {
                    startDt = LocalDateTime.parse(startDate);
                } catch (Exception e) {
                    try {
                        startDt = java.time.LocalDate.parse(startDate).atStartOfDay();
                    } catch (Exception ex) {
                    }
                }
            }
            if (endDate != null && !endDate.isBlank()) {
                try {
                    endDt = LocalDateTime.parse(endDate);
                } catch (Exception e) {
                    try {
                        endDt = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);
                    } catch (Exception ex) {
                    }
                }
            }
        }

        Specification<Order> spec = OrderSpecification.withFilter(
                keyword, type, status, startDt, endDt);

        Page<Order> page = orderRepository.findAll(spec, pageable);

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
        Order updatedOrder = orderRepository.save(order);
        return mapToDTO(updatedOrder);
    }

    private ResOrderDTO mapToDTO(Order entity) {
        List<ResOrderItemDTO> itemDTOs = new ArrayList<>();
        if (entity.getOrderItems() != null) {
            itemDTOs = entity.getOrderItems().stream().map(item -> {
                String packageName = null;
                Object features = null;

                if (item.getServicePackage() != null) {
                    packageName = item.getServicePackage().getName();
                    features = item.getServicePackage().getFeatures();
                } else if (item.getServicePackageId() != null) {
                    try {
                        var spOpt = servicePackageRepository.findById(item.getServicePackageId());
                        if (spOpt.isPresent()) {
                            packageName = spOpt.get().getName();
                            features = spOpt.get().getFeatures();
                        }
                    } catch (Exception e) {}
                }

                if (packageName == null && item.getAddonPackage() != null) {
                    packageName = item.getAddonPackage().getName();
                } else if (packageName == null && item.getAddonPackageId() != null) {
                    try {
                        var addonOpt = addonPackageRepository.findById(item.getAddonPackageId());
                        if (addonOpt.isPresent()) {
                            packageName = addonOpt.get().getName();
                        }
                    } catch (Exception e) {}
                }

                return ResOrderItemDTO.builder()
                        .id(item.getId())
                        .itemType(item.getItemType())
                        .servicePackageId(item.getServicePackageId())
                        .addonPackageId(item.getAddonPackageId())
                        .packageName(packageName)
                        .features(features)
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
