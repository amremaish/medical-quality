package com.app.medicalquality.web;

import com.app.medicalquality.auth.security.PermissionEvaluator;
import com.app.medicalquality.auth.security.annotation.HasPermission;
import com.app.medicalquality.common.AppResponse;
import com.app.medicalquality.dashboard.service.DashboardService;
import com.app.medicalquality.evidence.service.EvidenceService;
import com.app.medicalquality.finding.service.FindingService;
import com.app.medicalquality.gsr.service.AuditImportService;
import com.app.medicalquality.gsr.service.AuditRemovalService;
import com.app.medicalquality.hospital.service.HospitalQueryService;
import com.app.medicalquality.role.PermissionEnum;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
public class OperationsController {

    private final FindingService findingService;
    private final HospitalQueryService hospitalQueryService;
    private final EvidenceService evidenceService;
    private final DashboardService dashboardService;
    private final AuditImportService auditImportService;
    private final AuditRemovalService auditRemovalService;
    private final PermissionEvaluator permissionEvaluator;

    public OperationsController(
            FindingService a_findingService,
            HospitalQueryService a_hospitalQueryService,
            EvidenceService a_evidenceService,
            DashboardService a_dashboardService,
            AuditImportService a_auditImportService,
            AuditRemovalService a_auditRemovalService,
            PermissionEvaluator a_permissionEvaluator) {
        this.findingService = a_findingService;
        this.hospitalQueryService = a_hospitalQueryService;
        this.evidenceService = a_evidenceService;
        this.dashboardService = a_dashboardService;
        this.auditImportService = a_auditImportService;
        this.auditRemovalService = a_auditRemovalService;
        this.permissionEvaluator = a_permissionEvaluator;
    }

    @GetMapping("/api/findings")
    public ResponseEntity<AppResponse<List<FindingService.FindingView>>> findings(Authentication a_authentication) {
        return AppResponse.success(findingService.list(a_authentication));
    }

    @PostMapping("/api/findings")
    @HasPermission(PermissionEnum.MANAGE_FINDINGS)
    public ResponseEntity<AppResponse<FindingService.FindingView>> openFinding(
            @RequestBody FindingService.Create a_request, Authentication a_authentication) {
        return AppResponse.success(findingService.open(a_request, a_authentication));
    }

    @GetMapping("/api/capas")
    public ResponseEntity<AppResponse<List<FindingService.CapaView>>> capas(Authentication a_authentication) {
        return AppResponse.success(findingService.capas(a_authentication));
    }

    @PostMapping("/api/findings/{findingId}/capas")
    @HasPermission(PermissionEnum.MANAGE_CAPA)
    public ResponseEntity<AppResponse<FindingService.CapaView>> addCapa(
            @PathVariable("findingId") Long a_findingId,
            @RequestBody FindingService.CapaForm a_form,
            Authentication a_authentication) {
        return AppResponse.success(findingService.addCapa(a_findingId, a_form, a_authentication));
    }

    @PutMapping("/api/capas/{capaId}")
    @HasPermission(PermissionEnum.MANAGE_CAPA)
    public ResponseEntity<AppResponse<FindingService.CapaView>> updateCapa(
            @PathVariable("capaId") Long a_capaId,
            @RequestBody FindingService.CapaForm a_form,
            Authentication a_authentication) {
        return AppResponse.success(findingService.updateCapa(a_capaId, a_form, a_authentication));
    }

    @GetMapping("/api/department-types")
    public ResponseEntity<AppResponse<List<HospitalQueryService.DepartmentTypeView>>> departmentTypes() {
        return AppResponse.success(hospitalQueryService.departmentTypes());
    }

    @GetMapping("/api/departments")
    public ResponseEntity<AppResponse<List<HospitalQueryService.DepartmentView>>> departments(
            Authentication a_authentication) {
        return AppResponse.success(hospitalQueryService.departments(a_authentication));
    }

    @PostMapping("/api/departments")
    @HasPermission(PermissionEnum.MANAGE_DEPARTMENTS)
    public ResponseEntity<AppResponse<HospitalQueryService.DepartmentView>> createDepartment(
            @RequestBody HospitalQueryService.DepartmentForm a_form, Authentication a_authentication) {
        return AppResponse.success(hospitalQueryService.createDepartment(a_form, a_authentication));
    }

    @GetMapping("/api/assessors")
    public ResponseEntity<AppResponse<List<HospitalQueryService.AssessorView>>> assessors(
            Authentication a_authentication) {
        return AppResponse.success(hospitalQueryService.assessors(a_authentication));
    }

    @GetMapping("/api/assessors/{id}")
    public ResponseEntity<AppResponse<HospitalQueryService.AssessorDetail>> assessor(
            @PathVariable("id") Long a_id, Authentication a_authentication) {
        return AppResponse.success(hospitalQueryService.assessor(a_id, a_authentication));
    }

    @PutMapping("/api/assessors/{id}")
    @HasPermission(PermissionEnum.MANAGE_ASSESSORS)
    public ResponseEntity<AppResponse<HospitalQueryService.AssessorDetail>> updateAssessor(
            @PathVariable("id") Long a_id,
            @RequestBody HospitalQueryService.AssessorForm a_form,
            Authentication a_authentication) {
        return AppResponse.success(hospitalQueryService.updateAssessor(a_id, a_form, a_authentication));
    }

