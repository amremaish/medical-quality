package com.app.medicalquality.hospital.service;

import com.app.medicalquality.exception.NotFoundException;
import com.app.medicalquality.hospital.entity.Assessor;
import com.app.medicalquality.hospital.entity.Department;
import com.app.medicalquality.hospital.repository.AssessorRepository;
import com.app.medicalquality.hospital.repository.DepartmentRepository;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HospitalQueryService {

    private final DepartmentRepository departmentRepository;
    private final AssessorRepository assessorRepository;

    public HospitalQueryService(DepartmentRepository a_departmentRepository, AssessorRepository a_assessorRepository) {
        this.departmentRepository = a_departmentRepository;
        this.assessorRepository = a_assessorRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentView> departments() {
        return departmentRepository.findByDeletedAtIsNullOrderByCodeAsc().stream()
                .map(this::departmentView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AssessorView> assessors() {
        return assessorRepository.findByDeletedAtIsNullOrderByCodeAsc().stream()
                .map(a_assessor -> new AssessorView(
                        a_assessor.getId(), a_assessor.getCode(), a_assessor.getNameEn(), a_assessor.getNameAr(), a_assessor.isActive()))
                .toList();
    }

    @Transactional
    public DepartmentView createDepartment(DepartmentForm a_form) {
        Department department = new Department();
        department.setCode(a_form.getCode().trim());
        department.setNameAr(a_form.getNameAr().trim());
        department.setNameEn(a_form.getNameEn() == null ? a_form.getNameAr().trim() : a_form.getNameEn().trim());
        department.setDeptType(a_form.getDeptType());
        department.setStatus("Active");
        if (a_form.getAssessorId() != null) {
            department.setAssessor(assessorRepository
                    .findById(a_form.getAssessorId())
                    .orElseThrow(() -> new NotFoundException("error.assessor.not_found")));
        }
        departmentRepository.save(department);
        return departmentView(department);
    }

    private DepartmentView departmentView(Department a_department) {
        String assessor = a_department.getAssessor() == null ? "" : a_department.getAssessor().getNameEn();
        return new DepartmentView(
                a_department.getId(),
                a_department.getCode(),
                a_department.getNameAr(),
                a_department.getDeptType(),
                assessor);
    }

    public record DepartmentView(Long id, String code, String nameAr, String deptType, String assessorName) {}

    public record AssessorView(Long id, String code, String nameEn, String nameAr, boolean active) {}

    @Getter
    @Setter
    public static class DepartmentForm {
        private String code;
        private String nameAr;
        private String nameEn;
        private String deptType;
        private Long assessorId;
    }
}
