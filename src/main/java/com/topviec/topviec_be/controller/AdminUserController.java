package com.topviec.topviec_be.controller;

import com.topviec.topviec_be.dto.request.ReqCreateAdmin;
import com.topviec.topviec_be.dto.request.ReqUpdateAdmin;
import com.topviec.topviec_be.dto.response.ResAdminUser;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import com.topviec.topviec_be.enums.adminUsers.AdminRoleConstants;
import com.topviec.topviec_be.service.AdminUserService;
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
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

        private final AdminUserService adminUserService;

        /**
         * POST /api/admin/users
         * Chỉ super_admin được tạo admin mới
         */
        @PostMapping
        @PreAuthorize("hasRole('ADMIN') and @adminSecurity.hasRole(authentication, '" + AdminRoleConstants.SUPER_ADMIN
                        + "')")
        public ResponseEntity<ResAdminUser> createAdmin(
                        @AuthenticationPrincipal Jwt jwt,
                        @Valid @RequestBody ReqCreateAdmin request) {

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(adminUserService.createAdmin(request, extractUserId(jwt)));
        }

        /**
         * GET /admin/users?keyword=nguyen&adminRole=content_moderator&page=0&size=10
         * Lấy danh sách admin, hỗ trợ tìm theo tên và lọc theo role
         * - keyword : tìm kiếm theo fullName (optional)
         * - adminRole : lọc theo role (optional) — "super_admin" | "content_moderator"
         * | "support_admin" | "finance_admin"
         */
        @GetMapping
        @PreAuthorize("hasRole('ADMIN') and @adminSecurity.hasAnyRole(authentication, '"
                        + AdminRoleConstants.SUPER_ADMIN + "', '"
                        + AdminRoleConstants.CONTENT_MODERATOR + "', '"
                        + AdminRoleConstants.SUPPORT_ADMIN + "')")
        public ResponseEntity<ResultPaginationDTO> getAllAdmins(
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) String adminRole,
                        @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

                return ResponseEntity.ok(adminUserService.getAllAdmins(keyword, adminRole, pageable));
        }

        /**
         * GET /api/admin/users/{id}
         * super_admin + content_moderator + support_admin xem được chi tiết
         */
        @GetMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN') and @adminSecurity.hasAnyRole(authentication, '"
                        + AdminRoleConstants.SUPER_ADMIN
                        + "', '" + AdminRoleConstants.CONTENT_MODERATOR + "', '" + AdminRoleConstants.SUPPORT_ADMIN
                        + "')")
        public ResponseEntity<ResAdminUser> getAdminById(@PathVariable Long id) {
                return ResponseEntity.ok(adminUserService.getAdminById(id));
        }

        /**
         * PUT /api/admin/users/{id}
         * Chỉ super_admin được cập nhật thông tin admin
         */
        @PutMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN') and @adminSecurity.hasRole(authentication, '" + AdminRoleConstants.SUPER_ADMIN
                        + "')")
        public ResponseEntity<ResAdminUser> updateAdmin(
                        @AuthenticationPrincipal Jwt jwt,
                        @PathVariable Long id,
                        @Valid @RequestBody ReqUpdateAdmin request) {

                return ResponseEntity.ok(adminUserService.updateAdmin(id, request, extractUserId(jwt)));
        }

        /**
         * PATCH /api/admin/users/{id}/toggle-active
         * Chỉ super_admin được bật/tắt tài khoản
         */
        @PatchMapping("/{id}/toggle-active")
        @PreAuthorize("hasRole('ADMIN') and @adminSecurity.hasRole(authentication, '" + AdminRoleConstants.SUPER_ADMIN
                        + "')")
        public ResponseEntity<ResAdminUser> toggleActive(
                        @AuthenticationPrincipal Jwt jwt,
                        @PathVariable Long id) {

                return ResponseEntity.ok(adminUserService.toggleActive(id, extractUserId(jwt)));
        }

        /**
         * DELETE /api/admin/users/{id}
         * Chỉ super_admin được xóa admin
         */
        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN') and @adminSecurity.hasRole(authentication, '" + AdminRoleConstants.SUPER_ADMIN
                        + "')")
        public ResponseEntity<Void> deleteAdmin(
                        @AuthenticationPrincipal Jwt jwt,
                        @PathVariable Long id) {

                adminUserService.deleteAdmin(id, extractUserId(jwt));
                return ResponseEntity.noContent().build();
        }

        private Long extractUserId(Jwt jwt) {
                return Long.parseLong(jwt.getSubject());
        }
}