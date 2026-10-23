package com.app.medicalquality.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

public final class MrRequests {

    private MrRequests() {}

    @Getter
    @Setter
    public static class Create {
        @NotBlank
        private String hospitalName;

        @NotBlank
        private String reviewerName;

        @NotNull
        private LocalDate reviewDate;

        @NotBlank
        private String departmentName;
    }

    @Getter
    @Setter
    public static class Answer {
        @NotBlank
        private String recordCode;

        @NotNull
        private Long itemId;

        private boolean legible;
        private boolean accurate;
        private boolean complete;
        private boolean na;
        private String comment;
    }

    @Getter
    @Setter
    public static class RecordId {
        private String patientIdentifier;
    }
}
