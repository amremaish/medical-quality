package com.app.medicalquality.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class PolicyDocuments {

    public record FieldError(String key, String message) {}

    public record Change(String key, JsonNode oldValue, JsonNode newValue) {}

    public record Window(
            long id,
            int versionNumber,
            LocalDate effectiveFrom,
            LocalDate effectiveUntil,
            String status,
            String documentJson) {}

    private static final Set<String> FRAMEWORKS = Set.of("GAHAR", "JCI", "ISO_9001", "ISO_15189", "OTHER");
    private static final Set<String> DAYS = Set.of("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT");
    private static final Set<String> SAMPLE_MODES = Set.of("fixed", "percent", "confidence");
    private static final Set<String> SCALES = Set.of("met_partial_not_na", "percent");
    private static final Set<String> LANGUAGES = Set.of("en", "ar");
    private static final Set<String> CALENDARS = Set.of("gregorian", "hijri");

    private PolicyDocuments() {}

    public static ObjectNode overlay(ObjectNode a_base, ObjectNode a_overrides) {
        ObjectNode out = a_base.deepCopy();
        if (a_overrides == null) {
            return out;
        }
        Iterator<String> names = a_overrides.fieldNames();
        while (names.hasNext()) {
            String key = names.next();
            out.set(key, a_overrides.get(key).deepCopy());
        }
        return out;
    }

    public static List<Change> diff(ObjectNode a_before, ObjectNode a_after) {
        List<Change> changes = new ArrayList<>();
        for (QualityPolicyCatalog.Setting setting : QualityPolicyCatalog.settings()) {
            JsonNode oldValue = a_before.get(setting.key());
            JsonNode newValue = a_after.get(setting.key());
            if (oldValue == null && newValue == null) {
                continue;
            }
            if (oldValue == null || newValue == null || !oldValue.equals(newValue)) {
                changes.add(new Change(setting.key(), oldValue == null ? null : oldValue.deepCopy(),
                        newValue == null ? null : newValue.deepCopy()));
            }
        }
        return changes;
    }

    public static Optional<Window> select(List<Window> a_windows, LocalDate a_asOf) {
        return a_windows.stream()
                .filter(a_window -> applicable(a_window.status()))
                .filter(a_window -> !a_window.effectiveFrom().isAfter(a_asOf))
                .filter(a_window -> a_window.effectiveUntil() == null || a_window.effectiveUntil().isAfter(a_asOf))
                .max(Comparator.comparing(Window::effectiveFrom).thenComparingInt(Window::versionNumber));
    }

    public static boolean applicable(String a_status) {
        return "ACTIVE".equals(a_status) || "SCHEDULED".equals(a_status) || "SUPERSEDED".equals(a_status);
    }

    public static List<FieldError> validate(ObjectNode a_overrides, ObjectNode a_effective) {
        List<FieldError> errors = new ArrayList<>();
        if (a_overrides != null) {
            Iterator<String> names = a_overrides.fieldNames();
            while (names.hasNext()) {
                String key = names.next();
                QualityPolicyCatalog.Setting setting = QualityPolicyCatalog.setting(key);
                if (setting == null) {
                    errors.add(new FieldError(key, "Unknown setting"));
                    continue;
                }
                checkType(setting, a_overrides.get(key), errors);
            }
        }
        for (QualityPolicyCatalog.Setting setting : QualityPolicyCatalog.settings()) {
            JsonNode value = a_effective.get(setting.key());
            if (value == null) {
                errors.add(new FieldError(setting.key(), "Missing value"));
                continue;
            }
            checkType(setting, value, errors);
            checkRange(setting, value, errors);
        }
        checkOrder(a_effective, "assessors.minPerAudit", "assessors.maxPerAudit", errors);
        checkOrder(a_effective, "assessors.minPerDepartment", "assessors.maxPerDepartmentDefault", errors);
        checkSpecial(a_effective, errors);
        return errors;
    }

    public static List<String> accreditationWarnings(ObjectNode a_effective) {
        List<String> warnings = new ArrayList<>();
        Set<String> frameworks = texts(a_effective.get("standards.frameworks"));
        int critical = a_effective.path("capa.deadline.critical").asInt(7);
        int coverage = a_effective.path("planning.requiredCoveragePercent").asInt(100);
        if (frameworks.contains("GAHAR") && critical > 7) {
            warnings.add("GAHAR floor used by this product: a critical CAPA deadline above 7 days is more lenient than the floor.");
        }
        if (frameworks.contains("GAHAR") && coverage < 100) {
            warnings.add("GAHAR floor used by this product: required coverage is below 100%.");
        }
        if (frameworks.contains("JCI") && critical > 45) {
            warnings.add("JCI floor used by this product: a critical CAPA deadline above 45 days is more lenient than the floor.");
        }
        return warnings;
    }

    private static void checkOrder(ObjectNode a_effective, String a_minKey, String a_maxKey, List<FieldError> a_errors) {
        JsonNode min = a_effective.get(a_minKey);
        JsonNode max = a_effective.get(a_maxKey);
        if (min != null && max != null && min.isNumber() && max.isNumber() && min.doubleValue() > max.doubleValue()) {
            a_errors.add(new FieldError(a_minKey, "Minimum cannot be greater than maximum"));
        }
    }

    private static void checkRange(QualityPolicyCatalog.Setting a_setting, JsonNode a_value, List<FieldError> a_errors) {
        if (!a_value.isNumber() || a_setting.min() == null) {
            return;
        }
        double value = a_value.doubleValue();
        if (value < a_setting.min() || (a_setting.max() != null && value > a_setting.max())) {
            a_errors.add(new FieldError(a_setting.key(), "Value is outside the allowed range"));
        }
        if ("int".equals(a_setting.type()) && a_value.isDouble() && a_value.doubleValue() != a_value.intValue()) {
            a_errors.add(new FieldError(a_setting.key(), "Use a whole number"));
        }
    }

    private static void checkType(QualityPolicyCatalog.Setting a_setting, JsonNode a_value, List<FieldError> a_errors) {
        switch (a_setting.type()) {
            case "int", "decimal" -> {
                if (!a_value.isNumber()) {
                    a_errors.add(new FieldError(a_setting.key(), "Use a number"));
                }
            }
            case "bool" -> {
                if (!a_value.isBoolean()) {
                    a_errors.add(new FieldError(a_setting.key(), "Use yes or no"));
                }
            }
            case "text" -> {
                if (!a_value.isTextual() || a_value.asText().length() > 200) {
                    a_errors.add(new FieldError(a_setting.key(), "Use text of at most 200 characters"));
                }
            }
            case "json" -> {
                if ("array".equals(a_setting.shape()) && !a_value.isArray()) {
                    a_errors.add(new FieldError(a_setting.key(), "Use a list"));
                } else if ("object".equals(a_setting.shape()) && !a_value.isObject()) {
                    a_errors.add(new FieldError(a_setting.key(), "Use an object"));
                }
            }
            default -> a_errors.add(new FieldError(a_setting.key(), "Unsupported type"));
        }
    }

    private static void checkSpecial(ObjectNode a_doc, List<FieldError> a_errors) {
        objectOfInts(a_doc, "assessors.maxPerDepartmentByName", 0, 20, a_errors);
        objectOfInts(a_doc, "planning.frequencyByDepartment", 1, 60, a_errors);
        objectOfInts(a_doc, "capa.deadlineBySeverity", 1, 365, a_errors);
        JsonNode risk = a_doc.get("planning.frequencyByRisk");
        if (risk != null && risk.isObject()) {
            for (String name : List.of("high", "medium", "low")) {
                JsonNode value = risk.get(name);
                if (value == null || !value.isIntegralNumber() || value.intValue() < 1 || value.intValue() > 60) {
                    a_errors.add(new FieldError("planning.frequencyByRisk", "high, medium, and low must be months from 1 to 60"));
                    break;
                }
            }
        }
        dates(a_doc, "planning.blackoutDates", a_errors);
        dates(a_doc, "notifications.holidays", a_errors);
        dayNames(a_doc.get("notifications.workingDays"), a_errors);
        codes(a_doc.get("standards.frameworks"), FRAMEWORKS, "standards.frameworks", a_errors);
        oneOf(a_doc, "sampling.mode", SAMPLE_MODES, a_errors);
        oneOf(a_doc, "scoring.scale", SCALES, a_errors);
        oneOf(a_doc, "notifications.language", LANGUAGES, a_errors);
        oneOf(a_doc, "notifications.calendar", CALENDARS, a_errors);
        levels(a_doc.get("findings.levels"), a_errors);
        aliases(a_doc, a_errors);
        reminders(a_doc.get("workflow.reminders"), a_errors);
        escalation(a_doc.get("workflow.escalation"), a_errors);
        channels(a_doc.get("notifications.channels"), a_errors);
        matrix(a_doc.get("incidents.riskMatrix"), a_errors);
        if ("fixed".equals(a_doc.path("sampling.mode").asText())
                && a_doc.path("sampling.minRecords").asInt(0) > a_doc.path("sampling.fixedSize").asInt(0)) {
            a_errors.add(new FieldError("sampling.minRecords", "Minimum records cannot exceed the fixed sample size"));
        }
    }

    private static void objectOfInts(ObjectNode a_doc, String a_key, int a_min, int a_max, List<FieldError> a_errors) {
        JsonNode node = a_doc.get(a_key);
        if (node == null || !node.isObject()) {
            return;
        }
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            JsonNode value = node.get(names.next());
            if (!value.isIntegralNumber() || value.intValue() < a_min || value.intValue() > a_max) {
                a_errors.add(new FieldError(a_key, "Each value must be a whole number from " + a_min + " to " + a_max));
                return;
            }
        }
    }

    private static void dates(ObjectNode a_doc, String a_key, List<FieldError> a_errors) {
        JsonNode node = a_doc.get(a_key);
        if (node == null || !node.isArray()) {
            return;
        }
        for (JsonNode item : node) {
            if (!item.isTextual() || !isoDate(item.asText())) {
                a_errors.add(new FieldError(a_key, "Use ISO dates such as 2026-01-01"));
                return;
            }
        }
    }

    private static boolean isoDate(String a_value) {
        try {
            LocalDate.parse(a_value);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static void dayNames(JsonNode a_node, List<FieldError> a_errors) {
        if (a_node == null || !a_node.isArray() || a_node.isEmpty()) {
            a_errors.add(new FieldError("notifications.workingDays", "Choose at least one working day"));
            return;
        }
        for (JsonNode item : a_node) {
            if (!item.isTextual() || !DAYS.contains(item.asText().toUpperCase(Locale.ROOT))) {
                a_errors.add(new FieldError("notifications.workingDays", "Use SUN, MON, TUE, WED, THU, FRI, or SAT"));
                return;
            }
        }
    }

    private static void codes(JsonNode a_node, Set<String> a_allowed, String a_key, List<FieldError> a_errors) {
        if (a_node == null || !a_node.isArray()) {
            return;
        }
        for (JsonNode item : a_node) {
            if (!item.isTextual() || !a_allowed.contains(item.asText())) {
                a_errors.add(new FieldError(a_key, "Unknown value"));
                return;
            }
        }
    }

    private static void oneOf(ObjectNode a_doc, String a_key, Set<String> a_allowed, List<FieldError> a_errors) {
        JsonNode node = a_doc.get(a_key);
        if (node == null || !node.isTextual() || !a_allowed.contains(node.asText())) {
            a_errors.add(new FieldError(a_key, "Choose one of the allowed values"));
        }
    }

    private static void levels(JsonNode a_node, List<FieldError> a_errors) {
        if (a_node == null || !a_node.isArray() || a_node.isEmpty()) {
            a_errors.add(new FieldError("findings.levels", "Add at least one severity"));
            return;
        }
        for (JsonNode level : a_node) {
            if (!level.isObject()
                    || !level.path("code").isTextual()
                    || level.path("code").asText().isBlank()
                    || !level.path("escalate").isBoolean()
                    || !level.path("evidenceRequired").isBoolean()) {
                a_errors.add(new FieldError("findings.levels", "Each severity needs a code, escalate, and evidenceRequired"));
                return;
            }
        }
    }

    private static void aliases(ObjectNode a_doc, List<FieldError> a_errors) {
        JsonNode aliases = a_doc.get("findings.legacyAliases");
        if (aliases == null || !aliases.isObject()) {
            return;
        }
        Set<String> codes = texts(a_doc.get("findings.levels"), "code");
        Iterator<String> names = aliases.fieldNames();
        while (names.hasNext()) {
            JsonNode target = aliases.get(names.next());
            if (!target.isTextual() || !codes.contains(target.asText())) {
                a_errors.add(new FieldError("findings.legacyAliases", "Each alias must point at a severity code"));
                return;
            }
        }
    }

    private static void reminders(JsonNode a_node, List<FieldError> a_errors) {
        if (a_node == null || !a_node.isArray()) {
            return;
        }
        for (JsonNode item : a_node) {
            if (!item.isIntegralNumber() || item.intValue() < 0 || item.intValue() > 90) {
                a_errors.add(new FieldError("workflow.reminders", "Reminder days must be from 0 to 90"));
                return;
            }
        }
    }

    private static void escalation(JsonNode a_node, List<FieldError> a_errors) {
        if (a_node == null || !a_node.isArray()) {
            return;
        }
        for (JsonNode step : a_node) {
            if (!step.isObject() || !step.path("afterDays").isIntegralNumber() || step.path("afterDays").intValue() < 0
                    || !step.path("role").isTextual() || step.path("role").asText().isBlank()) {
                a_errors.add(new FieldError("workflow.escalation", "Each step needs afterDays and role"));
                return;
            }
        }
    }

    private static void channels(JsonNode a_node, List<FieldError> a_errors) {
        if (a_node == null || !a_node.isObject()) {
            return;
        }
        for (String name : List.of("inApp", "email", "sms", "whatsapp")) {
            if (!a_node.path(name).isBoolean()) {
                a_errors.add(new FieldError("notifications.channels", "inApp, email, sms, and whatsapp must be yes or no"));
                return;
            }
        }
    }

    private static void matrix(JsonNode a_node, List<FieldError> a_errors) {
        if (a_node == null || !a_node.isObject()) {
            return;
        }
        for (String name : List.of("likelihood", "impact")) {
            int value = a_node.path(name).asInt(-1);
            if (value < 2 || value > 10) {
                a_errors.add(new FieldError("incidents.riskMatrix", "Likelihood and impact scales must be from 2 to 10"));
                return;
            }
        }
        int review = a_node.path("reviewMonths").asInt(-1);
        if (review < 1 || review > 60) {
            a_errors.add(new FieldError("incidents.riskMatrix", "Review frequency must be from 1 to 60 months"));
        }
    }

    private static Set<String> texts(JsonNode a_node) {
        return texts(a_node, null);
    }

    private static Set<String> texts(JsonNode a_node, String a_field) {
        java.util.HashSet<String> values = new java.util.HashSet<>();
        if (a_node == null || !a_node.isArray()) {
            return values;
        }
        for (JsonNode item : a_node) {
            if (a_field == null && item.isTextual()) {
                values.add(item.asText());
            } else if (a_field != null && item.path(a_field).isTextual()) {
                values.add(item.path(a_field).asText());
            }
        }
        return values;
    }
}
