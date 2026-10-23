package com.app.medicalquality.auth.service;

import com.app.medicalquality.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMillis;

    public JwtService(JwtProperties a_properties) {
        byte[] secretBytes = a_properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMillis = a_properties.getExpirationMinutes() * 60_000L;
    }

    public String createToken(String a_username, String a_role) {
        Date issuedAt = new Date();
        JwtBuilder builder = Jwts.builder()
                .subject(a_username)
                .issuedAt(issuedAt)
                .expiration(new Date(issuedAt.getTime() + expirationMillis));
        if (a_role != null) {
            builder.claim("role", a_role);
        }
        return builder.signWith(key).compact();
    }

    public String readUsername(String a_token) {
        return readClaims(a_token).getSubject();
    }

    public String readRole(String a_token) {
        return readClaims(a_token).get("role", String.class);
    }

    private Claims readClaims(String a_token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(a_token).getPayload();
    }
}
