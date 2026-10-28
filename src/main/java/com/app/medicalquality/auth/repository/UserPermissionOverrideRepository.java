package com.app.medicalquality.auth.repository;

import com.app.medicalquality.auth.entity.UserPermissionOverride;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPermissionOverrideRepository extends JpaRepository<UserPermissionOverride, Long> {

    @EntityGraph(attributePaths = {"permission"})
    List<UserPermissionOverride> findByUser_IdAndDeletedAtIsNull(Long a_userId);

    @EntityGraph(attributePaths = {"permission"})
    Optional<UserPermissionOverride> findByUser_IdAndPermission_IdAndDeletedAtIsNull(Long a_userId, Long a_permissionId);
}
