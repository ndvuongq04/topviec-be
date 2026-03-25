package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
         * - keyword null → không lọc theo tên
         */
        @Query("""
                        SELECT a FROM Application a
                        WHERE a.jobPostId = :jobPostId
                        AND a.deletedAt IS NULL
                        AND (:status IS NULL OR a.status = :status)
                        ORDER BY a.createdAt DESC
                        """)
        Page<Application> findByJobPost(
                        @Param("jobPostId") Long jobPostId,
                        @Param("status") String status,
                        Pageable pageable);
}