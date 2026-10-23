package com.app.medicalquality.score;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ComplianceCalculatorTest {

    @Test
    void halfCreditExcludesNaAndKeepsUnscoredInTheDenominator() {
        assertEquals(100, ComplianceCalculator.fromCounts(1, 0, 0, 0, 1).percent());
        assertEquals(50, ComplianceCalculator.fromCounts(0, 1, 0, 0, 1).percent());
        assertEquals(0, ComplianceCalculator.fromCounts(0, 0, 1, 0, 1).percent());
        assertEquals(0, ComplianceCalculator.fromCounts(0, 0, 0, 1, 1).percent());
        assertEquals(100, ComplianceCalculator.fromCounts(1, 0, 0, 1, 2).percent());
        assertEquals(50, ComplianceCalculator.fromCounts(1, 0, 0, 0, 2).percent());
        assertEquals(50, ComplianceCalculator.fromCounts(1, 1, 1, 1, 4).percent());
    }

    @Test
    void standardToneFollowsNotThenPartialThenMet() {
        assertEquals("not", ComplianceCalculator.standardTone(1, 1, 1, 0, 3));
        assertEquals("partial", ComplianceCalculator.standardTone(0, 1, 0, 0, 2));
        assertEquals("met", ComplianceCalculator.standardTone(1, 0, 0, 1, 2));
        assertEquals("unscored", ComplianceCalculator.standardTone(0, 0, 0, 0, 3));
        assertEquals("mixed", ComplianceCalculator.standardTone(1, 0, 0, 0, 2));
    }

    @Test
    void valuesUseTheSameFormula() {
        ComplianceCalculator.ComplianceView view = ComplianceCalculator.fromValues(3, List.of("met", "partial"));
        assertEquals(50, view.percent());
        assertEquals(1, view.unscored());
    }
}
