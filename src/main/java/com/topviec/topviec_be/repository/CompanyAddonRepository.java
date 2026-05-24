package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.CompanyAddon;
import com.topviec.topviec_be.enums.services.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyAddonRepository extends JpaRepository<CompanyAddon, Long> {

    List<CompanyAddon> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<CompanyAddon> findByCompanyIdAndStatusOrderByCreatedAtDesc(Long companyId, SubscriptionStatus status);

    Optional<CompanyAddon> findByOrderId(Long orderId);

    Optional<CompanyAddon> findByOrderIdAndAddonServiceId(Long orderId, Long addonServiceId);

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

