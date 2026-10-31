package com.app.medicalquality.hospital.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.app.medicalquality.hospital.entity.Department;
import com.app.medicalquality.hospital.entity.DepartmentType;
import com.app.medicalquality.hospital.repository.AssessorRepository;
import com.app.medicalquality.hospital.repository.DepartmentRepository;
import com.app.medicalquality.hospital.repository.DepartmentTypeRepository;
import com.app.medicalquality.hospital.repository.UnitRepository;
import com.app.medicalquality.organization.entity.Organization;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;

class DepartmentCreateTest {

    private DepartmentRepository departmentRepository;
    private HospitalQueryService service;
    private DepartmentType pharmacy;

    @BeforeEach
    void setUp() {
        departmentRepository = mock(DepartmentRepository.class);
        DepartmentTypeRepository departmentTypeRepository = mock(DepartmentTypeRepository.class);
        HospitalContext hospitalContext = mock(HospitalContext.class);
        service = new HospitalQueryService(
                departmentRepository,
                departmentTypeRepository,
                mock(AssessorRepository.class),
                mock(UnitRepository.class),
                hospitalContext);
        Organization organization = new Organization();
        organization.setId(1L);
        when(hospitalContext.require(any())).thenReturn(organization);
        pharmacy = new DepartmentType();
        pharmacy.setCode("PHARMACY");
        pharmacy.setNameEn("Pharmacy Services");
        when(departmentTypeRepository.findById("PHARMACY")).thenReturn(Optional.of(pharmacy));
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createAssignsTheNextCodeAndIgnoresOneFromTheClient() {
        when(departmentRepository.findCodes()).thenReturn(List.of("DEPT-001", "DEPT-012", "RAD"));
        when(departmentRepository.existsByCode("DEPT-013")).thenReturn(false);
        HospitalQueryService.DepartmentForm form = form();
        form.setCode("CUSTOM");
        HospitalQueryService.DepartmentView created = service.createDepartment(form, mock(Authentication.class));
        ArgumentCaptor<Department> saved = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).save(saved.capture());
        assertEquals("DEPT-013", saved.getValue().getCode());
        assertEquals("DEPT-013", created.code());
        assertEquals(pharmacy, saved.getValue().getDepartmentType());
    }

    @Test
    void createStartsAtTheFirstCodeWhenNoneExist() {
        when(departmentRepository.findCodes()).thenReturn(List.of());
        when(departmentRepository.existsByCode("DEPT-001")).thenReturn(false);
        HospitalQueryService.DepartmentView created = service.createDepartment(form(), mock(Authentication.class));
        assertEquals("DEPT-001", created.code());
    }

    private HospitalQueryService.DepartmentForm form() {
        HospitalQueryService.DepartmentForm form = new HospitalQueryService.DepartmentForm();
        form.setNameAr("صيدلية");
        form.setNameEn("Pharmacy");
        form.setDeptType("PHARMACY");
        form.setStatus("Active");
        return form;
    }
}
