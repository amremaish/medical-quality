package com.app.medicalquality.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePermissionOverridesRequest {

    @NotNull
    @Valid
    private List<Change> changes;

    @Getter
    @Setter
    public static class Change {

        @NotNull
        private String permission;

        @NotNull
        @Pattern(regexp = "INHERIT|ALLOW|DENY", message = "Override must be inherit, allow, or deny")
        private String effect;
    }
}
