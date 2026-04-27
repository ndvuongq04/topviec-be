package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    Optional<Complaint> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByJobPostIdAndReporterUserIdAndDeletedAtIsNull(Long jobPostId, Long reporterUserId);

    long countByReporterUserIdAndCreatedAtBetween(Long reporterUserId, LocalDateTime start, LocalDateTime end);

    @Query("""
            SELECT c FROM Complaint c
            WHERE c.jobPostId IN :jobPostIds
            AND c.deletedAt IS NULL
            AND (:status IS NULL OR c.status = :status)
            ORDER BY c.createdAt DESC
            """)
    Page<Complaint> findByJobPostIdsAndStatus(
            @Param("jobPostIds") List<Long> jobPostIds,
            @Param("status") String status,
            Pageable pageable);

    @Query("""
            SELECT c FROM Complaint c
            WHERE c.reporterUserId = :userId
            AND c.deletedAt IS NULL
            AND (:status IS NULL OR c.status = :status)
            """)
    Page<Complaint> findMyReports(
            @Param("userId") Long userId,
            @Param("status") String status,
            Pageable pageable);

    @Query(value = """
            SELECT c FROM Complaint c
            JOIN JobPosting j ON c.jobPostId = j.id
            JOIN Company co ON j.companyId = co.id
            LEFT JOIN CandidateProfile cp ON c.reporterUserId = cp.userId
            LEFT JOIN User u ON c.reporterUserId = u.id
            WHERE c.deletedAt IS NULL
            AND (:status IS NULL OR c.status = :status)
            AND (:groupValue IS NULL OR c.violationGroup = :groupValue)
            AND (:complaintType IS NULL OR c.complaintType = :complaintType)
            AND (:fromDate IS NULL OR c.createdAt >= :fromDate)
            AND (:toDate IS NULL OR c.createdAt < :toDate)
            AND (
                :search IS NULL
                OR LOWER(co.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(cp.fullName, u.email)) LIKE LOWER(CONCAT('%', :search, '%'))
                OR CAST(c.id AS string) LIKE CONCAT('%', :search, '%')
            )
            """, countQuery = """
            SELECT COUNT(c) FROM Complaint c
            JOIN JobPosting j ON c.jobPostId = j.id
            JOIN Company co ON j.companyId = co.id
            LEFT JOIN CandidateProfile cp ON c.reporterUserId = cp.userId
            LEFT JOIN User u ON c.reporterUserId = u.id
            WHERE c.deletedAt IS NULL
            AND (:status IS NULL OR c.status = :status)
            AND (:groupValue IS NULL OR c.violationGroup = :groupValue)
            AND (:complaintType IS NULL OR c.complaintType = :complaintType)
            AND (:fromDate IS NULL OR c.createdAt >= :fromDate)
            AND (:toDate IS NULL OR c.createdAt < :toDate)
            AND (
                :search IS NULL
                OR LOWER(co.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(COALESCE(cp.fullName, u.email)) LIKE LOWER(CONCAT('%', :search, '%'))
                OR CAST(c.id AS string) LIKE CONCAT('%', :search, '%')
            )
            """)
    Page<Complaint> findAdminReports(
            @Param("search") String search,
            @Param("status") String status,
            @Param("groupValue") String groupValue,
            @Param("complaintType") String complaintType,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
}
