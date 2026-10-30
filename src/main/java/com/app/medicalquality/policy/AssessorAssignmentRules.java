package com.app.medicalquality.policy;

import com.app.medicalquality.exception.AppException;
import java.time.LocalDate;

public final class AssessorAssignmentRules {

    public record Facts(
            String departmentNameEn,
            String departmentNameAr,
            String departmentCode,
            int teamSize,
            boolean alreadyOnTeam,
            boolean homeAssessor,
            boolean coiDeclared,
            int concurrentOpenAudits,
            int consecutiveSameDepartment,
            Integer daysSinceLastAudit,
            boolean external,
            boolean sameHospital) {}

    private AssessorAssignmentRules() {}

    public static void check(ResolvedPolicy a_policy, Facts a_facts) {
        if (a_facts.alreadyOnTeam()) {
            throw new AppException("error.policy.assessor.duplicate");
        }
        if (!a_facts.sameHospital() && !a_policy.bool("assessors.allowCrossHospital")) {
            throw new AppException("error.policy.assessor.cross_hospital");
        }
        if (a_facts.external() && !a_policy.bool("assessors.allowExternal")) {
            throw new AppException("error.policy.assessor.external");
        }
        if (a_facts.homeAssessor() && !a_policy.bool("assessors.allowOwnDepartment")) {
            throw new AppException("error.policy.assessor.own_department");
        }
        if (a_policy.bool("assessors.coiDeclarationRequired") && !a_facts.coiDeclared()) {
            throw new AppException("error.policy.assessor.declaration");
        }
        int byDepartment = a_policy.maxAssessorsForDepartment(
                a_facts.departmentNameEn(), a_facts.departmentNameAr(), a_facts.departmentCode());
        int byAudit = a_policy.integer("assessors.maxPerAudit");
        int cap = Math.min(byDepartment, byAudit);
        if (a_facts.teamSize() >= cap) {
            if (byDepartment <= byAudit && a_policy.hasDepartmentCap(
                    a_facts.departmentNameEn(), a_facts.departmentNameAr(), a_facts.departmentCode())) {
                throw new AppException("error.policy.assessor.max", new Object[] {
                        label(a_facts), byDepartment
                });
            }
            throw new AppException("error.policy.assessor.max_audit", new Object[] {byAudit});
        }
        if (a_facts.concurrentOpenAudits() >= a_policy.integer("assessors.maxConcurrent")) {
            throw new AppException("error.policy.assessor.concurrent", new Object[] {
                    a_policy.integer("assessors.maxConcurrent")
            });
        }
        int cooling = a_policy.integer("assessors.coolingOffDays");
        if (cooling > 0 && a_facts.daysSinceLastAudit() != null && a_facts.daysSinceLastAudit() < cooling) {
            throw new AppException("error.policy.assessor.cooling", new Object[] {cooling});
        }
        int consecutive = a_policy.integer("assessors.maxConsecutiveDepartment");
        if (a_facts.consecutiveSameDepartment() >= consecutive) {
            throw new AppException("error.policy.assessor.rotation", new Object[] {consecutive});
        }
    }

    private static String label(Facts a_facts) {
        String name = a_facts.departmentNameEn();
        if (name == null || name.isBlank()) {
            name = a_facts.departmentNameAr();
        }
        if (name == null || name.isBlank()) {
            name = a_facts.departmentCode();
        }
        return name == null ? "This department" : name.replace("'", "''");
    }

    public static boolean blackout(ResolvedPolicy a_policy, LocalDate a_date) {
        return a_date != null && a_policy.blackoutDates().contains(a_date.toString());
    }
}
