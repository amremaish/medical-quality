package com.app.medicalquality.policy.controller;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.security.PermissionEvaluator;
import com.app.medicalquality.auth.security.annotation.HasPermission;
import com.app.medicalquality.auth.service.CurrentUserService;
import com.app.medicalquality.common.AppResponse;
import com.app.medicalquality.hospital.service.HospitalContext;
import com.app.medicalquality.organization.entity.Organization;
import com.app.medicalquality.policy.service.QualityPolicyService;
import com.app.medicalquality.policy.service.QualityPolicyService.CopyRequest;
import com.app.medicalquality.policy.service.QualityPolicyService.PublishRequest;
import com.app.medicalquality.role.PermissionEnum;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QualityPolicyController {

    private final QualityPolicyService policyService;
    private final HospitalContext hospitalContext;
    private final CurrentUserService currentUserService;
    private final PermissionEvaluator permissionEvaluator;

    public QualityPolicyController(
            QualityPolicyService a_policyService,
            HospitalContext a_hospitalContext,
            CurrentUserService a_currentUserService,
            PermissionEvaluator a_permissionEvaluator) {
        this.policyService = a_policyService;
        this.hospitalContext = a_hospitalContext;
        this.currentUserService = a_currentUserService;
        this.permissionEvaluator = a_permissionEvaluator;
    }

    @GetMapping("/api/quality-policy")
    @HasPermission(PermissionEnum.QUALITY_POLICY_VIEW)
    public ResponseEntity<AppResponse<QualityPolicyService.PolicyPage>> page(Authentication a_authentication) {
        Organization organization = hospitalContext.require(a_authentication);
        return AppResponse.success(policyService.page(organization, canEdit(a_authentication)));
    }

    @PostMapping("/api/quality-policy/versions")
    @HasPermission(PermissionEnum.QUALITY_POLICY_EDIT)
    public ResponseEntity<AppResponse<QualityPolicyService.PolicyPreview>> publish(
            @RequestBody PublishRequest a_request, Authentication a_authentication) {
        return AppResponse.success(policyService.publish(acting(a_authentication), actor(a_authentication), a_request));
    }

    @GetMapping("/api/quality-policy/versions")
    @HasPermission(PermissionEnum.QUALITY_POLICY_VIEW)
    public ResponseEntity<AppResponse<java.util.List<QualityPolicyService.HistoryEntry>>> history(
            Authentication a_authentication) {
        return AppResponse.success(policyService.history(acting(a_authentication)));
    }

    @PostMapping("/api/quality-policy/versions/{versionId}/approve")
    @HasPermission(PermissionEnum.QUALITY_POLICY_EDIT)
    public ResponseEntity<AppResponse<QualityPolicyService.PolicyPreview>> approve(
            @PathVariable("versionId") Long a_versionId, Authentication a_authentication) {
        return AppResponse.success(policyService.approve(acting(a_authentication), actor(a_authentication), a_versionId));
    }

    @PostMapping("/api/quality-policy/versions/{versionId}/reject")
    @HasPermission(PermissionEnum.QUALITY_POLICY_EDIT)
    public ResponseEntity<AppResponse<String>> reject(
            @PathVariable("versionId") Long a_versionId, Authentication a_authentication) {
        policyService.reject(acting(a_authentication), actor(a_authentication), a_versionId);
        return AppResponse.success("Rejected");
    }

    @GetMapping("/api/quality-policy/export")
    @HasPermission(PermissionEnum.QUALITY_POLICY_VIEW)
    public ResponseEntity<AppResponse<QualityPolicyService.ExportView>> export(Authentication a_authentication) {
        return AppResponse.success(policyService.export(acting(a_authentication)));
    }

    @PostMapping("/api/quality-policy/import")
    @HasPermission(PermissionEnum.QUALITY_POLICY_EDIT)
    public ResponseEntity<AppResponse<QualityPolicyService.PolicyPreview>> importPolicy(
            @RequestBody PublishRequest a_request, Authentication a_authentication) {
        return AppResponse.success(policyService.publish(acting(a_authentication), actor(a_authentication), a_request));
    }

    @PostMapping("/api/quality-policy/copies")
    @HasPermission(PermissionEnum.QUALITY_POLICY_EDIT)
    public ResponseEntity<AppResponse<QualityPolicyService.PolicyPreview>> copy(
            @RequestBody CopyRequest a_request, Authentication a_authentication) {
        User user = actor(a_authentication);
        return AppResponse.success(policyService.copy(acting(a_authentication), user, user.isSuperAdmin(), a_request));
    }

    @GetMapping("/api/quality-policy/violations")
    @HasPermission(PermissionEnum.QUALITY_POLICY_VIEW)
    public ResponseEntity<AppResponse<java.util.List<QualityPolicyService.ViolationView>>> violations(
            Authentication a_authentication) {
        return AppResponse.success(policyService.violations(acting(a_authentication)));
    }

    @PostMapping("/api/quality-policy/group-templates")
    @HasPermission(PermissionEnum.QUALITY_POLICY_EDIT)
    public ResponseEntity<AppResponse<QualityPolicyService.GroupTemplateView>> createGroup(
            @RequestBody GroupRequest a_request, Authentication a_authentication) {
        User user = actor(a_authentication);
        return AppResponse.success(policyService.createGroupTemplate(
                user, user.isSuperAdmin(), a_request.getName(), a_request.getDocument()));
    }

    @PostMapping("/api/quality-policy/group-templates/{templateId}/members")
    @HasPermission(PermissionEnum.QUALITY_POLICY_EDIT)
    public ResponseEntity<AppResponse<String>> assignGroup(
            @PathVariable("templateId") Long a_templateId,
            @RequestBody MemberRequest a_request,
            Authentication a_authentication) {
        policyService.assignGroup(actor(a_authentication).isSuperAdmin(), a_templateId, a_request.getOrganizationId());
        return AppResponse.success("Assigned");
    }

    @DeleteMapping("/api/quality-policy/group-templates/{templateId}/members/{organizationId}")
    @HasPermission(PermissionEnum.QUALITY_POLICY_EDIT)
    public ResponseEntity<AppResponse<String>> clearGroup(
            @PathVariable("templateId") Long a_templateId,
            @PathVariable("organizationId") Long a_organizationId,
            Authentication a_authentication) {
        policyService.removeGroup(actor(a_authentication).isSuperAdmin(), a_templateId, a_organizationId);
        return AppResponse.success("Removed");
    }

    private Organization acting(Authentication a_authentication) {
        return hospitalContext.require(a_authentication);
    }

    private User actor(Authentication a_authentication) {
        return currentUserService.require(a_authentication);
    }

    private boolean canEdit(Authentication a_authentication) {
        User user = actor(a_authentication);
        return user.isSuperAdmin()
                || permissionEvaluator.hasPermission(a_authentication, PermissionEnum.QUALITY_POLICY_EDIT.name());
    }

    @Getter
    @Setter
    public static class GroupRequest {
        private String name;
        private JsonNode document;
    }

    @Getter
    @Setter
    public static class MemberRequest {
        private Long organizationId;
    }
}
