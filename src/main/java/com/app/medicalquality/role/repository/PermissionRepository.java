package com.app.medicalquality.role.repository;

import com.app.medicalquality.role.entity.Permission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByName(String a_name);

    List<Permission> findByDeletedAtIsNullOrderByNameAsc();
}
