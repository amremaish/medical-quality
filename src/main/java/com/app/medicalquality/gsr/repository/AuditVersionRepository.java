package com.app.medicalquality.gsr.repository;

import com.app.medicalquality.gsr.entity.AuditVersion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditVersionRepository extends JpaRepository<AuditVersion, Long> {

    List<AuditVersion> findByAuditIdAndDeletedAtIsNullOrderByVersionNumberDesc(Long a_auditId);

    int countByAuditId(Long a_auditId);
}
