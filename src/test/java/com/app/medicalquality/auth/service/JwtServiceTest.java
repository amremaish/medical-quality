package com.app.medicalquality.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.app.medicalquality.config.JwtProperties;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void createTokenCarriesUsernameAndRole() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("0123456789abcdef0123456789abcdef");
        properties.setExpirationMinutes(60);
        JwtService jwtService = new JwtService(properties);
        String token = jwtService.createToken("admin", "SUPER_ADMIN");
        assertEquals("admin", jwtService.readUsername(token));
        assertEquals("SUPER_ADMIN", jwtService.readRole(token));
    }
}
