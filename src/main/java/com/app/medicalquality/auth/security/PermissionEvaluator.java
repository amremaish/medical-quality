package com.app.medicalquality.auth.security;

import com.app.medicalquality.auth.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class PermissionEvaluator {

    private final EffectivePermissionService effectivePermissionService;
    private final UserRepository userRepository;

    public PermissionEvaluator(
            EffectivePermissionService a_effectivePermissionService, UserRepository a_userRepository) {
        this.effectivePermissionService = a_effectivePermissionService;
        this.userRepository = a_userRepository;
    }

    public boolean hasPermission(Authentication a_authentication, String a_permissionName) {
        if (a_authentication == null || !a_authentication.isAuthenticated()) {
            return false;
        }
        if (AccessRules.isSuperAdmin(a_authentication)) {
            return true;
        }
        return userRepository
                .findByUsername(a_authentication.getName())
                .map(a_user -> effectivePermissionService.has(a_user, a_permissionName))
                .orElse(false);
    }
}
