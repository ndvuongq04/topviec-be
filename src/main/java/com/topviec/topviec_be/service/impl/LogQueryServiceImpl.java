package com.topviec.topviec_be.service.impl;

import com.topviec.topviec_be.dto.response.*;
import com.topviec.topviec_be.entity.AdminUser;
import com.topviec.topviec_be.entity.AuditLog;
import com.topviec.topviec_be.entity.BusinessEventLog;
import com.topviec.topviec_be.entity.CompanyMember;
import com.topviec.topviec_be.entity.User;
import com.topviec.topviec_be.enums.logging.LogCategory;
import com.topviec.topviec_be.enums.users.UserType;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.AdminUserRepository;
import com.topviec.topviec_be.repository.AuditLogRepository;
import com.topviec.topviec_be.repository.BusinessEventLogRepository;
import com.topviec.topviec_be.repository.CompanyMemberRepository;
import com.topviec.topviec_be.repository.UserRepository;
import com.topviec.topviec_be.service.LogQueryService;
import com.topviec.topviec_be.util.SecurityUtil;
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
    private final CompanyMemberRepository companyMemberRepository;

    // ═══════════════════════════════════════════════
    // AUDIT LOG
    // ═══════════════════════════════════════════════

    @Override
    public ResultPaginationDTO getAuditLogs(
            List<Long> userIds,
            String action, String category, String severity, String status,
            String keyword, String userRole,
            LocalDate startDate, LocalDate endDate,
            Long companyId,
            Pageable pageable) {

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        // Áp dụng filter theo userRole nếu có
        List<Long> finalUserIds = applyRoleFilter(userIds, userRole);

        // Nếu lọc role mà không tìm thấy user nào → trả về rỗng
        if (finalUserIds != null && finalUserIds.isEmpty()) {
            return buildEmptyPagination(pageable);
        }

        String trimmedKeyword = trimToNull(keyword);

        Page<AuditLog> page = auditLogRepository.findByFilters(
                finalUserIds, action, category, severity, status,
                trimmedKeyword, start, end, pageable);

        // Batch load user info cho tất cả userId trong trang
        List<Long> distinctUserIds = page.getContent().stream()
                .map(AuditLog::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> emailMap = loadEmailMap(distinctUserIds);
        Map<Long, String> roleMap = loadRoleMap(distinctUserIds, companyId);

        List<ResAuditLogDTO> dtos = page.getContent().stream()
                .map(log -> toAuditLogDTO(log, emailMap, roleMap))
                .toList();

        return buildPagination(page, dtos);
    }

    @Override
    public ResAuditLogDetailDTO getAuditLogDetail(Long id) {
        return getAuditLogDetail(id, null);
    }

    @Override
    public ResAuditLogDetailDTO getAuditLogDetail(Long id, Long companyId) {
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Audit log không tồn tại: " + id));

        String email = null;
        String role = null;
        if (log.getUserId() != null) {
            User user = userRepository.findById(log.getUserId()).orElse(null);
            email = user != null ? user.getEmail() : null;
            role = resolveRole(log.getUserId(), user, companyId);
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
            String keyword, String userRole,
            LocalDate startDate, LocalDate endDate,
            Long companyId,
            Pageable pageable) {

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        List<Long> finalUserIds = applyRoleFilter(userIds, userRole);

        if (finalUserIds != null && finalUserIds.isEmpty()) {
            return buildEmptyPagination(pageable);
        }

        String trimmedKeyword = trimToNull(keyword);

        Page<BusinessEventLog> page = businessEventLogRepository.findByFilters(
                finalUserIds, action, category, status,
                trimmedKeyword, start, end, pageable);

        List<Long> distinctUserIds = page.getContent().stream()
                .map(BusinessEventLog::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> emailMap = loadEmailMap(distinctUserIds);
        Map<Long, String> roleMap = loadRoleMap(distinctUserIds, companyId);

        List<ResBusinessEventLogDTO> dtos = page.getContent().stream()
                .map(log -> toBusinessEventLogDTO(log, emailMap, roleMap))
                .toList();

        return buildPagination(page, dtos);
    }

    @Override
    public ResBusinessEventLogDetailDTO getBusinessEventLogDetail(Long id) {
        return getBusinessEventLogDetail(id, null);
    }

    @Override
    public ResBusinessEventLogDetailDTO getBusinessEventLogDetail(Long id, Long companyId) {
        BusinessEventLog log = businessEventLogRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Business event log không tồn tại: " + id));

        String email = null;
        String role = null;
        if (log.getUserId() != null) {
            User user = userRepository.findById(log.getUserId()).orElse(null);
            email = user != null ? user.getEmail() : null;
            role = resolveRole(log.getUserId(), user, companyId);
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
    // STATISTICS — Admin Dashboard
    // ═══════════════════════════════════════════════

    @Override
    public ResAdminLogStatisticsDTO getAdminLogStatistics() {
        // 1. Lấy tất cả userId của admin (không lấy employer/candidate)
        List<Long> adminUserIds = adminUserRepository.findAllActiveAdminUserIds();

        if (adminUserIds.isEmpty()) {
            return ResAdminLogStatisticsDTO.builder()
                    .totalLogs(0)
                    .criticalLogs(0)
                    .systemErrors(0)
                    .activeAdmins(0)
                    .build();
        }

        // 2. Tổng log all-time của tất cả admin (audit + business)
        long totalAudit = auditLogRepository.countByUserIdIn(adminUserIds);
        long totalBusiness = businessEventLogRepository.countByUserIdIn(adminUserIds);
        long totalLogs = totalAudit + totalBusiness;

        // 3. Xác định khoảng thời gian hôm nay
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();

        // 4. Log có mức độ nghiêm trọng (severity = HIGH hoặc CRITICAL) — chỉ audit log có severity
        List<String> criticalSeverities = List.of("HIGH", "CRITICAL");
        long criticalLogs = auditLogRepository
                .countByUserIdsAndSeveritiesAndDateRange(adminUserIds, criticalSeverities, todayStart, todayEnd);

        // 5. Lỗi hệ thống (status = FAILURE)
        long auditErrors = auditLogRepository
                .countByUserIdsAndStatusAndDateRange(adminUserIds, "FAILURE", todayStart, todayEnd);
        long businessErrors = businessEventLogRepository
                .countByUserIdsAndStatusAndDateRange(adminUserIds, "FAILURE", todayStart, todayEnd);
        long systemErrors = auditErrors + businessErrors;

        // 6. Số admin đang active trong hệ thống
        long activeAdmins = adminUserIds.size();

        return ResAdminLogStatisticsDTO.builder()
                .totalLogs(totalLogs)
                .criticalLogs(criticalLogs)
                .systemErrors(systemErrors)
                .activeAdmins(activeAdmins)
                .build();
    }

    // ═══════════════════════════════════════════════
    // STATISTICS — Employer Dashboard
    // ═══════════════════════════════════════════════

    @Override
    public ResEmployerLogStatisticsDTO getEmployerLogStatistics() {
        Long currentUserId = SecurityUtil.getCurrentUserId();

        // 1. Lấy thông tin công ty của user hiện tại
        CompanyMember currentMember = companyMemberRepository.findFirstByUserIdAndStatusAndDeletedAtIsNull(currentUserId, "active")
                .orElseThrow(() -> AppException.forbidden("Bạn không thuộc công ty nào hoặc tài khoản chưa kích hoạt"));

        Long companyId = currentMember.getCompanyId();

        // 2. Lấy danh sách userId của tất cả member active trong công ty
        List<Long> memberUserIds = companyMemberRepository.findAllActiveByCompanyId(companyId)
                .stream()
                .map(CompanyMember::getUserId)
                .toList();

        if (memberUserIds.isEmpty()) {
            return ResEmployerLogStatisticsDTO.builder()
                    .totalActivity(0)
                    .candidateProcessing(0)
                    .dataUpdates(0)
                    .activeMembers(0)
                    .build();
        }

        // 3. Tổng hoạt động (audit + business) của công ty
        long totalAudit = auditLogRepository.countByUserIdIn(memberUserIds);
        long totalBusiness = businessEventLogRepository.countByUserIdIn(memberUserIds);
        long totalActivity = totalAudit + totalBusiness;

        // 4. Xử lý ứng viên (Application, Interview, Talent Pool, CV Management)
        List<String> candidateCategories = List.of(
                LogCategory.APPLICATION.name(),
                LogCategory.APPLICATION_REVIEW.name(),
                LogCategory.INTERVIEW.name(),
                LogCategory.TALENT_POOL.name(),
                LogCategory.CV_MANAGEMENT.name()
        );
        long candidateAudit = auditLogRepository.countByUserIdsAndCategories(memberUserIds, candidateCategories);
        long candidateBusiness = businessEventLogRepository.countByUserIdsAndCategories(memberUserIds, candidateCategories);
        long candidateProcessing = candidateAudit + candidateBusiness;

        // 5. Cập nhật dữ liệu (Job Management, Company Management, Member Management)
        List<String> dataCategories = List.of(
                LogCategory.JOB_MANAGEMENT.name(),
                LogCategory.COMPANY_MANAGEMENT.name(),
                LogCategory.MEMBER_MANAGEMENT.name()
        );
        long dataAudit = auditLogRepository.countByUserIdsAndCategories(memberUserIds, dataCategories);
        long dataBusiness = businessEventLogRepository.countByUserIdsAndCategories(memberUserIds, dataCategories);
        long dataUpdates = dataAudit + dataBusiness;

        // 6. Số thành viên đang hoạt động trong công ty
        long activeMembers = memberUserIds.size();

        return ResEmployerLogStatisticsDTO.builder()
                .totalActivity(totalActivity)
                .candidateProcessing(candidateProcessing)
                .dataUpdates(dataUpdates)
                .activeMembers(activeMembers)
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
     * Nếu companyId != null (context Employer):
     *   → lấy vai trò từ CompanyMember (owner, manager, recruiter, viewer)
     *
     * Nếu companyId == null (context Admin):
     *   → ADMIN  → lấy adminRole từ AdminUser (super_admin, content_moderator, ...)
     *   → EMPLOYER / CANDIDATE → dùng userType làm role
     */
    private Map<Long, String> loadRoleMap(List<Long> userIds, Long companyId) {
        if (userIds == null || userIds.isEmpty()) return Map.of();

        // ── Employer context: lấy vai trò trong công ty ──
        if (companyId != null) {
            return loadMemberRoleMap(userIds, companyId);
        }

        // ── Admin context: lấy vai trò hệ thống ──
        return loadSystemRoleMap(userIds);
    }

    /**
     * Batch load member role từ CompanyMember — dùng cho Employer context.
     */
    private Map<Long, String> loadMemberRoleMap(List<Long> userIds, Long companyId) {
        List<CompanyMember> members = companyMemberRepository
                .findByCompanyIdAndUserIds(companyId, userIds);

        Map<Long, String> roleMap = new HashMap<>();
        for (CompanyMember m : members) {
            String roleName = m.getMemberRole() != null
                    ? m.getMemberRole().getValue()
                    : "member";
            roleMap.put(m.getUserId(), roleName);
        }
        return roleMap;
    }

    /**
     * Batch load system role — dùng cho Admin context.
     */
    private Map<Long, String> loadSystemRoleMap(List<Long> userIds) {
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, String> roleMap = new HashMap<>();

        List<Long> adminUserIds = users.stream()
                .filter(u -> u.getUserType() != null
                        && "ADMIN".equalsIgnoreCase(u.getUserType().name()))
                .map(User::getId)
                .toList();

        Map<Long, String> adminRoleMap = new HashMap<>();
        if (!adminUserIds.isEmpty()) {
            adminUserRepository.findByUserIdIn(adminUserIds)
                    .forEach(a -> adminRoleMap.put(a.getUser().getId(), a.getAdminRole()));
        }

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
    private String resolveRole(Long userId, User user, Long companyId) {
        // Employer context: lấy vai trò trong công ty
        if (companyId != null) {
            return companyMemberRepository.findByCompanyIdAndUserId(companyId, userId)
                    .map(m -> m.getMemberRole() != null ? m.getMemberRole().getValue() : "member")
                    .orElse(null);
        }

        // Admin context
        if (user == null || user.getUserType() == null) return null;

        if ("ADMIN".equalsIgnoreCase(user.getUserType().name())) {
            return adminUserRepository.findActiveByUserId(userId)
                    .map(AdminUser::getAdminRole)
                    .orElse("admin");
        }
        return user.getUserType().getValue();
    }

    /**
     * Áp dụng filter theo userRole (chỉ dùng cho Admin context).
     */
    private List<Long> applyRoleFilter(List<Long> currentUserIds, String userRole) {
        if (userRole == null || userRole.isBlank()) {
            return currentUserIds;
        }

        String role = userRole.trim().toLowerCase();
        List<Long> roleUserIds;

        if ("employer".equals(role) || "candidate".equals(role)) {
            UserType type = UserType.fromValue(role);
            roleUserIds = userRepository.findAllByUserType(type);
        } else {
            roleUserIds = adminUserRepository.findAllByRole(role).stream()
                    .map(a -> a.getUser().getId())
                    .toList();
        }

        if (currentUserIds == null) {
            return roleUserIds;
        }

        Set<Long> currentSet = new HashSet<>(currentUserIds);
        return roleUserIds.stream()
                .filter(currentSet::contains)
                .toList();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
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

    private ResultPaginationDTO buildEmptyPagination(Pageable pageable) {
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                0, 0L);
        return new ResultPaginationDTO(meta, List.of());
    }
}
