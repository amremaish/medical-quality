package com.app.medicalquality.gsr.repository;

import com.app.medicalquality.gsr.entity.AuditComment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditCommentRepository extends JpaRepository<AuditComment, Long> {

    List<AuditComment> findByAuditIdAndDeletedAtIsNull(Long a_auditId);

    Optional<AuditComment> findByAuditIdAndElementId(Long a_auditId, Long a_elementId);

    void deleteByAuditId(Long a_auditId);
}
