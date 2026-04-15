package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicePackageRepository extends JpaRepository<ServicePackage, Long> {
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
    List<ServicePackage> findByIsActiveTrueOrderBySortOrderAsc();
}
