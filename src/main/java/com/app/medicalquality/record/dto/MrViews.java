package com.app.medicalquality.record.dto;

import com.app.medicalquality.score.RecordScoreCalculator;
import java.time.LocalDate;
import java.util.List;

public final class MrViews {

    private MrViews() {}

    public record ReviewSummary(
            Long id, String hospitalName, String reviewerName, LocalDate reviewDate, String departmentName) {}

    public record ItemLine(Long id, String section, String standardCode, String eocNo, String text) {}

    public record AnswerLine(
            Long itemId, boolean legible, boolean accurate, boolean complete, boolean na, String comment, String tone) {}

    public record RecordLine(
            String code,
            String patientIdentifier,
            RecordScoreCalculator.RecordStats stats,
            List<AnswerLine> answers) {}

    public record ReviewDetail(
            Long id,
            String hospitalName,
            String reviewerName,
            LocalDate reviewDate,
            String departmentName,
            List<ItemLine> items,
            List<RecordLine> records) {}
}
