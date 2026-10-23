package com.app.medicalquality.role.controller;

import com.app.medicalquality.auth.security.annotation.HasPermission;
import com.app.medicalquality.common.AppResponse;
import com.app.medicalquality.role.PermissionEnum;
import com.app.medicalquality.role.dto.CreateRoleRequest;
import com.app.medicalquality.role.dto.PermissionResponse;
import com.app.medicalquality.role.dto.RoleResponse;
import com.app.medicalquality.role.service.RoleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService a_roleService) {
        this.roleService = a_roleService;
    }

    @PostMapping
    @HasPermission(PermissionEnum.CREATE_ROLE)
    public ResponseEntity<AppResponse<RoleResponse>> create(@Valid @RequestBody CreateRoleRequest a_request) {
        return AppResponse.success(roleService.create(a_request), "Role created successfully");
    }

    @GetMapping
    @HasPermission(PermissionEnum.VIEW_ROLES)
    public ResponseEntity<AppResponse<List<RoleResponse>>> list() {
        return AppResponse.success(roleService.list(), "Roles retrieved successfully");
    }

    @GetMapping("/permissions")
    @HasPermission(PermissionEnum.VIEW_PERMISSIONS)
    public ResponseEntity<AppResponse<List<PermissionResponse>>> permissions() {
        return AppResponse.success(roleService.permissions(), "Permissions retrieved successfully");
    }
}
