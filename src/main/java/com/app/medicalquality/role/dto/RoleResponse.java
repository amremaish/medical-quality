package com.app.medicalquality.role.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RoleResponse {

    private final Long id;
    private final String name;
    private final List<String> permissions;
}
