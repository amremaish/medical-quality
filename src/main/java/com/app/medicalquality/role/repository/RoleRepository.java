package com.app.medicalquality.role.repository;

import com.app.medicalquality.role.entity.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByRoleCodeAndOrganizationIsNullAndDeletedAtIsNull(String a_roleCode);

    Optional<Role> findByRoleCodeAndOrganizationIdAndDeletedAtIsNull(String a_roleCode, Long a_organizationId);

    boolean existsByRoleCodeAndOrganizationIdAndDeletedAtIsNull(String a_roleCode, Long a_organizationId);

    List<Role> findByTemplateTrueAndOrganizationIsNullAndDeletedAtIsNull();

    List<Role> findByOrganizationIsNullAndDeletedAtIsNullOrderByNameAsc();

    List<Role> findByOrganizationIdAndDeletedAtIsNullOrderByNameAsc(Long a_organizationId);

    boolean existsByNameAndOrganizationIsNullAndDeletedAtIsNull(String a_name);

    boolean existsByOrganizationIdAndNameAndDeletedAtIsNull(Long a_organizationId, String a_name);

    Optional<Role> findByIdAndOrganizationIdAndDeletedAtIsNull(Long a_id, Long a_organizationId);
}
