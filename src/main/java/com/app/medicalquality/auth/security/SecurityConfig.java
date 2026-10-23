package com.app.medicalquality.auth.security;

import com.app.medicalquality.config.AdminProperties;
import com.app.medicalquality.config.JwtProperties;
import com.app.medicalquality.config.StorageProperties;
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
    public SecurityFilterChain securityFilterChain(HttpSecurity a_http) throws Exception {
        a_http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.POST, "/api/auth/login")
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
                                "/css/**",
                                "/js/**",
                                "/icons/**",
                                "/fonts/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(handling -> handling.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
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
