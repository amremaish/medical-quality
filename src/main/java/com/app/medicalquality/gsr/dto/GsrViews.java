package com.app.medicalquality.gsr.dto;

import com.app.medicalquality.score.ComplianceCalculator;
import java.time.LocalDate;
import java.util.List;

public final class GsrViews {

    private GsrViews() {}

    public record AuditSummary(
            Long id,
            String departmentName,
            Long departmentId,
            String auditorName,
            LocalDate auditDate,
            String status,
            boolean archived,
            int percent) {}

    public record ElementLine(Long id, int sortOrder, String text, String score, String comment) {}

    public record StandardLine(
            Long id,
            String code,
            String title,
            String handbookRef,
            String intent,
            String tone,
            List<ElementLine> elements) {}

    public record ChapterLine(Long id, String name, List<StandardLine> standards) {}

    public record Checklist(
            AuditSummary audit, ComplianceCalculator.ComplianceView compliance, List<ChapterLine> chapters) {}
}
