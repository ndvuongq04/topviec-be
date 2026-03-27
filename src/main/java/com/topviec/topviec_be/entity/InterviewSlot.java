package com.topviec.topviec_be.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_slots", uniqueConstraints = @UniqueConstraint(columnNames = { "round_id", "proposed_at" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "proposed_at", nullable = false)
    private LocalDateTime proposedAt;

    @Column(name = "interview_type", nullable = false, length = 20)
    private String interviewType;

    @Column(name = "location", columnDefinition = "TEXT")
    private String location;

    @Column(name = "meeting_link", columnDefinition = "TEXT")
    private String meetingLink;

    // ── Relations ─────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", insertable = false, updatable = false)
    private InterviewRound round;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
