package com.app.medicalquality.policy.entity;

import com.app.medicalquality.auth.entity.User;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "quality_policy_versions")
@Getter
@Setter
@NoArgsConstructor
public class QualityPolicyVersion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_until")
    private LocalDate effectiveUntil;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "document_json", nullable = false)
    private String documentJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "overrides_json", nullable = false)
    private String overridesJson;

    @Column(nullable = false, length = 1000)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
