package com.app.medicalquality.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.app.medicalquality.auth.security.annotation.HasPermission;
import com.app.medicalquality.role.PermissionEnum;
import java.lang.reflect.Method;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class PermissionEnforcementTest {

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void annotationDeniesWhenTheEvaluatorRejectsThePermission() throws Throwable {
        PermissionEvaluator evaluator = mock(PermissionEvaluator.class);
        UsernamePasswordAuthenticationToken authentication = authentication(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(evaluator.hasPermission(authentication, "CONDUCT_AUDIT")).thenReturn(false);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        PermissionAspect aspect = new PermissionAspect(evaluator);

        assertThrows(
                AccessDeniedException.class,
                () -> aspect.checkPermission(joinPoint, annotation()));
    }

    @Test
    void annotationProceedsWhenTheEvaluatorAllowsThePermission() throws Throwable {
        PermissionEvaluator evaluator = mock(PermissionEvaluator.class);
        UsernamePasswordAuthenticationToken authentication = authentication(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(evaluator.hasPermission(authentication, "CONDUCT_AUDIT")).thenReturn(true);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("saved");
        PermissionAspect aspect = new PermissionAspect(evaluator);

        assertEquals("saved", aspect.checkPermission(joinPoint, annotation()));
        verify(joinPoint).proceed();
    }

    @Test
    void evaluatorUsesAllowAndDenyFromTheSameResolver() {
        com.app.medicalquality.auth.repository.UserRepository users =
                mock(com.app.medicalquality.auth.repository.UserRepository.class);
        com.app.medicalquality.role.repository.RolePermissionRepository roles =
                mock(com.app.medicalquality.role.repository.RolePermissionRepository.class);
        com.app.medicalquality.auth.repository.UserPermissionOverrideRepository overrides =
                mock(com.app.medicalquality.auth.repository.UserPermissionOverrideRepository.class);
        com.app.medicalquality.auth.entity.User user = new com.app.medicalquality.auth.entity.User();
        user.setId(8L);
        user.setUsername("staff");
        com.app.medicalquality.role.entity.Role role = new com.app.medicalquality.role.entity.Role();
        role.setId(3L);
        user.setRole(role);
        com.app.medicalquality.role.entity.Permission conduct = permission("CONDUCT_AUDIT", 1L);
        com.app.medicalquality.role.entity.RolePermission link = new com.app.medicalquality.role.entity.RolePermission();
        link.setPermission(conduct);
        when(roles.findByRoleAndDeletedAtIsNull(role)).thenReturn(List.of(link));
        when(users.findByUsername("staff")).thenReturn(java.util.Optional.of(user));
        com.app.medicalquality.auth.entity.UserPermissionOverride deny =
                new com.app.medicalquality.auth.entity.UserPermissionOverride();
        deny.setEffect(EffectivePermissions.DENY);
        deny.setPermission(conduct);
        when(overrides.findByUser_IdAndDeletedAtIsNull(8L))
                .thenReturn(List.of(deny))
                .thenReturn(List.of());
        EffectivePermissionService effective = new EffectivePermissionService(roles, overrides);
        PermissionEvaluator evaluator = new PermissionEvaluator(effective, users);
        UsernamePasswordAuthenticationToken authentication = authentication(false);

        assertFalse(evaluator.hasPermission(authentication, "CONDUCT_AUDIT"));
        assertTrue(evaluator.hasPermission(authentication, "CONDUCT_AUDIT"));
    }

    private static UsernamePasswordAuthenticationToken authentication(boolean a_superAdmin) {
        List<SimpleGrantedAuthority> authorities = a_superAdmin
                ? List.of(new SimpleGrantedAuthority(AccessRules.SUPER_ADMIN))
                : List.of(new SimpleGrantedAuthority("ROLE_staff"));
        return new UsernamePasswordAuthenticationToken("staff", null, authorities);
    }

    private static HasPermission annotation() throws Exception {
        Method method = Sample.class.getDeclaredMethod("conduct");
        return method.getAnnotation(HasPermission.class);
    }

    private static com.app.medicalquality.role.entity.Permission permission(String a_name, Long a_id) {
        com.app.medicalquality.role.entity.Permission permission = new com.app.medicalquality.role.entity.Permission();
        permission.setId(a_id);
        permission.setName(a_name);
        return permission;
    }

    static class Sample {
        @HasPermission(PermissionEnum.CONDUCT_AUDIT)
        void conduct() {}
    }
}
