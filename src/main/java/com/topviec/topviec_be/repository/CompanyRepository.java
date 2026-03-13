package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findBySlug(String slug);

    Optional<Company> findByCreatedBy(Long userId);

    boolean existsBySlug(String slug);

    boolean existsByTaxCode(String taxCode);

    boolean existsByCreatedBy(Long userId);

    // Dùng cho admin: lấy danh sách công ty theo verificationStatus
    @Query("SELECT c FROM Company c WHERE c.verificationStatus = :status")
    Page<Company> findAllByVerificationStatus(@Param("status") String status, Pageable pageable);

    // Dùng cho admin: lấy danh sách công ty theo status
    @Query("SELECT c FROM Company c WHERE c.status = :status")
    Page<Company> findAllByStatus(@Param("status") String status, Pageable pageable);
}