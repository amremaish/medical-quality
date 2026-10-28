package com.app.medicalquality.auth.service;

import com.app.medicalquality.auth.entity.PasswordSetupToken;
import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.repository.PasswordSetupTokenRepository;
import com.app.medicalquality.exception.AppException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetupTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordSetupTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    public SetupTokenService(PasswordSetupTokenRepository a_tokenRepository, PasswordEncoder a_passwordEncoder) {
        this.tokenRepository = a_tokenRepository;
        this.passwordEncoder = a_passwordEncoder;
    }

    @Transactional
    public String issueUrl(User a_user, String a_baseUrl) {
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.consumeUnused(a_user.getId(), now);
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        PasswordSetupToken row = new PasswordSetupToken();
        row.setUser(a_user);
        row.setTokenHash(hash(token));
        row.setExpiresAt(now.plusHours(72));
        row.setCreatedAt(now);
        tokenRepository.save(row);
        return a_baseUrl + "/account/setup#token=" + token;
    }

    public void revokeUnused(Long a_userId) {
        tokenRepository.consumeUnused(a_userId, LocalDateTime.now());
    }

    @Transactional
    public void accept(String a_token, String a_newPassword) {
        if (a_token == null || a_token.isBlank()) {
            throw new AppException("error.auth.invitation_invalid");
        }
        PasswordSetupToken row = tokenRepository.lockByHash(hash(a_token.trim())).orElseThrow(
                () -> new AppException("error.auth.invitation_invalid"));
        LocalDateTime now = LocalDateTime.now();
        if (row.getUsedAt() != null || !row.getExpiresAt().isAfter(now)) {
            throw new AppException("error.auth.invitation_invalid");
        }
        User user = row.getUser();
        user.setPasswordHash(passwordEncoder.encode(a_newPassword));
        user.setEnabled(true);
        user.setMustChangePassword(false);
        row.setUsedAt(now);
    }

    public static String discardedPasswordHash(PasswordEncoder a_encoder) {
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        return a_encoder.encode(Base64.getUrlEncoder().withoutPadding().encodeToString(raw));
    }

    private String hash(String a_token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(a_token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
