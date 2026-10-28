package com.app.medicalquality.role.repository;

import com.app.medicalquality.role.entity.Role;
import com.app.medicalquality.role.entity.RolePermission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRoleAndDeletedAtIsNull(Role a_role);

    boolean existsByRole_IdAndPermission_NameAndDeletedAtIsNullAndRole_DeletedAtIsNull(
            Long a_roleId, String a_permissionName);

    @Modifying(flushAutomatically = true)
    @Query("delete from RolePermission link where link.role.id = :roleId")
    void deleteByRoleId(@Param("roleId") Long a_roleId);
}
