package com.app.medicalquality.gsr.service;

import com.app.medicalquality.exception.NotFoundException;
import com.app.medicalquality.gsr.entity.Audit;
import com.app.medicalquality.gsr.entity.AuditComment;
import com.app.medicalquality.gsr.entity.AuditScore;
import com.app.medicalquality.gsr.entity.GsrElement;
import com.app.medicalquality.gsr.repository.AuditCommentRepository;
import com.app.medicalquality.gsr.repository.AuditRepository;
import com.app.medicalquality.gsr.repository.AuditScoreRepository;
import com.app.medicalquality.gsr.repository.GsrElementRepository;
import com.app.medicalquality.policy.SubmitRules;
import com.app.medicalquality.policy.service.QualityPolicyService;
import com.app.medicalquality.score.ComplianceCalculator;
import com.app.medicalquality.score.ScoreValues;
import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.service.CurrentUserService;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditScoreService {

    private final AuditAccess auditAccess;
    private final AuditScoreRepository scoreRepository;
    private final AuditCommentRepository commentRepository;
    private final GsrElementRepository elementRepository;
    private final QualityPolicyService policyService;
    private final AuditRepository auditRepository;
    private final CurrentUserService currentUserService;

    public AuditScoreService(
            AuditAccess a_auditAccess,
            AuditScoreRepository a_scoreRepository,
            AuditCommentRepository a_commentRepository,
            GsrElementRepository a_elementRepository,
            QualityPolicyService a_policyService,
            AuditRepository a_auditRepository,
            CurrentUserService a_currentUserService) {
        this.auditAccess = a_auditAccess;
        this.scoreRepository = a_scoreRepository;
        this.commentRepository = a_commentRepository;
        this.elementRepository = a_elementRepository;
        this.policyService = a_policyService;
        this.auditRepository = a_auditRepository;
        this.currentUserService = a_currentUserService;
    }

    @Transactional
    public ComplianceCalculator.ComplianceView saveScore(
            Long a_auditId, Long a_elementId, String a_value, Authentication a_authentication) {
        Audit audit = auditAccess.requireEditable(a_auditId, a_authentication);
        String value = ScoreValues.normalize(a_value);
        checkNotApplicable(audit, value, a_authentication);
        GsrElement element = element(a_elementId);
        Optional<AuditScore> existing = scoreRepository.findByAuditIdAndElementId(a_auditId, a_elementId);
        if (value == null) {
            existing.ifPresent(scoreRepository::delete);
        } else {
            writeScore(audit, element, value, existing);
        }
        markStarted(audit);
        return compliance(a_auditId);
    }

    @Transactional
    public ComplianceCalculator.ComplianceView saveComment(
            Long a_auditId, Long a_elementId, String a_text, Authentication a_authentication) {
        Audit audit = auditAccess.requireEditable(a_auditId, a_authentication);
        GsrElement element = element(a_elementId);
        Optional<AuditComment> existing = commentRepository.findByAuditIdAndElementId(a_auditId, a_elementId);
        String text = a_text == null ? "" : a_text.trim();
        if (text.isEmpty()) {
            existing.ifPresent(commentRepository::delete);
        } else {
            writeComment(audit, element, text, existing);
        }
        return compliance(a_auditId);
    }

    @Transactional
    public ComplianceCalculator.ComplianceView bulk(
            Long a_auditId, Long a_standardId, String a_value, Authentication a_authentication) {
        Audit audit = auditAccess.requireEditable(a_auditId, a_authentication);
        String value = ScoreValues.normalize(a_value);
        checkNotApplicable(audit, value, a_authentication);
        List<GsrElement> elements = elementRepository.findByStandardIdAndDeletedAtIsNullOrderBySortOrderAsc(a_standardId);
        if (elements.isEmpty()) {
            throw new NotFoundException("error.element.not_found");
        }
        for (GsrElement element : elements) {
            Optional<AuditScore> existing = scoreRepository.findByAuditIdAndElementId(a_auditId, element.getId());
            if (value == null) {
                existing.ifPresent(scoreRepository::delete);
            } else {
                writeScore(audit, element, value, existing);
            }
        }
        markStarted(audit);
        return compliance(a_auditId);
    }

    @Transactional
    public ComplianceCalculator.ComplianceView clear(Long a_auditId, Authentication a_authentication) {
        auditAccess.requireEditable(a_auditId, a_authentication);
        scoreRepository.deleteByAuditId(a_auditId);
        commentRepository.deleteByAuditId(a_auditId);
        return compliance(a_auditId);
    }

    @Transactional(readOnly = true)
    public ComplianceCalculator.ComplianceView compliance(Long a_auditId) {
        int total = (int) elementRepository.countByDeletedAtIsNull();
        List<String> values = scoreRepository.findByAuditIdAndDeletedAtIsNull(a_auditId).stream()
                .map(AuditScore::getScoreValue)
                .toList();
        Audit audit = auditRepository.findByIdAndDeletedAtIsNull(a_auditId).orElse(null);
        double weight = audit == null ? 0.5 : policyService.forAudit(audit).decimal("scoring.partialWeight");
        return ComplianceCalculator.fromValues(total, values, weight);
    }

    private void writeScore(Audit a_audit, GsrElement a_element, String a_value, Optional<AuditScore> a_existing) {
        AuditScore score = a_existing.orElseGet(AuditScore::new);
        score.setAudit(a_audit);
        score.setElement(a_element);
        score.setScoreValue(a_value);
        score.setDeletedAt(null);
        scoreRepository.save(score);
    }

    private void writeComment(Audit a_audit, GsrElement a_element, String a_text, Optional<AuditComment> a_existing) {
        AuditComment comment = a_existing.orElseGet(AuditComment::new);
        comment.setAudit(a_audit);
        comment.setElement(a_element);
        comment.setBody(a_text);
        comment.setDeletedAt(null);
        commentRepository.save(comment);
    }

    private void checkNotApplicable(Audit a_audit, String a_value, Authentication a_authentication) {
        if (!"na".equals(a_value)) {
            return;
        }
        User user = currentUserService.require(a_authentication);
        String role = user.getRole() == null ? "" : user.getRole().getRoleCode();
        SubmitRules.checkNotApplicable(policyService.forAudit(a_audit), role, user.isSuperAdmin());
    }

    private void markStarted(Audit a_audit) {
        if ("Draft".equals(a_audit.getStatus())) {
            a_audit.setStatus("In Progress");
        }
    }

    private GsrElement element(Long a_elementId) {
        return elementRepository
                .findByIdAndDeletedAtIsNull(a_elementId)
                .orElseThrow(() -> new NotFoundException("error.element.not_found"));
    }
}
