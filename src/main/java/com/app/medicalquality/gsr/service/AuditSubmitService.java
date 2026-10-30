package com.app.medicalquality.gsr.service;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.service.CurrentUserService;
import com.app.medicalquality.exception.AppException;
import com.app.medicalquality.finding.entity.Capa;
import com.app.medicalquality.finding.entity.Finding;
import com.app.medicalquality.finding.repository.CapaRepository;
import com.app.medicalquality.finding.repository.FindingRepository;
import com.app.medicalquality.gsr.entity.Audit;
import com.app.medicalquality.gsr.entity.AuditScore;
import com.app.medicalquality.gsr.repository.AuditScoreRepository;
import com.app.medicalquality.policy.ResolvedPolicy;
import com.app.medicalquality.policy.SubmitRules;
import com.app.medicalquality.policy.entity.AuditAssessor;
import com.app.medicalquality.policy.repository.AuditAssessorRepository;
import com.app.medicalquality.policy.service.QualityPolicyService;
import com.app.medicalquality.evidence.repository.EvidenceRepository;
import com.app.medicalquality.score.ComplianceCalculator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditSubmitService {

    private static final Set<String> STATUSES = Set.of(
            "Draft", "In Progress", "Pending Review", "Submitted", "Approved", "Rejected", "Closed");

    private final AuditAccess auditAccess;
    private final AuditScoreRepository scoreRepository;
    private final FindingRepository findingRepository;
    private final CapaRepository capaRepository;
    private final CurrentUserService currentUserService;
    private final AuditTimelineWriter timelineWriter;
    private final QualityPolicyService policyService;
    private final AuditAssessorRepository auditAssessorRepository;
    private final EvidenceRepository evidenceRepository;
    private final com.app.medicalquality.gsr.service.AuditScoreService auditScoreService;

    public AuditSubmitService(
            AuditAccess a_auditAccess,
            AuditScoreRepository a_scoreRepository,
            FindingRepository a_findingRepository,
            CapaRepository a_capaRepository,
            CurrentUserService a_currentUserService,
            AuditTimelineWriter a_timelineWriter,
            QualityPolicyService a_policyService,
            AuditAssessorRepository a_auditAssessorRepository,
            EvidenceRepository a_evidenceRepository,
            AuditScoreService a_auditScoreService) {
        this.auditAccess = a_auditAccess;
        this.scoreRepository = a_scoreRepository;
        this.findingRepository = a_findingRepository;
        this.capaRepository = a_capaRepository;
        this.currentUserService = a_currentUserService;
        this.timelineWriter = a_timelineWriter;
        this.policyService = a_policyService;
        this.auditAssessorRepository = a_auditAssessorRepository;
        this.evidenceRepository = a_evidenceRepository;
        this.auditScoreService = a_auditScoreService;
    }

    @Transactional
    public Audit submit(Long a_auditId, Authentication a_authentication) {
        Audit audit = auditAccess.requireEditable(a_auditId, a_authentication);
        List<AuditScore> scores = scoreRepository.findByAuditIdAndDeletedAtIsNull(a_auditId);
        if (scores.isEmpty()) {
            throw new AppException("error.audit.no_scores");
        }
        List<Finding> findings = findingRepository.findByAuditIdAndDeletedAtIsNull(a_auditId);
        if (findings.stream().anyMatch(a_finding -> a_finding.getDescription() == null || a_finding.getDescription().isBlank())) {
            throw new AppException("error.audit.finding_description");
        }
        if (findings.stream().filter(a_finding -> "Open".equals(a_finding.getStatus())).anyMatch(this::missingCapa)) {
            throw new AppException("error.audit.finding_capa");
        }
        enforcePolicy(audit, findings);
        User user = currentUserService.require(a_authentication);
        audit.setStatus("Submitted");
        audit.setSubmittedAt(LocalDateTime.now());
        audit.setSubmittedBy(user);
        timelineWriter.add(audit, "submitted", "Audit submitted", user);
        return audit;
    }

    @Transactional
    public Audit status(Long a_auditId, String a_status, Authentication a_authentication) {
        if (!STATUSES.contains(a_status)) {
            throw new AppException("error.audit.status");
        }
        Audit audit = auditAccess.require(a_auditId, a_authentication);
        if (audit.getLockedAt() != null) {
            throw new AppException("error.audit.locked");
        }
        if ("Submitted".equals(audit.getStatus()) && !("Approved".equals(a_status) || "Rejected".equals(a_status))) {
            throw new AppException("error.audit.status");
        }
        if ("Submitted".equals(audit.getStatus()) || "Approved".equals(audit.getStatus()) || "Closed".equals(audit.getStatus())) {
            auditAccess.require(a_auditId, a_authentication);
        }
        audit.setStatus(a_status);
        if ("Closed".equals(a_status) && policyService.forAudit(audit).bool("security.lockClosedAudits")) {
            audit.setLockedAt(LocalDateTime.now());
        }
        timelineWriter.add(audit, "status", a_status, currentUserService.require(a_authentication));
        return audit;
    }

    private boolean missingCapa(Finding a_finding) {
        return capaRepository.findByFindingIdAndDeletedAtIsNull(a_finding.getId()).isEmpty();
    }

    private void enforcePolicy(Audit a_audit, List<Finding> a_findings) {
        ResolvedPolicy policy = policyService.forAudit(a_audit);
        List<AuditAssessor> team = auditAssessorRepository.findByAudit_IdAndDeletedAtIsNull(a_audit.getId());
        List<SubmitRules.FindingFact> facts = a_findings.stream().map(a_finding -> {
            String severity = a_finding.getCanonicalSeverity() == null ? a_finding.getRiskLevel() : a_finding.getCanonicalSeverity();
            boolean evidence = false;
            try {
                evidence = policy.evidenceRequired(severity);
            } catch (RuntimeException ex) {
                evidence = false;
            }
            return new SubmitRules.FindingFact(
                    severity,
                    evidence,
                    evidenceRepository.countByFinding_IdAndDeletedAtIsNull(a_finding.getId()),
                    a_finding.getGsrCode(),
                    "Open".equals(a_finding.getStatus()) ? "not" : "met");
        }).toList();
        SubmitRules.check(policy, new SubmitRules.Facts(
                auditScoreService.compliance(a_audit.getId()).percent(),
                team.size(),
                team.stream().anyMatch(AuditAssessor::isLead),
                facts));
    }
}
