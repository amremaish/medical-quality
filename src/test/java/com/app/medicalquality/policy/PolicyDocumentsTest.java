package com.app.medicalquality.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PolicyDocumentsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void rejectsAMinimumAboveItsMaximum() {
        ObjectNode effective = QualityPolicyCatalog.defaults(mapper);
        effective.put("assessors.minPerAudit", 5);
        effective.put("assessors.maxPerAudit", 2);
        List<PolicyDocuments.FieldError> errors = PolicyDocuments.validate(effective, effective);
        assertTrue(errors.stream().anyMatch(a_error -> a_error.key().equals("assessors.minPerAudit")));
    }

    @Test
    void rejectsANegativeDeadline() {
        ObjectNode effective = QualityPolicyCatalog.defaults(mapper);
        effective.put("capa.deadline.major", -1);
        List<PolicyDocuments.FieldError> errors = PolicyDocuments.validate(effective, effective);
        assertTrue(errors.stream().anyMatch(a_error -> a_error.key().equals("capa.deadline.major")));
    }

    @Test
    void warnsWhenAGaharCriticalDeadlineIsMoreLenientThanTheFloor() {
        ObjectNode effective = QualityPolicyCatalog.defaults(mapper);
        effective.set("standards.frameworks", mapper.createArrayNode().add("GAHAR"));
        effective.put("capa.deadline.critical", 14);
        assertFalse(PolicyDocuments.accreditationWarnings(effective).isEmpty());
    }

    @Test
    void selectsTheVersionInForceOnThatDate() {
        PolicyDocuments.Window first = new PolicyDocuments.Window(
                1, 1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1), "SUPERSEDED", "{}");
        PolicyDocuments.Window second = new PolicyDocuments.Window(
                2, 2, LocalDate.of(2026, 6, 1), null, "ACTIVE", "{}");
        Optional<PolicyDocuments.Window> may = PolicyDocuments.select(List.of(first, second), LocalDate.of(2026, 5, 1));
        Optional<PolicyDocuments.Window> june = PolicyDocuments.select(List.of(first, second), LocalDate.of(2026, 6, 1));
        assertEquals(1, may.orElseThrow().id());
        assertEquals(2, june.orElseThrow().id());
    }

    @Test
    void recordsTheOldAndNewValue() {
        ObjectNode before = QualityPolicyCatalog.defaults(mapper);
        ObjectNode after = before.deepCopy();
        after.put("capa.deadline.major", 7);
        List<PolicyDocuments.Change> changes = PolicyDocuments.diff(before, after);
        assertEquals(1, changes.size());
        assertEquals("capa.deadline.major", changes.get(0).key());
        assertEquals(30, changes.get(0).oldValue().intValue());
        assertEquals(7, changes.get(0).newValue().intValue());
    }

    @Test
    void departmentCapOverridesTheDefault() {
        ObjectNode document = QualityPolicyCatalog.defaults(mapper);
        document.set("assessors.maxPerDepartmentByName", mapper.createObjectNode().put("Radiology", 2));
        ResolvedPolicy policy = new ResolvedPolicy(document);
        assertEquals(2, policy.maxAssessorsForDepartment("Radiology", "الأشعة", "RAD"));
        assertTrue(policy.hasDepartmentCap("Radiology", null, null));
        assertEquals(5, policy.maxAssessorsForDepartment("Pharmacy", "الصيدلية", "PHARM"));
    }
}
