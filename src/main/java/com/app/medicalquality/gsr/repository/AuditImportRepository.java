package com.app.medicalquality.gsr.repository;

import com.app.medicalquality.gsr.entity.AuditImport;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditImportRepository extends JpaRepository<AuditImport, Long> {

    Optional<AuditImport> findByIdAndDeletedAtIsNull(Long a_id);

    List<AuditImport> findByDeletedAtIsNullOrderByCreatedAtDesc();

    Optional<AuditImport> findFirstByAuditIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long a_auditId);
}
