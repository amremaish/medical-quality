package com.app.medicalquality.auth.service;

import com.app.medicalquality.auth.dto.LoginRequest;
import com.app.medicalquality.auth.dto.MeResponse;
import com.app.medicalquality.auth.dto.TokenResponse;
import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.repository.UserRepository;
import com.app.medicalquality.exception.AppException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository a_userRepository, PasswordEncoder a_passwordEncoder, JwtService a_jwtService) {
        this.userRepository = a_userRepository;
        this.passwordEncoder = a_passwordEncoder;
        this.jwtService = a_jwtService;
    }

    public TokenResponse login(LoginRequest a_request) {
        return userRepository
                .findByUsername(a_request.getUsername())
                .filter(User::isEnabled)
                .filter(a_user -> a_user.getDeletedAt() == null)
                .filter(a_user -> passwordEncoder.matches(a_request.getPassword(), a_user.getPasswordHash()))
                .map(a_user -> new TokenResponse(jwtService.createToken(a_user.getUsername(), roleName(a_user))))
                .orElseThrow(() -> new AppException("error.auth.invalid_credentials"));
    }

    public MeResponse me(String a_username) {
        return userRepository
                .findByUsername(a_username)
                .filter(User::isEnabled)
                .filter(a_user -> a_user.getDeletedAt() == null)
                .map(a_user -> new MeResponse(
                        a_user.getId(), a_user.getUsername(), roleName(a_user), a_user.isSuperAdmin()))
                .orElseThrow(() -> new AppException("error.auth.unauthorized"));
    }

    private String roleName(User a_user) {
        return a_user.getRole() == null ? null : a_user.getRole().getName();
    }
}
