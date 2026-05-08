package com.topviec.topviec_be.service.impl;

import com.topviec.topviec_be.dto.response.*;
import com.topviec.topviec_be.entity.AdminUser;
import com.topviec.topviec_be.entity.AuditLog;
import com.topviec.topviec_be.entity.BusinessEventLog;
import com.topviec.topviec_be.entity.User;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.AdminUserRepository;
import com.topviec.topviec_be.repository.AuditLogRepository;
import com.topviec.topviec_be.repository.BusinessEventLogRepository;
import com.topviec.topviec_be.repository.UserRepository;
import com.topviec.topviec_be.service.LogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LogQueryServiceImpl implements LogQueryService {

    private final AuditLogRepository auditLogRepository;
    private final BusinessEventLogRepository businessEventLogRepository;
    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;

    // ═══════════════════════════════════════════════
    // AUDIT LOG
    // ═══════════════════════════════════════════════

    @Override
    public ResultPaginationDTO getAuditLogs(
            List<Long> userIds,
            String action, String category, String severity, String status,
            LocalDate startDate, LocalDate endDate,
            Pageable pageable) {

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        Page<AuditLog> page = auditLogRepository.findByFilters(
                userIds, action, category, severity, status, start, end, pageable);

        // Batch load user info cho tất cả userId trong trang
        List<Long> distinctUserIds = page.getContent().stream()
                .map(AuditLog::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> emailMap = loadEmailMap(distinctUserIds);
        Map<Long, String> roleMap = loadRoleMap(distinctUserIds);

        List<ResAuditLogDTO> dtos = page.getContent().stream()
                .map(log -> toAuditLogDTO(log, emailMap, roleMap))
                .toList();

        return buildPagination(page, dtos);
    }

    @Override
    public ResAuditLogDetailDTO getAuditLogDetail(Long id) {
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Audit log không tồn tại: " + id));

        String email = null;
        String role = null;
        if (log.getUserId() != null) {
            User user = userRepository.findById(log.getUserId()).orElse(null);
            email = user != null ? user.getEmail() : null;
            role = resolveRole(log.getUserId(), user);
        }

        return ResAuditLogDetailDTO.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .userEmail(email)
                .userRole(role)
                .action(log.getAction())
                .category(log.getCategory())
                .severity(log.getSeverity())
                .targetEntity(log.getTargetEntity())
                .targetId(log.getTargetId())
                .description(log.getDescription())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .status(log.getStatus())
                .durationMs(log.getDurationMs())
                .errorMessage(log.getErrorMessage())
                .createdAt(log.getCreatedAt())
                .build();
    }

    // ═══════════════════════════════════════════════
    // BUSINESS EVENT LOG
    // ═══════════════════════════════════════════════

    @Override
    public ResultPaginationDTO getBusinessEventLogs(
            List<Long> userIds,
            String action, String category, String status,
            LocalDate startDate, LocalDate endDate,
            Pageable pageable) {

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        Page<BusinessEventLog> page = businessEventLogRepository.findByFilters(
                userIds, action, category, status, start, end, pageable);

        List<Long> distinctUserIds = page.getContent().stream()
                .map(BusinessEventLog::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> emailMap = loadEmailMap(distinctUserIds);
        Map<Long, String> roleMap = loadRoleMap(distinctUserIds);

        List<ResBusinessEventLogDTO> dtos = page.getContent().stream()
                .map(log -> toBusinessEventLogDTO(log, emailMap, roleMap))
                .toList();

        return buildPagination(page, dtos);
    }

    @Override
    public ResBusinessEventLogDetailDTO getBusinessEventLogDetail(Long id) {
        BusinessEventLog log = businessEventLogRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Business event log không tồn tại: " + id));

        String email = null;
        String role = null;
        if (log.getUserId() != null) {
            User user = userRepository.findById(log.getUserId()).orElse(null);
            email = user != null ? user.getEmail() : null;
            role = resolveRole(log.getUserId(), user);
        }

        return ResBusinessEventLogDetailDTO.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .userEmail(email)
                .userRole(role)
                .action(log.getAction())
                .category(log.getCategory())
                .targetEntity(log.getTargetEntity())
                .targetId(log.getTargetId())
                .metadata(log.getMetadata())
                .status(log.getStatus())
                .durationMs(log.getDurationMs())
                .createdAt(log.getCreatedAt())
                .build();
    }

    // ═══════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════

    /** Batch load email bằng findAllById — tránh N+1 */
    private Map<Long, String> loadEmailMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();

        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail, (a, b) -> a));
    }

    /**
     * Batch load vai trò người dùng — tránh N+1.
     *
     * Logic:
     *   - ADMIN  → lấy adminRole từ AdminUser (super_admin, content_moderator, ...)
     *   - EMPLOYER / CANDIDATE → dùng userType làm role
     *   - null / unknown → null
     */
    private Map<Long, String> loadRoleMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();

        // Load tất cả User để biết userType
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, String> roleMap = new HashMap<>();

        // Tách ra danh sách admin userId để batch load AdminUser
        List<Long> adminUserIds = users.stream()
                .filter(u -> u.getUserType() != null
                        && "ADMIN".equalsIgnoreCase(u.getUserType().name()))
                .map(User::getId)
                .toList();

        // Batch load admin roles
        Map<Long, String> adminRoleMap = new HashMap<>();
        if (!adminUserIds.isEmpty()) {
            adminUserRepository.findByUserIdIn(adminUserIds)
                    .forEach(a -> adminRoleMap.put(a.getUser().getId(), a.getAdminRole()));
        }

        // Gán role cho từng user
        for (User user : users) {
            if (user.getUserType() == null) {
                roleMap.put(user.getId(), null);
            } else if ("ADMIN".equalsIgnoreCase(user.getUserType().name())) {
                roleMap.put(user.getId(), adminRoleMap.getOrDefault(user.getId(), "admin"));
            } else {
                roleMap.put(user.getId(), user.getUserType().getValue());
            }
        }

        return roleMap;
    }

    /**
     * Resolve role cho 1 user đơn lẻ (dùng cho API xem chi tiết).
     */
    private String resolveRole(Long userId, User user) {
        if (user == null || user.getUserType() == null) return null;

        if ("ADMIN".equalsIgnoreCase(user.getUserType().name())) {
            return adminUserRepository.findActiveByUserId(userId)
                    .map(AdminUser::getAdminRole)
                    .orElse("admin");
        }
        return user.getUserType().getValue();
    }

    private ResAuditLogDTO toAuditLogDTO(AuditLog log, Map<Long, String> emailMap, Map<Long, String> roleMap) {
        return ResAuditLogDTO.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .userEmail(log.getUserId() != null ? emailMap.get(log.getUserId()) : null)
                .userRole(log.getUserId() != null ? roleMap.get(log.getUserId()) : null)
                .action(log.getAction())
                .category(log.getCategory())
                .severity(log.getSeverity())
                .targetEntity(log.getTargetEntity())
                .targetId(log.getTargetId())
                .status(log.getStatus())
                .durationMs(log.getDurationMs())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private ResBusinessEventLogDTO toBusinessEventLogDTO(BusinessEventLog log, Map<Long, String> emailMap, Map<Long, String> roleMap) {
        return ResBusinessEventLogDTO.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .userEmail(log.getUserId() != null ? emailMap.get(log.getUserId()) : null)
                .userRole(log.getUserId() != null ? roleMap.get(log.getUserId()) : null)
                .action(log.getAction())
                .category(log.getCategory())
                .targetEntity(log.getTargetEntity())
                .targetId(log.getTargetId())
                .status(log.getStatus())
                .durationMs(log.getDurationMs())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private <T> ResultPaginationDTO buildPagination(Page<T> page, Object data) {
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta(
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements());
        return new ResultPaginationDTO(meta, data);
    }
}
