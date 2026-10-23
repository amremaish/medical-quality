package com.app.medicalquality.finding.entity;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.common.BaseEntity;
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

    @Column(nullable = false)
    private int progress;

    @Column(nullable = false, length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;
}
