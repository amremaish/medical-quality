package com.app.medicalquality.gsr.repository;

import com.app.medicalquality.gsr.entity.Audit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<Audit, Long> {

    @EntityGraph(attributePaths = {"department", "createdBy"})
    Optional<Audit> findByIdAndDeletedAtIsNull(Long a_id);

    @EntityGraph(attributePaths = {"department", "createdBy"})
    List<Audit> findByDeletedAtIsNullOrderByAuditDateDesc();
}
