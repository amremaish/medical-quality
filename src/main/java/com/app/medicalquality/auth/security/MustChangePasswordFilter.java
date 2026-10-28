package com.app.medicalquality.auth.security;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.repository.UserRepository;
import com.app.medicalquality.common.AppResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class MustChangePasswordFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;

    public MustChangePasswordFilter(
            UserRepository a_userRepository, MessageSource a_messageSource, ObjectMapper a_objectMapper) {
        this.userRepository = a_userRepository;
        this.messageSource = a_messageSource;
        this.objectMapper = a_objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest a_request, HttpServletResponse a_response, FilterChain a_chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || allowed(path(a_request))) {
            a_chain.doFilter(a_request, a_response);
            return;
        }
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user != null && user.isMustChangePassword()) {
            a_response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            a_response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(
                    a_response.getOutputStream(),
                    new AppResponse<>(
                            null,
                            messageSource.getMessage(
                                    "error.auth.password_change_required", null, "error.auth.password_change_required", Locale.ENGLISH)));
            return;
        }
        a_chain.doFilter(a_request, a_response);
    }

    private boolean allowed(String a_path) {
        return !a_path.startsWith("/api/") || "/api/auth/me".equals(a_path) || "/api/auth/password".equals(a_path);
    }

    private String path(HttpServletRequest a_request) {
        String uri = a_request.getRequestURI();
        String context = a_request.getContextPath();
        if (context != null && !context.isEmpty() && uri.startsWith(context)) {
            return uri.substring(context.length());
        }
        return uri;
    }
}
