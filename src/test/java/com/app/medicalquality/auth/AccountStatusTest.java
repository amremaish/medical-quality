package com.app.medicalquality.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AccountStatusTest {

    @Test
    void activeWhenTheAccountCanSignIn() {
        assertEquals(AccountStatus.ACTIVE, AccountStatus.of(true));
    }

    @Test
    void inactiveWhenSignInIsOff() {
        assertEquals(AccountStatus.INACTIVE, AccountStatus.of(false));
    }
}
