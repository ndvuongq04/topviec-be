package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.AddonPackage;
import com.topviec.topviec_be.enums.services.AddonPackageGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddonPackageRepository extends JpaRepository<AddonPackage, Long> {
    Page<AddonPackage> findByGroupCode(AddonPackageGroup groupCode, Pageable pageable);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
    List<AddonPackage> findByIsActiveTrue();
    List<AddonPackage> findByIsActiveTrueAndGroupCode(AddonPackageGroup groupCode);

    @Query("SELECT ap FROM AddonPackage ap WHERE " +
           "(:groupCode IS NULL OR ap.groupCode = :groupCode) AND " +
           "(:keyword IS NULL OR LOWER(ap.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(ap.code) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<AddonPackage> searchAll(@Param("groupCode") AddonPackageGroup groupCode,
                                 @Param("keyword") String keyword,
                                 Pageable pageable);
}
