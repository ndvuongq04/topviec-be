package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.SubscriptionUsage;
import com.topviec.topviec_be.enums.services.SubscriptionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SubscriptionUsageRepository extends JpaRepository<SubscriptionUsage, Long> {

    List<SubscriptionUsage> findByCompanySubscriptionId(Long companySubscriptionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT u FROM SubscriptionUsage u
            JOIN u.companySubscription cs
            WHERE u.companyId = :companyId
            AND u.featureCode = :serviceCode
            AND u.quantityRemaining > 0
            AND cs.status = :status
            AND (cs.expiredAt IS NULL OR cs.expiredAt > :now)
            ORDER BY CASE WHEN cs.expiredAt IS NULL THEN 1 ELSE 0 END ASC,
                     cs.expiredAt ASC,
                     u.createdAt ASC
            """)
    List<SubscriptionUsage> findAvailableForUpdate(
            @Param("companyId") Long companyId,
            @Param("serviceCode") String serviceCode,
            @Param("status") SubscriptionStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @Query("""
            SELECT COUNT(u) FROM SubscriptionUsage u
            JOIN u.companySubscription cs
            WHERE u.companyId = :companyId
            AND u.featureCode = :serviceCode
            AND u.quantityRemaining > 0
            AND cs.status = :status
            AND (cs.expiredAt IS NULL OR cs.expiredAt > :now)
            """)
    long countAvailable(
            @Param("companyId") Long companyId,
            @Param("serviceCode") String serviceCode,
            @Param("status") SubscriptionStatus status,
            @Param("now") LocalDateTime now);
}
