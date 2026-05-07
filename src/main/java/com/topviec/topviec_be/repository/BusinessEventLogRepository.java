package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.BusinessEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessEventLogRepository extends JpaRepository<BusinessEventLog, Long> {
}
