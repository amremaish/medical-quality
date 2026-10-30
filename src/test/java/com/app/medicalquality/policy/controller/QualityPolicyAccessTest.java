package com.app.medicalquality.policy.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.app.medicalquality.auth.security.PermissionAspect;
import com.app.medicalquality.auth.security.PermissionEvaluator;
import com.app.medicalquality.auth.security.annotation.HasPermission;
import com.app.medicalquality.role.PermissionEnum;
import com.app.medicalquality.role.UserPermissionOverridePolicy;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class QualityPolicyAccessTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void viewAndEditAreSeparateAndNotAPersonalOverride() throws Exception {
        HasPermission view = method("page").getAnnotation(HasPermission.class);
        HasPermission edit = method("publish").getAnnotation(HasPermission.class);
        assertEquals(PermissionEnum.QUALITY_POLICY_VIEW, view.value()[0]);
        assertEquals(PermissionEnum.QUALITY_POLICY_EDIT, edit.value()[0]);
        assertFalse(UserPermissionOverridePolicy.eligible(PermissionEnum.QUALITY_POLICY_VIEW));
        assertFalse(UserPermissionOverridePolicy.eligible(PermissionEnum.QUALITY_POLICY_EDIT));
    }

    @Test
    void aUserWithoutThePrivilegeCannotCallThePolicyApi() throws Throwable {
        PermissionEvaluator evaluator = mock(PermissionEvaluator.class);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("staff", "n/a", java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(evaluator.hasPermission(authentication, "QUALITY_POLICY_VIEW")).thenReturn(false);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        PermissionAspect aspect = new PermissionAspect(evaluator);
        assertThrows(
                AccessDeniedException.class,
                () -> aspect.checkPermission(joinPoint, method("page").getAnnotation(HasPermission.class)));
    }

    @Test
    void aUserWithViewCanReadThePolicy() throws Throwable {
        PermissionEvaluator evaluator = mock(PermissionEvaluator.class);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("director", "n/a", java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(evaluator.hasPermission(authentication, "QUALITY_POLICY_VIEW")).thenReturn(true);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("page");
        PermissionAspect aspect = new PermissionAspect(evaluator);
        assertEquals("page", aspect.checkPermission(joinPoint, method("page").getAnnotation(HasPermission.class)));
        assertTrue(UserPermissionOverridePolicy.eligible(PermissionEnum.CONDUCT_AUDIT));
    }

    private Method method(String a_name) throws NoSuchMethodException {
        for (Method method : QualityPolicyController.class.getDeclaredMethods()) {
            if (method.getName().equals(a_name)) {
                return method;
            }
        }
        throw new NoSuchMethodException(a_name);
    }
}
