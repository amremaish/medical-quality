package com.app.medicalquality.gsr.repository;

import com.app.medicalquality.gsr.entity.AuditLifecycleLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLifecycleLogRepository extends JpaRepository<AuditLifecycleLog, Long> {}
