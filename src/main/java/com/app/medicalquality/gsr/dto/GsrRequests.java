package com.app.medicalquality.gsr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public final class GsrRequests {

    private GsrRequests() {}

    @Getter
    @Setter
    public static class Create {
        @NotBlank
        private String departmentName;

        @NotBlank
        private String auditorName;

        @NotNull
        private LocalDate auditDate;

        private Long departmentId;
    }

    @Getter
    @Setter
    public static class Score {
        @NotNull
        private Long elementId;

        private String value;
    }

    @Getter
    @Setter
    public static class Comment {
        @NotNull
        private Long elementId;

        private String text;
    }

    @Getter
    @Setter
    public static class BulkScore {
        @NotNull
        private Long standardId;

        private String value;
    }

    @Getter
    @Setter
    public static class Update {
        private String departmentName;
        private String auditorName;
        private LocalDate auditDate;
        private String status;
        private Long departmentId;
        private String reason;
    }

    @Getter
    @Setter
    public static class StatusChange {
        @NotBlank
        private String status;
    }

    @Getter
    @Setter
    public static class Delete {
        private String confirm;
        private boolean overrideCapa;
        private String reason;
    }

    @Getter
    @Setter
    public static class Bulk {
        @NotNull
        private List<Long> ids;

        @NotBlank
        private String operation;

        private String confirm;
        private boolean overrideCapa;
    }
}
