package com.app.medicalquality.score;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class RecordScoreCalculatorTest {

    @Test
    void fullCreditRequiresAllThreeChecksAndSkipsNa() {
        RecordScoreCalculator.RecordStats one = RecordScoreCalculator.summarize(
                List.of(new RecordScoreCalculator.Mark(true, true, true, false)));
        assertEquals(100, one.overall());
        assertEquals(1, one.compliant());

        RecordScoreCalculator.RecordStats partial = RecordScoreCalculator.summarize(
                List.of(new RecordScoreCalculator.Mark(true, false, false, false)));
        assertEquals(0, partial.overall());
        assertEquals(100, partial.percentLegible());

        RecordScoreCalculator.RecordStats excluded = RecordScoreCalculator.summarize(List.of(
                new RecordScoreCalculator.Mark(false, false, false, true),
                new RecordScoreCalculator.Mark(true, true, true, false)));
        assertEquals(100, excluded.overall());
        assertEquals(1, excluded.applicable());
    }

    @Test
    void toneMatchesCheckedCount() {
        assertEquals("na", RecordScoreCalculator.tone(true, true, true, true));
        assertEquals("compliant", RecordScoreCalculator.tone(true, true, true, false));
        assertEquals("partial", RecordScoreCalculator.tone(true, false, false, false));
        assertEquals("non-compliant", RecordScoreCalculator.tone(false, false, false, false));
    }
}
