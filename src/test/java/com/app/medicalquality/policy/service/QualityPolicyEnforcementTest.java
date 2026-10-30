package com.app.medicalquality.policy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.service.CurrentUserService;
import com.app.medicalquality.exception.AppException;
import com.app.medicalquality.finding.entity.Finding;
import com.app.medicalquality.finding.repository.CapaRepository;
import com.app.medicalquality.finding.repository.FindingRepository;
import com.app.medicalquality.finding.service.FindingService;
import com.app.medicalquality.gsr.entity.Audit;
import com.app.medicalquality.gsr.service.AuditAccess;
import com.app.medicalquality.gsr.service.AuditTimelineWriter;
import com.app.medicalquality.hospital.entity.Assessor;
import com.app.medicalquality.hospital.entity.Department;
import com.app.medicalquality.hospital.repository.AssessorRepository;
import com.app.medicalquality.hospital.service.DepartmentScope;
import com.app.medicalquality.organization.entity.Organization;
import com.app.medicalquality.policy.QualityPolicyCatalog;
import com.app.medicalquality.policy.ResolvedPolicy;
import com.app.medicalquality.policy.entity.AuditAssessor;
import com.app.medicalquality.policy.repository.AuditAssessorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class QualityPolicyEnforcementTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void capaDueDateUsesThePolicyFrozenOnTheFinding() {
        FindingRepository findings = mock(FindingRepository.class);
        CapaRepository capas = mock(CapaRepository.class);
        QualityPolicyService policies = mock(QualityPolicyService.class);
        DepartmentScope scope = mock(DepartmentScope.class);
        CurrentUserService users = mock(CurrentUserService.class);
        FindingService service = new FindingService(
                findings, capas, mock(AuditAccess.class), users, mock(AuditTimelineWriter.class), scope, policies);
        Finding early = finding(4L, LocalDateTime.of(2026, 2, 1, 9, 0));
        Finding later = finding(5L, LocalDateTime.of(2026, 6, 2, 9, 0));
        when(findings.findByIdAndDeletedAtIsNull(4L)).thenReturn(Optional.of(early));
        when(findings.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(later));
        User actor = new User();
        actor.setId(1L);
        when(users.require(any())).thenReturn(actor);
        when(scope.departmentIds(any(), any())).thenReturn(Set.of(3L));
        when(policies.forFinding(early)).thenReturn(policy(30));
        when(policies.forFinding(later)).thenReturn(policy(7));
        when(capas.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Authentication authentication = mock(Authentication.class);

        assertEquals(LocalDate.of(2026, 3, 3), service.addCapa(4L, form(null), authentication).targetDate());
        assertEquals(LocalDate.of(2026, 6, 9), service.addCapa(5L, form(null), authentication).targetDate());
    }

    @Test
    void aLaterTargetDateIsRejected() {
        FindingRepository findings = mock(FindingRepository.class);
        CapaRepository capas = mock(CapaRepository.class);
        QualityPolicyService policies = mock(QualityPolicyService.class);
        DepartmentScope scope = mock(DepartmentScope.class);
        CurrentUserService users = mock(CurrentUserService.class);
        FindingService service = new FindingService(
                findings, capas, mock(AuditAccess.class), users, mock(AuditTimelineWriter.class), scope, policies);
        Finding finding = finding(4L, LocalDateTime.of(2026, 6, 2, 9, 0));
        when(findings.findByIdAndDeletedAtIsNull(4L)).thenReturn(Optional.of(finding));
        User actor = new User();
        when(users.require(any())).thenReturn(actor);
        when(scope.departmentIds(any(), any())).thenReturn(Set.of(3L));
        when(policies.forFinding(finding)).thenReturn(policy(7));

        AppException error = assertThrows(
                AppException.class,
                () -> service.addCapa(4L, form(LocalDate.of(2026, 7, 1)), mock(Authentication.class)));
        assertEquals("error.policy.capa.deadline", error.getMessageKey());
        verify(capas, never()).save(any());
    }

    @Test
    void assigningAThirdRadiologyAssessorIsBlocked() {
        AuditAccess access = mock(AuditAccess.class);
        AssessorRepository assessors = mock(AssessorRepository.class);
        AuditAssessorRepository teams = mock(AuditAssessorRepository.class);
        QualityPolicyService policies = mock(QualityPolicyService.class);
        CurrentUserService users = mock(CurrentUserService.class);
        AuditTeamService service = new AuditTeamService(access, assessors, teams, policies, users);
        Organization organization = new Organization();
        organization.setId(8L);
        Department department = new Department();
        department.setId(3L);
        department.setNameEn("Radiology");
        department.setNameAr("الأشعة");
        department.setCode("RAD");
        department.setOrganization(organization);
        Audit audit = new Audit();
        audit.setId(11L);
        audit.setDepartment(department);
        audit.setAuditDate(LocalDate.of(2026, 10, 30));
        audit.setStatus("Draft");
        when(access.requireEditable(any(), any())).thenReturn(audit);
        User actor = new User();
        actor.setId(1L);
        when(users.require(any())).thenReturn(actor);
        ObjectNode document = QualityPolicyCatalog.defaults(mapper);
        document.put("assessors.maxPerAudit", 5);
        document.set("assessors.maxPerDepartmentByName", mapper.createObjectNode().put("Radiology", 2));
        when(policies.current(8L)).thenReturn(new ResolvedPolicy(document));
        Assessor joining = assessor(9L, organization);
        when(assessors.findByIdAndOrganizationIdAndDeletedAtIsNull(9L, 8L)).thenReturn(Optional.of(joining));
        when(teams.findByAudit_IdAndDeletedAtIsNull(11L)).thenReturn(List.of(
                member(assessor(1L, organization)), member(assessor(2L, organization))));
        when(teams.findByAssessor_IdAndDeletedAtIsNull(9L)).thenReturn(List.of());
        AuditTeamService.AssignRequest request = new AuditTeamService.AssignRequest();
        request.setAssessorId(9L);
        request.setCoiDeclared(true);

        AppException error = assertThrows(
                AppException.class, () -> service.assign(11L, request, mock(Authentication.class)));
        assertEquals("error.policy.assessor.max", error.getMessageKey());
        assertEquals("Radiology", error.getArgs()[0]);
        assertEquals(2, error.getArgs()[1]);
        verify(teams, never()).save(any());
    }

    private ResolvedPolicy policy(int a_majorDays) {
        ObjectNode document = QualityPolicyCatalog.defaults(mapper);
        document.put("capa.deadline.major", a_majorDays);
        return new ResolvedPolicy(document);
    }

    private Finding finding(Long a_id, LocalDateTime a_opened) {
        Organization organization = new Organization();
        organization.setId(8L);
        Department department = new Department();
        department.setId(3L);
        department.setOrganization(organization);
        Finding finding = new Finding();
        finding.setId(a_id);
        finding.setDepartment(department);
        finding.setCanonicalSeverity("major");
        finding.setRiskLevel("High");
        finding.setCreatedAt(a_opened);
        finding.setGsrCode("GSR-1");
        finding.setDescription("Gap");
        finding.setStatus("Open");
        return finding;
    }

    private FindingService.CapaForm form(LocalDate a_date) {
        FindingService.CapaForm form = new FindingService.CapaForm();
        form.setCorrectiveAction("Correct the gap");
        form.setResponsiblePerson("Coordinator");
        form.setTargetDate(a_date);
        form.setProgress(0);
        form.setStatus("Open");
        return form;
    }

    private Assessor assessor(Long a_id, Organization a_organization) {
        Assessor assessor = new Assessor();
        assessor.setId(a_id);
        assessor.setNameEn("Assessor " + a_id);
        assessor.setNameAr("مقيّم");
        assessor.setOrganization(a_organization);
        assessor.setActive(true);
        return assessor;
    }

    private AuditAssessor member(Assessor a_assessor) {
        AuditAssessor row = new AuditAssessor();
        row.setAssessor(a_assessor);
        row.setLead(false);
        return row;
    }
}
