package com.topviec.topviec_be.controller;

import com.topviec.topviec_be.dto.response.*;
import com.topviec.topviec_be.enums.adminUsers.AdminRoleConstants;
import com.topviec.topviec_be.enums.users.UserType;
import com.topviec.topviec_be.repository.UserRepository;
import com.topviec.topviec_be.service.LogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Admin Log Controller — xem log hoạt động hệ thống.
 *
 * Phạm vi: Admin thấy log của admin khác + chính mình + toàn hệ thống (trừ NTD).
 * Tức là lấy log theo userId mà user_type IN (ADMIN, CANDIDATE) hoặc userId IS NULL (system).
 *
 * Filter:
 *  - userId: lọc theo userId cụ thể
 *  - action, category, severity, status: filter trực tiếp
 *  - startDate, endDate: lọc theo khoảng thời gian
 */
@RestController
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLogController {

    private final LogQueryService logQueryService;
    private final UserRepository userRepository;

    // ═══════════════════════════════════════════════
    // AUDIT LOG
    // ═══════════════════════════════════════════════

    /**
     * Danh sách audit log — thông tin cơ bản, phân trang + filter.
     * Admin xem log trừ NTD → truyền userIds = tất cả admin + candidate + null.
     */
    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN') and @adminSecurity.hasAnyRole(authentication, '"
            + AdminRoleConstants.SUPER_ADMIN + "')")
    public ResponseEntity<ResultPaginationDTO> getAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {

        // Nếu chỉ định userId cụ thể → lọc theo userId đó
        // Nếu không chỉ định → null (lấy tất cả, không giới hạn theo NTD ở tầng query
        // vì audit log ít khi chứa employer-only actions)
        List<Long> userIds = userId != null ? List.of(userId) : null;

        ResultPaginationDTO result = logQueryService.getAuditLogs(
                userIds, action, category, severity, status, startDate, endDate, pageable);

        return ResponseEntity.ok(result);
    }

    /**
     * Chi tiết 1 audit log — bao gồm IP, user agent, error message.
     */
    @GetMapping("/audit/{id}")
    @PreAuthorize("hasRole('ADMIN') and @adminSecurity.hasAnyRole(authentication, '"
            + AdminRoleConstants.SUPER_ADMIN + "')")
    public ResponseEntity<ResAuditLogDetailDTO> getAuditLogDetail(@PathVariable Long id) {
        return ResponseEntity.ok(logQueryService.getAuditLogDetail(id));
    }

    // ═══════════════════════════════════════════════
    // BUSINESS EVENT LOG
    // ═══════════════════════════════════════════════

    /**
     * Danh sách business event log — phân trang + filter.
     */
    @GetMapping("/business")
    @PreAuthorize("hasRole('ADMIN') and @adminSecurity.hasAnyRole(authentication, '"
            + AdminRoleConstants.SUPER_ADMIN + "')")
    public ResponseEntity<ResultPaginationDTO> getBusinessEventLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {

        List<Long> userIds = userId != null ? List.of(userId) : null;

        ResultPaginationDTO result = logQueryService.getBusinessEventLogs(
                userIds, action, category, status, startDate, endDate, pageable);

        return ResponseEntity.ok(result);
    }

    /**
     * Chi tiết 1 business event log — bao gồm metadata JSON.
     */
    @GetMapping("/business/{id}")
    @PreAuthorize("hasRole('ADMIN') and @adminSecurity.hasAnyRole(authentication, '"
            + AdminRoleConstants.SUPER_ADMIN + "')")
    public ResponseEntity<ResBusinessEventLogDetailDTO> getBusinessEventLogDetail(@PathVariable Long id) {
        return ResponseEntity.ok(logQueryService.getBusinessEventLogDetail(id));
    }
}
