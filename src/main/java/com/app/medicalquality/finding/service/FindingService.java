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
import java.time.LocalDate;
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

    public FindingService(
            FindingRepository a_findingRepository,
            CapaRepository a_capaRepository,
            AuditAccess a_auditAccess,
            CurrentUserService a_currentUserService,
            AuditTimelineWriter a_timelineWriter,
            DepartmentScope a_departmentScope) {
        this.findingRepository = a_findingRepository;
        this.capaRepository = a_capaRepository;
        this.auditAccess = a_auditAccess;
        this.currentUserService = a_currentUserService;
        this.timelineWriter = a_timelineWriter;
        this.departmentScope = a_departmentScope;
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
        Audit audit = a_request.getAuditId() == null
                ? null
                : auditAccess.require(a_request.getAuditId(), a_authentication);
        if (audit != null) {
            Finding existing = findingRepository
                    .findFirstByAuditIdAndGsrCodeAndStatusAndDeletedAtIsNull(audit.getId(), a_request.getGsrCode(), "Open")
                    .orElse(null);
            if (existing != null) {
                return findingView(existing);
            }
        }
        Finding finding = new Finding();
        finding.setAudit(audit);
        finding.setDepartment(audit == null ? null : audit.getDepartment());
        finding.setGsrCode(a_request.getGsrCode());
        finding.setElementIndex(a_request.getElementIndex());
        finding.setDescription(a_request.getDescription().trim());
        finding.setRiskLevel(a_request.getRiskLevel());
        finding.setCategory(a_request.getCategory() == null ? "Practice" : a_request.getCategory());
        finding.setStatus("Open");
        finding.setCreatedBy(currentUserService.require(a_authentication));
        findingRepository.save(finding);
        if (audit != null) {
            timelineWriter.add(audit, "finding", finding.getGsrCode(), finding.getCreatedBy());
        }
        return findingView(finding);
    }

    @Transactional
    public CapaView addCapa(Long a_findingId, CapaForm a_form, Authentication a_authentication) {
        Finding finding = findingRepository
                .findByIdAndDeletedAtIsNull(a_findingId)
                .orElseThrow(() -> new NotFoundException("error.finding.not_found"));
        Capa capa = new Capa();
        capa.setFinding(finding);
        capa.setCorrectiveAction(a_form.getCorrectiveAction().trim());
        capa.setPreventiveAction(a_form.getPreventiveAction());
        capa.setResponsiblePerson(a_form.getResponsiblePerson().trim());
        capa.setTargetDate(a_form.getTargetDate());
        capa.setProgress(a_form.getProgress());
        capa.setStatus(a_form.getStatus() == null ? "Open" : a_form.getStatus());
        capa.setCreatedBy(currentUserService.require(a_authentication));
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
    public CapaView updateCapa(Long a_capaId, CapaForm a_form) {
        Capa capa = capaRepository
                .findByIdAndDeletedAtIsNull(a_capaId)
                .orElseThrow(() -> new NotFoundException("error.capa.not_found"));
        if (a_form.getProgress() < 0 || a_form.getProgress() > 100) {
            throw new AppException("error.validation.failed");
        }
        capa.setProgress(a_form.getProgress());
        capa.setStatus(a_form.getStatus() == null ? capa.getStatus() : a_form.getStatus());
        if ("Closed".equals(capa.getStatus()) && capa.getFinding() != null) {
            capa.getFinding().setStatus("Closed");
        }
        return capaView(capa);
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
    }
}
