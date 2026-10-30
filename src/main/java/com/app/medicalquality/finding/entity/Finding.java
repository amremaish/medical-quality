package com.app.medicalquality.finding.entity;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.common.BaseEntity;
import com.app.medicalquality.gsr.entity.Audit;
import com.app.medicalquality.hospital.entity.Department;
import com.app.medicalquality.policy.entity.QualityPolicyVersion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "findings")
@Getter
@Setter
@NoArgsConstructor
public class Finding extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_id")
    private Audit audit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "gsr_code", nullable = false, length = 20)
    private String gsrCode;

    @Column(name = "element_index", nullable = false)
    private int elementIndex;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel;

    @Column(name = "canonical_severity", length = 40)
    private String canonicalSeverity;

    @Column(nullable = false)
    private boolean escalated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_version_id")
    private QualityPolicyVersion policyVersion;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(nullable = false, length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;
}
