package com.app.medicalquality.hospital.repository;

import com.app.medicalquality.hospital.entity.UserDepartmentAccess;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDepartmentAccessRepository extends JpaRepository<UserDepartmentAccess, Long> {

    @EntityGraph(attributePaths = {"department", "department.organization"})
    List<UserDepartmentAccess> findByUserIdAndDeletedAtIsNull(Long a_userId);
}
