package com.app.medicalquality.gsr.repository;

import com.app.medicalquality.gsr.entity.Audit;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<Audit, Long> {

    @EntityGraph(attributePaths = {"department", "createdBy"})
    Optional<Audit> findByIdAndDeletedAtIsNull(Long a_id);

    @EntityGraph(attributePaths = {"department", "createdBy"})
    List<Audit> findByDeletedAtIsNullOrderByAuditDateDesc();

    long countByDepartment_Organization_IdAndDeletedAtIsNullAndStatusIn(
            Long a_organizationId, Collection<String> a_statuses);

    long countByDepartment_Organization_IdAndDeletedAtIsNullAndAuditDateGreaterThanEqualAndAuditDateLessThan(
            Long a_organizationId, LocalDate a_from, LocalDate a_to);

    Optional<Audit> findFirstByDepartment_IdAndDeletedAtIsNullOrderByAuditDateDesc(Long a_departmentId);
}
