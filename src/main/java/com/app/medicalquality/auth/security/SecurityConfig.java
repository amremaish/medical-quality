package com.app.medicalquality.auth.security;

import com.app.medicalquality.auth.repository.UserRepository;
import com.app.medicalquality.config.AdminProperties;
import com.app.medicalquality.config.JwtProperties;
import com.app.medicalquality.config.StorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.MessageSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, AdminProperties.class, StorageProperties.class})
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(
            JwtAuthenticationFilter a_jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint a_jwtAuthenticationEntryPoint) {
        this.jwtAuthenticationFilter = a_jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = a_jwtAuthenticationEntryPoint;
    }

    @Bean
    public MustChangePasswordFilter mustChangePasswordFilter(
            UserRepository a_userRepository, MessageSource a_messageSource, ObjectMapper a_objectMapper) {
        return new MustChangePasswordFilter(a_userRepository, a_messageSource, a_objectMapper);
    }

    @Bean
    public FilterRegistrationBean<MustChangePasswordFilter> mustChangePasswordFilterRegistration(
            MustChangePasswordFilter a_filter) {
        FilterRegistrationBean<MustChangePasswordFilter> registration = new FilterRegistrationBean<>(a_filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity a_http, MustChangePasswordFilter a_mustChangePasswordFilter)
            throws Exception {
        a_http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/invitations/accept")
                        .permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**")
                        .permitAll()
                        .requestMatchers(
                                "/",
                                "/login",
                                "/audits",
                                "/audits/*",
                                "/record-reviews",
                                "/record-reviews/*",
                                "/dashboard",
                                "/dashboard/*",
                                "/departments",
                                "/departments/**",
                                "/assessors",
                                "/findings",
                                "/capas",
                                "/capas/**",
                                "/evidence",
                                "/evidence/**",
                                "/imports",
                                "/imports/**",
                                "/hospitals",
                                "/organizations",
                                "/organizations/**",
                                "/owners",
                                "/owners/**",
                                "/account/password",
                                "/account/setup",
                                "/users",
                                "/users/**",
                                "/roles",
                                "/roles/**",
                                "/css/**",
                                "/js/**",
                                "/icons/**",
                                "/fonts/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(handling -> handling.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(a_mustChangePasswordFilter, JwtAuthenticationFilter.class);
        return a_http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter a_filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(a_filter);
        registration.setEnabled(false);
        return registration;
    }
}
