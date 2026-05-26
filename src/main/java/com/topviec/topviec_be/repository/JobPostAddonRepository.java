package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.JobPostAddon;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobPostAddonRepository extends JpaRepository<JobPostAddon, Long> {

    @Query("SELECT COUNT(jpa) FROM JobPostAddon jpa " +
           "LEFT JOIN jpa.addonService asv " +
           "LEFT JOIN asv.service s " +
           "WHERE jpa.jobPostingId = :jobPostingId " +
           "AND (jpa.serviceCode = :serviceCode OR s.code = :serviceCode) " +
           "AND jpa.status = 'ACTIVE' " +
           "AND (jpa.expiredAt IS NULL OR jpa.expiredAt > :now)")
    long countActiveAddonForJob(@Param("jobPostingId") Long jobPostingId,
                                @Param("serviceCode") String serviceCode,
                                @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(jpa) FROM JobPostAddon jpa " +
           "LEFT JOIN jpa.addonService asv " +
           "LEFT JOIN asv.service s " +
           "JOIN jpa.jobPosting j " +
           "WHERE (jpa.serviceCode = :serviceCode OR s.code = :serviceCode) " +
           "AND jpa.status = 'ACTIVE' " +
           "AND (jpa.expiredAt IS NULL OR jpa.expiredAt > :now) " +
           "AND j.deletedAt IS NULL " +
           "AND j.status = 'published'")
    long countActiveGlobalAddons(@Param("serviceCode") String serviceCode,
                                 @Param("now") LocalDateTime now);

    @Query("SELECT jpa.jobPosting FROM JobPostAddon jpa " +
           "LEFT JOIN jpa.addonService asv " +
           "LEFT JOIN asv.service s " +
           "WHERE (jpa.serviceCode = :serviceCode OR s.code = :serviceCode) " +
           "AND jpa.status = 'ACTIVE' " +
           "AND (jpa.expiredAt IS NULL OR jpa.expiredAt > :now) " +
           "AND jpa.jobPosting.deletedAt IS NULL " +
           "AND jpa.jobPosting.status = 'published' " +
           "ORDER BY jpa.startedAt DESC")
    List<com.topviec.topviec_be.entity.JobPosting> findActiveGlobalAddonsPosts(
            @Param("serviceCode") String serviceCode,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @Query("SELECT jpa FROM JobPostAddon jpa " +
           "LEFT JOIN jpa.addonService asv " +
           "LEFT JOIN asv.service s " +
           "WHERE (jpa.serviceCode = :serviceCode OR s.code = :serviceCode) " +
           "AND jpa.status = 'ACTIVE' " +
           "AND jpa.expiredAt <= :now")
    List<JobPostAddon> findExpiredAddons(@Param("serviceCode") String serviceCode,
                                         @Param("now") LocalDateTime now);
}
