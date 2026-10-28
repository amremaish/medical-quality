package com.app.medicalquality.auth.service;

import com.app.medicalquality.auth.dto.InvitationAcceptRequest;
import com.app.medicalquality.auth.dto.LoginRequest;
import com.app.medicalquality.auth.dto.MeResponse;
import com.app.medicalquality.auth.dto.PasswordChangeRequest;
import com.app.medicalquality.auth.dto.TokenResponse;
import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.repository.UserRepository;
import com.app.medicalquality.auth.security.EffectivePermissionService;
import com.app.medicalquality.exception.AppException;
import com.app.medicalquality.role.entity.Role;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SetupTokenService setupTokenService;
    private final EffectivePermissionService effectivePermissionService;

    public AuthService(
            UserRepository a_userRepository,
            PasswordEncoder a_passwordEncoder,
            JwtService a_jwtService,
            SetupTokenService a_setupTokenService,
            EffectivePermissionService a_effectivePermissionService) {
        this.userRepository = a_userRepository;
        this.passwordEncoder = a_passwordEncoder;
        this.jwtService = a_jwtService;
        this.setupTokenService = a_setupTokenService;
        this.effectivePermissionService = a_effectivePermissionService;
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
                .map(this::meOf)
                .orElseThrow(() -> new AppException("error.auth.unauthorized"));
    }

    @Transactional
    public void changePassword(String a_username, PasswordChangeRequest a_request) {
        User user = userRepository
                .findByUsername(a_username)
                .filter(a_user -> a_user.getDeletedAt() == null)
                .orElseThrow(() -> new AppException("error.auth.unauthorized"));
        if (!passwordEncoder.matches(a_request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException("error.auth.invalid_credentials");
        }
        user.setPasswordHash(passwordEncoder.encode(a_request.getNewPassword()));
        user.setMustChangePassword(false);
    }

    @Transactional
    public void acceptInvitation(InvitationAcceptRequest a_request) {
        setupTokenService.accept(a_request.getToken(), a_request.getNewPassword());
    }

    private MeResponse meOf(User a_user) {
        String organization = a_user.getOrganization() == null ? null : a_user.getOrganization().getName();
        Long organizationId = a_user.getOrganization() == null ? null : a_user.getOrganization().getId();
        return new MeResponse(
                a_user.getId(),
                a_user.getUsername(),
                roleName(a_user),
                a_user.isSuperAdmin(),
                a_user.isMustChangePassword(),
                organization,
                roleCode(a_user),
                organizationId,
                permissionsOf(a_user));
    }

    private List<String> permissionsOf(User a_user) {
        return effectivePermissionService.names(a_user);
    }

    private String roleCode(User a_user) {
        return a_user.getRole() == null ? null : a_user.getRole().getRoleCode();
    }

    private String roleName(User a_user) {
        return a_user.getRole() == null ? null : a_user.getRole().getName();
    }
}
