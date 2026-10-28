package com.app.medicalquality.role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.app.medicalquality.exception.AppException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CustomRolePolicyTest {

    @Test
    void rejectsPermissionOutsideTheAllowlist() {
        AppException ex = assertThrows(
                AppException.class,
                () -> CustomRolePolicy.check(List.of(PermissionEnum.MANAGE_USERS), Set.of(PermissionEnum.MANAGE_USERS), true));
        assertEquals("error.role.permission_forbidden", ex.getMessageKey());
    }

    @Test
    void rejectsPermissionTheOwnerDoesNotHold() {
        assertThrows(
                AppException.class,
                () -> CustomRolePolicy.check(
                        List.of(PermissionEnum.DELETE_AUDIT_EVERYTHING), Set.of(PermissionEnum.CONDUCT_AUDIT), false));
    }

    @Test
    void allowsAnOwnerToGrantAPermissionTheyHold() {
        CustomRolePolicy.check(List.of(PermissionEnum.CONDUCT_AUDIT), Set.of(PermissionEnum.CONDUCT_AUDIT), false);
    }

    @Test
    void platformAdminMayGrantExceptionalPermissionManagement() {
        CustomRolePolicy.check(
                List.of(PermissionEnum.MANAGE_USER_PERMISSION_OVERRIDES), Set.of(), true);
    }

    @Test
    void ownerCannotGrantExceptionalPermissionManagementWithoutHoldingIt() {
        assertThrows(
                AppException.class,
                () -> CustomRolePolicy.check(
                        List.of(PermissionEnum.MANAGE_USER_PERMISSION_OVERRIDES),
                        Set.of(PermissionEnum.CONDUCT_AUDIT),
                        false));
    }

    @Test
    void rejectsAnEmptyList() {
        assertThrows(AppException.class, () -> CustomRolePolicy.check(List.of(), Set.of(), true));
    }

    @Test
    void lastOwnerCannotBeRemoved() {
        assertTrue(CustomRolePolicy.removesLastOwner(1, true, false));
        assertFalse(CustomRolePolicy.removesLastOwner(2, true, false));
        assertFalse(CustomRolePolicy.removesLastOwner(1, false, false));
        assertFalse(CustomRolePolicy.removesLastOwner(0, false, false));
    }
}
