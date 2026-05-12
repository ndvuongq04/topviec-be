package com.topviec.topviec_be.dto.request;

import com.topviec.topviec_be.enums.services.OrderType;
import com.topviec.topviec_be.enums.services.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReqCreateOrderDTO {

    @NotNull(message = "Loai order khong duoc de trong")
    private OrderType type;

    @NotNull(message = "ID goi khong duoc de trong")
    private Long packageId;

    @NotNull(message = "So luong khong duoc de trong")
    @Min(value = 1, message = "So luong toi thieu la 1")
    private Integer quantity;

    @NotNull(message = "Phuong thuc thanh toan khong duoc de trong")
    private PaymentMethod paymentMethod;

    private Boolean payNow;
}
