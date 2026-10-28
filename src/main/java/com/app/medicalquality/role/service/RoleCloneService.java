package com.app.medicalquality.role.service;

import com.app.medicalquality.organization.entity.Organization;
import com.app.medicalquality.role.RoleCodes;
import com.app.medicalquality.role.entity.Role;
import com.app.medicalquality.role.entity.RolePermission;
import com.app.medicalquality.role.repository.RolePermissionRepository;
import com.app.medicalquality.role.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleCloneService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public RoleCloneService(RoleRepository a_roleRepository, RolePermissionRepository a_rolePermissionRepository) {
        this.roleRepository = a_roleRepository;
        this.rolePermissionRepository = a_rolePermissionRepository;
    }

    @Transactional
    public void ensureCopies(Organization a_organization) {
        if (roleRepository.existsByRoleCodeAndOrganizationIdAndDeletedAtIsNull(
                RoleCodes.HOSPITAL_OWNER, a_organization.getId())) {
            return;
        }
        for (Role template : roleRepository.findByTemplateTrueAndOrganizationIsNullAndDeletedAtIsNull()) {
            Role copy = new Role();
            copy.setName(template.getName());
            copy.setRoleCode(template.getRoleCode());
            copy.setOrganization(a_organization);
            copy.setBuiltIn(true);
            copy.setTemplate(false);
            copy.setScopeMode(template.getScopeMode());
            roleRepository.save(copy);
            for (RolePermission link : rolePermissionRepository.findByRoleAndDeletedAtIsNull(template)) {
                RolePermission created = new RolePermission();
                created.setRole(copy);
                created.setPermission(link.getPermission());
                rolePermissionRepository.save(created);
            }
        }
    }
}
