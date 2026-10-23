package com.app.medicalquality.gsr.service;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.gsr.entity.Audit;
import com.app.medicalquality.gsr.entity.AuditLifecycleLog;
import com.app.medicalquality.gsr.repository.AuditLifecycleLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class LifecycleLogWriter {

    private final AuditLifecycleLogRepository logRepository;

    public LifecycleLogWriter(AuditLifecycleLogRepository a_logRepository) {
        this.logRepository = a_logRepository;
    }

    public void add(Audit a_audit, String a_operation, String a_reason, User a_user) {
        AuditLifecycleLog row = new AuditLifecycleLog();
        row.setAuditId(a_audit.getId());
        row.setDepartmentId(a_audit.getDepartment() == null ? null : a_audit.getDepartment().getId());
        row.setOperation(a_operation);
        row.setReason(a_reason);
        if (a_user != null) {
            row.setUserId(a_user.getId());
            row.setUsername(a_user.getUsername());
        }
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            row.setIpAddress(clip(attributes.getRequest().getRemoteAddr(), 80));
            row.setBrowser(clip(attributes.getRequest().getHeader("User-Agent"), 80));
        }
        logRepository.save(row);
    }

    private String clip(String a_value, int a_max) {
        if (a_value == null) {
            return null;
        }
        return a_value.length() <= a_max ? a_value : a_value.substring(0, a_max);
    }
}
