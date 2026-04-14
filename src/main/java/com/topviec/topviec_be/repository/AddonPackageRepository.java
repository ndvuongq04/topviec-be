package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.AddonPackage;
import com.topviec.topviec_be.enums.services.AddonPackageGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddonPackageRepository extends JpaRepository<AddonPackage, Long> {
    Page<AddonPackage> findByGroupCode(AddonPackageGroup groupCode, Pageable pageable);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
}
