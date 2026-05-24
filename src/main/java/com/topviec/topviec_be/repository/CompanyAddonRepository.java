package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.CompanyAddon;
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
import java.util.Optional;

@Repository
public interface CompanyAddonRepository extends JpaRepository<CompanyAddon, Long> {

    List<CompanyAddon> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<CompanyAddon> findByCompanyIdAndStatusOrderByCreatedAtDesc(Long companyId, SubscriptionStatus status);

    Optional<CompanyAddon> findByOrderId(Long orderId);

    Optional<CompanyAddon> findByOrderIdAndAddonServiceId(Long orderId, Long addonServiceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ca FROM CompanyAddon ca
            JOIN ca.addonService asv
            JOIN asv.service s
            WHERE ca.id = :id
            """)
    Optional<CompanyAddon> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ca FROM CompanyAddon ca
            JOIN ca.addonService asv
            JOIN asv.service s
            WHERE ca.companyId = :companyId
            AND s.code = :serviceCode
            AND ca.status = :status
            AND ca.quantityRemaining > 0
            AND (ca.expiredAt IS NULL OR ca.expiredAt > :now)
            ORDER BY CASE WHEN ca.expiredAt IS NULL THEN 1 ELSE 0 END ASC,
                     ca.expiredAt ASC,
                     ca.createdAt ASC
            """)
    List<CompanyAddon> findAvailableByServiceCodeForUpdate(
            @Param("companyId") Long companyId,
            @Param("serviceCode") String serviceCode,
            @Param("status") SubscriptionStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @Query("""
            SELECT COUNT(ca) FROM CompanyAddon ca
            JOIN ca.addonService asv
            JOIN asv.service s
            WHERE ca.companyId = :companyId
            AND s.code = :serviceCode
            AND ca.status = :status
            AND ca.quantityRemaining > 0
            AND (ca.expiredAt IS NULL OR ca.expiredAt > :now)
            """)
    long countAvailableByServiceCode(
            @Param("companyId") Long companyId,
            @Param("serviceCode") String serviceCode,
            @Param("status") SubscriptionStatus status,
            @Param("now") LocalDateTime now);

    @Query("""
            SELECT ca FROM CompanyAddon ca
            WHERE ca.status = :status
            AND ca.expiredAt IS NOT NULL
            AND ca.expiredAt <= :now
            """)
    List<CompanyAddon> findExpiredActiveAddons(
            @Param("status") SubscriptionStatus status,
            @Param("now") LocalDateTime now);

    /**
     * Tìm CompanyAddon active đầu tiên (FIFO) theo addon service code.
     * Dùng cho dịch vụ trừ lượt (ví dụ: CV_SEARCH_BASIC).
     * Ưu tiên dùng lượt mua cũ nhất trước.
     */
    @Query("""
            SELECT ca FROM CompanyAddon ca
            JOIN ca.addonService asv
            WHERE ca.companyId = :companyId
            AND asv.code = :addonCode
            AND ca.status = :status
            AND ca.quantityRemaining > 0
            AND (ca.expiredAt IS NULL OR ca.expiredAt > CURRENT_TIMESTAMP)
            ORDER BY ca.createdAt ASC
            LIMIT 1
            """)
    Optional<CompanyAddon> findFirstActiveByCompanyIdAndAddonCode(
            @Param("companyId") Long companyId,
            @Param("addonCode") String addonCode,
            @Param("status") SubscriptionStatus status);
}

