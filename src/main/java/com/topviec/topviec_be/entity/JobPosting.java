package com.topviec.topviec_be.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.topviec.topviec_be.enums.jobs.CloseReason;
import com.topviec.topviec_be.enums.jobs.JobPostStatus;
import com.topviec.topviec_be.enums.jobs.RejectionReason;
import com.topviec.topviec_be.enums.jobs.WorkType;

@Entity
@Table(name = "job_postings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "requirements", nullable = false, columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "benefits", columnDefinition = "TEXT")
    private String benefits;

    @Column(name = "industry_id", nullable = false)
    private Long industryId;

    @Column(name = "level_id", nullable = false)
    private Long levelId;

    @Column(name = "experience_years_min", nullable = false)
    private Integer experienceYearsMin;

    @Column(name = "experience_years_max")
    private Integer experienceYearsMax;

    @Column(name = "salary_min")
    private Long salaryMin;

    @Column(name = "salary_max")
    private Long salaryMax;

    @Column(name = "salary_negotiable", nullable = false)
    private Boolean salaryNegotiable;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_type", nullable = false, length = 20)
    @Builder.Default
    private WorkType workType = WorkType.FULL_TIME;

    @Column(name = "headcount", nullable = false)
    private Integer headcount;

    @Column(name = "deadline", nullable = false)
    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private JobPostStatus status = JobPostStatus.DRAFT;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "close_reason")
    private CloseReason closeReason;

    @Column(name = "close_note", columnDefinition = "TEXT")
    private String closeNote;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured;

    @Column(name = "is_urgent", nullable = false)
    private Boolean isUrgent;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "edit_count", nullable = false)
    private Integer editCount;

    @Column(name = "refreshed_at")
    private LocalDateTime refreshedAt;

    @Column(name = "moderation_note", columnDefinition = "TEXT")
    private String moderationNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_reason")
    private RejectionReason rejectionReason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Vector embedding cho hệ thống gợi ý việc làm (pgvector - 1536 chiều)
    // Yêu cầu extension pgvector trong PostgreSQL và dependency hibernate-vector
    // @Column(name = "embedding", columnDefinition = "vector(1536)")
    // @JdbcTypeCode(SqlTypes.VECTOR)
    // private float[] embedding;

    // Relationships (comment để mở sau khi có đủ các entity liên quan)
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "company_id", insertable = false, updatable = false)
    // private Company company;

    @JsonIgnore
    @OneToMany(mappedBy = "jobPosting", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<JobPostSkill> skills = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "jobPosting", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<JobPostLocation> locations = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.viewCount == null)
            this.viewCount = 0;
        if (this.editCount == null)
            this.editCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}