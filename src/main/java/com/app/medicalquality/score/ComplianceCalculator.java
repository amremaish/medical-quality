package com.app.medicalquality.score;

import java.util.Collection;
import java.util.Set;

public final class ComplianceCalculator {

    private static final Set<String> LOCKED = Set.of("Submitted", "Approved", "Closed");

    private ComplianceCalculator() {}

    public static boolean locked(String a_status) {
        return LOCKED.contains(a_status);
    }

    public static ComplianceView fromCounts(int a_met, int a_partial, int a_not, int a_na, int a_total) {
        int denominator = a_total - a_na;
        int scored = a_met + a_partial + a_not + a_na;
        int percent = denominator > 0
                ? (int) Math.round(((a_met + a_partial * 0.5) / denominator) * 100)
                : 0;
        return new ComplianceView(
                a_met, a_partial, a_not, a_na, a_total, denominator, scored, percent, a_total - scored);
    }

    public static ComplianceView fromValues(int a_total, Collection<String> a_values) {
        int met = 0;
        int partial = 0;
        int not = 0;
        int na = 0;
        for (String value : a_values) {
            if ("met".equals(value)) {
                met++;
            } else if ("partial".equals(value)) {
                partial++;
            } else if ("not".equals(value)) {
                not++;
            } else if ("na".equals(value)) {
                na++;
            }
        }
        return fromCounts(met, partial, not, na, a_total);
    }

    public static String standardTone(int a_met, int a_partial, int a_not, int a_na, int a_total) {
        int unscored = a_total - a_met - a_partial - a_not - a_na;
        if (a_not > 0) {
            return "not";
        }
        if (a_partial > 0) {
            return "partial";
        }
        if (unscored == a_total) {
            return "unscored";
        }
        if (a_met + a_na == a_total) {
            return "met";
        }
        return "mixed";
    }

    public record ComplianceView(
            int met,
            int partial,
            int notMet,
            int na,
            int total,
            int denominator,
            int scored,
            int percent,
            int unscored) {}
}
