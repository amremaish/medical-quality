package com.app.medicalquality.policy.entity;

import com.app.medicalquality.common.BaseEntity;
import com.app.medicalquality.organization.entity.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quality_policy_violations")
@Getter
@Setter
@NoArgsConstructor
public class QualityPolicyViolation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private QualityPolicyVersion version;

    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private long entityId;

    @Column(name = "setting_key", nullable = false, length = 160)
    private String settingKey;

    @Column(nullable = false, length = 500)
    private String detail;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
