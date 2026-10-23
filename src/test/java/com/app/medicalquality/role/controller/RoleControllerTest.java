package com.app.medicalquality.role.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.repository.UserRepository;
import com.app.medicalquality.auth.security.JwtAuthenticationEntryPoint;
import com.app.medicalquality.auth.security.JwtAuthenticationFilter;
import com.app.medicalquality.auth.security.PermissionAspect;
import com.app.medicalquality.auth.security.PermissionEvaluator;
import com.app.medicalquality.auth.security.RoleAspect;
import com.app.medicalquality.auth.security.SecurityConfig;
import com.app.medicalquality.auth.service.JwtService;
import com.app.medicalquality.exception.GlobalExceptionHandler;
import com.app.medicalquality.role.dto.RoleResponse;
import com.app.medicalquality.role.entity.Role;
import com.app.medicalquality.role.repository.RolePermissionRepository;
import com.app.medicalquality.role.service.RoleService;
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

@WebMvcTest(controllers = RoleController.class)
@AutoConfigureMockMvc
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    JwtAuthenticationEntryPoint.class,
    JwtService.class,
    PermissionAspect.class,
    PermissionEvaluator.class,
    RoleAspect.class,
    GlobalExceptionHandler.class
})
@ImportAutoConfiguration({MessageSourceAutoConfiguration.class, AopAutoConfiguration.class})
@TestPropertySource(properties = {
    "JWT_SECRET=0123456789abcdef0123456789abcdef",
    "SUPER_ADMIN_USERNAME=admin",
    "SUPER_ADMIN_PASSWORD=admin-pass"
})
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RolePermissionRepository rolePermissionRepository;

    @MockitoBean
    private RoleService roleService;

    @Test
    void superAdminCreatesRole() throws Exception {
        User admin = new User();
        admin.setUsername("admin");
        admin.setEnabled(true);
        admin.setSuperAdmin(true);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(roleService.create(any())).thenReturn(new RoleResponse(1L, "auditor", List.of("VIEW_ROLES")));
        mockMvc.perform(post("/api/roles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createToken("admin", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"auditor\",\"permissions\":[\"VIEW_ROLES\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("auditor"))
                .andExpect(jsonPath("$.message").value("Role created successfully"));
    }

    @Test
    void userWithoutPermissionIsForbidden() throws Exception {
        Role role = new Role();
        role.setName("auditor");
        User staff = new User();
        staff.setUsername("staff");
        staff.setEnabled(true);
        staff.setRole(role);
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        mockMvc.perform(post("/api/roles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.createToken("staff", "auditor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"reviewer\",\"permissions\":[\"VIEW_ROLES\"]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }
}
