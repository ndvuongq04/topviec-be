package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.Skill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    @Query("SELECT s FROM Skill s WHERE " +
            "(:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND s.isActive = true " +
            "ORDER BY s.usageCount DESC")
    Page<Skill> searchSkills(@Param("keyword") String keyword, Pageable pageable);
}