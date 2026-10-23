package com.app.medicalquality.organization;

import java.security.SecureRandom;

public final class PasswordGenerator {

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordGenerator() {}

    public static String generate() {
        char[] value = new char[12];
        for (int index = 0; index < value.length; index++) {
            value[index] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return new String(value);
    }
}
