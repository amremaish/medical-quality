package com.app.medicalquality.policy;

import com.app.medicalquality.exception.AppException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

public final class CapaSchedule {

    private CapaSchedule() {}

    public static String severity(ResolvedPolicy a_policy, String a_canonical, String a_raw) {
        if (a_canonical != null && !a_canonical.isBlank() && known(a_policy, a_canonical)) {
            return a_canonical;
        }
        String raw = a_raw == null ? "" : a_raw.trim();
        if (known(a_policy, raw)) {
            return code(a_policy, raw);
        }
        var aliases = a_policy.json("findings.legacyAliases");
        if (aliases != null && aliases.isObject()) {
            var names = aliases.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                if (name.equalsIgnoreCase(raw) && known(a_policy, aliases.get(name).asText())) {
                    return aliases.get(name).asText();
                }
            }
        }
        throw new AppException("error.policy.finding.severity", new Object[] {raw.isEmpty() ? "blank" : raw});
    }

    public static LocalDate dueDate(ResolvedPolicy a_policy, String a_severity, LocalDate a_opened, LocalDate a_requested) {
        LocalDate derived = addDays(a_policy, a_opened, a_policy.deadlineDays(a_severity));
        if (a_requested != null && a_requested.isAfter(derived)) {
            throw new AppException("error.policy.capa.deadline", new Object[] {a_severity, derived.toString()});
        }
        return a_requested == null ? derived : a_requested;
    }

    public static LocalDate addDays(ResolvedPolicy a_policy, LocalDate a_start, int a_days) {
        if (!a_policy.bool("notifications.deadlineUsesWorkingDays")) {
            return a_start.plusDays(a_days);
        }
        Set<DayOfWeek> working = a_policy.workingDays();
        Set<LocalDate> holidays = a_policy.holidays();
        LocalDate date = a_start;
        int left = a_days;
        while (left > 0) {
            date = date.plusDays(1);
            if (working.contains(date.getDayOfWeek()) && !holidays.contains(date)) {
                left--;
            }
        }
        return date;
    }

    private static boolean known(ResolvedPolicy a_policy, String a_code) {
        if (a_code == null || a_code.isBlank()) {
            return false;
        }
        var levels = a_policy.json("findings.levels");
        if (levels == null || !levels.isArray()) {
            return false;
        }
        for (var level : levels) {
            if (a_code.equalsIgnoreCase(level.path("code").asText())) {
                return true;
            }
        }
        return false;
    }

    private static String code(ResolvedPolicy a_policy, String a_raw) {
        var levels = a_policy.json("findings.levels");
        for (var level : levels) {
            if (a_raw.equalsIgnoreCase(level.path("code").asText())) {
                return level.path("code").asText();
            }
        }
        return a_raw.toLowerCase(Locale.ROOT);
    }
}
