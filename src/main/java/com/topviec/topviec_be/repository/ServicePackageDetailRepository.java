package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.ServicePackageDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicePackageDetailRepository extends JpaRepository<ServicePackageDetail, Long> {
    List<ServicePackageDetail> findByServicePackageId(Long servicePackageId);
    Optional<ServicePackageDetail> findByServicePackageIdAndServiceId(Long servicePackageId, Long serviceId);
    boolean existsByServicePackageIdAndServiceId(Long servicePackageId, Long serviceId);
    void deleteByServicePackageId(Long servicePackageId);
}
