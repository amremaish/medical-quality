package com.app.medicalquality.auth.controller;

import com.app.medicalquality.auth.dto.PermissionOverrideMatrix;
import com.app.medicalquality.auth.dto.UpdatePermissionOverridesRequest;
import com.app.medicalquality.auth.security.annotation.HasPermission;
import com.app.medicalquality.auth.service.UserPermissionOverrideService;
import com.app.medicalquality.common.AppResponse;
import com.app.medicalquality.role.PermissionEnum;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations/{organizationId}/users/{userId}/permission-overrides")
public class UserPermissionOverrideController {

    private final UserPermissionOverrideService overrideService;

    public UserPermissionOverrideController(UserPermissionOverrideService a_overrideService) {
        this.overrideService = a_overrideService;
    }

    @GetMapping
    @HasPermission(PermissionEnum.MANAGE_USER_PERMISSION_OVERRIDES)
    public ResponseEntity<AppResponse<PermissionOverrideMatrix>> matrix(
            @PathVariable("organizationId") Long a_organizationId,
            @PathVariable("userId") Long a_userId,
            Authentication a_authentication) {
        return AppResponse.success(overrideService.matrix(a_organizationId, a_userId, a_authentication));
    }

    @PutMapping
    @HasPermission(PermissionEnum.MANAGE_USER_PERMISSION_OVERRIDES)
    public ResponseEntity<AppResponse<PermissionOverrideMatrix>> update(
            @PathVariable("organizationId") Long a_organizationId,
            @PathVariable("userId") Long a_userId,
            @Valid @RequestBody UpdatePermissionOverridesRequest a_request,
            Authentication a_authentication) {
        return AppResponse.success(
                overrideService.update(a_organizationId, a_userId, a_request, a_authentication));
    }
}
