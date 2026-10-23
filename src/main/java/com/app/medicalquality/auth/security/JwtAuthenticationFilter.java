package com.app.medicalquality.auth.security;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.repository.UserRepository;
import com.app.medicalquality.auth.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService a_jwtService, UserRepository a_userRepository) {
        this.jwtService = a_jwtService;
        this.userRepository = a_userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest a_request, HttpServletResponse a_response, FilterChain a_chain)
            throws ServletException, IOException {
        String header = a_request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            authenticate(header.substring(7));
        }
        a_chain.doFilter(a_request, a_response);
    }

    private void authenticate(String a_token) {
        try {
            userRepository
                    .findByUsername(jwtService.readUsername(a_token))
                    .filter(User::isEnabled)
                    .filter(a_user -> a_user.getDeletedAt() == null)
                    .ifPresent(this::setAuthentication);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }
    }

    private void setAuthentication(User a_user) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (a_user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + a_user.getRole().getName()));
        }
        if (a_user.isSuperAdmin()) {
            authorities.add(new SimpleGrantedAuthority(AccessRules.SUPER_ADMIN));
        }
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(a_user.getUsername(), null, authorities));
    }
}
