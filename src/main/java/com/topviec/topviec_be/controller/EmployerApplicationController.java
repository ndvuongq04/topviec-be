package com.topviec.topviec_be.controller;

import com.topviec.topviec_be.dto.request.ReqUpdateApplicationStatusDTO;
import com.topviec.topviec_be.dto.request.ReqEvaluateApplicationDTO;
import com.topviec.topviec_be.dto.response.ResEmployerApplicationDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import com.topviec.topviec_be.service.ApplicationService;
import com.topviec.topviec_be.service.CompanyService;
import com.topviec.topviec_be.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller dành cho Employer nhận và xem CV ứng tuyển.
 * Base URL: /api/v1/employer/applications
 */
@RestController
@RequestMapping("/employer/applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYER')")
public class EmployerApplicationController {

    private final ApplicationService applicationService;
    private final CompanyService companyService;

    /**
     * Lấy danh sách hồ sơ ứng tuyển của 1 tin tuyển dụng.
     */
    @GetMapping("/job/{jobPostId}")
    public ResponseEntity<ResultPaginationDTO> getApplicationsByJobPost(
            @PathVariable Long jobPostId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        Long userId = SecurityUtil.getCurrentUserId();
        Long companyId = companyService.getCompanyIdByUserId(userId);

        return ResponseEntity.ok(
                applicationService.getApplicationsByJobPost(userId, companyId, jobPostId, status, search, pageable));
    }

    /**
     * Xem chi tiết 1 hồ sơ ứng tuyển. Tự động chuyển status PENDING -> SEEN.
     */
    @GetMapping("/{applicationId}")
    public ResponseEntity<ResEmployerApplicationDTO> getApplicationDetail(
            @PathVariable Long applicationId) {

        Long userId = SecurityUtil.getCurrentUserId();
        Long companyId = companyService.getCompanyIdByUserId(userId);

        return ResponseEntity.ok(
                applicationService.getApplicationDetailByEmployer(userId, companyId, applicationId));
    }

    /**
     * NTD cập nhật trạng thái CV ứng tuyển (ví dụ: interviewing, rejected, hired...).
     */
    @PatchMapping("/{applicationId}/status")
    public ResponseEntity<ResEmployerApplicationDTO> updateApplicationStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody ReqUpdateApplicationStatusDTO request) {

        Long userId = SecurityUtil.getCurrentUserId();
        Long companyId = companyService.getCompanyIdByUserId(userId);

        return ResponseEntity.ok(
                applicationService.changeApplicationStatus(userId, companyId, applicationId, request));
    }

    /**
     * NTD đánh giá (cho điểm, ghi chú, gán tag) CV.
     */
    @PatchMapping("/{applicationId}/evaluate")
    public ResponseEntity<ResEmployerApplicationDTO> evaluateApplication(
            @PathVariable Long applicationId,
            @Valid @RequestBody ReqEvaluateApplicationDTO request) {

        Long userId = SecurityUtil.getCurrentUserId();
        Long companyId = companyService.getCompanyIdByUserId(userId);

        return ResponseEntity.ok(
                applicationService.evaluateApplication(userId, companyId, applicationId, request));
    }
}
