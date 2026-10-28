package com.app.medicalquality.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.medicalquality.auth.dto.PermissionOverrideMatrix;
import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.repository.UserPermissionOverrideRepository;
import com.app.medicalquality.auth.repository.UserRepository;
import com.app.medicalquality.auth.security.EffectivePermissionService;
import com.app.medicalquality.auth.security.JwtAuthenticationEntryPoint;
import com.app.medicalquality.auth.security.JwtAuthenticationFilter;
import com.app.medicalquality.auth.security.PermissionAspect;
import com.app.medicalquality.auth.security.PermissionEvaluator;
import com.app.medicalquality.auth.security.SecurityConfig;
import com.app.medicalquality.auth.service.JwtService;
import com.app.medicalquality.auth.service.UserPermissionOverrideService;
import com.app.medicalquality.exception.GlobalExceptionHandler;
import com.app.medicalquality.role.entity.Permission;
import com.app.medicalquality.role.entity.Role;
import com.app.medicalquality.role.entity.RolePermission;
import com.app.medicalquality.role.repository.RolePermissionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserPermissionOverrideController.class)
@AutoConfigureMockMvc
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    JwtAuthenticationEntryPoint.class,
    JwtService.class,
    PermissionAspect.class,
    PermissionEvaluator.class,
    EffectivePermissionService.class,
    GlobalExceptionHandler.class
})
@ImportAutoConfiguration({MessageSourceAutoConfiguration.class, AopAutoConfiguration.class})
@TestPropertySource(properties = {
    "JWT_SECRET=0123456789abcdef0123456789abcdef",
    "SUPER_ADMIN_USERNAME=admin",
    "SUPER_ADMIN_PASSWORD=admin-pass"
})
class UserPermissionOverrideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RolePermissionRepository rolePermissionRepository;

    @MockitoBean
    private UserPermissionOverrideRepository userPermissionOverrideRepository;

    @MockitoBean
    private UserPermissionOverrideService overrideService;

    @Test
    void aUserWithoutTheManagementPermissionIsForbidden() throws Exception {
        User staff = new User();
        staff.setId(4L);
        staff.setUsername("staff");
        staff.setEnabled(true);
        staff.setRole(new Role());
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        String token = jwtService.createToken("staff", "Assessor");

        mockMvc.perform(get("/api/organizations/1/users/5/permission-overrides")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/organizations/1/users/5/permission-overrides")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changes\":[{\"permission\":\"CONDUCT_AUDIT\",\"effect\":\"ALLOW\"}]}"))
                .andExpect(status().isForbidden());
        verify(overrideService, never()).matrix(any(), any(), any());
        verify(overrideService, never()).update(any(), any(), any(), any());
    }

    @Test
    void theManagementPermissionReachesTheService() throws Exception {
        Role role = new Role();
        role.setId(3L);
        User manager = new User();
        manager.setId(2L);
        manager.setUsername("manager");
        manager.setEnabled(true);
        manager.setRole(role);
        Permission permission = new Permission();
        permission.setName("MANAGE_USER_PERMISSION_OVERRIDES");
        RolePermission link = new RolePermission();
        link.setPermission(permission);
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(rolePermissionRepository.findByRoleAndDeletedAtIsNull(role)).thenReturn(List.of(link));
        when(overrideService.matrix(eq(1L), eq(5L), any()))
                .thenReturn(new PermissionOverrideMatrix(List.of()));
        String token = jwtService.createToken("manager", "Custom");

        mockMvc.perform(get("/api/organizations/1/users/5/permission-overrides")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions").isEmpty());
    }
}
