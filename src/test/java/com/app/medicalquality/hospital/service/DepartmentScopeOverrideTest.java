package com.app.medicalquality.hospital.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.security.PermissionEvaluator;
import com.app.medicalquality.gsr.entity.Audit;
import com.app.medicalquality.hospital.entity.Department;
import com.app.medicalquality.hospital.repository.DepartmentRepository;
import com.app.medicalquality.hospital.repository.UserDepartmentAccessRepository;
import com.app.medicalquality.organization.entity.Organization;
import com.app.medicalquality.role.PermissionEnum;
import com.app.medicalquality.role.RoleCodes;
import com.app.medicalquality.role.entity.Role;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class DepartmentScopeOverrideTest {

    @Test
    void anotherDepartmentStaysOutOfScopeWhenAPermissionIsAllowed() {
        DepartmentScope scope = scope(false);
        User user = user(false);
        assertFalse(scope.canSee(mock(Authentication.class), user, audit(9L)));
    }

    @Test
    void deniedHospitalWideViewDoesNotOpenOtherDepartments() {
        DepartmentScope scope = scope(true);
        User user = user(true);
        Authentication authentication = mock(Authentication.class);
        when(scopeEvaluator.hasPermission(authentication, PermissionEnum.VIEW_ALL_ASSESSORS_DATA.name()))
                .thenReturn(false);
        assertFalse(scope.seesAll(authentication, user));
        assertFalse(scope.canSee(authentication, user, audit(9L)));
    }

    private static PermissionEvaluator scopeEvaluator;

    private static DepartmentScope scope(boolean a_builtIn) {
        scopeEvaluator = mock(PermissionEvaluator.class);
        Organization organization = new Organization();
        organization.setId(1L);
        DepartmentRepository departments = mock(DepartmentRepository.class);
        when(departments.findByOrganizationIdAndDeletedAtIsNullOrderByCodeAsc(1L))
                .thenReturn(List.of(department(5L), department(9L)));
        UserDepartmentAccessRepository access = mock(UserDepartmentAccessRepository.class);
        when(access.findByUserIdAndDeletedAtIsNull(4L)).thenReturn(List.of());
        HospitalContext context = mock(HospitalContext.class);
        when(context.require(org.mockito.ArgumentMatchers.any())).thenReturn(organization);
        return new DepartmentScope(departments, access, scopeEvaluator, context);
    }

    private static User user(boolean a_builtIn) {
        Organization organization = new Organization();
        organization.setId(1L);
        Role role = new Role();
        role.setId(3L);
        role.setBuiltIn(a_builtIn);
        role.setScopeMode(a_builtIn ? RoleCodes.SCOPE_HOSPITAL : RoleCodes.SCOPE_DEPARTMENT);
        User user = new User();
        user.setId(4L);
        user.setOrganization(organization);
        user.setRole(role);
        user.setDepartment(department(5L));
        return user;
    }

    private static Department department(Long a_id) {
        Department department = new Department();
        department.setId(a_id);
        return department;
    }

    private static Audit audit(Long a_departmentId) {
        Audit audit = new Audit();
        audit.setDepartment(department(a_departmentId));
        return audit;
    }
}
