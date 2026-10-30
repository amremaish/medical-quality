package com.app.medicalquality.policy.controller;

import com.app.medicalquality.auth.security.annotation.HasPermission;
import com.app.medicalquality.common.AppResponse;
import com.app.medicalquality.policy.service.AuditTeamService;
import com.app.medicalquality.role.PermissionEnum;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditTeamController {

    private final AuditTeamService auditTeamService;

    public AuditTeamController(AuditTeamService a_auditTeamService) {
        this.auditTeamService = a_auditTeamService;
    }

    @GetMapping("/api/audits/{auditId}/assessors")
    @HasPermission(PermissionEnum.CONDUCT_AUDIT)
    public ResponseEntity<AppResponse<List<AuditTeamService.Member>>> list(
            @PathVariable("auditId") Long a_auditId, Authentication a_authentication) {
        return AppResponse.success(auditTeamService.list(a_auditId, a_authentication));
    }

    @PostMapping("/api/audits/{auditId}/assessors")
    @HasPermission(PermissionEnum.CONDUCT_AUDIT)
    public ResponseEntity<AppResponse<List<AuditTeamService.Member>>> assign(
            @PathVariable("auditId") Long a_auditId,
            @RequestBody AuditTeamService.AssignRequest a_request,
            Authentication a_authentication) {
        return AppResponse.success(auditTeamService.assign(a_auditId, a_request, a_authentication));
    }

    @DeleteMapping("/api/audits/{auditId}/assessors/{assessorId}")
    @HasPermission(PermissionEnum.CONDUCT_AUDIT)
    public ResponseEntity<AppResponse<List<AuditTeamService.Member>>> unassign(
            @PathVariable("auditId") Long a_auditId,
            @PathVariable("assessorId") Long a_assessorId,
            Authentication a_authentication) {
        return AppResponse.success(auditTeamService.unassign(a_auditId, a_assessorId, a_authentication));
    }
}
