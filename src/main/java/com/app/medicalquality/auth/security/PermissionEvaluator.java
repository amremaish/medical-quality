package com.app.medicalquality.auth.security;

import com.app.medicalquality.role.repository.RolePermissionRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class PermissionEvaluator {

    private final RolePermissionRepository rolePermissionRepository;

    public PermissionEvaluator(RolePermissionRepository a_rolePermissionRepository) {
        this.rolePermissionRepository = a_rolePermissionRepository;
    }

    public boolean hasPermission(Authentication a_authentication, String a_permissionName) {
        if (a_authentication == null || !a_authentication.isAuthenticated()) {
            return false;
        }
        if (AccessRules.isSuperAdmin(a_authentication)) {
            return true;
        }
        String roleName = roleName(a_authentication);
        return roleName != null
                && rolePermissionRepository
                        .existsByRole_NameAndPermission_NameAndDeletedAtIsNullAndRole_DeletedAtIsNull(
                                roleName, a_permissionName);
    }

    private String roleName(Authentication a_authentication) {
        return a_authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a_authority -> a_authority.startsWith("ROLE_"))
                .map(a_authority -> a_authority.substring("ROLE_".length()))
                .findFirst()
                .orElse(null);
    }
}
