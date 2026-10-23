package com.app.medicalquality.role.repository;

import com.app.medicalquality.role.entity.Role;
import com.app.medicalquality.role.entity.RolePermission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRoleAndDeletedAtIsNull(Role a_role);

    boolean existsByRole_NameAndPermission_NameAndDeletedAtIsNullAndRole_DeletedAtIsNull(
            String a_roleName, String a_permissionName);
}
