package com.app.medicalquality.hospital.repository;

import com.app.medicalquality.hospital.entity.Unit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitRepository extends JpaRepository<Unit, Long> {

    List<Unit> findByDepartmentIdAndDeletedAtIsNullOrderByNameAsc(Long a_departmentId);
}
