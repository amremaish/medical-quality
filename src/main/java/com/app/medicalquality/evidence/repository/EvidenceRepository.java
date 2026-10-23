package com.app.medicalquality.evidence.repository;

import com.app.medicalquality.evidence.entity.Evidence;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    @EntityGraph(attributePaths = {"document", "audit", "department", "finding"})
    List<Evidence> findByDeletedAtIsNullOrderByCreatedAtDesc();

    List<Evidence> findByAuditIdAndDeletedAtIsNull(Long a_auditId);
}
