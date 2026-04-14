package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.CompanyAddon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyAddonRepository extends JpaRepository<CompanyAddon, Long> {
}
