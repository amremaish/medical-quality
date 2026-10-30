package com.app.medicalquality.finding.entity;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.common.BaseEntity;
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
@Table(name = "capas")
@Getter
@Setter
@NoArgsConstructor
public class Capa extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "finding_id", nullable = false)
    private Finding finding;

    @Column(name = "corrective_action", nullable = false, columnDefinition = "text")
    private String correctiveAction;

    @Column(name = "preventive_action", columnDefinition = "text")
    private String preventiveAction;

    @Column(name = "responsible_person", nullable = false, length = 200)
    private String responsiblePerson;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_version_id")
    private QualityPolicyVersion policyVersion;

    @Column(name = "deadline_days")
    private Integer deadlineDays;

    @Column(name = "root_cause", columnDefinition = "text")
    private String rootCause;

    @Column(name = "extension_count", nullable = false)
    private int extensionCount;

    @Column(name = "extension_reason", length = 500)
    private String extensionReason;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private User verifiedBy;

    @Column(nullable = false)
    private int progress;

    @Column(nullable = false, length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;
}
