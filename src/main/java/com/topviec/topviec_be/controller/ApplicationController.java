package com.topviec.topviec_be.controller;

import com.topviec.topviec_be.dto.request.ReqApplyJobDTO;
import com.topviec.topviec_be.dto.request.ReqBulkApplyDTO;
import com.topviec.topviec_be.dto.request.ReqWithdrawApplicationDTO;
import com.topviec.topviec_be.dto.response.ResApplicationDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import com.topviec.topviec_be.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller cho ứng viên — yêu cầu đăng nhập.
 * Base URL: /api/v1/applications
 */
@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    /**
     * POST /applications/{jobPostId}
     * CN-UV-010: Nộp đơn đầy đủ
     */
    @PostMapping("/{jobPostId}")
    public ResponseEntity<ResApplicationDTO> apply(
            @PathVariable Long jobPostId,
            @RequestBody ReqApplyJobDTO request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(applicationService.apply(extractUserId(jwt), jobPostId, request));
    }

    /**
     * POST /applications/{jobPostId}/quick
     * CN-UV-011: Ứng tuyển nhanh (CV mặc định)
     */
    @PostMapping("/{jobPostId}/quick")
    public ResponseEntity<ResApplicationDTO> quickApply(
            @PathVariable Long jobPostId,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(applicationService.quickApply(extractUserId(jwt), jobPostId));
    }

    /**
     * POST /applications/bulk
     * CN-UV-012: Ứng tuyển hàng loạt (tối đa 10 tin)
     */
    @PostMapping("/bulk")
    public ResponseEntity<List<ResApplicationDTO>> bulkApply(
            @RequestBody ReqBulkApplyDTO request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(applicationService.bulkApply(extractUserId(jwt), request));
    }

    /**
     * GET /applications/me
     * CN-UV-013: Theo dõi trạng thái đơn ứng tuyển
     */
    @GetMapping("/me")
    public ResponseEntity<ResultPaginationDTO> getMyApplications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        return ResponseEntity.ok(
                applicationService.getMyApplications(extractUserId(jwt), status, pageable));
    }

    /**
     * PATCH /applications/{applicationId}/withdraw
     * CN-UV-015: Rút đơn ứng tuyển
     */
    @PatchMapping("/{applicationId}/withdraw")
    public ResponseEntity<ResApplicationDTO> withdraw(
            @PathVariable Long applicationId,
            @RequestBody(required = false) ReqWithdrawApplicationDTO request,
            @AuthenticationPrincipal Jwt jwt) {

        if (request == null)
            request = new ReqWithdrawApplicationDTO();

        return ResponseEntity.ok(
                applicationService.withdraw(extractUserId(jwt), applicationId, request));
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private Long extractUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}