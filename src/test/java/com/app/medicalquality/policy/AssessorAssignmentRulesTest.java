package com.app.medicalquality.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.app.medicalquality.exception.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AssessorAssignmentRulesTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void blocksAThirdAssessorWhenRadiologyAllowsTwo() {
        ObjectNode document = QualityPolicyCatalog.defaults(mapper);
        document.put("assessors.maxPerAudit", 5);
        document.set("assessors.maxPerDepartmentByName", mapper.createObjectNode().put("Radiology", 2));
        ResolvedPolicy policy = new ResolvedPolicy(document);
        AppException error = assertThrows(AppException.class, () -> AssessorAssignmentRules.check(policy, facts(2)));
        assertEquals("error.policy.assessor.max", error.getMessageKey());
        assertEquals("Radiology", error.getArgs()[0]);
        assertEquals(2, error.getArgs()[1]);
    }

    @Test
    void allowsTheSecondAssessor() {
        ObjectNode document = QualityPolicyCatalog.defaults(mapper);
        document.set("assessors.maxPerDepartmentByName", mapper.createObjectNode().put("Radiology", 2));
        AssessorAssignmentRules.check(new ResolvedPolicy(document), facts(1));
    }

    private AssessorAssignmentRules.Facts facts(int a_teamSize) {
        return new AssessorAssignmentRules.Facts(
                "Radiology", "الأشعة", "RAD", a_teamSize, false, false, true, 0, 0, null, false, true);
    }

    @Test
    void majorDeadlineFollowsThePolicyGivenToTheFinding() {
        LocalDate opened = LocalDate.of(2026, 2, 1);
        assertEquals(LocalDate.of(2026, 3, 3), CapaSchedule.dueDate(policy(30), "major", opened, null));
        LocalDate later = LocalDate.of(2026, 6, 2);
        assertEquals(LocalDate.of(2026, 6, 9), CapaSchedule.dueDate(policy(7), "major", later, null));
        AppException error = assertThrows(
                AppException.class,
                () -> CapaSchedule.dueDate(policy(7), "major", later, LocalDate.of(2026, 7, 1)));
        assertEquals("error.policy.capa.deadline", error.getMessageKey());
    }

    private ResolvedPolicy policy(int a_majorDays) {
        ObjectNode document = QualityPolicyCatalog.defaults(mapper);
        document.put("capa.deadline.major", a_majorDays);
        return new ResolvedPolicy(document);
    }
}
