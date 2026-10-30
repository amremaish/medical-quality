package com.app.medicalquality.policy.repository;

import com.app.medicalquality.policy.entity.AuditAssessor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditAssessorRepository extends JpaRepository<AuditAssessor, Long> {

    @EntityGraph(attributePaths = {"assessor", "audit", "audit.department"})
    List<AuditAssessor> findByAudit_IdAndDeletedAtIsNull(Long a_auditId);

    @EntityGraph(attributePaths = {"assessor", "audit", "audit.department"})
    List<AuditAssessor> findByAssessor_IdAndDeletedAtIsNull(Long a_assessorId);

    Optional<AuditAssessor> findByAudit_IdAndAssessor_IdAndDeletedAtIsNull(Long a_auditId, Long a_assessorId);

    @EntityGraph(attributePaths = {"audit", "audit.department"})
    List<AuditAssessor> findByAudit_Department_Organization_IdAndDeletedAtIsNull(Long a_organizationId);
}
