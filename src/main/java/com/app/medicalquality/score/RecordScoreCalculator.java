package com.app.medicalquality.score;

import java.util.List;

public final class RecordScoreCalculator {

    private RecordScoreCalculator() {}

    public static String tone(boolean a_legible, boolean a_accurate, boolean a_complete, boolean a_na) {
        if (a_na) {
            return "na";
        }
        int checked = (a_legible ? 1 : 0) + (a_accurate ? 1 : 0) + (a_complete ? 1 : 0);
        if (checked == 3) {
            return "compliant";
        }
        if (checked > 0) {
            return "partial";
        }
        return "non-compliant";
    }

    public static RecordStats summarize(List<Mark> a_marks) {
        int applicable = 0;
        int compliant = 0;
        int legible = 0;
        int accurate = 0;
        int complete = 0;
        for (Mark mark : a_marks) {
            if (mark.na()) {
                continue;
            }
            applicable++;
            legible += mark.legible() ? 1 : 0;
            accurate += mark.accurate() ? 1 : 0;
            complete += mark.complete() ? 1 : 0;
            if (mark.legible() && mark.accurate() && mark.complete()) {
                compliant++;
            }
        }
        return new RecordStats(
                applicable,
                compliant,
                percent(legible, applicable),
                percent(accurate, applicable),
                percent(complete, applicable),
                percent(compliant, applicable));
    }

    private static int percent(int a_part, int a_whole) {
        return a_whole == 0 ? 0 : Math.round(a_part * 100f / a_whole);
    }

    public record Mark(boolean legible, boolean accurate, boolean complete, boolean na) {}

    public record RecordStats(
            int applicable,
            int compliant,
            int percentLegible,
            int percentAccurate,
            int percentComplete,
            int overall) {}
}
