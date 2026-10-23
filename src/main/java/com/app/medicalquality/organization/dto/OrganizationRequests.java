package com.app.medicalquality.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public final class OrganizationRequests {

    private OrganizationRequests() {}

    @Getter
    @Setter
    public static class Create {

        @NotBlank
        @Size(max = 255)
        private String name;

        @NotBlank
        @Size(max = 40)
        private String orgType;

        @NotBlank
        @Size(max = 128)
        private String ownerFirstName;

        @Size(max = 128)
        private String ownerLastName;

        @NotBlank
        @Email
        @Size(max = 320)
        private String ownerEmail;
    }

    @Getter
    @Setter
    public static class Invite {

        @NotNull
        private Long organizationId;

        @NotBlank
        @Size(max = 128)
        private String ownerFirstName;

        @Size(max = 128)
        private String ownerLastName;

        @NotBlank
        @Email
        @Size(max = 320)
        private String ownerEmail;
    }
}
