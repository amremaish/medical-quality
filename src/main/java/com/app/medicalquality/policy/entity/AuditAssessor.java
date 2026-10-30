package com.app.medicalquality.policy.entity;

import com.app.medicalquality.common.BaseEntity;
import com.app.medicalquality.gsr.entity.Audit;
import com.app.medicalquality.hospital.entity.Assessor;
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
@Table(name = "audit_assessors")
@Getter
@Setter
@NoArgsConstructor
public class AuditAssessor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id", nullable = false)
    private Audit audit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessor_id", nullable = false)
    private Assessor assessor;

    @Column(name = "lead_assessor", nullable = false)
    private boolean lead;

    @Column(name = "coi_declared", nullable = false)
    private boolean coiDeclared;
}
