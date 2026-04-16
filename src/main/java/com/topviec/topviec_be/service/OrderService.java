package com.topviec.topviec_be.service;

import com.topviec.topviec_be.dto.request.ReqCreateOrderDTO;
import com.topviec.topviec_be.dto.request.ReqUpdateOrderStatusDTO;
import com.topviec.topviec_be.dto.response.ResOrderDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import com.topviec.topviec_be.enums.services.OrderStatus;
import com.topviec.topviec_be.enums.services.OrderType;

import org.springframework.data.domain.Pageable;

public interface OrderService {
        ResOrderDTO createOrder(Long userId, ReqCreateOrderDTO request);

        ResultPaginationDTO getMyOrders(
                        Long userId,
                        String keyword, OrderType type, OrderStatus status,
                        String dateFilter, String startDate, String endDate,
                        Pageable pageable);

        ResOrderDTO getMyOrderById(Long userId, Long orderId);

        ResOrderDTO cancelOrder(Long userId, Long orderId);

        ResultPaginationDTO getAllOrders(
                        String keyword, OrderType type, OrderStatus status,
                        String dateFilter, String startDate, String endDate,
                        Pageable pageable);

        ResOrderDTO getOrderById(Long orderId);

        ResOrderDTO updateOrderStatus(Long adminId, Long orderId, ReqUpdateOrderStatusDTO request);
}
