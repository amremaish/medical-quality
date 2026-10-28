package com.app.medicalquality.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.entity.UserPermissionOverride;
import com.app.medicalquality.auth.repository.UserPermissionOverrideRepository;
import com.app.medicalquality.role.entity.Permission;
import com.app.medicalquality.role.entity.Role;
import com.app.medicalquality.role.entity.RolePermission;
import com.app.medicalquality.role.repository.RolePermissionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class EffectivePermissionServiceTest {

    @Test
    void oneUsersOverrideDoesNotChangeAnotherUserOnTheSameRole() {
        Role role = new Role();
        role.setId(3L);
        Permission conduct = permission("CONDUCT_AUDIT");
        RolePermission link = new RolePermission();
        link.setPermission(conduct);
        RolePermissionRepository roles = mock(RolePermissionRepository.class);
        when(roles.findByRoleAndDeletedAtIsNull(role)).thenReturn(List.of(link));
        UserPermissionOverrideRepository overrides = mock(UserPermissionOverrideRepository.class);
        UserPermissionOverride allow = new UserPermissionOverride();
        allow.setEffect(EffectivePermissions.ALLOW);
        allow.setPermission(permission("MANAGE_CAPA"));
        UserPermissionOverride deny = new UserPermissionOverride();
        deny.setEffect(EffectivePermissions.DENY);
        deny.setPermission(conduct);
        when(overrides.findByUser_IdAndDeletedAtIsNull(1L)).thenReturn(List.of(allow, deny));
        when(overrides.findByUser_IdAndDeletedAtIsNull(2L)).thenReturn(List.of());
        EffectivePermissionService service = new EffectivePermissionService(roles, overrides);

        assertEquals(List.of("MANAGE_CAPA"), service.names(user(1L, role)));
        assertEquals(List.of("CONDUCT_AUDIT"), service.names(user(2L, role)));
        assertFalse(service.has(user(1L, role), "CONDUCT_AUDIT"));
        assertTrue(service.has(user(2L, role), "CONDUCT_AUDIT"));
    }

    @Test
    void anIneligibleAllowDoesNotGrant() {
        Role role = new Role();
        Permission reports = permission("GENERATE_REPORTS");
        RolePermission link = new RolePermission();
        link.setPermission(reports);
        RolePermissionRepository roles = mock(RolePermissionRepository.class);
        when(roles.findByRoleAndDeletedAtIsNull(role)).thenReturn(List.of(link));
        UserPermissionOverride allow = new UserPermissionOverride();
        allow.setEffect(EffectivePermissions.ALLOW);
        allow.setPermission(permission("RESET_DATA"));
        UserPermissionOverrideRepository overrides = mock(UserPermissionOverrideRepository.class);
        when(overrides.findByUser_IdAndDeletedAtIsNull(1L)).thenReturn(List.of(allow));
        EffectivePermissionService service = new EffectivePermissionService(roles, overrides);

        assertEquals(List.of("GENERATE_REPORTS"), service.names(user(1L, role)));
        assertFalse(service.has(user(1L, role), "RESET_DATA"));
    }

    private static User user(Long a_id, Role a_role) {
        User user = new User();
        user.setId(a_id);
        user.setRole(a_role);
        return user;
    }

    private static Permission permission(String a_name) {
        Permission permission = new Permission();
        permission.setName(a_name);
        return permission;
    }
}
