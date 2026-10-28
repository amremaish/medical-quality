package com.app.medicalquality.role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserPermissionOverridePolicyTest {

    @Test
    void classifiesEveryPermission() {
        Set<PermissionEnum> expected = EnumSet.of(
                PermissionEnum.CREATE_ROLE,
                PermissionEnum.VIEW_ROLES,
                PermissionEnum.UPDATE_ROLE,
                PermissionEnum.DELETE_ROLE,
                PermissionEnum.VIEW_PERMISSIONS,
                PermissionEnum.MANAGE_USERS,
                PermissionEnum.MANAGE_DEPARTMENTS,
                PermissionEnum.MANAGE_ASSESSORS,
                PermissionEnum.MANAGE_USER_PERMISSION_OVERRIDES,
                PermissionEnum.VIEW_ALL_ASSESSORS_DATA,
                PermissionEnum.GENERATE_REPORTS,
                PermissionEnum.MANAGE_EXPORT,
                PermissionEnum.MANAGE_NOTIFICATIONS,
                PermissionEnum.MANAGE_SETTINGS,
                PermissionEnum.VIEW_SYSTEM_LOGS,
                PermissionEnum.MANAGE_BACKUP,
                PermissionEnum.MANAGE_RESTORE,
                PermissionEnum.RESET_DATA);
        EnumSet<PermissionEnum> actual = EnumSet.allOf(PermissionEnum.class);
        actual.removeAll(UserPermissionOverridePolicy.ELIGIBLE);
        assertEquals(expected, actual);
    }

    @Test
    void eligiblePermissionsStayInsideTheCustomRoleAllowlist() {
        assertTrue(CustomRolePolicy.ALLOWED.containsAll(UserPermissionOverridePolicy.ELIGIBLE));
        assertFalse(UserPermissionOverridePolicy.eligible(PermissionEnum.MANAGE_USER_PERMISSION_OVERRIDES));
        assertFalse(UserPermissionOverridePolicy.eligible(PermissionEnum.VIEW_ALL_ASSESSORS_DATA));
        assertFalse(UserPermissionOverridePolicy.eligible(PermissionEnum.RESET_DATA));
        assertFalse(UserPermissionOverridePolicy.eligible("NOT_A_PERMISSION"));
        assertTrue(UserPermissionOverridePolicy.eligible(PermissionEnum.CONDUCT_AUDIT));
    }
}
