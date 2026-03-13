package com.topviec.topviec_be.controller;

import com.topviec.topviec_be.dto.request.ReqSuspendCompanyDTO;
import com.topviec.topviec_be.dto.request.ReqUpdateCompanyDTO;
import com.topviec.topviec_be.dto.request.ReqVerifyCompanyDTO;
import com.topviec.topviec_be.dto.response.ResCompanyDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import com.topviec.topviec_be.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Controller dành cho ADMIN quản lý hồ sơ công ty.
 * Base URL: /api/v1/admin/companies
 */
@RestController
@RequestMapping("/admin/companies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCompanyController {

    private final CompanyService companyService;

    /**
     * GET /admin/companies
     * Lấy danh sách tất cả công ty, có thể lọc theo status.
     * Ví dụ: GET /admin/companies?status=pending&page=0&size=10
     */
    @GetMapping
    public ResponseEntity<ResultPaginationDTO> getAllCompanies(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        return ResponseEntity.ok(companyService.getAllCompanies(status, pageable));
    }

    /**
     * GET /admin/companies/pending-verification
     * Lấy danh sách công ty đang chờ duyệt hồ sơ.
     */
    @GetMapping("/pending-verification")
    public ResponseEntity<ResultPaginationDTO> getPendingVerification(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        return ResponseEntity.ok(companyService.getPendingVerification(pageable));
    }

    /**
     * GET /admin/companies/{id}
     * Admin xem chi tiết 1 công ty bất kỳ (kể cả pending/suspended).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResCompanyDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.adminGetById(id));
    }

    /**
     * PATCH /admin/companies/{id}/verify
     * Duyệt hoặc từ chối hồ sơ công ty.
     * Body: { "approved": true } hoặc { "approved": false, "rejectionReason": "..."
     * }
     */
    @PatchMapping("/{id}/verify")
    public ResponseEntity<ResCompanyDTO> verifyCompany(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody ReqVerifyCompanyDTO request) {

        ResCompanyDTO data = companyService.verifyCompany(id, extractUserId(jwt), request);
        return ResponseEntity.ok(data);
    }

    /**
     * PATCH /admin/companies/{id}/suspend
     * Suspend công ty vi phạm.
     */
    @PatchMapping("/{id}/suspend")
    public ResponseEntity<ResCompanyDTO> suspendCompany(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody ReqSuspendCompanyDTO request) {

        ResCompanyDTO data = companyService.suspendCompany(id, extractUserId(jwt), request);
        return ResponseEntity.ok(data);
    }

    /**
     * PATCH /admin/companies/{id}/unsuspend
     * Mở khóa công ty đang bị suspend.
     */
    @PatchMapping("/{id}/unsuspend")
    public ResponseEntity<ResCompanyDTO> unsuspendCompany(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        ResCompanyDTO data = companyService.unsuspendCompany(id, extractUserId(jwt));
        return ResponseEntity.ok(data);
    }

    /**
     * PUT /admin/companies/{id}
     * Admin sửa thông tin công ty bất kỳ.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResCompanyDTO> adminUpdateCompany(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody ReqUpdateCompanyDTO request) {

        ResCompanyDTO data = companyService.adminUpdateCompany(id, extractUserId(jwt), request);
        return ResponseEntity.ok(data);
    }

    /**
     * DELETE /admin/companies/{id}
     * Xóa mềm công ty.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        companyService.deleteCompany(id, extractUserId(jwt));
        return ResponseEntity.noContent().build();
    }

    private Long extractUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}