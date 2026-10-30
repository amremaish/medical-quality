package com.app.medicalquality.role;

import com.app.medicalquality.exception.AppException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class CustomRolePolicy {

    public static final Set<PermissionEnum> ALLOWED = EnumSet.of(
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
            PermissionEnum.DELETE_AUDIT_EVERYTHING,
            PermissionEnum.MANAGE_USER_PERMISSION_OVERRIDES,
            PermissionEnum.QUALITY_POLICY_VIEW,
            PermissionEnum.QUALITY_POLICY_EDIT);

    private CustomRolePolicy() {}

    public static void check(List<PermissionEnum> a_requested, Set<PermissionEnum> a_held, boolean a_platformAdmin) {
        if (a_requested == null || a_requested.isEmpty()) {
            throw new AppException("error.role.permission_forbidden", new Object[] {"none"});
        }
        for (PermissionEnum permission : a_requested.stream().distinct().toList()) {
            if (!ALLOWED.contains(permission) || (!a_platformAdmin && !a_held.contains(permission))) {
                throw new AppException("error.role.permission_forbidden", new Object[] {permission.name()});
            }
        }
    }

    public static boolean removesLastOwner(
            long a_enabledOwners, boolean a_currentlyEnabledOwner, boolean a_willBeEnabledOwner) {
        return a_currentlyEnabledOwner && !a_willBeEnabledOwner && a_enabledOwners <= 1;
    }
}
