package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.ViolationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ViolationLogRepository extends JpaRepository<ViolationLog, Long> {
}
