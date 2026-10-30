package com.app.medicalquality.hospital.service;

import com.app.medicalquality.exception.AppException;
import com.app.medicalquality.exception.NotFoundException;
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
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HospitalQueryService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentTypeRepository departmentTypeRepository;
    private final AssessorRepository assessorRepository;
    private final UnitRepository unitRepository;
    private final HospitalContext hospitalContext;

    public HospitalQueryService(
            DepartmentRepository a_departmentRepository,
            DepartmentTypeRepository a_departmentTypeRepository,
            AssessorRepository a_assessorRepository,
            UnitRepository a_unitRepository,
            HospitalContext a_hospitalContext) {
        this.departmentRepository = a_departmentRepository;
        this.departmentTypeRepository = a_departmentTypeRepository;
        this.assessorRepository = a_assessorRepository;
        this.unitRepository = a_unitRepository;
        this.hospitalContext = a_hospitalContext;
    }

    @Transactional(readOnly = true)
    public List<DepartmentTypeView> departmentTypes() {
        return departmentTypeRepository.findAllByOrderBySortOrderAsc().stream()
                .map(a_type -> new DepartmentTypeView(a_type.getCode(), a_type.getNameEn(), a_type.getExamplesAr()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DepartmentView> departments(Authentication a_authentication) {
        Organization organization = hospitalContext.require(a_authentication);
        return departmentRepository.findByOrganizationIdAndDeletedAtIsNullOrderByCodeAsc(organization.getId()).stream()
                .map(this::departmentView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AssessorView> assessors(Authentication a_authentication) {
        Organization organization = hospitalContext.require(a_authentication);
        return assessorRepository.findByOrganizationIdAndDeletedAtIsNullOrderByCodeAsc(organization.getId()).stream()
                .map(this::assessorView)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssessorDetail assessor(Long a_id, Authentication a_authentication) {
        Organization organization = hospitalContext.require(a_authentication);
        return assessorDetail(loadAssessor(a_id, organization.getId()), organization.getId());
    }

    @Transactional
    public AssessorDetail updateAssessor(Long a_id, AssessorForm a_form, Authentication a_authentication) {
        Organization organization = hospitalContext.require(a_authentication);
        Assessor assessor = loadAssessor(a_id, organization.getId());
        String code = required(a_form.getCode(), 20);
        String nameAr = required(a_form.getNameAr(), 200);
        String nameEn = required(a_form.getNameEn(), 200);
        if (code == null || nameAr == null || nameEn == null) {
            throw new AppException("error.assessor.fields");
        }
        if (assessorRepository.existsByCodeAndIdNot(code, assessor.getId())) {
            throw new AppException("error.assessor.code_taken");
        }
        assessor.setCode(code);
        assessor.setNameAr(nameAr);
        assessor.setNameEn(nameEn);
        assessor.setNote(optional(a_form.getNote(), 300));
        assessor.setActive(Boolean.TRUE.equals(a_form.getActive()));
        assessorRepository.save(assessor);
        return assessorDetail(assessor, organization.getId());
    }

    private Assessor loadAssessor(Long a_id, Long a_organizationId) {
        return assessorRepository
                .findByIdAndOrganizationIdAndDeletedAtIsNull(a_id, a_organizationId)
                .orElseThrow(() -> new NotFoundException("error.assessor.not_found"));
    }

    private AssessorView assessorView(Assessor a_assessor) {
        return new AssessorView(
                a_assessor.getId(),
                a_assessor.getCode(),
                a_assessor.getNameEn(),
                a_assessor.getNameAr(),
                a_assessor.isActive());
    }

    private AssessorDetail assessorDetail(Assessor a_assessor, Long a_organizationId) {
        List<AssignedDepartment> departments = departmentRepository
                .findByAssessorIdAndOrganizationIdAndDeletedAtIsNullOrderByCodeAsc(a_assessor.getId(), a_organizationId)
                .stream()
                .map(a_department -> new AssignedDepartment(
                        a_department.getId(),
                        a_department.getCode(),
                        a_department.getNameAr(),
                        a_department.getNameEn(),
                        a_department.getStatus()))
                .toList();
        return new AssessorDetail(
                a_assessor.getId(),
                a_assessor.getCode(),
                a_assessor.getNameEn(),
                a_assessor.getNameAr(),
                a_assessor.getNote() == null ? "" : a_assessor.getNote(),
                a_assessor.isActive(),
                departments);
    }

    @Transactional
    public DepartmentView createDepartment(DepartmentForm a_form, Authentication a_authentication) {
        hospitalContext.requireManager(a_authentication);
        Organization organization = hospitalContext.require(a_authentication);
        String code = required(a_form.getCode(), 20);
        String nameAr = required(a_form.getNameAr(), 200);
        String nameEn = required(a_form.getNameEn(), 200);
        String deptType = required(a_form.getDeptType(), 40);
        if (code == null || nameAr == null || nameEn == null || deptType == null) {
            throw new AppException("error.department.fields");
        }
        DepartmentType type = departmentTypeRepository
                .findById(deptType)
                .orElseThrow(() -> new AppException("error.department.type"));
        if (departmentRepository.existsByCode(code)) {
            throw new AppException("error.department.code_taken");
        }
        Department department = new Department();
        department.setOrganization(organization);
        department.setCode(code);
        department.setNameAr(nameAr);
        department.setNameEn(nameEn);
        department.setDepartmentType(type);
        department.setHeadName(optional(a_form.getHeadName(), 200));
        department.setCoordinatorName(optional(a_form.getCoordinatorName(), 200));
        department.setStatus(status(a_form.getStatus()));
        if (a_form.getAssessorId() != null) {
            department.setAssessor(assessorRepository
                    .findByIdAndOrganizationIdAndDeletedAtIsNull(a_form.getAssessorId(), organization.getId())
                    .orElseThrow(() -> new NotFoundException("error.assessor.not_found")));
        }
        departmentRepository.save(department);
        saveUnits(department, a_form.getUnits());
        return departmentView(department);
    }

    private void saveUnits(Department a_department, List<String> a_units) {
        if (a_units == null) {
            return;
        }
        for (String unitName : a_units) {
            String name = optional(unitName, 200);
            if (name == null) {
                continue;
            }
            Unit unit = new Unit();
            unit.setDepartment(a_department);
            unit.setName(name);
            unitRepository.save(unit);
        }
    }

    private String status(String a_status) {
        if (a_status == null || a_status.isBlank() || "Active".equals(a_status.trim())) {
            return "Active";
        }
        if ("Inactive".equals(a_status.trim())) {
            return "Inactive";
        }
        throw new AppException("error.validation.failed");
    }

    private String required(String a_value, int a_max) {
        String value = optional(a_value, a_max);
        return value;
    }

    private String optional(String a_value, int a_max) {
        if (a_value == null || a_value.isBlank()) {
            return null;
        }
        String value = a_value.trim();
        if (value.length() > a_max) {
            throw new AppException("error.validation.failed");
        }
        return value;
    }

    private DepartmentView departmentView(Department a_department) {
        String assessor = a_department.getAssessor() == null ? "" : a_department.getAssessor().getNameEn();
        return new DepartmentView(
                a_department.getId(),
                a_department.getCode(),
                a_department.getNameAr(),
                a_department.getNameEn(),
                a_department.getDepartmentType().getNameEn(),
                a_department.getHeadName() == null ? "" : a_department.getHeadName(),
                assessor,
                a_department.getStatus());
    }

    public record DepartmentView(
            Long id,
            String code,
            String nameAr,
            String nameEn,
            String deptType,
            String headName,
            String assessorName,
            String status) {}

    public record DepartmentTypeView(String code, String nameEn, String examplesAr) {}

    public record AssessorView(Long id, String code, String nameEn, String nameAr, boolean active) {}

    public record AssignedDepartment(Long id, String code, String nameAr, String nameEn, String status) {}

    public record AssessorDetail(
            Long id,
            String code,
            String nameEn,
            String nameAr,
            String note,
            boolean active,
            List<AssignedDepartment> departments) {}

    @Getter
    @Setter
    public static class AssessorForm {
        private String code;
        private String nameAr;
        private String nameEn;
        private String note;
        private Boolean active;
    }

    @Getter
    @Setter
    public static class DepartmentForm {
        private String code;
        private String nameAr;
        private String nameEn;
        private String deptType;
        private String headName;
        private String coordinatorName;
        private String status;
        private Long assessorId;
        private List<String> units;
    }
}
