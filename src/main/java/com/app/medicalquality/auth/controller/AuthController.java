package com.app.medicalquality.auth.controller;

import com.app.medicalquality.auth.dto.LoginRequest;
import com.app.medicalquality.auth.dto.MeResponse;
import com.app.medicalquality.auth.dto.PasswordChangeRequest;
import com.app.medicalquality.auth.dto.TokenResponse;
import com.app.medicalquality.auth.service.AuthService;
import com.app.medicalquality.common.AppResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService a_authService) {
        this.authService = a_authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AppResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest a_request) {
        return AppResponse.success(authService.login(a_request));
    }

    @GetMapping("/me")
    public ResponseEntity<AppResponse<MeResponse>> me(Authentication a_authentication) {
        return AppResponse.success(authService.me(a_authentication.getName()));
    }

    @PostMapping("/password")
    public ResponseEntity<AppResponse<String>> password(
            @Valid @RequestBody PasswordChangeRequest a_request, Authentication a_authentication) {
        authService.changePassword(a_authentication.getName(), a_request);
        return AppResponse.success("Password updated");
    }
}
