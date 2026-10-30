package com.app.medicalquality.finding.repository;

import com.app.medicalquality.finding.entity.Finding;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FindingRepository extends JpaRepository<Finding, Long> {

    @EntityGraph(attributePaths = {"audit", "department"})
    List<Finding> findByDeletedAtIsNullOrderByCreatedAtDesc();

    List<Finding> findByAuditIdAndDeletedAtIsNull(Long a_auditId);

    Optional<Finding> findByIdAndDeletedAtIsNull(Long a_id);

    Optional<Finding> findFirstByAuditIdAndGsrCodeAndStatusAndDeletedAtIsNull(
            Long a_auditId, String a_gsrCode, String a_status);

    long countByDepartment_Organization_IdAndDeletedAtIsNullAndStatus(Long a_organizationId, String a_status);
}
