package com.app.medicalquality.hospital.repository;

import com.app.medicalquality.hospital.entity.Department;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    @EntityGraph(attributePaths = {"assessor", "departmentType"})
    List<Department> findByDeletedAtIsNullOrderByCodeAsc();

    @EntityGraph(attributePaths = {"assessor", "departmentType"})
    List<Department> findByOrganizationIdAndDeletedAtIsNullOrderByCodeAsc(Long a_organizationId);

    List<Department> findByAssessorIdAndDeletedAtIsNull(Long a_assessorId);

    Optional<Department> findByIdAndDeletedAtIsNull(Long a_id);

    boolean existsByCode(String a_code);

    long countByDeletedAtIsNull();

    long countByOrganizationIdAndDeletedAtIsNull(Long a_organizationId);
}
