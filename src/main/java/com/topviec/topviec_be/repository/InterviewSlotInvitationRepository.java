package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.InterviewSlotInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewSlotInvitationRepository extends JpaRepository<InterviewSlotInvitation, Long> {

    List<InterviewSlotInvitation> findByApplicationIdAndRoundIdOrderByBatchNumberAsc(Long applicationId, Long roundId);

    /** Lấy batch number lớn nhất hiện tại của 1 round để tính batch tiếp theo. */
    @Query("SELECT COALESCE(MAX(i.batchNumber), 0) FROM InterviewSlotInvitation i WHERE i.roundId = :roundId")
    int findMaxBatchNumber(@Param("roundId") Long roundId);

    /** Lấy tất cả invitation của 1 round (để map batchNumber → deadline). */
    List<InterviewSlotInvitation> findByRoundId(Long roundId);

    /** Lấy invitation mới nhất (batch lớn nhất) của 1 UV trong 1 round. */
    Optional<InterviewSlotInvitation> findTopByApplicationIdAndRoundIdOrderByBatchNumberDesc(Long applicationId, Long roundId);
}
