package com.app.medicalquality.auth;

public final class AccountStatus {

    public static final String ACTIVE = "Active";
    public static final String INACTIVE = "Inactive";

    private AccountStatus() {}

    public static String of(boolean a_enabled) {
        return a_enabled ? ACTIVE : INACTIVE;
    }
}
