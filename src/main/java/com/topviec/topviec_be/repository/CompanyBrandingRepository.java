package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.CompanyBranding;
import com.topviec.topviec_be.enums.services.BrandingAddonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CompanyBrandingRepository extends JpaRepository<CompanyBranding, Long> {

    @Query("SELECT COUNT(cb) FROM CompanyBranding cb " +
           "WHERE cb.companyId = :companyId " +
           "AND cb.serviceCode = :serviceCode " +
           "AND cb.status = 'ACTIVE' " +
           "AND (cb.expiredAt IS NULL OR cb.expiredAt > :now)")
    long countActiveForCompany(@Param("companyId") Long companyId,
                               @Param("serviceCode") String serviceCode,
                               @Param("now") LocalDateTime now);

    List<CompanyBranding> findByServiceCodeAndStatusAndExpiredAtBefore(
            String serviceCode, BrandingAddonStatus status, LocalDateTime now);
}
