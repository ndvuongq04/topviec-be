package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Tìm kiếm audit log với filter đa điều kiện.
     * Dùng cho cả Admin và Employer.
     *
     * @param userIds    danh sách userId cần lọc (null = all)
     * @param action     tên action (null = all)
     * @param category   category (null = all)
     * @param severity   severity (null = all)
     * @param status     SUCCESS/FAILURE (null = all)
     * @param keyword    tìm kiếm trong action, targetEntity, description (null = all)
     * @param startDate  từ ngày (null = all)
     * @param endDate    đến ngày (null = all)
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:userIds IS NULL OR a.userId IN :userIds)
            AND (:action IS NULL OR a.action = :action)
            AND (:category IS NULL OR a.category = :category)
            AND (:severity IS NULL OR a.severity = :severity)
            AND (:status IS NULL OR a.status = :status)
            AND (:keyword IS NULL
                 OR LOWER(a.action) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(a.targetEntity) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:startDate IS NULL OR a.createdAt >= :startDate)
            AND (:endDate IS NULL OR a.createdAt <= :endDate)
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> findByFilters(
            @Param("userIds") List<Long> userIds,
            @Param("action") String action,
            @Param("category") String category,
            @Param("severity") String severity,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /** Đếm log theo userId — dùng cho thống kê */
    long countByUserId(Long userId);
}
