package com.app.medicalquality.role;

import java.util.EnumSet;
import java.util.Set;

/**
 * Permissions a user-level ALLOW or DENY may target.
 * Organization, department, and record scope stay outside this set:
 * an override never widens who a person can see or which hospital they belong to.
 * {@link PermissionEnum#MANAGE_USER_PERMISSION_OVERRIDES} is assignable only on a custom role,
 * and only through {@link CustomRolePolicy}, so it cannot be taken through this API.
 */
public final class UserPermissionOverridePolicy {

    public static final Set<PermissionEnum> ELIGIBLE = EnumSet.of(
            PermissionEnum.CONDUCT_AUDIT,
            PermissionEnum.MANAGE_FINDINGS,
            PermissionEnum.MANAGE_CAPA,
            PermissionEnum.MANAGE_EVIDENCE,
            PermissionEnum.VIEW_COMMAND_CENTER,
            PermissionEnum.VIEW_EXECUTIVE_DASHBOARD,
            PermissionEnum.VIEW_ASSESSOR_DASHBOARD,
            PermissionEnum.VIEW_QUALITY_DIRECTOR_DASHBOARD,
            PermissionEnum.MANAGE_IMPORT,
            PermissionEnum.ARCHIVE_AUDIT,
            PermissionEnum.EDIT_AUDIT_LIFECYCLE,
            PermissionEnum.DELETE_AUDIT_PDF,
            PermissionEnum.DELETE_AUDIT_ONLY,
            PermissionEnum.DELETE_AUDIT_EVERYTHING);

    private UserPermissionOverridePolicy() {}

    public static boolean eligible(PermissionEnum a_permission) {
        return a_permission != null && ELIGIBLE.contains(a_permission);
    }

    public static boolean eligible(String a_permissionName) {
        if (a_permissionName == null || a_permissionName.isBlank()) {
            return false;
        }
        try {
            return eligible(PermissionEnum.valueOf(a_permissionName));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
