package com.app.medicalquality.gsr.repository;

import com.app.medicalquality.gsr.entity.AuditTimeline;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditTimelineRepository extends JpaRepository<AuditTimeline, Long> {

    List<AuditTimeline> findByAuditIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long a_auditId);
}
