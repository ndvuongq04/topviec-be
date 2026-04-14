package com.topviec.topviec_be.dto.response;

import com.topviec.topviec_be.enums.services.OrderStatus;
import com.topviec.topviec_be.enums.services.OrderType;
import com.topviec.topviec_be.enums.services.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ResOrderDTO {
    private Long id;
    private String orderCode;
    private OrderType type;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private String paymentTransactionId;
    private LocalDateTime paidAt;
    private String note;
    private LocalDateTime createdAt;
    private List<ResOrderItemDTO> items;
}
