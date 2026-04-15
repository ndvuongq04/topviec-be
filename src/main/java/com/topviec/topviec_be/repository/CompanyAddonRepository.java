package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.CompanyAddon;
import com.topviec.topviec_be.enums.services.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyAddonRepository extends JpaRepository<CompanyAddon, Long> {

    List<CompanyAddon> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<CompanyAddon> findByCompanyIdAndStatusOrderByCreatedAtDesc(Long companyId, SubscriptionStatus status);
}