    @PostMapping("/api/assessors/{id}/departments")
    @HasPermission(PermissionEnum.MANAGE_ASSESSORS)
    public ResponseEntity<AppResponse<HospitalQueryService.AssessorDetail>> assignDepartment(
            @PathVariable("id") Long a_id,
            @RequestBody HospitalQueryService.AssignDepartmentForm a_form,
            Authentication a_authentication) {
        return AppResponse.success(hospitalQueryService.assignDepartment(
                a_id, a_form == null ? null : a_form.getDepartmentId(), a_authentication));
    }

    @DeleteMapping("/api/assessors/{id}/departments/{departmentId}")
    @HasPermission(PermissionEnum.MANAGE_ASSESSORS)
    public ResponseEntity<AppResponse<HospitalQueryService.AssessorDetail>> unassignDepartment(
            @PathVariable("id") Long a_id,
            @PathVariable("departmentId") Long a_departmentId,
            Authentication a_authentication) {
        return AppResponse.success(hospitalQueryService.unassignDepartment(a_id, a_departmentId, a_authentication));
    }

    @GetMapping("/api/dashboard")
    public ResponseEntity<AppResponse<DashboardService.Summary>> dashboard(Authentication a_authentication) {
        requireDashboard(a_authentication);
        return AppResponse.success(dashboardService.summary(a_authentication));
    }

    @GetMapping("/api/evidence")
    public ResponseEntity<AppResponse<List<EvidenceService.EvidenceView>>> evidence(Authentication a_authentication) {
        return AppResponse.success(evidenceService.list(a_authentication));
    }

    @PostMapping(value = "/api/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @HasPermission(PermissionEnum.MANAGE_EVIDENCE)
    public ResponseEntity<AppResponse<EvidenceService.EvidenceView>> uploadEvidence(
            @RequestParam("file") MultipartFile a_file,
            @RequestParam(name = "auditId", required = false) Long a_auditId,
            @RequestParam(name = "departmentId", required = false) Long a_departmentId,
            Authentication a_authentication) {
        return AppResponse.success(evidenceService.upload(a_file, a_auditId, a_departmentId, a_authentication));
    }

    @GetMapping("/api/files/{documentId}")
    public ResponseEntity<Resource> file(
            @PathVariable("documentId") Long a_documentId, Authentication a_authentication) {
        Resource resource = evidenceService.download(a_documentId, a_authentication);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .body(resource);
    }

    @PostMapping(value = "/api/audit-imports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @HasPermission(PermissionEnum.MANAGE_IMPORT)
    public ResponseEntity<AppResponse<AuditImportService.ImportView>> uploadImport(
            @RequestParam("file") MultipartFile a_file, Authentication a_authentication) {
        return AppResponse.success(auditImportService.upload(a_file, a_authentication));
    }

    @PutMapping("/api/audit-imports/{importId}")
    @HasPermission(PermissionEnum.MANAGE_IMPORT)
    public ResponseEntity<AppResponse<AuditImportService.ImportView>> updateImport(
            @PathVariable("importId") Long a_importId, @RequestBody JsonNode a_json, Authentication a_authentication) {
        return AppResponse.success(auditImportService.updatePreview(a_importId, a_json.toString(), a_authentication));
    }

    @PostMapping("/api/audit-imports/{importId}/confirm")
    @HasPermission({PermissionEnum.MANAGE_IMPORT, PermissionEnum.CONDUCT_AUDIT})
    public ResponseEntity<AppResponse<Long>> confirmImport(
            @PathVariable("importId") Long a_importId,
            @RequestParam(name = "auditId", required = false) Long a_auditId,
            Authentication a_authentication) {
        return AppResponse.success(auditImportService.confirm(a_importId, a_auditId, a_authentication));
    }

    @PostMapping("/api/audits/{auditId}/remove")
    public ResponseEntity<AppResponse<String>> remove(
            @PathVariable("auditId") Long a_auditId,
            @RequestParam("mode") String a_mode,
            @RequestParam(name = "confirm", required = false) String a_confirm,
            @RequestParam(name = "overrideCapa", defaultValue = "false") boolean a_override,
            Authentication a_authentication) {
        auditRemovalService.remove(a_auditId, a_mode, a_confirm, a_override, a_authentication);
        return AppResponse.success("Removed");
    }

    private void requireDashboard(Authentication a_authentication) {
        boolean allowed = permissionEvaluator.hasPermission(a_authentication, PermissionEnum.VIEW_COMMAND_CENTER.name())
                || permissionEvaluator.hasPermission(a_authentication, PermissionEnum.VIEW_EXECUTIVE_DASHBOARD.name())
                || permissionEvaluator.hasPermission(a_authentication, PermissionEnum.VIEW_ASSESSOR_DASHBOARD.name())
                || permissionEvaluator.hasPermission(
                        a_authentication, PermissionEnum.VIEW_QUALITY_DIRECTOR_DASHBOARD.name());
        if (!allowed) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
