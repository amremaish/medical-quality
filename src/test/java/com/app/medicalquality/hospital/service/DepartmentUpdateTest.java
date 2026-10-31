package com.app.medicalquality.hospital.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.app.medicalquality.hospital.entity.Assessor;
import com.app.medicalquality.hospital.entity.Department;
import com.app.medicalquality.hospital.entity.DepartmentType;
import com.app.medicalquality.hospital.entity.Unit;
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

class DepartmentUpdateTest {

    private DepartmentRepository departmentRepository;
    private DepartmentTypeRepository departmentTypeRepository;
    private AssessorRepository assessorRepository;
    private UnitRepository unitRepository;
    private HospitalQueryService service;
    private Department department;
    private DepartmentType pharmacy;
    private Unit existing;

    @BeforeEach
    void setUp() {
        departmentRepository = mock(DepartmentRepository.class);
        departmentTypeRepository = mock(DepartmentTypeRepository.class);
        assessorRepository = mock(AssessorRepository.class);
        unitRepository = mock(UnitRepository.class);
        HospitalContext hospitalContext = mock(HospitalContext.class);
        service = new HospitalQueryService(
                departmentRepository,
                departmentTypeRepository,
                assessorRepository,
                unitRepository,
                hospitalContext);
        Organization organization = new Organization();
        organization.setId(1L);
        when(hospitalContext.require(org.mockito.ArgumentMatchers.any())).thenReturn(organization);
        pharmacy = type("PHARMACY", "Pharmacy Services");
        department = department(4L, "DEPT-004");
        department.setDepartmentType(pharmacy);
        Assessor assessor = new Assessor();
        assessor.setId(8L);
        assessor.setNameEn("Mary Fayez");
        department.setAssessor(assessor);
        existing = new Unit();
        existing.setName("Old unit");
        when(departmentRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(4L, 1L)).thenReturn(Optional.of(department));
        when(unitRepository.findByDepartmentIdAndDeletedAtIsNullOrderByIdAsc(4L)).thenReturn(List.of(existing));
    }

    @Test
    void saveRewritesTheDepartmentAndReplacesItsUnits() {
        DepartmentType inpatient = type("INPATIENT", "Inpatient Services");
        when(departmentTypeRepository.findById("INPATIENT")).thenReturn(Optional.of(inpatient));
        when(unitRepository.findByDepartmentIdAndDeletedAtIsNullOrderByIdAsc(4L))
                .thenReturn(List.of(existing), List.of());
        HospitalQueryService.DepartmentForm form = form("DEPT-014", "INPATIENT");
        form.setHeadName("Dr. Mona");
        form.setCoordinatorName("");
        form.setStatus("Inactive");
        form.setAssessorId(null);
        form.setUnits(List.of("Ward A", "  ", "Ward B"));
        HospitalQueryService.DepartmentDetail detail = service.updateDepartment(4L, form, mock(Authentication.class));
        assertEquals("DEPT-004", department.getCode());
        assertEquals("صيدلية", department.getNameAr());
        assertEquals("Pharmacy", department.getNameEn());
        assertEquals(inpatient, department.getDepartmentType());
        assertEquals("Dr. Mona", department.getHeadName());
        assertNull(department.getCoordinatorName());
        assertEquals("Inactive", department.getStatus());
        assertNull(department.getAssessor());
        assertNotNull(existing.getDeletedAt());
        assertEquals("DEPT-004", detail.code());
        assertEquals("INPATIENT", detail.deptType());
        assertNull(detail.assessorId());
    }

    @Test
    void saveKeepsTheCodeAssignedAtCreation() {
        when(departmentTypeRepository.findById("PHARMACY")).thenReturn(Optional.of(pharmacy));
        when(unitRepository.findByDepartmentIdAndDeletedAtIsNullOrderByIdAsc(4L)).thenReturn(List.of());
        service.updateDepartment(4L, form("CUSTOM", "PHARMACY"), mock(Authentication.class));
        assertEquals("DEPT-004", department.getCode());
    }

    private HospitalQueryService.DepartmentForm form(String a_code, String a_type) {
        HospitalQueryService.DepartmentForm form = new HospitalQueryService.DepartmentForm();
        form.setCode(a_code);
        form.setNameAr("صيدلية");
        form.setNameEn("Pharmacy");
        form.setDeptType(a_type);
        form.setStatus("Active");
        form.setUnits(List.of());
        return form;
    }

    private DepartmentType type(String a_code, String a_name) {
        DepartmentType type = new DepartmentType();
        type.setCode(a_code);
        type.setNameEn(a_name);
        return type;
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
