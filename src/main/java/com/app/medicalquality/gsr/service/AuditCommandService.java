package com.app.medicalquality.gsr.service;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.service.CurrentUserService;
import com.app.medicalquality.exception.AppException;
import com.app.medicalquality.exception.NotFoundException;
import com.app.medicalquality.gsr.dto.GsrRequests;
import com.app.medicalquality.gsr.dto.GsrViews;
import com.app.medicalquality.gsr.entity.Audit;
import com.app.medicalquality.gsr.entity.AuditVersion;
import com.app.medicalquality.gsr.repository.AuditRepository;
import com.app.medicalquality.gsr.repository.AuditVersionRepository;
import com.app.medicalquality.hospital.entity.Department;
import com.app.medicalquality.hospital.repository.DepartmentRepository;
import com.app.medicalquality.hospital.service.DepartmentScope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditCommandService {

    private static final Set<String> STATUSES = Set.of(
            "Draft", "In Progress", "Pending Review", "Submitted", "Approved", "Rejected", "Closed");

    private final AuditRepository auditRepository;
    private final AuditVersionRepository versionRepository;
    private final DepartmentRepository departmentRepository;
    private final CurrentUserService currentUserService;
    private final DepartmentScope departmentScope;
    private final AuditAccess auditAccess;
    private final AuditQueryService auditQueryService;
    private final AuditTimelineWriter timelineWriter;
    private final LifecycleLogWriter lifecycleLogWriter;
    private final ObjectMapper objectMapper;

    public AuditCommandService(
            AuditRepository a_auditRepository,
            AuditVersionRepository a_versionRepository,
            DepartmentRepository a_departmentRepository,
            CurrentUserService a_currentUserService,
            DepartmentScope a_departmentScope,
            AuditAccess a_auditAccess,
            AuditQueryService a_auditQueryService,
            AuditTimelineWriter a_timelineWriter,
            LifecycleLogWriter a_lifecycleLogWriter,
            ObjectMapper a_objectMapper) {
        this.auditRepository = a_auditRepository;
        this.versionRepository = a_versionRepository;
        this.departmentRepository = a_departmentRepository;
        this.currentUserService = a_currentUserService;
        this.departmentScope = a_departmentScope;
        this.auditAccess = a_auditAccess;
        this.auditQueryService = a_auditQueryService;
        this.timelineWriter = a_timelineWriter;
        this.lifecycleLogWriter = a_lifecycleLogWriter;
        this.objectMapper = a_objectMapper;
    }

    @Transactional
    public GsrViews.AuditSummary create(GsrRequests.Create a_request, Authentication a_authentication) {
        User user = currentUserService.require(a_authentication);
        Audit audit = new Audit();
        audit.setDepartmentName(a_request.getDepartmentName().trim());
        audit.setAuditorName(a_request.getAuditorName().trim());
        audit.setAuditDate(a_request.getAuditDate());
        audit.setStatus("Draft");
        audit.setArchived(false);
        audit.setCreatedBy(user);
        applyDepartment(audit, a_request.getDepartmentId(), a_authentication, user);
        auditRepository.save(audit);
        timelineWriter.add(audit, "created", "Audit created", user);
        return auditQueryService.summary(audit, 0);
    }

    @Transactional
    public GsrViews.AuditSummary update(Long a_auditId, GsrRequests.Update a_request, Authentication a_authentication) {
        Audit audit = auditAccess.requireEditable(a_auditId, a_authentication);
        User user = currentUserService.require(a_authentication);
        List<Map<String, String>> changes = new ArrayList<>();
        applyText(changes, "departmentName", audit.getDepartmentName(), a_request.getDepartmentName(), audit::setDepartmentName);
        applyText(changes, "auditorName", audit.getAuditorName(), a_request.getAuditorName(), audit::setAuditorName);
        applyDate(changes, audit, a_request);
        applyStatus(changes, audit, a_request.getStatus());
        if (a_request.getDepartmentId() != null) {
            applyDepartment(audit, a_request.getDepartmentId(), a_authentication, user);
            changes.add(change("departmentId", "", String.valueOf(a_request.getDepartmentId())));
        }
        saveVersion(audit, user, changes);
        timelineWriter.add(audit, "edited", a_request.getReason(), user);
        return auditQueryService.summary(audit, 0);
    }

    @Transactional
    public GsrViews.AuditSummary archive(Long a_auditId, boolean a_archived, Authentication a_authentication) {
        Audit audit = auditAccess.require(a_auditId, a_authentication);
        User user = currentUserService.require(a_authentication);
        audit.setArchived(a_archived);
        timelineWriter.add(audit, a_archived ? "archived" : "restored", null, user);
        lifecycleLogWriter.add(audit, a_archived ? "archived" : "restored", null, user);
        return auditQueryService.summary(audit, 0);
    }

    private void applyDepartment(Audit a_audit, Long a_departmentId, Authentication a_authentication, User a_user) {
        if (a_departmentId == null) {
            return;
        }
        Department department = departmentRepository
                .findByIdAndDeletedAtIsNull(a_departmentId)
                .orElseThrow(() -> new NotFoundException("error.department.not_found"));
        if (!departmentScope.seesAll(a_authentication, a_user)
                && !departmentScope.departmentIds(a_authentication, a_user).contains(department.getId())) {
            throw new AccessDeniedException("Access denied");
        }
        a_audit.setDepartment(department);
        a_audit.setDepartmentName(department.getNameAr());
    }

    private void applyText(
            List<Map<String, String>> a_changes,
            String a_field,
            String a_old,
            String a_new,
            java.util.function.Consumer<String> a_setter) {
        if (a_new == null || a_new.isBlank() || a_new.trim().equals(a_old)) {
            return;
        }
        a_setter.accept(a_new.trim());
        a_changes.add(change(a_field, a_old, a_new.trim()));
    }

    private void applyDate(List<Map<String, String>> a_changes, Audit a_audit, GsrRequests.Update a_request) {
        if (a_request.getAuditDate() == null || a_request.getAuditDate().equals(a_audit.getAuditDate())) {
            return;
        }
        a_changes.add(change("auditDate", String.valueOf(a_audit.getAuditDate()), String.valueOf(a_request.getAuditDate())));
        a_audit.setAuditDate(a_request.getAuditDate());
    }

    private void applyStatus(List<Map<String, String>> a_changes, Audit a_audit, String a_status) {
        if (a_status == null || a_status.equals(a_audit.getStatus())) {
            return;
        }
        if (!STATUSES.contains(a_status)) {
            throw new AppException("error.audit.status");
        }
        a_changes.add(change("status", a_audit.getStatus(), a_status));
        a_audit.setStatus(a_status);
    }

    private void saveVersion(Audit a_audit, User a_user, List<Map<String, String>> a_changes) {
        if (a_changes.isEmpty()) {
            return;
        }
        AuditVersion version = new AuditVersion();
        version.setAudit(a_audit);
        version.setVersionNumber(versionRepository.countByAuditId(a_audit.getId()) + 1);
        version.setChangedBy(a_user);
        try {
            version.setChangesJson(objectMapper.writeValueAsString(a_changes));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot store audit version", ex);
        }
        versionRepository.save(version);
    }

    private Map<String, String> change(String a_field, String a_old, String a_new) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("field", a_field);
        row.put("oldValue", a_old == null ? "" : a_old);
        row.put("newValue", a_new);
        return row;
    }
}
