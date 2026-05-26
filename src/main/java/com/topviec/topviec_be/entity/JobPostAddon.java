package com.topviec.topviec_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.topviec.topviec_be.enums.services.JobPostAddonStatus;
import com.topviec.topviec_be.enums.services.ServiceUsageSourceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_post_addons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPostAddon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_posting_id", nullable = false)
    private Long jobPostingId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_posting_id", insertable = false, updatable = false)
    private JobPosting jobPosting;

    @Column(name = "company_addon_id")
    private Long companyAddonId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_addon_id", insertable = false, updatable = false)
    private CompanyAddon companyAddon;

    @Column(name = "addon_service_id")
    private Long addonServiceId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addon_service_id", insertable = false, updatable = false)
    private AddonService addonService;

    @Column(name = "subscription_usage_id")
    private Long subscriptionUsageId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_usage_id", insertable = false, updatable = false)
    private SubscriptionUsage subscriptionUsage;

    @Column(name = "service_code", length = 100)
    private String serviceCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_source_type", length = 30)
    private ServiceUsageSourceType usageSourceType;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private JobPostAddonStatus status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = JobPostAddonStatus.ACTIVE;
        }
    }
}
