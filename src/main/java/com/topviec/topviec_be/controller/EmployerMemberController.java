package com.topviec.topviec_be.controller;

import com.topviec.topviec_be.dto.request.ReqAddMemberDTO;
import com.topviec.topviec_be.dto.request.ReqBatchMemberPermissionDTO;
import com.topviec.topviec_be.dto.request.ReqUpdatePermissionDTO;
import com.topviec.topviec_be.dto.response.ResCompanyMemberDTO;
import com.topviec.topviec_be.dto.response.ResMemberPermissionDetailDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;

import java.util.List;
import com.topviec.topviec_be.service.CompanyMemberService;
import com.topviec.topviec_be.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller dành cho EMPLOYER quản lý thành viên công ty.
 * Base URL: /api/v1/employer/member
 */
@RestController
@RequestMapping("/employer/member")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYER')")
public class EmployerMemberController {

    private final CompanyMemberService companyMemberService;
    private final CompanyService companyService;

    /**
     * POST /employer/member
     * Thêm thành viên mới vào công ty (TH1: email chưa có tài khoản).
     *
     * companyId được lấy tự động từ công ty của người dùng hiện tại.
     */
    @PostMapping
    @PreAuthorize("@companyPerm.hasPermission(authentication, 'member:add')")
    public ResponseEntity<ResCompanyMemberDTO> addMember(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReqAddMemberDTO request) {

        Long inviterUserId = extractUserId(jwt);

        // Lấy companyId từ công ty của inviter
        Long companyId = companyService.getMyCompany(inviterUserId).getId();

        ResCompanyMemberDTO result = companyMemberService.addMember(inviterUserId, companyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PatchMapping("/{targetUserId}/permission")
    @PreAuthorize("@companyPerm.hasPermission(authentication, 'member:permission')")
    public ResponseEntity<ResCompanyMemberDTO> updateMemberPermission(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long targetUserId,
            @Valid @RequestBody ReqUpdatePermissionDTO req) {

        Long inviterId = extractUserId(jwt);
        Long companyId = companyService.getMyCompany(inviterId).getId();

        ResCompanyMemberDTO updatedMember = companyMemberService.updateMemberPermission(inviterId, companyId,
                targetUserId, req);
        return ResponseEntity.ok(updatedMember);
    }

    @GetMapping
    @PreAuthorize("@companyPerm.hasPermission(authentication, 'member:view_activity')")
    public ResponseEntity<ResultPaginationDTO> getMembers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Long userId = extractUserId(jwt);
        Long companyId = companyService.getMyCompany(userId).getId();

        ResultPaginationDTO members = companyMemberService.getMembers(companyId, status, role, keyword, pageable);
        return ResponseEntity.ok(members);
    }

    @PostMapping("/permissions/batch")
    @PreAuthorize("@companyPerm.hasPermission(authentication, 'member:view_activity')")
    public ResponseEntity<List<ResMemberPermissionDetailDTO>> getBatchMemberPermissions(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReqBatchMemberPermissionDTO request) {

        Long userId = extractUserId(jwt);
        Long companyId = companyService.getMyCompany(userId).getId();

        List<ResMemberPermissionDetailDTO> result =
                companyMemberService.getBatchMemberPermissions(companyId, request.getUserIds());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{targetUserId}")
    @PreAuthorize("@companyPerm.hasPermission(authentication, 'member:delete')")
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long targetUserId) {
        Long inviterId = extractUserId(jwt);
        Long companyId = companyService.getMyCompany(inviterId).getId();

        companyMemberService.removeMember(inviterId, companyId, targetUserId);
        return ResponseEntity.noContent().build();
    }

    private Long extractUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
