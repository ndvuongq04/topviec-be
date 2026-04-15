package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.CompanySubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanySubscriptionRepository extends JpaRepository<CompanySubscription, Long> {
}
