package com.app.medicalquality.role;

import java.util.Map;

public final class PermissionText {

    private static final Map<PermissionEnum, String> LABELS = Map.ofEntries(
            Map.entry(PermissionEnum.CONDUCT_AUDIT, "Conduct audits"),
            Map.entry(PermissionEnum.MANAGE_FINDINGS, "Manage findings"),
            Map.entry(PermissionEnum.MANAGE_CAPA, "Manage CAPA"),
            Map.entry(PermissionEnum.MANAGE_EVIDENCE, "Manage evidence"),
            Map.entry(PermissionEnum.VIEW_COMMAND_CENTER, "Command center"),
            Map.entry(PermissionEnum.VIEW_EXECUTIVE_DASHBOARD, "Executive dashboard"),
            Map.entry(PermissionEnum.VIEW_ASSESSOR_DASHBOARD, "Assessor dashboard"),
            Map.entry(PermissionEnum.VIEW_QUALITY_DIRECTOR_DASHBOARD, "Quality director dashboard"),
            Map.entry(PermissionEnum.MANAGE_IMPORT, "Import PDFs"),
            Map.entry(PermissionEnum.ARCHIVE_AUDIT, "Archive audits"),
            Map.entry(PermissionEnum.EDIT_AUDIT_LIFECYCLE, "Edit audit lifecycle"),
            Map.entry(PermissionEnum.DELETE_AUDIT_PDF, "Delete an audit PDF"),
            Map.entry(PermissionEnum.DELETE_AUDIT_ONLY, "Delete an audit"),
            Map.entry(PermissionEnum.DELETE_AUDIT_EVERYTHING, "Delete an audit and related records"),
            Map.entry(PermissionEnum.MANAGE_USER_PERMISSION_OVERRIDES, "Manage exceptional permissions"));

    private static final Map<PermissionEnum, String> DESCRIPTIONS = Map.ofEntries(
            Map.entry(PermissionEnum.CONDUCT_AUDIT, "Create audits and medical-record reviews, and score checklist items."),
            Map.entry(PermissionEnum.MANAGE_FINDINGS, "Record and update findings."),
            Map.entry(PermissionEnum.MANAGE_CAPA, "Create and update corrective actions."),
            Map.entry(PermissionEnum.MANAGE_EVIDENCE, "Upload and manage evidence files."),
            Map.entry(PermissionEnum.VIEW_COMMAND_CENTER, "Open the command center dashboard."),
            Map.entry(PermissionEnum.VIEW_EXECUTIVE_DASHBOARD, "Open the executive dashboard."),
            Map.entry(PermissionEnum.VIEW_ASSESSOR_DASHBOARD, "Open the assessor dashboard."),
            Map.entry(PermissionEnum.VIEW_QUALITY_DIRECTOR_DASHBOARD, "Open the quality director dashboard."),
            Map.entry(PermissionEnum.MANAGE_IMPORT, "Import audit PDFs."),
            Map.entry(PermissionEnum.ARCHIVE_AUDIT, "Archive and restore audits."),
            Map.entry(PermissionEnum.EDIT_AUDIT_LIFECYCLE, "Move a submitted audit through its lifecycle."),
            Map.entry(PermissionEnum.DELETE_AUDIT_PDF, "Delete the PDF attached to an audit."),
            Map.entry(PermissionEnum.DELETE_AUDIT_ONLY, "Delete an audit while leaving related records."),
            Map.entry(PermissionEnum.DELETE_AUDIT_EVERYTHING, "Delete an audit and its related records."),
            Map.entry(PermissionEnum.MANAGE_USER_PERMISSION_OVERRIDES, "Grant or deny individual permissions for one person."));

    private PermissionText() {}

    public static String label(PermissionEnum a_permission) {
        return LABELS.getOrDefault(a_permission, a_permission.name());
    }

    public static String description(PermissionEnum a_permission) {
        return DESCRIPTIONS.getOrDefault(a_permission, "");
    }
}
