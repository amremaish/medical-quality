package com.app.medicalquality.score;

import com.app.medicalquality.exception.AppException;
import java.util.Locale;
import java.util.Set;

public final class ScoreValues {

    private static final Set<String> ALLOWED = Set.of("met", "partial", "not", "na");

    private ScoreValues() {}

    public static String normalize(String a_value) {
        if (a_value == null || a_value.isBlank()) {
            return null;
        }
        String value = a_value.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED.contains(value)) {
            throw new AppException("error.score.value");
        }
        return value;
    }
}
