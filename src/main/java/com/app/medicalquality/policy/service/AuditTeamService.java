package com.app.medicalquality.policy.service;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.service.CurrentUserService;
import com.app.medicalquality.exception.NotFoundException;
import com.app.medicalquality.gsr.entity.Audit;
import com.app.medicalquality.gsr.service.AuditAccess;
import com.app.medicalquality.hospital.entity.Assessor;
import com.app.medicalquality.hospital.entity.Department;
import com.app.medicalquality.hospital.repository.AssessorRepository;
import com.app.medicalquality.policy.AssessorAssignmentRules;
import com.app.medicalquality.policy.ResolvedPolicy;
import com.app.medicalquality.policy.entity.AuditAssessor;
import com.app.medicalquality.policy.repository.AuditAssessorRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditTeamService {

    private static final Set<String> OPEN = Set.of("Draft", "In Progress", "Pending Review", "Rejected");

    private final AuditAccess auditAccess;
    private final AssessorRepository assessorRepository;
    private final AuditAssessorRepository auditAssessorRepository;
    private final QualityPolicyService policyService;
    private final CurrentUserService currentUserService;

    public AuditTeamService(
            AuditAccess a_auditAccess,
            AssessorRepository a_assessorRepository,
            AuditAssessorRepository a_auditAssessorRepository,
            QualityPolicyService a_policyService,
            CurrentUserService a_currentUserService) {
        this.auditAccess = a_auditAccess;
        this.assessorRepository = a_assessorRepository;
        this.auditAssessorRepository = a_auditAssessorRepository;
        this.policyService = a_policyService;
        this.currentUserService = a_currentUserService;
    }

    @Transactional(readOnly = true)
    public List<Member> list(Long a_auditId, Authentication a_authentication) {
        Audit audit = auditAccess.require(a_auditId, a_authentication);
        return auditAssessorRepository.findByAudit_IdAndDeletedAtIsNull(audit.getId()).stream()
                .map(this::member)
                .toList();
    }

    @Transactional
    public List<Member> assign(Long a_auditId, AssignRequest a_request, Authentication a_authentication) {
        Audit audit = auditAccess.requireEditable(a_auditId, a_authentication);
        Department department = audit.getDepartment();
        if (department == null || department.getOrganization() == null) {
            throw new NotFoundException("error.department.not_found");
        }
        User actor = currentUserService.require(a_authentication);
        Long organizationId = department.getOrganization().getId();
        ResolvedPolicy policy = policyService.current(organizationId);
        Assessor assessor = assessor(a_request.getAssessorId(), organizationId, actor, policy);
        List<AuditAssessor> team = auditAssessorRepository.findByAudit_IdAndDeletedAtIsNull(audit.getId());
        boolean already = team.stream().anyMatch(a_row -> a_row.getAssessor().getId().equals(assessor.getId()));
        List<AuditAssessor> history = auditAssessorRepository.findByAssessor_IdAndDeletedAtIsNull(assessor.getId());
        AssessorAssignmentRules.check(policy, new AssessorAssignmentRules.Facts(
                department.getNameEn(),
                department.getNameAr(),
                department.getCode(),
                team.size(),
                already,
                department.getAssessor() != null && assessor.getId().equals(department.getAssessor().getId()),
                Boolean.TRUE.equals(a_request.getCoiDeclared()),
                concurrent(history, audit.getId()),
                consecutive(history, department.getId()),
                daysSince(history, department.getId(), audit.getAuditDate()),
                assessor.isExternal(),
                assessor.getOrganization() != null && organizationId.equals(assessor.getOrganization().getId())));
        AuditAssessor row = new AuditAssessor();
        row.setAudit(audit);
        row.setAssessor(assessor);
        row.setLead(Boolean.TRUE.equals(a_request.getLead()));
        row.setCoiDeclared(Boolean.TRUE.equals(a_request.getCoiDeclared()));
        auditAssessorRepository.save(row);
        team.add(row);
        return team.stream().map(this::member).toList();
    }

    @Transactional
    public List<Member> unassign(Long a_auditId, Long a_assessorId, Authentication a_authentication) {
        Audit audit = auditAccess.requireEditable(a_auditId, a_authentication);
        AuditAssessor row = auditAssessorRepository
                .findByAudit_IdAndAssessor_IdAndDeletedAtIsNull(audit.getId(), a_assessorId)
                .orElseThrow(() -> new NotFoundException("error.assessor.not_found"));
        row.setDeletedAt(java.time.LocalDateTime.now());
        return auditAssessorRepository.findByAudit_IdAndDeletedAtIsNull(audit.getId()).stream()
                .filter(a_member -> !a_member.getId().equals(row.getId()))
                .map(this::member)
                .toList();
    }

    private Assessor assessor(Long a_assessorId, Long a_organizationId, User a_actor, ResolvedPolicy a_policy) {
        if (a_assessorId == null) {
            throw new NotFoundException("error.assessor.not_found");
        }
        Assessor sameHospital = assessorRepository
                .findByIdAndOrganizationIdAndDeletedAtIsNull(a_assessorId, a_organizationId)
                .orElse(null);
        if (sameHospital != null) {
            return sameHospital;
        }
        if (!a_actor.isSuperAdmin() || !a_policy.bool("assessors.allowCrossHospital")) {
            throw new NotFoundException("error.assessor.not_found");
        }
        return assessorRepository
                .findById(a_assessorId)
                .filter(a_assessor -> a_assessor.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("error.assessor.not_found"));
    }

    private int concurrent(List<AuditAssessor> a_history, Long a_auditId) {
        int count = 0;
        for (AuditAssessor row : a_history) {
            Audit audit = row.getAudit();
            if (audit == null || audit.getDeletedAt() != null || audit.getId().equals(a_auditId)) {
                continue;
            }
            if (OPEN.contains(audit.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private int consecutive(List<AuditAssessor> a_history, Long a_departmentId) {
        List<AuditAssessor> ordered = a_history.stream()
                .filter(a_row -> a_row.getAudit() != null && a_row.getAudit().getDeletedAt() == null)
                .sorted(Comparator.comparing(
                        (AuditAssessor a_row) -> a_row.getAudit().getAuditDate(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        int count = 0;
        for (AuditAssessor row : ordered) {
            Department department = row.getAudit().getDepartment();
            if (department != null && a_departmentId.equals(department.getId())) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    private Integer daysSince(List<AuditAssessor> a_history, Long a_departmentId, LocalDate a_auditDate) {
        LocalDate latest = null;
        for (AuditAssessor row : a_history) {
            Audit audit = row.getAudit();
            if (audit == null || audit.getDeletedAt() != null || audit.getDepartment() == null || audit.getAuditDate() == null) {
                continue;
            }
            if (!a_departmentId.equals(audit.getDepartment().getId())) {
                continue;
            }
            if (latest == null || audit.getAuditDate().isAfter(latest)) {
                latest = audit.getAuditDate();
            }
        }
        if (latest == null || a_auditDate == null) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(latest, a_auditDate);
        return (int) Math.max(0, days);
    }

    private Member member(AuditAssessor a_row) {
        Assessor assessor = a_row.getAssessor();
        return new Member(
                assessor.getId(),
                assessor.getNameEn(),
                assessor.getNameAr(),
                a_row.isLead(),
                a_row.isCoiDeclared());
    }

    public record Member(long assessorId, String nameEn, String nameAr, boolean lead, boolean coiDeclared) {}

    @Getter
    @Setter
    public static class AssignRequest {
        private Long assessorId;
        private Boolean lead;
        private Boolean coiDeclared;
    }
}
