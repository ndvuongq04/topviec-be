package com.topviec.topviec_be.entity;

import com.topviec.topviec_be.enums.services.AddonPackageGroup;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "addon_packages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddonPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_code", length = 50)
    private AddonPackageGroup groupCode;

    @Column(name = "group_name", length = 100)
    private String groupName;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "code", length = 100, unique = true)
    private String code;

    @Column(name = "price", precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.groupCode != null && this.groupName == null) {
            this.groupName = this.groupCode.getValue();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.groupCode != null) {
            this.groupName = this.groupCode.getValue();
        }
    }
}
