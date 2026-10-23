package com.app.medicalquality.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.app.medicalquality.common.AppResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    public JwtAuthenticationEntryPoint(ObjectMapper a_objectMapper, MessageSource a_messageSource) {
        this.objectMapper = a_objectMapper;
        this.messageSource = a_messageSource;
    }

    @Override
    public void commence(
            HttpServletRequest a_request, HttpServletResponse a_response, AuthenticationException a_exception)
            throws IOException {
        write(a_response, HttpStatus.UNAUTHORIZED, "error.auth.unauthorized");
    }

    @Override
    public void handle(
            HttpServletRequest a_request, HttpServletResponse a_response, AccessDeniedException a_exception)
            throws IOException {
        write(a_response, HttpStatus.FORBIDDEN, "error.auth.access_denied");
    }

    private void write(HttpServletResponse a_response, HttpStatus a_status, String a_key) throws IOException {
        a_response.setStatus(a_status.value());
        a_response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                a_response.getOutputStream(),
                new AppResponse<>(null, messageSource.getMessage(a_key, null, a_key, Locale.ENGLISH)));
    }
}
