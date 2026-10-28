package com.app.medicalquality.gsr.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.repository.UserRepository;
import com.app.medicalquality.auth.security.EffectivePermissionService;
import com.app.medicalquality.auth.security.JwtAuthenticationEntryPoint;
import com.app.medicalquality.auth.security.JwtAuthenticationFilter;
import com.app.medicalquality.auth.security.PermissionAspect;
import com.app.medicalquality.auth.security.PermissionEvaluator;
import com.app.medicalquality.auth.security.SecurityConfig;
import com.app.medicalquality.auth.service.JwtService;
import com.app.medicalquality.exception.GlobalExceptionHandler;
import com.app.medicalquality.gsr.service.AuditCommandService;
import com.app.medicalquality.gsr.service.AuditQueryService;
import com.app.medicalquality.gsr.service.AuditScoreService;
import com.app.medicalquality.gsr.service.AuditSubmitService;
import com.app.medicalquality.role.repository.RolePermissionRepository;
import com.app.medicalquality.auth.repository.UserPermissionOverrideRepository;
import com.app.medicalquality.score.ComplianceCalculator;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuditController.class)
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
@ImportAutoConfiguration(MessageSourceAutoConfiguration.class)
@TestPropertySource(properties = {
    "JWT_SECRET=0123456789abcdef0123456789abcdef",
    "SUPER_ADMIN_USERNAME=admin",
    "SUPER_ADMIN_PASSWORD=admin-pass"
})
class AuditScoreControllerTest {

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
    private AuditQueryService auditQueryService;

    @MockitoBean
    private AuditCommandService auditCommandService;

    @MockitoBean
    private AuditScoreService auditScoreService;

    @MockitoBean
    private AuditSubmitService auditSubmitService;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setEnabled(true);
        user.setSuperAdmin(true);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
    }

    @Test
    void saveScoreReturnsHalfCreditCompliance() throws Exception {
        when(auditScoreService.saveScore(eq(5L), eq(9L), eq("met"), any()))
                .thenReturn(ComplianceCalculator.fromCounts(1, 0, 0, 0, 2));
        String token = jwtService.createToken("admin", null);
        mockMvc.perform(put("/api/audits/5/scores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"elementId\":9,\"value\":\"met\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.percent").value(50))
                .andExpect(jsonPath("$.data.met").value(1))
                .andExpect(jsonPath("$.data.unscored").value(1));
    }
}
