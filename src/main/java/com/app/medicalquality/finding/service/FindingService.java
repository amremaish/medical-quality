package com.app.medicalquality.finding.service;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.service.CurrentUserService;
import com.app.medicalquality.exception.AppException;
import com.app.medicalquality.exception.NotFoundException;
import com.app.medicalquality.finding.entity.Capa;
import com.app.medicalquality.finding.entity.Finding;
import com.app.medicalquality.finding.repository.CapaRepository;
import com.app.medicalquality.finding.repository.FindingRepository;
import com.app.medicalquality.gsr.entity.Audit;
import com.app.medicalquality.gsr.service.AuditAccess;
import com.app.medicalquality.gsr.service.AuditTimelineWriter;
import com.app.medicalquality.hospital.service.DepartmentScope;
import com.app.medicalquality.policy.CapaSchedule;
import com.app.medicalquality.policy.ResolvedPolicy;
import com.app.medicalquality.policy.service.QualityPolicyService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FindingService {

    private final FindingRepository findingRepository;
    private final CapaRepository capaRepository;
    private final AuditAccess auditAccess;
    private final CurrentUserService currentUserService;
    private final AuditTimelineWriter timelineWriter;
    private final DepartmentScope departmentScope;
    private final QualityPolicyService policyService;

    public FindingService(
            FindingRepository a_findingRepository,
            CapaRepository a_capaRepository,
            AuditAccess a_auditAccess,
            CurrentUserService a_currentUserService,
            AuditTimelineWriter a_timelineWriter,
            DepartmentScope a_departmentScope,
            QualityPolicyService a_policyService) {
        this.findingRepository = a_findingRepository;
        this.capaRepository = a_capaRepository;
        this.auditAccess = a_auditAccess;
        this.currentUserService = a_currentUserService;
        this.timelineWriter = a_timelineWriter;
        this.departmentScope = a_departmentScope;
        this.policyService = a_policyService;
    }

    @Transactional(readOnly = true)
    public List<FindingView> list(Authentication a_authentication) {
        Set<Long> departmentIds = visibleDepartments(a_authentication);
        return findingRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .filter(a_finding -> a_finding.getDepartment() != null
                        && departmentIds.contains(a_finding.getDepartment().getId()))
                .map(this::findingView)
                .toList();
    }

    @Transactional
    public FindingView open(Create a_request, Authentication a_authentication) {
        if (a_request.getAuditId() == null) {
            throw new AppException("error.finding.audit_required");
        }
        Audit audit = auditAccess.require(a_request.getAuditId(), a_authentication);
        Finding existing = findingRepository
                .findFirstByAuditIdAndGsrCodeAndStatusAndDeletedAtIsNull(audit.getId(), a_request.getGsrCode(), "Open")
                .orElse(null);
        if (existing != null) {
            return findingView(existing);
        }
        Finding finding = new Finding();
        finding.setAudit(audit);
        finding.setDepartment(audit.getDepartment());
        finding.setGsrCode(a_request.getGsrCode());
        finding.setElementIndex(a_request.getElementIndex());
        finding.setDescription(a_request.getDescription().trim());
        finding.setRiskLevel(a_request.getRiskLevel());
        finding.setCategory(a_request.getCategory() == null ? "Practice" : a_request.getCategory());
        finding.setStatus("Open");
        finding.setCreatedBy(currentUserService.require(a_authentication));
        if (audit.getDepartment() != null && audit.getDepartment().getOrganization() != null) {
            Long organizationId = audit.getDepartment().getOrganization().getId();
            finding.setPolicyVersion(policyService.applicable(organizationId, LocalDate.now()));
            ResolvedPolicy policy = policyService.forFinding(finding);
            String severity = CapaSchedule.severity(policy, null, a_request.getRiskLevel());
            finding.setCanonicalSeverity(severity);
            finding.setEscalated(policy.escalates(severity));
        }
        findingRepository.save(finding);
        timelineWriter.add(audit, "finding", finding.getGsrCode(), finding.getCreatedBy());
        return findingView(finding);
    }

    @Transactional
    public CapaView addCapa(Long a_findingId, CapaForm a_form, Authentication a_authentication) {
        Finding finding = findingRepository
                .findByIdAndDeletedAtIsNull(a_findingId)
                .orElseThrow(() -> new NotFoundException("error.finding.not_found"));
        if (finding.getDepartment() == null
                || !visibleDepartments(a_authentication).contains(finding.getDepartment().getId())) {
            throw new NotFoundException("error.finding.not_found");
        }
        User actor = currentUserService.require(a_authentication);
        ResolvedPolicy policy = policyService.forFinding(finding);
        String severity = CapaSchedule.severity(policy, finding.getCanonicalSeverity(), finding.getRiskLevel());
        if (policy.requiresRootCause(severity)
                && (a_form.getRootCause() == null || a_form.getRootCause().isBlank())) {
            throw new AppException("error.policy.capa.root_cause", new Object[] {severity});
        }
        LocalDate opened = finding.getCreatedAt() == null ? LocalDate.now() : finding.getCreatedAt().toLocalDate();
        Capa capa = new Capa();
        capa.setFinding(finding);
        capa.setCorrectiveAction(a_form.getCorrectiveAction().trim());
        capa.setPreventiveAction(a_form.getPreventiveAction());
        capa.setResponsiblePerson(a_form.getResponsiblePerson().trim());
        capa.setTargetDate(CapaSchedule.dueDate(policy, severity, opened, a_form.getTargetDate()));
        capa.setDeadlineDays(policy.deadlineDays(severity));
        capa.setPolicyVersion(finding.getPolicyVersion());
        if (a_form.getRootCause() != null && !a_form.getRootCause().isBlank()) {
            capa.setRootCause(trimReason(a_form.getRootCause(), 4000));
        }
        capa.setProgress(a_form.getProgress());
        capa.setStatus(a_form.getStatus() == null ? "Open" : a_form.getStatus());
        capa.setCreatedBy(actor);
        capaRepository.save(capa);
        return capaView(capa);
    }

    @Transactional(readOnly = true)
    public List<CapaView> capas(Authentication a_authentication) {
        Set<Long> departmentIds = visibleDepartments(a_authentication);
        return capaRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .filter(a_capa -> a_capa.getFinding() != null
                        && a_capa.getFinding().getDepartment() != null
                        && departmentIds.contains(a_capa.getFinding().getDepartment().getId()))
                .map(this::capaView)
                .toList();
    }

    private Set<Long> visibleDepartments(Authentication a_authentication) {
        User user = currentUserService.require(a_authentication);
        return departmentScope.departmentIds(a_authentication, user);
    }

    @Transactional
    public CapaView updateCapa(Long a_capaId, CapaForm a_form, Authentication a_authentication) {
        Capa capa = capaRepository
                .findByIdAndDeletedAtIsNull(a_capaId)
                .orElseThrow(() -> new NotFoundException("error.capa.not_found"));
        Finding finding = capa.getFinding();
        if (finding == null
                || finding.getDepartment() == null
                || !visibleDepartments(a_authentication).contains(finding.getDepartment().getId())) {
            throw new NotFoundException("error.capa.not_found");
        }
        if (a_form.getProgress() < 0 || a_form.getProgress() > 100) {
            throw new AppException("error.validation.failed");
        }
        User actor = currentUserService.require(a_authentication);
        ResolvedPolicy policy = policyService.forFinding(finding);
        applyTargetDate(capa, finding, a_form, actor, policy);
        capa.setProgress(a_form.getProgress());
        String status = a_form.getStatus() == null ? capa.getStatus() : a_form.getStatus();
        if ("Closed".equals(status) && policy.bool("capa.closeOnlyAfterVerification")) {
            if (!Boolean.TRUE.equals(a_form.getVerified()) && capa.getVerifiedAt() == null) {
                throw new AppException("error.policy.capa.verification");
            }
            if (!allowed(actor, policy, "capa.verifierRoleCodes")) {
                throw new AppException("error.policy.capa.verifier");
            }
            if (capa.getVerifiedAt() == null) {
                capa.setVerifiedAt(LocalDateTime.now());
                capa.setVerifiedBy(actor);
            }
        }
        capa.setStatus(status);
        if ("Closed".equals(capa.getStatus()) && capa.getFinding() != null) {
            if (!policy.bool("capa.closeOnlyAfterVerification") || capa.getVerifiedAt() != null) {
                capa.getFinding().setStatus("Closed");
            }
        }
        return capaView(capa);
    }

    private void applyTargetDate(Capa a_capa, Finding a_finding, CapaForm a_form, User a_actor, ResolvedPolicy a_policy) {
        if (a_form.getTargetDate() == null) {
            return;
        }
        if (a_capa.getTargetDate() != null && a_form.getTargetDate().isAfter(a_capa.getTargetDate())) {
            int limit = a_policy.integer("capa.maxExtensions");
            if (a_capa.getExtensionCount() >= limit) {
                throw new AppException("error.policy.capa.extension_limit", new Object[] {limit});
            }
            if (a_policy.bool("capa.extensionReasonRequired")
                    && (a_form.getExtensionReason() == null || a_form.getExtensionReason().isBlank())) {
                throw new AppException("error.policy.capa.extension_reason");
            }
            if (!allowed(a_actor, a_policy, "capa.extensionApproverRoleCodes")) {
                throw new AppException("error.policy.capa.extension_role");
            }
            a_capa.setExtensionCount(a_capa.getExtensionCount() + 1);
            if (a_form.getExtensionReason() != null) {
                a_capa.setExtensionReason(trimReason(a_form.getExtensionReason(), 500));
            }
            a_capa.setTargetDate(a_form.getTargetDate());
            return;
        }
        String severity = CapaSchedule.severity(a_policy, a_finding.getCanonicalSeverity(), a_finding.getRiskLevel());
        LocalDate opened = a_finding.getCreatedAt() == null ? LocalDate.now() : a_finding.getCreatedAt().toLocalDate();
        a_capa.setTargetDate(CapaSchedule.dueDate(a_policy, severity, opened, a_form.getTargetDate()));
    }

    private boolean allowed(User a_user, ResolvedPolicy a_policy, String a_key) {
        if (a_user.isSuperAdmin()) {
            return true;
        }
        String role = a_user.getRole() == null ? "" : a_user.getRole().getRoleCode();
        return a_policy.roleAllowed(a_key, role);
    }

    private String trimReason(String a_value, int a_max) {
        String value = a_value.trim();
        if (value.length() > a_max) {
            throw new AppException("error.validation.failed");
        }
        return value;
    }

    private FindingView findingView(Finding a_finding) {
        Long auditId = a_finding.getAudit() == null ? null : a_finding.getAudit().getId();
        return new FindingView(
                a_finding.getId(),
                auditId,
                a_finding.getGsrCode(),
                a_finding.getElementIndex(),
                a_finding.getDescription(),
                a_finding.getRiskLevel(),
                a_finding.getCategory(),
                a_finding.getStatus());
    }

    private CapaView capaView(Capa a_capa) {
        return new CapaView(
                a_capa.getId(),
                a_capa.getFinding().getId(),
                a_capa.getCorrectiveAction(),
                a_capa.getPreventiveAction(),
                a_capa.getResponsiblePerson(),
                a_capa.getTargetDate(),
                a_capa.getProgress(),
                a_capa.getStatus());
    }

    public record FindingView(
            Long id,
            Long auditId,
            String gsrCode,
            int elementIndex,
            String description,
            String riskLevel,
            String category,
            String status) {}

    public record CapaView(
            Long id,
            Long findingId,
            String correctiveAction,
            String preventiveAction,
            String responsiblePerson,
            LocalDate targetDate,
            int progress,
            String status) {}

    @Getter
    @Setter
    public static class Create {
        private Long auditId;
        private String gsrCode;
        private int elementIndex;
        private String description;
        private String riskLevel;
        private String category;
    }

    @Getter
    @Setter
    public static class CapaForm {
        private String correctiveAction;
        private String preventiveAction;
        private String responsiblePerson;
        private LocalDate targetDate;
        private int progress;
        private String status;
        private String rootCause;
        private String extensionReason;
        private Boolean verified;
    }
}
