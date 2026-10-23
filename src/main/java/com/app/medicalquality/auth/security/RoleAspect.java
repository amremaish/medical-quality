package com.app.medicalquality.auth.security;

import com.app.medicalquality.auth.security.annotation.HasRole;
import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RoleAspect {

    @Around("@annotation(a_hasRole) || @within(a_hasRole)")
    public Object checkRoles(ProceedingJoinPoint a_joinPoint, HasRole a_hasRole) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (AccessRules.isSuperAdmin(authentication)) {
            return a_joinPoint.proceed();
        }
        boolean allowed = authentication != null
                && Arrays.stream(a_hasRole.value())
                        .anyMatch(a_role -> hasRole(authentication, a_role));
        if (!allowed) {
            throw new AccessDeniedException("Access denied");
        }
        return a_joinPoint.proceed();
    }

    private boolean hasRole(Authentication a_authentication, String a_role) {
        return a_authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a_authority -> a_authority.equals("ROLE_" + a_role));
    }
}
