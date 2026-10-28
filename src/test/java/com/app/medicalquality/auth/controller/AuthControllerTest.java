package com.app.medicalquality.auth.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.entity.UserPermissionOverride;
import com.app.medicalquality.auth.repository.UserRepository;
import com.app.medicalquality.auth.repository.UserPermissionOverrideRepository;
import com.app.medicalquality.auth.security.EffectivePermissionService;
import com.app.medicalquality.auth.security.JwtAuthenticationEntryPoint;
import com.app.medicalquality.auth.security.JwtAuthenticationFilter;
import com.app.medicalquality.auth.security.SecurityConfig;
import com.app.medicalquality.auth.service.AuthService;
import com.app.medicalquality.auth.service.JwtService;
import com.app.medicalquality.auth.service.SetupTokenService;
import com.app.medicalquality.exception.GlobalExceptionHandler;
import com.app.medicalquality.role.entity.Permission;
import com.app.medicalquality.role.entity.Role;
import com.app.medicalquality.role.entity.RolePermission;
import com.app.medicalquality.role.repository.RolePermissionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    JwtAuthenticationEntryPoint.class,
    AuthService.class,
    EffectivePermissionService.class,
    JwtService.class,
    GlobalExceptionHandler.class
})
@ImportAutoConfiguration(MessageSourceAutoConfiguration.class)
@TestPropertySource(properties = {
    "JWT_SECRET=0123456789abcdef0123456789abcdef",
    "SUPER_ADMIN_USERNAME=admin",
    "SUPER_ADMIN_PASSWORD=admin-pass"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private SetupTokenService setupTokenService;

    @MockitoBean
    private RolePermissionRepository rolePermissionRepository;

    @MockitoBean
    private UserPermissionOverrideRepository userPermissionOverrideRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPasswordHash(passwordEncoder.encode("secret"));
        user.setEnabled(true);
        user.setSuperAdmin(true);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
    }

    @Test
    void loginReturnsTokenAndMeReturnsUser() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully Done"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode token = objectMapper.readTree(body).get("data").get("token");
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token.asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.roleName").value(nullValue()))
                .andExpect(jsonPath("$.data.roleCode").value(nullValue()))
                .andExpect(jsonPath("$.data.superAdmin").value(true))
                .andExpect(jsonPath("$.data.permissions").isEmpty());
    }

    @Test
    void meReturnsThePermissionsCurrentlyOnTheRole() throws Exception {
        Role role = new Role();
        role.setRoleCode("CUSTOM_ROLE");
        role.setName("Floor reviewer");
        user.setSuperAdmin(false);
        user.setRole(role);
        Permission conduct = permission("CONDUCT_AUDIT");
        Permission removed = permission("MANAGE_CAPA");
        removed.setDeletedAt(LocalDateTime.now());
        Permission capa = permission("MANAGE_CAPA");
        when(rolePermissionRepository.findByRoleAndDeletedAtIsNull(role))
                .thenReturn(List.of(link(conduct), link(removed)))
                .thenReturn(List.of(link(capa)));

        String token = login();
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCode").value("CUSTOM_ROLE"))
                .andExpect(jsonPath("$.data.permissions", hasSize(1)))
                .andExpect(jsonPath("$.data.permissions[0]").value("CONDUCT_AUDIT"));
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions", hasSize(1)))
                .andExpect(jsonPath("$.data.permissions[0]").value("MANAGE_CAPA"));
    }

    @Test
    void meReflectsAllowAndDenyThenInherit() throws Exception {
        Role role = new Role();
        role.setRoleCode("ASSESSOR");
        role.setName("Assessor");
        user.setSuperAdmin(false);
        user.setRole(role);
        Permission conduct = permission("CONDUCT_AUDIT");
        Permission capa = permission("MANAGE_CAPA");
        when(rolePermissionRepository.findByRoleAndDeletedAtIsNull(role)).thenReturn(List.of(link(conduct)));
        UserPermissionOverride deny = new UserPermissionOverride();
        deny.setEffect("DENY");
        deny.setPermission(conduct);
        UserPermissionOverride allow = new UserPermissionOverride();
        allow.setEffect("ALLOW");
        allow.setPermission(capa);
        when(userPermissionOverrideRepository.findByUser_IdAndDeletedAtIsNull(1L))
                .thenReturn(List.of(deny))
                .thenReturn(List.of(allow))
                .thenReturn(List.of());

        String token = login();
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions").isEmpty());
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions", hasSize(2)))
                .andExpect(jsonPath("$.data.permissions[0]").value("CONDUCT_AUDIT"))
                .andExpect(jsonPath("$.data.permissions[1]").value("MANAGE_CAPA"));
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions", hasSize(1)))
                .andExpect(jsonPath("$.data.permissions[0]").value("CONDUCT_AUDIT"));
    }

    private String login() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).get("data").get("token").asText();
    }

    private static Permission permission(String a_name) {
        Permission permission = new Permission();
        permission.setName(a_name);
        return permission;
    }

    private static RolePermission link(Permission a_permission) {
        RolePermission link = new RolePermission();
        link.setPermission(a_permission);
        return link;
    }

    @Test
    void badCredentialsReturnInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void missingTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }
}
