package com.app.medicalquality.role.service;

import com.app.medicalquality.role.PermissionEnum;
import com.app.medicalquality.role.entity.Permission;
import com.app.medicalquality.role.entity.Role;
import com.app.medicalquality.role.entity.RolePermission;
import com.app.medicalquality.role.repository.PermissionRepository;
import com.app.medicalquality.role.repository.RolePermissionRepository;
import com.app.medicalquality.role.repository.RoleRepository;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Order(2)
public class RoleSeedService implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public RoleSeedService(
            RoleRepository a_roleRepository,
            PermissionRepository a_permissionRepository,
            RolePermissionRepository a_rolePermissionRepository) {
        this.roleRepository = a_roleRepository;
        this.permissionRepository = a_permissionRepository;
        this.rolePermissionRepository = a_rolePermissionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments a_args) {
        seed("System Administrator", EnumSet.allOf(PermissionEnum.class));
        seed("Quality Director", except(roleKeys(PermissionEnum.MANAGE_USERS, PermissionEnum.RESET_DATA)));
        seed("Quality Supervisor", except(roleKeys(PermissionEnum.RESET_DATA)));
        seed("Assessor", except(assessorDenied()));
        seed("Department Coordinator", except(coordinatorDenied()));
        seed("View Only", except(viewOnlyDenied()));
        seed("Hospital Owner", except(roleKeys(PermissionEnum.RESET_DATA)));
    }

    private void seed(String a_name, Set<PermissionEnum> a_granted) {
        Role role = roleRepository.findByName(a_name).orElseGet(() -> newRole(a_name));
        if (!rolePermissionRepository.findByRoleAndDeletedAtIsNull(role).isEmpty()) {
            return;
        }
        for (PermissionEnum permission : a_granted) {
            Permission row = permissionRepository.findByName(permission.name()).orElseThrow();
            RolePermission link = new RolePermission();
            link.setRole(role);
            link.setPermission(row);
            rolePermissionRepository.save(link);
        }
    }

    private Role newRole(String a_name) {
        Role role = new Role();
        role.setName(a_name);
        return roleRepository.save(role);
    }

    private Set<PermissionEnum> except(Set<PermissionEnum> a_denied) {
        EnumSet<PermissionEnum> granted = EnumSet.allOf(PermissionEnum.class);
        granted.removeAll(a_denied);
        return granted;
    }

    private Set<PermissionEnum> roleKeys(PermissionEnum... a_extra) {
        EnumSet<PermissionEnum> denied = EnumSet.of(
                PermissionEnum.CREATE_ROLE,
                PermissionEnum.VIEW_ROLES,
                PermissionEnum.UPDATE_ROLE,
                PermissionEnum.DELETE_ROLE,
                PermissionEnum.VIEW_PERMISSIONS);
        denied.addAll(Set.of(a_extra));
        return denied;
    }

    private Set<PermissionEnum> assessorDenied() {
        EnumSet<PermissionEnum> denied = EnumSet.copyOf(roleKeys(PermissionEnum.MANAGE_USERS, PermissionEnum.RESET_DATA));
        denied.add(PermissionEnum.MANAGE_DEPARTMENTS);
        denied.add(PermissionEnum.MANAGE_ASSESSORS);
        denied.add(PermissionEnum.VIEW_EXECUTIVE_DASHBOARD);
        denied.add(PermissionEnum.VIEW_QUALITY_DIRECTOR_DASHBOARD);
        denied.add(PermissionEnum.VIEW_COMMAND_CENTER);
        denied.add(PermissionEnum.VIEW_ALL_ASSESSORS_DATA);
        denied.add(PermissionEnum.MANAGE_NOTIFICATIONS);
        denied.add(PermissionEnum.MANAGE_SETTINGS);
        denied.add(PermissionEnum.VIEW_SYSTEM_LOGS);
        denied.add(PermissionEnum.MANAGE_BACKUP);
        denied.add(PermissionEnum.MANAGE_RESTORE);
        denied.add(PermissionEnum.MANAGE_IMPORT);
        denied.add(PermissionEnum.ARCHIVE_AUDIT);
        denied.add(PermissionEnum.EDIT_AUDIT_LIFECYCLE);
        denied.add(PermissionEnum.DELETE_AUDIT_PDF);
        denied.add(PermissionEnum.DELETE_AUDIT_ONLY);
        denied.add(PermissionEnum.DELETE_AUDIT_EVERYTHING);
        return denied;
    }

    private Set<PermissionEnum> coordinatorDenied() {
        EnumSet<PermissionEnum> denied = EnumSet.copyOf(assessorDenied());
        denied.add(PermissionEnum.CONDUCT_AUDIT);
        denied.add(PermissionEnum.MANAGE_FINDINGS);
        return denied;
    }

    private Set<PermissionEnum> viewOnlyDenied() {
        EnumSet<PermissionEnum> denied = EnumSet.copyOf(coordinatorDenied());
        denied.add(PermissionEnum.MANAGE_CAPA);
        denied.add(PermissionEnum.MANAGE_EVIDENCE);
        denied.add(PermissionEnum.GENERATE_REPORTS);
        denied.add(PermissionEnum.MANAGE_EXPORT);
        return denied;
    }
}
