package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

        boolean existsByJobPostIdAndCandidateUserIdAndDeletedAtIsNull(Long jobPostId, Long candidateUserId);

        Optional<Application> findByJobPostIdAndCandidateUserIdAndDeletedAtIsNull(Long jobPostId, Long candidateUserId);

        List<Application> findByJobPostIdAndDeletedAtIsNull(Long jobPostId);

        @Query("""
                        SELECT a FROM Application a
                        LEFT JOIN FETCH a.jobPosting
                        WHERE a.id = :id
                        AND a.candidateUserId = :candidateUserId
                        AND a.deletedAt IS NULL
                        """)
        Optional<Application> findByIdAndCandidateUserId(
                        @Param("id") Long id,
                        @Param("candidateUserId") Long candidateUserId);

        /**
         * Lấy danh sách đơn của UV — hỗ trợ lọc theo status
         * - status null → lấy tất cả
         */
        @Query("""
                        SELECT a FROM Application a
                        LEFT JOIN FETCH a.jobPosting
                        WHERE a.candidateUserId = :candidateUserId
                        AND a.deletedAt IS NULL
                        AND (:status IS NULL OR a.status = :status)
                        ORDER BY a.createdAt DESC
                        """)
        Page<Application> findByCandidate(
                        @Param("candidateUserId") Long candidateUserId,
                        @Param("status") String status,
                        Pageable pageable);

        /**
         * Lấy danh sách đơn theo job post — dành cho recruiter
         * - status null → lấy tất cả
         * - search null → không lọc theo tên/email
         */
        @Query("""
                        SELECT a FROM Application a
                        JOIN a.candidate u
                        WHERE a.jobPostId = :jobPostId
                        AND a.deletedAt IS NULL
                        AND (:status IS NULL OR a.status = :status)
                        AND (:search IS NULL
                            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR EXISTS (
                                SELECT cp FROM CandidateProfile cp
                                WHERE cp.userId = a.candidateUserId
                                AND cp.deletedAt IS NULL
                                AND LOWER(cp.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                            ))
                        ORDER BY a.createdAt DESC
                        """)
        Page<Application> findByJobPost(
                        @Param("jobPostId") Long jobPostId,
                        @Param("status") String status,
                        @Param("search") String search,
                        Pageable pageable);

        // ── Interview support ─────────────────────────────────────────────────────

        List<Application> findByJobPostIdAndStatusAndDeletedAtIsNull(Long jobPostId, String status);

        /**
         * Bulk update status — tránh load toàn bộ entity về rồi save từng cái.
         * Dùng cho startInterviewing (CV_PASSED → INTERVIEWING)
         * và completeRecruitment (remaining → REJECTED).
         */
        @Modifying
        @Query("""
                        UPDATE Application a
                        SET a.status = :newStatus, a.updatedAt = CURRENT_TIMESTAMP
                        WHERE a.jobPostId = :jobPostId
                        AND a.status = :currentStatus
                        AND a.deletedAt IS NULL
                        """)
        int bulkUpdateStatus(
                        @Param("jobPostId") Long jobPostId,
                        @Param("currentStatus") String currentStatus,
                        @Param("newStatus") String newStatus);

        /**
         * Bulk reject tất cả CV chưa terminal (dùng cho completeRecruitment).
         */
        @Modifying
        @Query("""
                        UPDATE Application a
                        SET a.status = 'rejected', a.rejectedAt = CURRENT_TIMESTAMP, a.updatedAt = CURRENT_TIMESTAMP
                        WHERE a.jobPostId = :jobPostId
                        AND a.status NOT IN ('hired', 'rejected', 'withdrawn', 'expired')
                        AND a.deletedAt IS NULL
                        AND a.id NOT IN :excludeIds
                        """)
        int bulkRejectExcluding(
                        @Param("jobPostId") Long jobPostId,
                        @Param("excludeIds") List<Long> excludeIds);

        /** Đếm số hồ sơ (chưa xóa) theo job post id. */
        long countByJobPostIdAndDeletedAtIsNull(Long jobPostId);

        /**
         * Batch đếm số hồ sơ theo nhiều job — tránh N+1 khi hiển thị danh sách.
         * Trả về List<Object[]> gồm [jobPostId, count].
         */
        @Query("""
                        SELECT a.jobPostId, COUNT(a)
                        FROM Application a
                        WHERE a.jobPostId IN :jobPostIds
                        AND a.deletedAt IS NULL
                        GROUP BY a.jobPostId
                        """)
        List<Object[]> countByJobPostIds(@Param("jobPostIds") List<Long> jobPostIds);
}