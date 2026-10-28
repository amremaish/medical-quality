package com.app.medicalquality.hospital.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.service.CurrentUserService;
import com.app.medicalquality.hospital.repository.UserDepartmentAccessRepository;
import com.app.medicalquality.organization.entity.Organization;
import com.app.medicalquality.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class HospitalContextTest {

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void hospitalUserCannotActInsideAnotherOrganization() {
        User user = hospitalUser(1L);
        HospitalContext context = context(user);
        header("9");
        assertThrows(AccessDeniedException.class, () -> context.require(mock(Authentication.class)));
    }

    @Test
    void directoryPathMustMatchTheActingHospital() {
        User user = hospitalUser(1L);
        HospitalContext context = context(user);
        header("1");
        assertThrows(AccessDeniedException.class, () -> context.requireDirectoryAdmin(mock(Authentication.class), 2L));
    }

    private static HospitalContext context(User a_user) {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.require(org.mockito.ArgumentMatchers.any())).thenReturn(a_user);
        return new HospitalContext(
                currentUserService, mock(OrganizationRepository.class), mock(UserDepartmentAccessRepository.class));
    }

    private static User hospitalUser(Long a_organizationId) {
        Organization organization = new Organization();
        organization.setId(a_organizationId);
        User user = new User();
        user.setId(2L);
        user.setSuperAdmin(false);
        user.setOrganization(organization);
        return user;
    }

    private static void header(String a_organizationId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HospitalContext.ORGANIZATION_HEADER, a_organizationId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
