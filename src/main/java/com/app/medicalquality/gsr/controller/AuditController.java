package com.app.medicalquality.gsr.controller;

import com.app.medicalquality.auth.security.annotation.HasPermission;
import com.app.medicalquality.common.AppResponse;
import com.app.medicalquality.gsr.dto.GsrRequests;
import com.app.medicalquality.gsr.dto.GsrViews;
import com.app.medicalquality.gsr.service.AuditCommandService;
import com.app.medicalquality.gsr.service.AuditQueryService;
import com.app.medicalquality.gsr.service.AuditScoreService;
import com.app.medicalquality.gsr.service.AuditSubmitService;
import com.app.medicalquality.role.PermissionEnum;
import com.app.medicalquality.score.ComplianceCalculator;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditController {

    private final AuditQueryService auditQueryService;
    private final AuditCommandService auditCommandService;
    private final AuditScoreService auditScoreService;
    private final AuditSubmitService auditSubmitService;

    public AuditController(
            AuditQueryService a_auditQueryService,
            AuditCommandService a_auditCommandService,
            AuditScoreService a_auditScoreService,
            AuditSubmitService a_auditSubmitService) {
        this.auditQueryService = a_auditQueryService;
        this.auditCommandService = a_auditCommandService;
        this.auditScoreService = a_auditScoreService;
        this.auditSubmitService = a_auditSubmitService;
    }

    @GetMapping("/api/audits")
    public ResponseEntity<AppResponse<List<GsrViews.AuditSummary>>> list(
            @RequestParam(name = "archived", defaultValue = "false") boolean a_archived,
            Authentication a_authentication) {
        return AppResponse.success(auditQueryService.list(a_archived, a_authentication));
    }

    @PostMapping("/api/audits")
    @HasPermission(PermissionEnum.CONDUCT_AUDIT)
    public ResponseEntity<AppResponse<GsrViews.AuditSummary>> create(
            @Valid @RequestBody GsrRequests.Create a_request, Authentication a_authentication) {
        return AppResponse.success(auditCommandService.create(a_request, a_authentication), "Successfully Done", HttpStatus.CREATED);
    }

    @GetMapping("/api/audits/{auditId}/checklist")
    public ResponseEntity<AppResponse<GsrViews.Checklist>> checklist(
            @PathVariable("auditId") Long a_auditId, Authentication a_authentication) {
        return AppResponse.success(auditQueryService.checklist(a_auditId, a_authentication));
    }

    @GetMapping("/api/audits/{auditId}/compliance")
    public ResponseEntity<AppResponse<ComplianceCalculator.ComplianceView>> compliance(
            @PathVariable("auditId") Long a_auditId, Authentication a_authentication) {
        auditQueryService.checklist(a_auditId, a_authentication);
        return AppResponse.success(auditScoreService.compliance(a_auditId));
    }

    @PutMapping("/api/audits/{auditId}")
    @HasPermission(PermissionEnum.EDIT_AUDIT_LIFECYCLE)
    public ResponseEntity<AppResponse<GsrViews.AuditSummary>> update(
            @PathVariable("auditId") Long a_auditId,
            @Valid @RequestBody GsrRequests.Update a_request,
            Authentication a_authentication) {
        return AppResponse.success(auditCommandService.update(a_auditId, a_request, a_authentication));
    }

    @PostMapping("/api/audits/{auditId}/archive")
    @HasPermission(PermissionEnum.ARCHIVE_AUDIT)
    public ResponseEntity<AppResponse<GsrViews.AuditSummary>> archive(
            @PathVariable("auditId") Long a_auditId, Authentication a_authentication) {
        return AppResponse.success(auditCommandService.archive(a_auditId, true, a_authentication));
    }

    @PostMapping("/api/audits/{auditId}/restore")
    @HasPermission(PermissionEnum.ARCHIVE_AUDIT)
    public ResponseEntity<AppResponse<GsrViews.AuditSummary>> restore(
            @PathVariable("auditId") Long a_auditId, Authentication a_authentication) {
        return AppResponse.success(auditCommandService.archive(a_auditId, false, a_authentication));
    }

    @PutMapping("/api/audits/{auditId}/scores")
    @HasPermission(PermissionEnum.CONDUCT_AUDIT)
    public ResponseEntity<AppResponse<ComplianceCalculator.ComplianceView>> score(
            @PathVariable("auditId") Long a_auditId,
            @Valid @RequestBody GsrRequests.Score a_request,
            Authentication a_authentication) {
        return AppResponse.success(auditScoreService.saveScore(
                a_auditId, a_request.getElementId(), a_request.getValue(), a_authentication));
    }

    @PutMapping("/api/audits/{auditId}/comments")
    @HasPermission(PermissionEnum.CONDUCT_AUDIT)
    public ResponseEntity<AppResponse<ComplianceCalculator.ComplianceView>> comment(
            @PathVariable("auditId") Long a_auditId,
            @Valid @RequestBody GsrRequests.Comment a_request,
            Authentication a_authentication) {
        return AppResponse.success(auditScoreService.saveComment(
                a_auditId, a_request.getElementId(), a_request.getText(), a_authentication));
    }

    @PostMapping("/api/audits/{auditId}/scores/bulk")
    @HasPermission(PermissionEnum.CONDUCT_AUDIT)
    public ResponseEntity<AppResponse<ComplianceCalculator.ComplianceView>> bulk(
            @PathVariable("auditId") Long a_auditId,
            @Valid @RequestBody GsrRequests.BulkScore a_request,
            Authentication a_authentication) {
        return AppResponse.success(auditScoreService.bulk(
                a_auditId, a_request.getStandardId(), a_request.getValue(), a_authentication));
    }

    @DeleteMapping("/api/audits/{auditId}/scores")
    @HasPermission(PermissionEnum.CONDUCT_AUDIT)
    public ResponseEntity<AppResponse<ComplianceCalculator.ComplianceView>> clear(
            @PathVariable("auditId") Long a_auditId, Authentication a_authentication) {
        return AppResponse.success(auditScoreService.clear(a_auditId, a_authentication));
    }

    @PostMapping("/api/audits/{auditId}/submit")
    @HasPermission(PermissionEnum.CONDUCT_AUDIT)
    public ResponseEntity<AppResponse<String>> submit(
            @PathVariable("auditId") Long a_auditId, Authentication a_authentication) {
        auditSubmitService.submit(a_auditId, a_authentication);
        return AppResponse.success("Submitted");
    }

    @PostMapping("/api/audits/{auditId}/status")
    @HasPermission(PermissionEnum.CONDUCT_AUDIT)
    public ResponseEntity<AppResponse<String>> status(
            @PathVariable("auditId") Long a_auditId,
            @Valid @RequestBody GsrRequests.StatusChange a_request,
            Authentication a_authentication) {
        return AppResponse.success(auditSubmitService.status(a_auditId, a_request.getStatus(), a_authentication).getStatus());
    }
}
