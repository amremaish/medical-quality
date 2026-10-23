package com.app.medicalquality.role.service;

import com.app.medicalquality.exception.AppException;
import com.app.medicalquality.role.PermissionEnum;
import com.app.medicalquality.role.dto.CreateRoleRequest;
import com.app.medicalquality.role.dto.PermissionResponse;
import com.app.medicalquality.role.dto.RoleResponse;
import com.app.medicalquality.role.entity.Permission;
import com.app.medicalquality.role.entity.Role;
import com.app.medicalquality.role.entity.RolePermission;
import com.app.medicalquality.role.repository.PermissionRepository;
import com.app.medicalquality.role.repository.RolePermissionRepository;
import com.app.medicalquality.role.repository.RoleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public RoleService(
            RoleRepository a_roleRepository,
            PermissionRepository a_permissionRepository,
            RolePermissionRepository a_rolePermissionRepository) {
        this.roleRepository = a_roleRepository;
        this.permissionRepository = a_permissionRepository;
        this.rolePermissionRepository = a_rolePermissionRepository;
    }

    public RoleResponse create(CreateRoleRequest a_request) {
        if (roleRepository.findByName(a_request.getName().trim()).isPresent()) {
            throw new AppException("error.role.already_exists");
        }
        Role role = new Role();
        role.setName(a_request.getName().trim());
        roleRepository.save(role);
        for (PermissionEnum permission : a_request.getPermissions().stream().distinct().toList()) {
            link(role, permission);
        }
        return toResponse(role);
    }

    public List<RoleResponse> list() {
        return roleRepository.findByDeletedAtIsNullOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<PermissionResponse> permissions() {
        return permissionRepository.findByDeletedAtIsNullOrderByNameAsc().stream()
                .map(a_permission -> new PermissionResponse(a_permission.getName()))
                .toList();
    }

    private void link(Role a_role, PermissionEnum a_permission) {
        Permission stored = permissionRepository
                .findByName(a_permission.name())
                .orElseThrow(() -> new AppException(
                        "error.role.permission_not_found", new Object[] {a_permission.name()}));
        RolePermission link = new RolePermission();
        link.setRole(a_role);
        link.setPermission(stored);
        rolePermissionRepository.save(link);
    }

    private RoleResponse toResponse(Role a_role) {
        List<String> permissions = rolePermissionRepository.findByRoleAndDeletedAtIsNull(a_role).stream()
                .map(a_link -> a_link.getPermission().getName())
                .toList();
        return new RoleResponse(a_role.getId(), a_role.getName(), permissions);
    }
}
