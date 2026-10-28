package com.app.medicalquality.auth.security;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.entity.UserPermissionOverride;
import com.app.medicalquality.auth.repository.UserPermissionOverrideRepository;
import com.app.medicalquality.role.UserPermissionOverridePolicy;
import com.app.medicalquality.role.entity.Permission;
import com.app.medicalquality.role.entity.Role;
import com.app.medicalquality.role.entity.RolePermission;
import com.app.medicalquality.role.repository.RolePermissionRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class EffectivePermissionService {

    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionOverrideRepository overrideRepository;

    public EffectivePermissionService(
            RolePermissionRepository a_rolePermissionRepository,
            UserPermissionOverrideRepository a_overrideRepository) {
        this.rolePermissionRepository = a_rolePermissionRepository;
        this.overrideRepository = a_overrideRepository;
    }

    public boolean has(User a_user, String a_permissionName) {
        if (a_user == null || a_permissionName == null || a_permissionName.isBlank()) {
            return false;
        }
        return names(a_user).contains(a_permissionName);
    }

    public List<String> names(User a_user) {
        if (a_user == null) {
            return List.of();
        }
        Set<String> names = roleNames(a_user.getRole());
        if (a_user.getId() == null) {
            return names.stream().sorted().toList();
        }
        for (UserPermissionOverride override : overrideRepository.findByUser_IdAndDeletedAtIsNull(a_user.getId())) {
            String name = permissionName(override.getPermission());
            if (name == null) {
                continue;
            }
            if (EffectivePermissions.DENY.equals(override.getEffect())) {
                names.remove(name);
            } else if (EffectivePermissions.ALLOW.equals(override.getEffect())
                    && UserPermissionOverridePolicy.eligible(name)) {
                names.add(name);
            }
        }
        return names.stream().sorted().toList();
    }

    public Map<String, String> effects(Long a_userId) {
        Map<String, String> effects = new HashMap<>();
        if (a_userId == null) {
            return effects;
        }
        for (UserPermissionOverride override : overrideRepository.findByUser_IdAndDeletedAtIsNull(a_userId)) {
            String name = permissionName(override.getPermission());
            if (name != null && override.getEffect() != null) {
                effects.put(name, override.getEffect());
            }
        }
        return effects;
    }

    public Set<String> roleNames(Role a_role) {
        Set<String> names = new HashSet<>();
        if (a_role == null || a_role.getDeletedAt() != null) {
            return names;
        }
        for (RolePermission link : rolePermissionRepository.findByRoleAndDeletedAtIsNull(a_role)) {
            String name = permissionName(link.getPermission());
            if (name != null) {
                names.add(name);
            }
        }
        return names;
    }

    private String permissionName(Permission a_permission) {
        if (a_permission == null || a_permission.getDeletedAt() != null) {
            return null;
        }
        String name = a_permission.getName();
        if (name == null || name.isBlank()) {
            return null;
        }
        return name;
    }
}
