package com.app.medicalquality.auth.repository;

import com.app.medicalquality.auth.entity.User;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {
        "role",
        "role.organization",
        "assessor",
        "assessor.organization",
        "department",
        "department.organization",
        "organization"
    })
    Optional<User> findByUsername(String a_username);

    @EntityGraph(attributePaths = {"role", "organization"})
    List<User> findByOrganizationIsNotNullAndDeletedAtIsNull();

    @EntityGraph(attributePaths = {"role"})
    List<User> findByOrganizationIdAndDeletedAtIsNullOrderByEmailAsc(Long a_organizationId);

    Optional<User> findByIdAndOrganizationIdAndDeletedAtIsNull(Long a_id, Long a_organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select user from User user
            where user.id = :id
              and user.organization.id = :orgId
              and user.deletedAt is null
            """)
    Optional<User> lockInOrganization(@Param("id") Long a_id, @Param("orgId") Long a_organizationId);

    boolean existsByAssessorIdAndIdNotAndDeletedAtIsNull(Long a_assessorId, Long a_id);

    long countByRoleIdAndDeletedAtIsNull(Long a_roleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select user from User user join user.role role
            where user.organization.id = :orgId
              and role.roleCode = 'HOSPITAL_OWNER'
              and role.builtIn = true
              and user.enabled = true
              and user.deletedAt is null
            """)
    List<User> lockEnabledOwners(@Param("orgId") Long a_organizationId);
}
