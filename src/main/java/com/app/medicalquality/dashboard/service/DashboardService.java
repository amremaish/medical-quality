package com.app.medicalquality.dashboard.service;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.service.CurrentUserService;
import com.app.medicalquality.evidence.repository.EvidenceRepository;
import com.app.medicalquality.finding.entity.Capa;
import com.app.medicalquality.finding.entity.Finding;
import com.app.medicalquality.finding.repository.CapaRepository;
import com.app.medicalquality.finding.repository.FindingRepository;
import com.app.medicalquality.gsr.entity.Audit;
import com.app.medicalquality.gsr.entity.AuditScore;
import com.app.medicalquality.gsr.repository.AuditRepository;
import com.app.medicalquality.gsr.repository.AuditScoreRepository;
import com.app.medicalquality.gsr.repository.GsrElementRepository;
import com.app.medicalquality.hospital.repository.DepartmentRepository;
import com.app.medicalquality.hospital.service.DepartmentScope;
import com.app.medicalquality.score.ComplianceCalculator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final DepartmentRepository departmentRepository;
    private final AuditRepository auditRepository;
    private final AuditScoreRepository scoreRepository;
    private final GsrElementRepository elementRepository;
    private final FindingRepository findingRepository;
    private final CapaRepository capaRepository;
    private final EvidenceRepository evidenceRepository;
    private final CurrentUserService currentUserService;
    private final DepartmentScope departmentScope;

    public DashboardService(
            DepartmentRepository a_departmentRepository,
            AuditRepository a_auditRepository,
            AuditScoreRepository a_scoreRepository,
            GsrElementRepository a_elementRepository,
            FindingRepository a_findingRepository,
            CapaRepository a_capaRepository,
            EvidenceRepository a_evidenceRepository,
            CurrentUserService a_currentUserService,
            DepartmentScope a_departmentScope) {
        this.departmentRepository = a_departmentRepository;
        this.auditRepository = a_auditRepository;
        this.scoreRepository = a_scoreRepository;
        this.elementRepository = a_elementRepository;
        this.findingRepository = a_findingRepository;
        this.capaRepository = a_capaRepository;
        this.evidenceRepository = a_evidenceRepository;
        this.currentUserService = a_currentUserService;
        this.departmentScope = a_departmentScope;
    }

    @Transactional(readOnly = true)
    public Summary summary(Authentication a_authentication) {
        User user = currentUserService.require(a_authentication);
        List<Audit> audits = auditRepository.findByDeletedAtIsNullOrderByAuditDateDesc().stream()
                .filter(a_audit -> !a_audit.isArchived())
                .filter(a_audit -> departmentScope.canSee(a_authentication, user, a_audit))
                .toList();
        int completed = (int) audits.stream()
                .filter(a_audit -> "Submitted".equals(a_audit.getStatus())
                        || "Approved".equals(a_audit.getStatus())
                        || "Closed".equals(a_audit.getStatus()))
                .count();
        List<Finding> findings = findingRepository.findByDeletedAtIsNullOrderByCreatedAtDesc();
        List<Capa> capas = capaRepository.findByDeletedAtIsNullOrderByCreatedAtDesc();
        return new Summary(
                (int) departmentRepository.countByDeletedAtIsNull(),
                audits.size(),
                completed,
                audits.size() - completed,
                (int) findings.stream().filter(a_finding -> "Open".equals(a_finding.getStatus())).count(),
                (int) capas.stream().filter(a_capa -> !"Closed".equals(a_capa.getStatus())).count(),
                evidenceRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().size(),
                average(audits));
    }

    private int average(List<Audit> a_audits) {
        if (a_audits.isEmpty()) {
            return 0;
        }
        int total = (int) elementRepository.countByDeletedAtIsNull();
        Map<Long, List<String>> scores = new LinkedHashMap<>();
        List<Long> ids = a_audits.stream().map(Audit::getId).toList();
        for (AuditScore score : scoreRepository.findByAuditIdInAndDeletedAtIsNull(ids)) {
            scores.computeIfAbsent(score.getAudit().getId(), a_id -> new ArrayList<>()).add(score.getScoreValue());
        }
        int sum = 0;
        for (Audit audit : a_audits) {
            sum += ComplianceCalculator.fromValues(total, scores.getOrDefault(audit.getId(), List.of())).percent();
        }
        return Math.round(sum / (float) a_audits.size());
    }

    public record Summary(
            int departments,
            int audits,
            int completedAudits,
            int pendingAudits,
            int openFindings,
            int openCapas,
            int evidence,
            int averageCompliance) {}
}
