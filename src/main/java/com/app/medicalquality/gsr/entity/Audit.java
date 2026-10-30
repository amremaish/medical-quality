package com.app.medicalquality.gsr.entity;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.common.BaseEntity;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audits")
@Getter
@Setter
@NoArgsConstructor
public class Audit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "department_name", nullable = false, length = 200)
    private String departmentName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "auditor_name", nullable = false, length = 200)
    private String auditorName;

    @Column(name = "audit_date", nullable = false)
    private LocalDate auditDate;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(nullable = false)
    private boolean archived;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_user_id")
    private User submittedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_version_id")
    private QualityPolicyVersion policyVersion;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;
}
