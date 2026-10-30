package com.app.medicalquality.gsr.service;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.service.CurrentUserService;
import com.app.medicalquality.exception.AppException;
import com.app.medicalquality.exception.NotFoundException;
import com.app.medicalquality.gsr.entity.Audit;
import com.app.medicalquality.gsr.repository.AuditRepository;
import com.app.medicalquality.hospital.service.DepartmentScope;
import com.app.medicalquality.score.ComplianceCalculator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuditAccess {

    private final AuditRepository auditRepository;
    private final CurrentUserService currentUserService;
    private final DepartmentScope departmentScope;

    public AuditAccess(
            AuditRepository a_auditRepository,
            CurrentUserService a_currentUserService,
            DepartmentScope a_departmentScope) {
        this.auditRepository = a_auditRepository;
        this.currentUserService = a_currentUserService;
        this.departmentScope = a_departmentScope;
    }

    public Audit require(Long a_auditId, Authentication a_authentication) {
        Audit audit = auditRepository
                .findByIdAndDeletedAtIsNull(a_auditId)
                .orElseThrow(() -> new NotFoundException("error.audit.not_found"));
        User user = currentUserService.require(a_authentication);
        if (!departmentScope.canSee(a_authentication, user, audit)) {
            throw new AccessDeniedException("Access denied");
        }
        return audit;
    }

    public Audit requireEditable(Long a_auditId, Authentication a_authentication) {
        Audit audit = require(a_auditId, a_authentication);
        if (audit.getLockedAt() != null || ComplianceCalculator.locked(audit.getStatus())) {
            throw new AppException("error.audit.locked");
        }
        return audit;
    }
}
