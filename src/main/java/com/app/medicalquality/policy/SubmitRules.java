package com.app.medicalquality.policy;

import com.app.medicalquality.exception.AppException;
import java.util.List;

public final class SubmitRules {

    public record FindingFact(String severity, boolean evidenceRequired, long evidenceCount, String standardCode, String score) {}

    public record Facts(
            int compliancePercent,
            int teamSize,
            boolean hasLead,
            List<FindingFact> findings) {}

    private SubmitRules() {}

    public static void check(ResolvedPolicy a_policy, Facts a_facts) {
        if (a_policy.bool("assessors.enforceTeamOnSubmit")) {
            int min = a_policy.integer("assessors.minPerAudit");
            if (a_facts.teamSize() < min) {
                throw new AppException("error.policy.assessor.min_team", new Object[] {min});
            }
            if (a_policy.bool("assessors.leadRequired") && !a_facts.hasLead()) {
                throw new AppException("error.policy.assessor.lead");
            }
        }
        if (a_policy.bool("scoring.enforcePassThreshold")
                && a_facts.compliancePercent() < a_policy.integer("scoring.minPassPercent")) {
            throw new AppException("error.policy.score.pass", new Object[] {a_policy.integer("scoring.minPassPercent")});
        }
        if (a_policy.bool("scoring.enforceCapaThreshold")
                && a_facts.compliancePercent() < a_policy.integer("scoring.capaTriggerPercent")
                && (a_facts.findings() == null || a_facts.findings().isEmpty())) {
            throw new AppException("error.policy.score.capa", new Object[] {a_policy.integer("scoring.capaTriggerPercent")});
        }
        var mustPass = a_policy.json("scoring.mustPassCodes");
        if (a_facts.findings() != null) {
            for (FindingFact finding : a_facts.findings()) {
                if (finding.evidenceRequired() && finding.evidenceCount() < 1) {
                    throw new AppException("error.policy.finding.evidence", new Object[] {finding.severity()});
                }
            }
        }
        if (mustPass != null && mustPass.isArray() && a_facts.findings() != null) {
            for (var code : mustPass) {
                if (!code.isTextual()) {
                    continue;
                }
                for (FindingFact finding : a_facts.findings()) {
                    if (code.asText().equals(finding.standardCode()) && !"met".equals(finding.score())) {
                        throw new AppException("error.policy.score.must_pass", new Object[] {code.asText()});
                    }
                }
            }
        }
    }

    public static void checkNotApplicable(ResolvedPolicy a_policy, String a_roleCode, boolean a_superAdmin) {
        if (!a_policy.bool("scoring.naRequiresApproval") || a_superAdmin) {
            return;
        }
        if (!a_policy.roleAllowed("scoring.naApproverRoleCodes", a_roleCode)) {
            throw new AppException("error.policy.score.na");
        }
    }
}
