package com.app.medicalquality.organization.controller;

import com.app.medicalquality.common.AppResponse;
import com.app.medicalquality.organization.FacilityType;
import com.app.medicalquality.organization.dto.OrganizationRequests;
import com.app.medicalquality.organization.dto.OrganizationViews;
import com.app.medicalquality.organization.service.OrganizationProvisioningService;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationProvisioningService provisioningService;

    public OrganizationController(OrganizationProvisioningService a_provisioningService) {
        this.provisioningService = a_provisioningService;
    }

    @GetMapping
    public ResponseEntity<AppResponse<List<OrganizationViews.Summary>>> list(Authentication a_authentication) {
        return AppResponse.success(provisioningService.list(a_authentication));
    }

    @GetMapping("/types")
    public ResponseEntity<AppResponse<List<String>>> types(Authentication a_authentication) {
        provisioningService.authorize(a_authentication);
        return AppResponse.success(Arrays.stream(FacilityType.values()).map(Enum::name).toList());
    }

    @PostMapping
    public ResponseEntity<AppResponse<OrganizationViews.Invitation>> create(
            @Valid @RequestBody OrganizationRequests.Create a_request, Authentication a_authentication) {
        return AppResponse.success(
                provisioningService.provision(a_request, a_authentication), "Successfully Done", HttpStatus.CREATED);
    }
}
