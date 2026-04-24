package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.PermissionChangeLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermissionChangeLogRepository extends JpaRepository<PermissionChangeLog, Long> {

    List<PermissionChangeLog> findByCompanyIdAndTargetUserIdOrderByCreatedAtDesc(Long companyId, Long targetUserId);

    Page<PermissionChangeLog> findByCompanyIdOrderByCreatedAtDesc(Long companyId, Pageable pageable);
}
