package com.app.medicalquality.record.controller;

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
import com.app.medicalquality.record.dto.MrViews;
import com.app.medicalquality.record.service.MrReviewService;
import com.app.medicalquality.role.repository.RolePermissionRepository;
import com.app.medicalquality.auth.repository.UserPermissionOverrideRepository;
import com.app.medicalquality.score.RecordScoreCalculator;
import java.time.LocalDate;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MrReviewController.class)
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
class MrAnswerControllerTest {

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
    private MrReviewService mrReviewService;

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
    void saveAnswerReturnsRecordStats() throws Exception {
        RecordScoreCalculator.RecordStats stats = RecordScoreCalculator.summarize(
                List.of(new RecordScoreCalculator.Mark(true, true, true, false)));
        MrViews.RecordLine record = new MrViews.RecordLine("MR.1", "P-1", stats, List.of());
        when(mrReviewService.saveAnswer(eq(3L), any())).thenReturn(new MrViews.ReviewDetail(
                3L, "Hospital", "Reviewer", LocalDate.of(2026, 3, 1), "Surgery", List.of(), List.of(record)));
        String token = jwtService.createToken("admin", null);
        mockMvc.perform(put("/api/record-reviews/3/answers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordCode\":\"MR.1\",\"itemId\":4,\"legible\":true,\"accurate\":true,\"complete\":true,\"na\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].stats.overall").value(100))
                .andExpect(jsonPath("$.data.records[0].code").value("MR.1"));
    }
}
