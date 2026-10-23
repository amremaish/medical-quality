package com.app.medicalquality.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public final class AccessRules {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";

    private AccessRules() {}

    public static boolean isSuperAdmin(Authentication a_authentication) {
        if (a_authentication == null || a_authentication.getAuthorities() == null) {
            return false;
        }
        return a_authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(SUPER_ADMIN::equals);
    }
}
