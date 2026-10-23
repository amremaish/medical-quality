package com.app.medicalquality.finding.repository;

import com.app.medicalquality.finding.entity.Capa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CapaRepository extends JpaRepository<Capa, Long> {

    @EntityGraph(attributePaths = "finding")
    List<Capa> findByDeletedAtIsNullOrderByCreatedAtDesc();

    List<Capa> findByFindingIdAndDeletedAtIsNull(Long a_findingId);

    Optional<Capa> findByIdAndDeletedAtIsNull(Long a_id);

    List<Capa> findByFinding_Audit_IdAndDeletedAtIsNull(Long a_auditId);
}
