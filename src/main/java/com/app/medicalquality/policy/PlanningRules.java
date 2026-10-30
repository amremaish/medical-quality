package com.app.medicalquality.policy;

import com.app.medicalquality.exception.AppException;
import java.time.LocalDate;

public final class PlanningRules {

    private PlanningRules() {}

    public static void check(ResolvedPolicy a_policy, LocalDate a_date, long a_auditsThisMonth, LocalDate a_previousAudit) {
        if (a_date != null && a_policy.blackoutDates().contains(a_date.toString())) {
            throw new AppException("error.policy.planning.blackout", new Object[] {a_date.toString()});
        }
        int cap = a_policy.integer("planning.maxAuditsPerMonth");
        if (a_auditsThisMonth >= cap) {
            throw new AppException("error.policy.planning.month", new Object[] {cap});
        }
        if (a_policy.bool("planning.enforceFrequency") && a_previousAudit != null && a_date != null) {
            int months = a_policy.integer("planning.frequencyMonthsDefault");
            LocalDate earliest = a_previousAudit.plusMonths(months);
            if (a_date.isBefore(earliest)) {
                throw new AppException("error.policy.planning.frequency", new Object[] {months, earliest.toString()});
            }
        }
    }
}
