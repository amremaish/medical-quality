package com.app.medicalquality.organization.controller;

import com.app.medicalquality.common.AppResponse;
import com.app.medicalquality.organization.service.DirectoryService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class DirectoryController {

    private final DirectoryService directoryService;

    public DirectoryController(DirectoryService a_directoryService) {
        this.directoryService = a_directoryService;
    }

    @GetMapping("/api/organizations/{organizationId}/users")
    public ResponseEntity<AppResponse<List<DirectoryService.UserView>>> users(
            @PathVariable("organizationId") Long a_organizationId, Authentication a_authentication) {
        return AppResponse.success(directoryService.users(a_organizationId, a_authentication));
    }

    @GetMapping("/api/organizations/{organizationId}/users/{userId}")
    public ResponseEntity<AppResponse<DirectoryService.UserView>> user(
            @PathVariable("organizationId") Long a_organizationId,
            @PathVariable("userId") Long a_userId,
            Authentication a_authentication) {
        return AppResponse.success(directoryService.user(a_organizationId, a_userId, a_authentication));
    }

    @PostMapping("/api/organizations/{organizationId}/users")
    public ResponseEntity<AppResponse<DirectoryService.SetupLink>> invite(
            @PathVariable("organizationId") Long a_organizationId,
            @RequestBody DirectoryService.InviteForm a_form,
            Authentication a_authentication) {
        return AppResponse.success(
                directoryService.invite(a_organizationId, a_form, a_authentication, baseUrl()),
                "Successfully Done",
                HttpStatus.CREATED);
    }

    @PutMapping("/api/organizations/{organizationId}/users/{userId}")
    public ResponseEntity<AppResponse<DirectoryService.SetupLink>> updateUser(
            @PathVariable("organizationId") Long a_organizationId,
            @PathVariable("userId") Long a_userId,
            @RequestBody DirectoryService.UserPatch a_form,
            Authentication a_authentication) {
        return AppResponse.success(
                directoryService.updateUser(a_organizationId, a_userId, a_form, a_authentication, baseUrl()));
    }

    @GetMapping("/api/organizations/{organizationId}/roles")
    public ResponseEntity<AppResponse<List<DirectoryService.RoleView>>> roles(
            @PathVariable("organizationId") Long a_organizationId, Authentication a_authentication) {
        return AppResponse.success(directoryService.roles(a_organizationId, a_authentication));
    }

    @PostMapping("/api/organizations/{organizationId}/roles")
    public ResponseEntity<AppResponse<DirectoryService.RoleView>> createRole(
            @PathVariable("organizationId") Long a_organizationId,
            @RequestBody DirectoryService.RoleForm a_form,
            Authentication a_authentication) {
        return AppResponse.success(
                directoryService.createRole(a_organizationId, a_form, a_authentication),
                "Successfully Done",
                HttpStatus.CREATED);
    }

    @PutMapping("/api/organizations/{organizationId}/roles/{roleId}")
    public ResponseEntity<AppResponse<DirectoryService.RoleView>> updateRole(
            @PathVariable("organizationId") Long a_organizationId,
            @PathVariable("roleId") Long a_roleId,
            @RequestBody DirectoryService.RoleForm a_form,
            Authentication a_authentication) {
        return AppResponse.success(directoryService.updateRole(a_organizationId, a_roleId, a_form, a_authentication));
    }

    @DeleteMapping("/api/organizations/{organizationId}/roles/{roleId}")
    public ResponseEntity<AppResponse<String>> deleteRole(
            @PathVariable("organizationId") Long a_organizationId,
            @PathVariable("roleId") Long a_roleId,
            Authentication a_authentication) {
        directoryService.deleteRole(a_organizationId, a_roleId, a_authentication);
        return AppResponse.success("Deleted");
    }

    private String baseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}
