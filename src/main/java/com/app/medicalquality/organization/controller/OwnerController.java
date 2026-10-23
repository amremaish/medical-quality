package com.app.medicalquality.organization.controller;

import com.app.medicalquality.common.AppResponse;
import com.app.medicalquality.organization.dto.OrganizationRequests;
import com.app.medicalquality.organization.dto.OrganizationViews;
import com.app.medicalquality.organization.service.OrganizationProvisioningService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/owners")
public class OwnerController {

    private final OrganizationProvisioningService provisioningService;

    public OwnerController(OrganizationProvisioningService a_provisioningService) {
        this.provisioningService = a_provisioningService;
    }

    @GetMapping
    public ResponseEntity<AppResponse<List<OrganizationViews.Owner>>> list(Authentication a_authentication) {
        return AppResponse.success(provisioningService.owners(a_authentication));
    }

    @PostMapping
    public ResponseEntity<AppResponse<OrganizationViews.Invitation>> invite(
            @Valid @RequestBody OrganizationRequests.Invite a_request, Authentication a_authentication) {
        return AppResponse.success(
                provisioningService.invite(a_request, a_authentication), "Successfully Done", HttpStatus.CREATED);
    }
}
