package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.AppealEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppealEvidenceRepository extends JpaRepository<AppealEvidence, Long> {

    List<AppealEvidence> findByAppealId(Long appealId);

    List<AppealEvidence> findByAppealIdIn(List<Long> appealIds);
}
