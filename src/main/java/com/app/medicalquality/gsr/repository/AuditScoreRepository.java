package com.app.medicalquality.gsr.repository;

import com.app.medicalquality.gsr.entity.AuditScore;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditScoreRepository extends JpaRepository<AuditScore, Long> {

    List<AuditScore> findByAuditIdAndDeletedAtIsNull(Long a_auditId);

    List<AuditScore> findByAuditIdInAndDeletedAtIsNull(Collection<Long> a_auditIds);

    Optional<AuditScore> findByAuditIdAndElementId(Long a_auditId, Long a_elementId);

    void deleteByAuditId(Long a_auditId);
}
