package com.app.medicalquality.hospital.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.app.medicalquality.exception.AppException;
import com.app.medicalquality.hospital.entity.Assessor;
import com.app.medicalquality.hospital.entity.Department;
import com.app.medicalquality.hospital.repository.AssessorRepository;
import com.app.medicalquality.hospital.repository.DepartmentRepository;
import com.app.medicalquality.hospital.repository.DepartmentTypeRepository;
import com.app.medicalquality.hospital.repository.UnitRepository;
import com.app.medicalquality.organization.entity.Organization;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class AssessorDepartmentAssignmentTest {

    private DepartmentRepository departmentRepository;
    private AssessorRepository assessorRepository;
    private HospitalContext hospitalContext;
    private HospitalQueryService service;
    private Organization organization;
    private Assessor assessor;
    private Department department;

    @BeforeEach
    void setUp() {
        departmentRepository = mock(DepartmentRepository.class);
        assessorRepository = mock(AssessorRepository.class);
        hospitalContext = mock(HospitalContext.class);
        service = new HospitalQueryService(
                departmentRepository,
                mock(DepartmentTypeRepository.class),
                assessorRepository,
                mock(UnitRepository.class),
                hospitalContext);
        organization = new Organization();
        organization.setId(1L);
        assessor = assessor(7L, "Norhan");
        department = department(3L, "DEPT-001");
        when(hospitalContext.require(org.mockito.ArgumentMatchers.any())).thenReturn(organization);
        when(assessorRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(7L, 1L)).thenReturn(Optional.of(assessor));
        when(departmentRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(3L, 1L)).thenReturn(Optional.of(department));
    }

    @Test
    void saveWritesTheProfileAndDepartmentListTogether() {
        department.setAssessor(assessor);
        Department added = department(9L, "DEPT-009");
        when(assessorRepository.existsByCodeAndIdNot("AS-7", 7L)).thenReturn(false);
        when(departmentRepository.findByAssessorIdAndOrganizationIdAndDeletedAtIsNullOrderByCodeAsc(7L, 1L))
                .thenReturn(List.of(department));
        when(departmentRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(9L, 1L)).thenReturn(Optional.of(added));
        HospitalQueryService.AssessorForm form = new HospitalQueryService.AssessorForm();
        form.setCode("AS-7");
        form.setNameEn("Norhan Updated");
        form.setNameAr("نورهان");
        form.setNote("Ward lead");
        form.setActive(true);
        form.setDepartmentIds(List.of(9L));
        service.updateAssessor(7L, form, mock(Authentication.class));
        assertEquals("Norhan Updated", assessor.getNameEn());
        assertEquals("Ward lead", assessor.getNote());
        assertNull(department.getAssessor());
        assertEquals(assessor, added.getAssessor());
    }

    @Test
    void assignPutsThisAssessorOnTheDepartment() {
        when(departmentRepository.findByAssessorIdAndOrganizationIdAndDeletedAtIsNullOrderByCodeAsc(7L, 1L))
                .thenReturn(List.of(department));
        HospitalQueryService.AssessorDetail detail = service.assignDepartment(7L, 3L, mock(Authentication.class));
        assertEquals(assessor, department.getAssessor());
        assertEquals(1, detail.departments().size());
        assertEquals(3L, detail.departments().get(0).id());
    }

    @Test
    void unassignClearsOnlyThisAssessorsDepartment() {
        department.setAssessor(assessor);
        when(departmentRepository.findByAssessorIdAndOrganizationIdAndDeletedAtIsNullOrderByCodeAsc(7L, 1L))
                .thenReturn(List.of());
        service.unassignDepartment(7L, 3L, mock(Authentication.class));
        assertNull(department.getAssessor());
    }

    @Test
    void unassignRefusesADepartmentHeldBySomeoneElse() {
        department.setAssessor(assessor(8L, "Mary"));
        AppException error = assertThrows(
                AppException.class, () -> service.unassignDepartment(7L, 3L, mock(Authentication.class)));
        assertEquals("error.assessor.not_on_department", error.getMessageKey());
        assertEquals(8L, department.getAssessor().getId());
    }

    private Assessor assessor(Long a_id, String a_name) {
        Assessor row = new Assessor();
        row.setId(a_id);
        row.setCode("AS-" + a_id);
        row.setNameEn(a_name);
        row.setNameAr(a_name);
        row.setActive(true);
        return row;
    }

    private Department department(Long a_id, String a_code) {
        Department row = new Department();
        row.setId(a_id);
        row.setCode(a_code);
        row.setNameAr(a_code);
        row.setNameEn(a_code);
        row.setStatus("Active");
        return row;
    }
}
