package com.app.medicalquality.gsr.service;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.gsr.entity.Audit;
import com.app.medicalquality.gsr.entity.AuditTimeline;
import com.app.medicalquality.gsr.repository.AuditTimelineRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditTimelineWriter {

    private final AuditTimelineRepository timelineRepository;

    public AuditTimelineWriter(AuditTimelineRepository a_timelineRepository) {
        this.timelineRepository = a_timelineRepository;
    }

    public void add(Audit a_audit, String a_event, String a_detail, User a_user) {
        AuditTimeline row = new AuditTimeline();
        row.setAudit(a_audit);
        row.setEventType(a_event);
        row.setDetail(a_detail);
        if (a_user != null) {
            row.setUserId(a_user.getId());
            row.setUsername(a_user.getUsername());
        }
        timelineRepository.save(row);
    }
}
