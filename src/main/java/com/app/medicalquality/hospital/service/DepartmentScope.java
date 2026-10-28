package com.app.medicalquality.hospital.service;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.security.AccessRules;
import com.app.medicalquality.auth.security.PermissionEvaluator;
import com.app.medicalquality.gsr.entity.Audit;
import com.app.medicalquality.role.RoleCodes;
import com.app.medicalquality.role.entity.Role;
import com.app.medicalquality.hospital.entity.Department;
import com.app.medicalquality.hospital.entity.UserDepartmentAccess;
import com.app.medicalquality.hospital.repository.DepartmentRepository;
import com.app.medicalquality.hospital.repository.UserDepartmentAccessRepository;
import com.app.medicalquality.role.PermissionEnum;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class DepartmentScope {

    private final DepartmentRepository departmentRepository;
    private final UserDepartmentAccessRepository accessRepository;
    private final PermissionEvaluator permissionEvaluator;
    private final HospitalContext hospitalContext;

    public DepartmentScope(
            DepartmentRepository a_departmentRepository,
            UserDepartmentAccessRepository a_accessRepository,
            PermissionEvaluator a_permissionEvaluator,
            HospitalContext a_hospitalContext) {
        this.departmentRepository = a_departmentRepository;
        this.accessRepository = a_accessRepository;
        this.permissionEvaluator = a_permissionEvaluator;
        this.hospitalContext = a_hospitalContext;
    }

    public boolean seesAll(Authentication a_authentication, User a_user) {
        if (a_user.isSuperAdmin() || AccessRules.isSuperAdmin(a_authentication)) {
            return true;
        }
        Role role = a_user.getRole();
        if (role == null || role.getDeletedAt() != null) {
            return false;
        }
        if (role.isBuiltIn()) {
            return permissionEvaluator.hasPermission(a_authentication, PermissionEnum.VIEW_ALL_ASSESSORS_DATA.name());
        }
        return RoleCodes.SCOPE_HOSPITAL.equals(role.getScopeMode());
    }

    public Set<Long> departmentIds(Authentication a_authentication, User a_user) {
        Long organizationId = hospitalContext.require(a_authentication).getId();
        Set<Long> inHospital = departmentRepository
                .findByOrganizationIdAndDeletedAtIsNullOrderByCodeAsc(organizationId)
                .stream()
                .map(Department::getId)
                .collect(Collectors.toCollection(HashSet::new));
        if (seesAll(a_authentication, a_user)) {
            return inHospital;
        }
        Set<Long> own = assigned(a_user);
        own.retainAll(inHospital);
        return own;
    }

    public boolean canSee(Authentication a_authentication, User a_user, Audit a_audit) {
        if (a_audit.getDepartment() == null) {
            return false;
        }
        return departmentIds(a_authentication, a_user).contains(a_audit.getDepartment().getId());
    }

    private Set<Long> assigned(User a_user) {
        Set<Long> base = new HashSet<>();
        if (a_user.getAssessor() != null) {
            departmentRepository
                    .findByAssessorIdAndDeletedAtIsNull(a_user.getAssessor().getId())
                    .forEach(a_department -> base.add(a_department.getId()));
        } else if (a_user.getDepartment() != null) {
            base.add(a_user.getDepartment().getId());
        }
        List<UserDepartmentAccess> extra = accessRepository.findByUserIdAndDeletedAtIsNull(a_user.getId());
        if (extra.isEmpty()) {
            return base;
        }
        Set<Long> allowed = extra.stream()
                .map(a_row -> a_row.getDepartment().getId())
                .collect(Collectors.toSet());
        if (base.isEmpty()) {
            return allowed;
        }
        base.retainAll(allowed);
        return base;
    }
}
