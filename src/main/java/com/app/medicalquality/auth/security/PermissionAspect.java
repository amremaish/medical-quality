package com.app.medicalquality.auth.security;

import com.app.medicalquality.auth.security.annotation.HasPermission;
import com.app.medicalquality.role.PermissionEnum;
import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PermissionAspect {

    private final PermissionEvaluator permissionEvaluator;

    public PermissionAspect(PermissionEvaluator a_permissionEvaluator) {
        this.permissionEvaluator = a_permissionEvaluator;
    }

    @Around("@annotation(a_hasPermission)")
    public Object checkPermission(ProceedingJoinPoint a_joinPoint, HasPermission a_hasPermission) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (AccessRules.isSuperAdmin(authentication)) {
            return a_joinPoint.proceed();
        }
        boolean allowed = Arrays.stream(a_hasPermission.value())
                .map(PermissionEnum::name)
                .allMatch(a_permission -> permissionEvaluator.hasPermission(authentication, a_permission));
        if (!allowed) {
            throw new AccessDeniedException("Access denied");
        }
        return a_joinPoint.proceed();
    }
}
