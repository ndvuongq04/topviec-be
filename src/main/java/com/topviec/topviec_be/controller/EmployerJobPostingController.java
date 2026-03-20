package com.topviec.topviec_be.controller;

import com.topviec.topviec_be.dto.request.ReqCreateJobPostingDTO;
import com.topviec.topviec_be.dto.request.ReqUpdateJobPostingDTO;
import com.topviec.topviec_be.dto.response.ResJobPostingDetail;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import com.topviec.topviec_be.service.CompanyService;
import com.topviec.topviec_be.service.JobPostingService;
import com.topviec.topviec_be.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller dành cho Employer — yêu cầu đăng nhập.
 * Base URL: /api/v1/employer/job-postings
 */
@RestController
@RequestMapping("/employer/job-postings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYER')")
public class EmployerJobPostingController {

    private final JobPostingService jobPostingService;
    private final CompanyService companyService;

    /**
     * POST /employer/job-postings
     * Tạo tin tuyển dụng mới, mặc định trạng thái draft.
     */
    @PostMapping
    public ResponseEntity<ResJobPostingDetail> create(
            @Valid @RequestBody ReqCreateJobPostingDTO request) {

        Long userId = SecurityUtil.getCurrentUserId();
        Long companyId = companyService.getCompanyIdByUserId(userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(jobPostingService.create(request, userId, companyId));
    }

    /**
     * GET /employer/job-postings
     * Lấy danh sách tin của công ty, hỗ trợ filter + phân trang.
     * companyId tự động lấy từ JWT → chỉ trả về tin của công ty đang đăng nhập.
     */
    @GetMapping
    public ResponseEntity<ResultPaginationDTO> getList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long industryId,
            @RequestParam(required = false) Long levelId,
            @RequestParam(required = false) String workType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isFeatured,
            @RequestParam(required = false) Boolean isUrgent,
            @RequestParam(required = false) Long salaryMin,
            @RequestParam(required = false) Long salaryMax,
            @RequestParam(required = false) Integer experienceYearsMin,
            @RequestParam(required = false) Integer experienceYearsMax,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        Long userId = SecurityUtil.getCurrentUserId();
        Long companyId = companyService.getCompanyIdByUserId(userId);

        return ResponseEntity.ok(jobPostingService.getList(
                keyword, companyId, industryId, levelId, workType, status,
                isFeatured, isUrgent, salaryMin, salaryMax,
                experienceYearsMin, experienceYearsMax, pageable));
    }

    /**
     * PUT /employer/job-postings/{id}
     * Chỉnh sửa tin tuyển dụng.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResJobPostingDetail> update(
            @PathVariable Long id,
            @Valid @RequestBody ReqUpdateJobPostingDTO request) {

        Long userId = SecurityUtil.getCurrentUserId();
        Long companyId = companyService.getCompanyIdByUserId(userId);

        return ResponseEntity.ok(jobPostingService.update(id, request, userId, companyId));
    }

    /**
     * PATCH /employer/job-postings/{id}/pause
     * Tạm dừng tin tuyển dụng.
     */
    @PatchMapping("/{id}/pause")
    public ResponseEntity<ResJobPostingDetail> pause(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        Long companyId = companyService.getCompanyIdByUserId(userId);
        return ResponseEntity.ok(jobPostingService.pause(id, companyId, userId));
    }

    /**
     * PATCH /employer/job-postings/{id}/resume
     * Mở lại tin tuyển dụng.
     */
    @PatchMapping("/{id}/resume")
    public ResponseEntity<ResJobPostingDetail> resume(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        Long companyId = companyService.getCompanyIdByUserId(userId);
        return ResponseEntity.ok(jobPostingService.resume(id, companyId, userId));
    }

    /**
     * PATCH /employer/job-postings/{id}/close
     * Đóng tin tuyển dụng.
     */
    @PatchMapping("/{id}/close")
    public ResponseEntity<ResJobPostingDetail> close(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        Long companyId = companyService.getCompanyIdByUserId(userId);
        return ResponseEntity.ok(jobPostingService.close(id, companyId, userId));
    }

    /**
     * PATCH /employer/job-postings/{id}/extend
     * Gia hạn tin tuyển dụng.
     */
    @PatchMapping("/{id}/extend")
    public ResponseEntity<ResJobPostingDetail> extend(
            @PathVariable Long id,
            @Valid @RequestBody com.topviec.topviec_be.dto.request.ReqExtendJobPostDTO request) {
        Long userId = SecurityUtil.getCurrentUserId();
        Long companyId = companyService.getCompanyIdByUserId(userId);
        return ResponseEntity.ok(jobPostingService.extend(id, companyId, userId, request));
    }

    /**
     * PATCH /employer/job-postings/{id}/refresh
     * Làm mới tin tuyển dụng (đẩy lên đầu).
     */
    @PatchMapping("/{id}/refresh")
    public ResponseEntity<ResJobPostingDetail> refresh(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        Long companyId = companyService.getCompanyIdByUserId(userId);
        return ResponseEntity.ok(jobPostingService.refresh(id, companyId, userId));
    }
}