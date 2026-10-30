package com.app.medicalquality.policy.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "quality_policy_templates")
@Getter
@Setter
@NoArgsConstructor
public class QualityPolicyTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String kind;

    @Column(nullable = false, length = 200)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "document_json", nullable = false)
    private String documentJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;
}
