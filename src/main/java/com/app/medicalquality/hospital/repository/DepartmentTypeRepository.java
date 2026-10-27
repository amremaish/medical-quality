package com.app.medicalquality.hospital.repository;

import com.app.medicalquality.hospital.entity.DepartmentType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentTypeRepository extends JpaRepository<DepartmentType, String> {

    List<DepartmentType> findAllByOrderBySortOrderAsc();
}
