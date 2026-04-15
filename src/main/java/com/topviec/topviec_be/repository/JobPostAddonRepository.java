package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.JobPostAddon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobPostAddonRepository extends JpaRepository<JobPostAddon, Long> {
}
