package com.app.medicalquality.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class ResolvedPolicy {

    private final ObjectNode document;

    public ResolvedPolicy(ObjectNode a_document) {
        this.document = a_document;
    }

    public ObjectNode document() {
        return document;
    }

    public int integer(String a_key) {
        return document.path(a_key).asInt();
    }

    public boolean bool(String a_key) {
        return document.path(a_key).asBoolean();
    }

    public double decimal(String a_key) {
        return document.path(a_key).asDouble();
    }

    public String text(String a_key) {
        return document.path(a_key).asText("");
    }

    public JsonNode json(String a_key) {
        return document.path(a_key);
    }

    public int maxAssessorsForDepartment(String a_nameEn, String a_nameAr, String a_code) {
        Integer specific = departmentCap(a_nameEn, a_nameAr, a_code);
        return specific == null ? integer("assessors.maxPerDepartmentDefault") : specific;
    }

    public boolean hasDepartmentCap(String a_nameEn, String a_nameAr, String a_code) {
        return departmentCap(a_nameEn, a_nameAr, a_code) != null;
    }

    public int deadlineDays(String a_severity) {
        JsonNode custom = json("capa.deadlineBySeverity");
        if (custom != null && custom.isObject() && custom.has(a_severity)) {
            return custom.get(a_severity).asInt();
        }
        return switch (a_severity) {
            case "critical" -> integer("capa.deadline.critical");
            case "major" -> integer("capa.deadline.major");
            case "minor" -> integer("capa.deadline.minor");
            default -> integer("capa.deadline.observation");
        };
    }

    public boolean escalates(String a_severity) {
        return levelFlag(a_severity, "escalate");
    }

    public boolean evidenceRequired(String a_severity) {
        return levelFlag(a_severity, "evidenceRequired");
    }

    public boolean requiresRootCause(String a_severity) {
        return texts("capa.rootCauseSeverities").contains(a_severity);
    }

    public boolean roleAllowed(String a_key, String a_roleCode) {
        JsonNode node = json(a_key);
        if (node == null || !node.isArray() || node.isEmpty()) {
            return true;
        }
        if (a_roleCode == null) {
            return false;
        }
        for (JsonNode item : node) {
            if (item.isTextual() && item.asText().equals(a_roleCode)) {
                return true;
            }
        }
        return false;
    }

    public Set<DayOfWeek> workingDays() {
        EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        JsonNode node = json("notifications.workingDays");
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual()) {
                    days.add(day(item.asText()));
                }
            }
        }
        if (days.isEmpty()) {
            days.add(DayOfWeek.SUNDAY);
            days.add(DayOfWeek.MONDAY);
            days.add(DayOfWeek.TUESDAY);
            days.add(DayOfWeek.WEDNESDAY);
            days.add(DayOfWeek.THURSDAY);
        }
        return days;
    }

    public Set<LocalDate> holidays() {
        Set<LocalDate> dates = new HashSet<>();
        JsonNode node = json("notifications.holidays");
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual()) {
                    dates.add(LocalDate.parse(item.asText()));
                }
            }
        }
        return dates;
    }

    public Set<String> blackoutDates() {
        return texts("planning.blackoutDates");
    }

    private boolean levelFlag(String a_severity, String a_field) {
        JsonNode levels = json("findings.levels");
        if (levels == null || !levels.isArray()) {
            return false;
        }
        for (JsonNode level : levels) {
            if (a_severity.equals(level.path("code").asText())) {
                return level.path(a_field).asBoolean(false);
            }
        }
        return false;
    }

    private Integer departmentCap(String a_nameEn, String a_nameAr, String a_code) {
        JsonNode map = json("assessors.maxPerDepartmentByName");
        if (map == null || !map.isObject()) {
            return null;
        }
        Integer found = match(map, a_nameEn);
        if (found == null) {
            found = match(map, a_nameAr);
        }
        if (found == null) {
            found = match(map, a_code);
        }
        return found;
    }

    private Integer match(JsonNode a_map, String a_name) {
        if (a_name == null || a_name.isBlank()) {
            return null;
        }
        var names = a_map.fieldNames();
        while (names.hasNext()) {
            String key = names.next();
            if (key.equalsIgnoreCase(a_name.trim()) && a_map.get(key).isNumber()) {
                return a_map.get(key).asInt();
            }
        }
        return null;
    }

    private Set<String> texts(String a_key) {
        Set<String> values = new HashSet<>();
        JsonNode node = json(a_key);
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual()) {
                    values.add(item.asText());
                }
            }
        }
        return values;
    }

    private static DayOfWeek day(String a_name) {
        return switch (a_name.toUpperCase(Locale.ROOT)) {
            case "SUN" -> DayOfWeek.SUNDAY;
            case "MON" -> DayOfWeek.MONDAY;
            case "TUE" -> DayOfWeek.TUESDAY;
            case "WED" -> DayOfWeek.WEDNESDAY;
            case "THU" -> DayOfWeek.THURSDAY;
            case "FRI" -> DayOfWeek.FRIDAY;
            default -> DayOfWeek.SATURDAY;
        };
    }
}
