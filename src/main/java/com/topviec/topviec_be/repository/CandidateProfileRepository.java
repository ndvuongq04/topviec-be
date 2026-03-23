package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {

    Optional<CandidateProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE CandidateProfile c SET c.profileCompletionPct = :pct WHERE c.userId = :userId")
    void updateProfileCompletionPct(@Param("userId") Long userId, @Param("pct") int pct);
}