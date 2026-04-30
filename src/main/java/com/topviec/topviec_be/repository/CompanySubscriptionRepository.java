package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.CompanySubscription;
import com.topviec.topviec_be.enums.services.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanySubscriptionRepository extends JpaRepository<CompanySubscription, Long> {

    List<CompanySubscription> findByCompanyIdAndStatusOrderByCreatedAtDesc(Long companyId, SubscriptionStatus status);

    Optional<CompanySubscription> findFirstByCompanyIdAndStatusOrderByCreatedAtDesc(Long companyId, SubscriptionStatus status);
}
