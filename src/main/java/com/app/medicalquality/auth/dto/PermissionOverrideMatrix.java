package com.app.medicalquality.auth.dto;

import java.util.List;

public record PermissionOverrideMatrix(List<PermissionOverrideRow> permissions) {

    public record PermissionOverrideRow(
            String permission,
            String label,
            String description,
            boolean roleGranted,
            boolean effective,
            String effect,
            String note) {}
}
