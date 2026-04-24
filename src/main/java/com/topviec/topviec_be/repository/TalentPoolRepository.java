package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.TalentPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TalentPoolRepository extends JpaRepository<TalentPool, Long> {

    boolean existsByCompanyIdAndCandidateUserId(Long companyId, Long candidateUserId);
}
