package com.topviec.topviec_be.controller;

import com.topviec.topviec_be.dto.request.ReqApplyAddonDTO;
import com.topviec.topviec_be.dto.response.ResCompanyAddonDTO;
import com.topviec.topviec_be.dto.response.ResCompanySubscriptionDTO;
import com.topviec.topviec_be.dto.response.ResJobPostAddonDTO;
import com.topviec.topviec_be.service.EmployerServiceManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employer/services")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYER')")
public class EmployerServiceManagementController {

    private final EmployerServiceManagementService employerServiceManagementService;

    /**
     * Lấy thông tin gói dịch vụ hiện tại NTD đang dùng và hạn mức còn lại
     */
    @GetMapping("/subscription")
    public ResponseEntity<ResCompanySubscriptionDTO> getMySubscription(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(employerServiceManagementService.getMySubscription(extractUserId(jwt)));
    }

    /**
     * Lấy danh sách các dịch vụ lẻ mà NTD đã mua và số lượng còn lại
     */
    @GetMapping("/addons")
    public ResponseEntity<List<ResCompanyAddonDTO>> getMyAddons(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(employerServiceManagementService.getMyAddons(extractUserId(jwt)));
    }

    /**
     * Áp dụng dịch vụ lẻ cho một tin tuyển dụng
     */
    @PostMapping("/job-posts/{jobPostingId}/apply-addon")
    public ResponseEntity<ResJobPostAddonDTO> applyAddonToJobPost(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long jobPostingId,
            @Valid @RequestBody ReqApplyAddonDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employerServiceManagementService.applyAddonToJobPost(
                        extractUserId(jwt), jobPostingId, request));
    }

    private Long extractUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
