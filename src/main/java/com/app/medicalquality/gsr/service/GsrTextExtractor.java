package com.app.medicalquality.gsr.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GsrTextExtractor {

    private static final Pattern CODE = Pattern.compile("GSR\\.\\d{2}", Pattern.CASE_INSENSITIVE);
    private static final Pattern STATUS = Pattern.compile(
            "partially\\s+met|not\\s+met|\\bmet\\b|n/?a|not\\s+applicable", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEPARTMENT = Pattern.compile("(?i)department\\s*[:\\-]\\s*([^\\n]{2,80})");
    private static final Pattern AUDITOR = Pattern.compile("(?i)auditor(?:'s)?\\s*name\\s*[:\\-]\\s*([^\\n]{2,80})");
    private static final Pattern DATE = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})|(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})");

    private GsrTextExtractor() {}

    public static Header header(String a_text) {
        return new Header(group(DEPARTMENT, a_text), group(AUDITOR, a_text), date(a_text));
    }

    public static List<Hit> standards(String a_text, List<String> a_codes) {
        List<Hit> hits = new ArrayList<>();
        for (String code : a_codes) {
            hits.add(new Hit(code, statusNear(a_text, code)));
        }
        return hits;
    }

    private static String statusNear(String a_text, String a_code) {
        Matcher matcher = CODE.matcher(a_text);
        while (matcher.find()) {
            if (!matcher.group().equalsIgnoreCase(a_code)) {
                continue;
            }
            int end = Math.min(a_text.length(), matcher.end() + 280);
            Matcher status = STATUS.matcher(a_text.substring(matcher.end(), end));
            if (status.find()) {
                return normalize(status.group());
            }
        }
        return null;
    }

    private static String normalize(String a_raw) {
        String value = a_raw.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (value.contains("partial")) {
            return "partial";
        }
        if (value.contains("not met") || value.contains("not applicable") || value.contains("n/a") || value.equals("na")) {
            return value.contains("met") ? "not" : "na";
        }
        if (value.equals("met")) {
            return "met";
        }
        return null;
    }

    private static String group(Pattern a_pattern, String a_text) {
        Matcher matcher = a_pattern.matcher(a_text == null ? "" : a_text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private static String date(String a_text) {
        Matcher matcher = DATE.matcher(a_text == null ? "" : a_text);
        if (!matcher.find()) {
            return "";
        }
        if (matcher.group(1) != null) {
            return matcher.group(1);
        }
        String[] parts = matcher.group(2).split("[/-]");
        return parts[2] + "-" + pad(parts[1]) + "-" + pad(parts[0]);
    }

    private static String pad(String a_part) {
        return a_part.length() == 1 ? "0" + a_part : a_part;
    }

    public record Header(String departmentName, String auditorName, String auditDate) {}

    public record Hit(String code, String status) {}
}
