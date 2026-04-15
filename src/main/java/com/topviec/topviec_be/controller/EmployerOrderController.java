package com.topviec.topviec_be.controller;

import com.topviec.topviec_be.dto.request.ReqCreateOrderDTO;
import com.topviec.topviec_be.dto.response.ResOrderDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import com.topviec.topviec_be.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employer/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYER')")
public class EmployerOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ResultPaginationDTO> getMyOrders(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(orderService.getMyOrders(extractUserId(jwt), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResOrderDTO> getOrderById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getMyOrderById(extractUserId(jwt), id));
    }

    @PostMapping
    public ResponseEntity<ResOrderDTO> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReqCreateOrderDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(extractUserId(jwt), request));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ResOrderDTO> cancelOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(extractUserId(jwt), id));
    }

    private Long extractUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
