package com.topviec.topviec_be.repository;

import com.topviec.topviec_be.entity.JobPosting;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long>,
        JpaSpecificationExecutor<JobPosting> {

    Optional<JobPosting> findByIdAndDeletedAtIsNull(Long id);

    /** Dùng cho restore: tìm tin kể cả đã bị xóa mềm. */
    Optional<JobPosting> findById(Long id);

    boolean existsBySlugAndDeletedAtIsNull(String slug);

    boolean existsBySlugAndIdNotAndDeletedAtIsNull(String slug, Long id);

    @Modifying
    @Query("UPDATE JobPosting j SET j.viewCount = j.viewCount + 1 WHERE j.id = :id")
    void incrementViewCount(@Param("id") Long id);

    // ─── HOT service queries ────────────────────────────────────────────────

    /** Đếm số tin HOT đang active — dùng kiểm tra slot */
    @Query("SELECT COUNT(j) FROM JobPosting j WHERE j.isHot = true AND j.hotExpiredAt > :now AND j.deletedAt IS NULL")
    long countActiveHotPosts(@Param("now") LocalDateTime now);

    /** Lấy danh sách tin HOT cho trang chủ (phân trang) */
    @Query("SELECT j FROM JobPosting j WHERE j.isHot = true AND j.hotExpiredAt > :now "
            + "AND j.deletedAt IS NULL AND j.status = 'published' ORDER BY j.hotStartedAt DESC")
    List<JobPosting> findActiveHotPosts(@Param("now") LocalDateTime now, Pageable pageable);

    /** Tìm tin HOT đã hết hạn — dùng cho scheduler gỡ HOT */
    @Query("SELECT j FROM JobPosting j WHERE j.isHot = true AND j.hotExpiredAt <= :now AND j.deletedAt IS NULL")
    List<JobPosting> findExpiredHotPosts(@Param("now") LocalDateTime now);
}