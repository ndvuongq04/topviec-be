package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.InterviewSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewSlotRepository extends JpaRepository<InterviewSlot, Long> {


    boolean existsByRoundId(Long id);
}
