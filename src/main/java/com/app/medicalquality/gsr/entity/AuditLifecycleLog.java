package com.app.medicalquality.gsr.entity;

import com.app.medicalquality.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_lifecycle_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLifecycleLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(nullable = false, length = 40)
    private String operation;

    @Column(length = 500)
    private String reason;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 100)
    private String username;

    @Column(name = "ip_address", length = 80)
    private String ipAddress;

    @Column(length = 80)
    private String computer;

    @Column(length = 80)
    private String browser;
}
