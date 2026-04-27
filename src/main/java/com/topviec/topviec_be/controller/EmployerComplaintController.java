package com.topviec.topviec_be.controller;

import com.topviec.topviec_be.dto.response.ResEmployerComplaintDetailDTO;
import com.topviec.topviec_be.dto.response.ResMyViolationScoreDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import com.topviec.topviec_be.service.ReportService;
import com.topviec.topviec_be.service.ViolationScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * NTD tự quan sát các khiếu nại nhắm vào tin tuyển dụng của mình.
 * Base URL: /employer/me
 */
@RestController
@RequestMapping("/employer/me")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYER')")
public class EmployerComplaintController {

    private final ReportService reportService;
    private final ViolationScoreService violationScoreService;

    /**
     * GET /employer/me/reports
     * Danh sách khiếu nại nhắm vào tin của NTD (ẩn danh người báo cáo).
     */
    @GetMapping("/reports")
    public ResponseEntity<ResultPaginationDTO> getMyReports(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        return ResponseEntity.ok(reportService.getEmployerReports(extractUserId(jwt), status, pageable));
    }

    /**
     * GET /employer/me/reports/{id}
     * Chi tiết một khiếu nại — chỉ cho phép nếu tin thuộc công ty của NTD.
     */
    @GetMapping("/reports/{id}")
    public ResponseEntity<ResEmployerComplaintDetailDTO> getReportDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        return ResponseEntity.ok(reportService.getEmployerReportDetail(extractUserId(jwt), id));
    }

    /**
     * GET /employer/me/violation-score
     * NTD xem điểm vi phạm hiện tại của mình.
     */
    @GetMapping("/violation-score")
    public ResponseEntity<ResMyViolationScoreDTO> getMyViolationScore(
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(violationScoreService.getMyScore(extractUserId(jwt)));
    }

    /**
     * POST /employer/me/reports/{id}/respond
     * NTD xác nhận đã sửa tin (nhóm A) → trigger tự đóng báo cáo.
     */
    @PostMapping("/reports/{id}/respond")
    public ResponseEntity<ResEmployerComplaintDetailDTO> respondToReport(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        return ResponseEntity.ok(reportService.respondToReport(extractUserId(jwt), id));
    }

    private Long extractUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
