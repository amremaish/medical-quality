package com.app.medicalquality.role.dto;

import com.app.medicalquality.role.PermissionEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoleRequest {

    @NotBlank
    private String name;

    @NotEmpty
    private List<PermissionEnum> permissions;
}
