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
import com.app.medicalquality.hospital.entity.Department;
import com.app.medicalquality.hospital.repository.AssessorRepository;
import com.app.medicalquality.hospital.repository.DepartmentRepository;
import com.app.medicalquality.hospital.service.DepartmentScope;
import com.app.medicalquality.hospital.service.HospitalContext;
import com.app.medicalquality.score.ComplianceCalculator;
import com.app.medicalquality.score.ComplianceCalculator.ComplianceView;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final AssessorRepository assessorRepository;
    private final CurrentUserService currentUserService;
    private final DepartmentScope departmentScope;
    private final HospitalContext hospitalContext;

    public DashboardService(
            DepartmentRepository a_departmentRepository,
            AuditRepository a_auditRepository,
            AuditScoreRepository a_scoreRepository,
            GsrElementRepository a_elementRepository,
            FindingRepository a_findingRepository,
            CapaRepository a_capaRepository,
            EvidenceRepository a_evidenceRepository,
            AssessorRepository a_assessorRepository,
            CurrentUserService a_currentUserService,
            DepartmentScope a_departmentScope,
            HospitalContext a_hospitalContext) {
        this.departmentRepository = a_departmentRepository;
        this.auditRepository = a_auditRepository;
        this.scoreRepository = a_scoreRepository;
        this.elementRepository = a_elementRepository;
        this.findingRepository = a_findingRepository;
        this.capaRepository = a_capaRepository;
        this.evidenceRepository = a_evidenceRepository;
        this.assessorRepository = a_assessorRepository;
        this.currentUserService = a_currentUserService;
        this.departmentScope = a_departmentScope;
        this.hospitalContext = a_hospitalContext;
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
        Set<Long> departmentIds = departmentScope.departmentIds(a_authentication, user);
        Long organizationId = hospitalContext.require(a_authentication).getId();
        List<Finding> findings = findingRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .filter(a_finding -> a_finding.getDepartment() != null
                        && departmentIds.contains(a_finding.getDepartment().getId()))
                .toList();
        List<Capa> capas = capaRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .filter(a_capa -> a_capa.getFinding() != null
                        && a_capa.getFinding().getDepartment() != null
                        && departmentIds.contains(a_capa.getFinding().getDepartment().getId()))
                .toList();
        int evidence = (int) evidenceRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .filter(a_evidence -> (a_evidence.getDepartment() != null
                                && departmentIds.contains(a_evidence.getDepartment().getId()))
                        || (a_evidence.getAudit() != null
                                && a_evidence.getAudit().getDepartment() != null
                                && departmentIds.contains(a_evidence.getAudit().getDepartment().getId())))
                .count();
        LocalDate today = LocalDate.now();
        List<Finding> openFindings = findings.stream()
                .filter(a_finding -> "Open".equals(a_finding.getStatus()))
                .toList();
        List<Capa> openCapas = capas.stream().filter(a_capa -> !closed(a_capa)).toList();
        Set<Long> findingsWithCapa = capas.stream()
                .map(Capa::getFinding)
                .filter(a_finding -> a_finding != null && a_finding.getId() != null)
                .map(Finding::getId)
                .collect(Collectors.toSet());
        Set<Long> findingsWithLateCapa = openCapas.stream()
                .filter(a_capa -> pastDue(a_capa, today))
                .map(Capa::getFinding)
                .filter(a_finding -> a_finding != null && a_finding.getId() != null)
                .map(Finding::getId)
                .collect(Collectors.toSet());
        int needingAction = (int) openFindings.stream()
                .filter(a_finding -> needsAction(a_finding, findingsWithCapa, today))
                .count();
        int overdueCapas = (int) openCapas.stream().filter(a_capa -> pastDue(a_capa, today)).count();
        int overdueAudits = (int) audits.stream()
                .filter(a_audit -> !done(a_audit) && a_audit.getAuditDate() != null && a_audit.getAuditDate().isBefore(today))
                .count();
        ComplianceRollup rollup = rollup(audits);
        List<AttentionItem> attention = attention(openFindings, openCapas, audits, findingsWithCapa, findingsWithLateCapa, today);
        int shown = Math.min(attention.size(), ATTENTION_LIMIT);
        return new Summary(
                (int) departmentRepository.countByOrganizationIdAndDeletedAtIsNull(organizationId),
                audits.size(),
                completed,
                audits.size() - completed,
                openFindings.size(),
                openCapas.size(),
                evidence,
                rollup.average(),
                (int) assessorRepository.countByOrganizationIdAndDeletedAtIsNull(organizationId),
                overdueAudits,
                needingAction,
                overdueCapas,
                rollup.met(),
                rollup.partial(),
                rollup.notMet(),
                rollup.notApplicable(),
                rollup.unscored(),
                rollup.departments().size(),
                rollup.departments().stream().limit(DEPARTMENT_LIMIT).toList(),
                attention.size(),
                attention.subList(0, shown));
    }

    private ComplianceRollup rollup(List<Audit> a_audits) {
        int standards = (int) elementRepository.countByDeletedAtIsNull();
        Map<Long, List<String>> scores = new LinkedHashMap<>();
        if (!a_audits.isEmpty()) {
            List<Long> ids = a_audits.stream().map(Audit::getId).toList();
            for (AuditScore score : scoreRepository.findByAuditIdInAndDeletedAtIsNull(ids)) {
                scores.computeIfAbsent(score.getAudit().getId(), a_id -> new ArrayList<>()).add(score.getScoreValue());
            }
        }
        int sum = 0;
        int met = 0;
        int partial = 0;
        int notMet = 0;
        int notApplicable = 0;
        int unscored = 0;
        Map<String, DeptAgg> departments = new LinkedHashMap<>();
        for (Audit audit : a_audits) {
            ComplianceView view = ComplianceCalculator.fromValues(
                    standards, scores.getOrDefault(audit.getId(), List.of()));
            sum += view.percent();
            met += view.met();
            partial += view.partial();
            notMet += view.notMet();
            notApplicable += view.na();
            unscored += view.unscored();
            Long departmentId = audit.getDepartment() == null ? null : audit.getDepartment().getId();
            String key = departmentId != null
                    ? "id:" + departmentId
                    : "name:" + blank(audit.getDepartmentName());
            String name = departmentName(audit.getDepartment(), audit.getDepartmentName());
            departments.merge(key, new DeptAgg(departmentId, name, view.percent(), 1), DeptAgg::plus);
        }
        int average = a_audits.isEmpty() ? 0 : Math.round(sum / (float) a_audits.size());
        List<DepartmentScore> ranked = departments.values().stream()
                .map(DeptAgg::score)
                .sorted(Comparator.comparingInt(DepartmentScore::percent).thenComparing(DepartmentScore::name))
                .toList();
        return new ComplianceRollup(average, met, partial, notMet, notApplicable, unscored, ranked);
    }

    private List<AttentionItem> attention(
            List<Finding> a_openFindings,
            List<Capa> a_openCapas,
            List<Audit> a_audits,
            Set<Long> a_findingsWithCapa,
            Set<Long> a_findingsWithLateCapa,
            LocalDate a_today) {
        List<Draft> rows = new ArrayList<>();
        for (Capa capa : a_openCapas) {
            if (!pastDue(capa, a_today)) {
                continue;
            }
            Finding finding = capa.getFinding();
            long days = ChronoUnit.DAYS.between(capa.getTargetDate(), a_today);
            rows.add(new Draft(0, days, new AttentionItem(
                    "danger",
                    "Past-due CAPA",
                    plain(finding == null ? "CAPA" : finding.getGsrCode() + " — " + capa.getCorrectiveAction()),
                    departmentName(finding == null ? null : finding.getDepartment(), null),
                    person(capa.getResponsiblePerson()),
                    ageLabel(days),
                    href(finding))));
        }
        for (Finding finding : a_openFindings) {
            if (a_findingsWithCapa.contains(finding.getId())) {
                continue;
            }
            Long age = ageDays(finding.getCreatedAt(), a_today);
            rows.add(new Draft(1, age == null ? 0 : age, new AttentionItem(
                    "danger",
                    "Finding with no CAPA",
                    plain(finding.getGsrCode() + " — " + finding.getDescription()),
                    departmentName(finding.getDepartment(), null),
                    "No owner",
                    age == null ? "Unknown" : ageLabel(age),
                    href(finding))));
        }
        for (Audit audit : a_audits) {
            if (done(audit) || audit.getAuditDate() == null || !audit.getAuditDate().isBefore(a_today)) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(audit.getAuditDate(), a_today);
            rows.add(new Draft(2, days, new AttentionItem(
                    "danger",
                    "Audit past its date",
                    plain(audit.getStatus()),
                    departmentName(audit.getDepartment(), audit.getDepartmentName()),
                    person(audit.getAuditorName()),
                    ageLabel(days),
                    "/audits/" + audit.getId())));
        }
        for (Finding finding : a_openFindings) {
            if (!a_findingsWithCapa.contains(finding.getId()) || a_findingsWithLateCapa.contains(finding.getId())) {
                continue;
            }
            Long age = ageDays(finding.getCreatedAt(), a_today);
            if (age == null || age <= FINDING_OPEN_DAYS) {
                continue;
            }
            rows.add(new Draft(3, age, new AttentionItem(
                    "danger",
                    "Open more than 30 days",
                    plain(finding.getGsrCode() + " — " + finding.getDescription()),
                    departmentName(finding.getDepartment(), null),
                    "No owner",
                    age == null ? "Unknown" : ageLabel(age),
                    href(finding))));
        }
        for (Audit audit : a_audits) {
            if (done(audit) || audit.getAuditDate() == null || !audit.getAuditDate().equals(a_today)) {
                continue;
            }
            rows.add(new Draft(4, 0, new AttentionItem(
                    "warning",
                    "Audit due today",
                    plain(audit.getStatus()),
                    departmentName(audit.getDepartment(), audit.getDepartmentName()),
                    person(audit.getAuditorName()),
                    ageLabel(0),
                    "/audits/" + audit.getId())));
        }
        return rows.stream()
                .sorted(Comparator.comparingInt(Draft::rank).thenComparing(Comparator.comparingLong(Draft::days).reversed()))
                .map(Draft::item)
                .toList();
    }

    private static boolean done(Audit a_audit) {
        return "Submitted".equals(a_audit.getStatus())
                || "Approved".equals(a_audit.getStatus())
                || "Closed".equals(a_audit.getStatus());
    }

    private static boolean closed(Capa a_capa) {
        return "Closed".equals(a_capa.getStatus());
    }

    private static boolean pastDue(Capa a_capa, LocalDate a_today) {
        return a_capa.getTargetDate() != null && a_capa.getTargetDate().isBefore(a_today);
    }

    private static boolean needsAction(Finding a_finding, Set<Long> a_withCapa, LocalDate a_today) {
        if (!a_withCapa.contains(a_finding.getId())) {
            return true;
        }
        Long age = ageDays(a_finding.getCreatedAt(), a_today);
        return age != null && age > FINDING_OPEN_DAYS;
    }

    private static Long ageDays(LocalDateTime a_created, LocalDate a_today) {
        if (a_created == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(a_created.toLocalDate(), a_today);
    }

    private static String ageLabel(long a_days) {
        if (a_days <= 0) {
            return "Today";
        }
        return dayPhrase(a_days, "day", "days");
    }

    private static String dayPhrase(long a_days, String a_singular, String a_plural) {
        long days = Math.max(a_days, 0);
        if (days == 1) {
            return "1 " + a_singular;
        }
        return days + " " + a_plural;
    }

    private static String href(Finding a_finding) {
        if (a_finding != null && a_finding.getAudit() != null && a_finding.getAudit().getId() != null) {
            return "/audits/" + a_finding.getAudit().getId();
        }
        return "/findings";
    }

    private static String departmentName(Department a_department, String a_fallback) {
        if (a_department != null && !blank(a_department.getNameEn()).isEmpty()) {
            return a_department.getNameEn().trim();
        }
        if (a_department != null && !blank(a_department.getNameAr()).isEmpty()) {
            return a_department.getNameAr().trim();
        }
        String fallback = blank(a_fallback).trim();
        return fallback.isEmpty() ? "—" : fallback;
    }

    private static String person(String a_name) {
        String name = blank(a_name).trim();
        return name.isEmpty() ? "No owner" : name;
    }

    private static String blank(String a_value) {
        return a_value == null ? "" : a_value;
    }

    private static String plain(String a_value) {
        return blank(a_value).trim().replaceAll("\\s+", " ");
    }

    private static final int FINDING_OPEN_DAYS = 30;
    private static final int ATTENTION_LIMIT = 12;
    private static final int DEPARTMENT_LIMIT = 8;

    private record DeptAgg(Long departmentId, String name, int percentSum, int audits) {
        private DeptAgg plus(DeptAgg a_other) {
            Long id = departmentId != null ? departmentId : a_other.departmentId;
            return new DeptAgg(id, name, percentSum + a_other.percentSum, audits + a_other.audits);
        }

        private DepartmentScore score() {
            int percent = audits == 0 ? 0 : Math.round(percentSum / (float) audits);
            return new DepartmentScore(name, percent, audits, departmentId);
        }
    }

    private record Draft(int rank, long days, AttentionItem item) {}

    private record ComplianceRollup(
            int average,
            int met,
            int partial,
            int notMet,
            int notApplicable,
            int unscored,
            List<DepartmentScore> departments) {}

    public record DepartmentScore(String name, int percent, int audits, Long departmentId) {}

    public record AttentionItem(
            String tone,
            String kind,
            String title,
            String department,
            String owner,
            String age,
            String href) {}

    public record Summary(
            int departments,
            int audits,
            int completedAudits,
            int pendingAudits,
            int openFindings,
            int openCapas,
            int evidence,
            int averageCompliance,
            int assessors,
            int overdueAudits,
            int findingsNeedingAction,
            int overdueCapas,
            int met,
            int partial,
            int notMet,
            int notApplicable,
            int unscored,
            int scoredDepartments,
            List<DepartmentScore> departmentsByCompliance,
            int attentionTotal,
            List<AttentionItem> attention) {}
}
