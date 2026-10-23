package com.app.medicalquality.gsr.service;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.service.CurrentUserService;
import com.app.medicalquality.gsr.dto.GsrViews;
import com.app.medicalquality.gsr.entity.Audit;
import com.app.medicalquality.gsr.entity.AuditComment;
import com.app.medicalquality.gsr.entity.AuditScore;
import com.app.medicalquality.gsr.entity.GsrChapter;
import com.app.medicalquality.gsr.entity.GsrElement;
import com.app.medicalquality.gsr.entity.GsrStandard;
import com.app.medicalquality.gsr.repository.AuditCommentRepository;
import com.app.medicalquality.gsr.repository.AuditRepository;
import com.app.medicalquality.gsr.repository.AuditScoreRepository;
import com.app.medicalquality.gsr.repository.GsrElementRepository;
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
public class AuditQueryService {

    private final AuditRepository auditRepository;
    private final AuditScoreRepository scoreRepository;
    private final AuditCommentRepository commentRepository;
    private final GsrElementRepository elementRepository;
    private final CurrentUserService currentUserService;
    private final DepartmentScope departmentScope;
    private final AuditAccess auditAccess;

    public AuditQueryService(
            AuditRepository a_auditRepository,
            AuditScoreRepository a_scoreRepository,
            AuditCommentRepository a_commentRepository,
            GsrElementRepository a_elementRepository,
            CurrentUserService a_currentUserService,
            DepartmentScope a_departmentScope,
            AuditAccess a_auditAccess) {
        this.auditRepository = a_auditRepository;
        this.scoreRepository = a_scoreRepository;
        this.commentRepository = a_commentRepository;
        this.elementRepository = a_elementRepository;
        this.currentUserService = a_currentUserService;
        this.departmentScope = a_departmentScope;
        this.auditAccess = a_auditAccess;
    }

    @Transactional(readOnly = true)
    public List<GsrViews.AuditSummary> list(boolean a_includeArchived, Authentication a_authentication) {
        User user = currentUserService.require(a_authentication);
        List<Audit> audits = auditRepository.findByDeletedAtIsNullOrderByAuditDateDesc().stream()
                .filter(a_audit -> a_includeArchived || !a_audit.isArchived())
                .filter(a_audit -> departmentScope.canSee(a_authentication, user, a_audit))
                .toList();
        int total = (int) elementRepository.countByDeletedAtIsNull();
        Map<Long, List<String>> scores = scoresByAudit(audits);
        return audits.stream()
                .map(a_audit -> summary(
                        a_audit,
                        ComplianceCalculator.fromValues(total, scores.getOrDefault(a_audit.getId(), List.of()))
                                .percent()))
                .toList();
    }

    @Transactional(readOnly = true)
    public GsrViews.Checklist checklist(Long a_auditId, Authentication a_authentication) {
        Audit audit = auditAccess.require(a_auditId, a_authentication);
        List<GsrElement> elements =
                elementRepository.findByDeletedAtIsNullOrderByStandard_Chapter_SortOrderAscStandard_SortOrderAscSortOrderAsc();
        Map<Long, String> scores = new LinkedHashMap<>();
        scoreRepository.findByAuditIdAndDeletedAtIsNull(a_auditId).forEach(a_score -> scores.put(
                a_score.getElement().getId(), a_score.getScoreValue()));
        Map<Long, String> comments = new LinkedHashMap<>();
        commentRepository
                .findByAuditIdAndDeletedAtIsNull(a_auditId)
                .forEach(a_comment -> comments.put(a_comment.getElement().getId(), a_comment.getBody()));
        ComplianceCalculator.ComplianceView compliance =
                ComplianceCalculator.fromValues(elements.size(), scores.values());
        return new GsrViews.Checklist(summary(audit, compliance.percent()), compliance, chapters(elements, scores, comments));
    }

    public GsrViews.AuditSummary summary(Audit a_audit, int a_percent) {
        Long departmentId = a_audit.getDepartment() == null ? null : a_audit.getDepartment().getId();
        return new GsrViews.AuditSummary(
                a_audit.getId(),
                a_audit.getDepartmentName(),
                departmentId,
                a_audit.getAuditorName(),
                a_audit.getAuditDate(),
                a_audit.getStatus(),
                a_audit.isArchived(),
                a_percent);
    }

    private Map<Long, List<String>> scoresByAudit(List<Audit> a_audits) {
        Map<Long, List<String>> grouped = new LinkedHashMap<>();
        if (a_audits.isEmpty()) {
            return grouped;
        }
        List<Long> ids = a_audits.stream().map(Audit::getId).toList();
        for (AuditScore score : scoreRepository.findByAuditIdInAndDeletedAtIsNull(ids)) {
            grouped.computeIfAbsent(score.getAudit().getId(), a_id -> new ArrayList<>()).add(score.getScoreValue());
        }
        return grouped;
    }

    private List<GsrViews.ChapterLine> chapters(
            List<GsrElement> a_elements, Map<Long, String> a_scores, Map<Long, String> a_comments) {
        Map<Long, List<GsrViews.StandardLine>> byChapter = new LinkedHashMap<>();
        Map<Long, GsrChapter> chapters = new LinkedHashMap<>();
        Map<Long, List<GsrElement>> byStandard = new LinkedHashMap<>();
        for (GsrElement element : a_elements) {
            byStandard.computeIfAbsent(element.getStandard().getId(), a_id -> new ArrayList<>()).add(element);
        }
        for (List<GsrElement> group : byStandard.values()) {
            GsrStandard standard = group.get(0).getStandard();
            GsrChapter chapter = standard.getChapter();
            chapters.putIfAbsent(chapter.getId(), chapter);
            byChapter.computeIfAbsent(chapter.getId(), a_id -> new ArrayList<>())
                    .add(standardLine(standard, group, a_scores, a_comments));
        }
        List<GsrViews.ChapterLine> lines = new ArrayList<>();
        for (GsrChapter chapter : chapters.values()) {
            lines.add(new GsrViews.ChapterLine(chapter.getId(), chapter.getName(), byChapter.get(chapter.getId())));
        }
        return lines;
    }

    private GsrViews.StandardLine standardLine(
            GsrStandard a_standard,
            List<GsrElement> a_group,
            Map<Long, String> a_scores,
            Map<Long, String> a_comments) {
        int met = 0;
        int partial = 0;
        int not = 0;
        int na = 0;
        List<GsrViews.ElementLine> lines = new ArrayList<>();
        for (GsrElement element : a_group) {
            String score = a_scores.get(element.getId());
            if ("met".equals(score)) {
                met++;
            } else if ("partial".equals(score)) {
                partial++;
            } else if ("not".equals(score)) {
                not++;
            } else if ("na".equals(score)) {
                na++;
            }
            lines.add(new GsrViews.ElementLine(
                    element.getId(),
                    element.getSortOrder(),
                    element.getBody(),
                    score,
                    a_comments.get(element.getId())));
        }
        String tone = ComplianceCalculator.standardTone(met, partial, not, na, a_group.size());
        return new GsrViews.StandardLine(
                a_standard.getId(),
                a_standard.getCode(),
                a_standard.getTitle(),
                a_standard.getHandbookRef(),
                a_standard.getIntent(),
                tone,
                lines);
    }
}
