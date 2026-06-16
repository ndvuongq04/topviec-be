package com.topviec.topviec_be.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Bằng chứng đính kèm cho kháng cáo của NTT.
 */
@Entity
@Table(name = "appeal_evidences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppealEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /** FK → complaint_appeals.id */
    @Column(name = "appeal_id", nullable = false)
    private Long appealId;

    @Column(name = "file_url", nullable = false, length = 512)
    private String fileUrl;

    /**
     * Loại file bằng chứng.
     * Giá trị hợp lệ: {@code image} | {@code pdf}
     */
    @Column(name = "file_type", nullable = false, length = 10)
    private String fileType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
