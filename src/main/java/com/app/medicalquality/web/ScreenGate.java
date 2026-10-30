package com.app.medicalquality.web;

import com.app.medicalquality.role.RoleCodes;
import java.util.Set;

/**
 * Which HTML screens a signed-in user may open. This matches the client gate in api.js.
 * API authorization stays on the controllers; a missing permission denies the screen.
 */
public final class ScreenGate {

    private ScreenGate() {
    }

    public static boolean allowed(
            String a_path,
            boolean a_superAdmin,
            boolean a_orgOpen,
            String a_roleCode,
            Set<String> a_permissions) {
        String path = screenPath(a_path);
        if (path.equals("/account/password") || path.equals("/account/setup") || path.equals("/login")) {
            return true;
        }
        if (platform(path)) {
            return a_superAdmin;
        }
        if (a_superAdmin && !a_orgOpen) {
            return false;
        }
        if (path.equals("/users/new") || path.startsWith("/users/new/")
                || path.equals("/departments/new") || path.startsWith("/departments/new/")) {
            return directoryAdmin(a_superAdmin, a_orgOpen, a_roleCode);
        }
        if (path.equals("/roles") || path.startsWith("/roles/")) {
            return directoryAdmin(a_superAdmin, a_orgOpen, a_roleCode);
        }
        if (path.equals("/users") || path.startsWith("/users/")) {
            return directoryAdmin(a_superAdmin, a_orgOpen, a_roleCode)
                    || holds(a_superAdmin, a_permissions, "MANAGE_USER_PERMISSION_OVERRIDES");
        }
        if (path.equals("/assessors") || path.startsWith("/assessors/")) {
            return directoryAdmin(a_superAdmin, a_orgOpen, a_roleCode)
                    || holds(a_superAdmin, a_permissions, "MANAGE_ASSESSORS");
        }
        if (path.equals("/quality-policy")) {
            return holds(a_superAdmin, a_permissions, "QUALITY_POLICY_VIEW")
                    || holds(a_superAdmin, a_permissions, "QUALITY_POLICY_EDIT");
        }
        String required = permissionFor(path);
        if (required == null) {
            return true;
        }
        return holds(a_superAdmin, a_permissions, required);
    }

    public static boolean action(boolean a_superAdmin, Set<String> a_permissions, String a_permission) {
        return holds(a_superAdmin, a_permissions, a_permission);
    }

    public static boolean everyAction(boolean a_superAdmin, Set<String> a_permissions, String... a_required) {
        if (a_required == null || a_required.length == 0) {
            return false;
        }
        for (String name : a_required) {
            if (!holds(a_superAdmin, a_permissions, name)) {
                return false;
            }
        }
        return true;
    }

    private static boolean directoryAdmin(boolean a_superAdmin, boolean a_orgOpen, String a_roleCode) {
        return (a_superAdmin && a_orgOpen) || RoleCodes.HOSPITAL_OWNER.equals(a_roleCode);
    }

    private static boolean holds(boolean a_superAdmin, Set<String> a_permissions, String a_permission) {
        if (a_permission == null || a_permission.isEmpty()) {
            return false;
        }
        if (a_superAdmin) {
            return true;
        }
        return a_permissions != null && a_permissions.contains(a_permission);
    }

    private static boolean platform(String a_path) {
        return a_path.equals("/organizations")
                || a_path.startsWith("/organizations/")
                || a_path.equals("/owners")
                || a_path.startsWith("/owners/")
                || a_path.equals("/hospitals")
                || a_path.startsWith("/hospitals/");
    }

    private static String screenPath(String a_path) {
        String path = a_path == null ? "" : a_path;
        int query = path.indexOf('?');
        int hash = path.indexOf('#');
        int cut = query < 0 ? hash : hash < 0 ? query : Math.min(query, hash);
        return cut < 0 ? path : path.substring(0, cut);
    }

    private static String permissionFor(String a_path) {
        if (a_path.equals("/dashboard") || a_path.equals("/dashboard/")) {
            return "VIEW_COMMAND_CENTER";
        }
        if (a_path.startsWith("/dashboard/executive")) {
            return "VIEW_EXECUTIVE_DASHBOARD";
        }
        if (a_path.startsWith("/dashboard/assessor")) {
            return "VIEW_ASSESSOR_DASHBOARD";
        }
        if (a_path.startsWith("/dashboard/quality-director")) {
            return "VIEW_QUALITY_DIRECTOR_DASHBOARD";
        }
        if (a_path.startsWith("/dashboard/")) {
            return "";
        }
        if (a_path.equals("/audits") || a_path.startsWith("/audits/")) {
            return "CONDUCT_AUDIT";
        }
        if (a_path.equals("/record-reviews") || a_path.startsWith("/record-reviews/")) {
            return "CONDUCT_AUDIT";
        }
        if (a_path.equals("/findings") || a_path.startsWith("/findings/")) {
            return "MANAGE_FINDINGS";
        }
        if (a_path.equals("/capas") || a_path.startsWith("/capas/")) {
            return "MANAGE_CAPA";
        }
        if (a_path.equals("/evidence") || a_path.startsWith("/evidence/")) {
            return "MANAGE_EVIDENCE";
        }
        if (a_path.equals("/departments") || a_path.startsWith("/departments/")) {
            return "MANAGE_DEPARTMENTS";
        }
        if (a_path.equals("/imports") || a_path.startsWith("/imports/")) {
            return "MANAGE_IMPORT";
        }
        return null;
    }
}
