package com.app.medicalquality.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MeResponse {

    private final Long id;
    private final String username;
    private final String roleName;
    private final boolean superAdmin;
}
