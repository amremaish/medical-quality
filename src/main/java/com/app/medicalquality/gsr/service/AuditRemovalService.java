package com.app.medicalquality.gsr.service;

import com.app.medicalquality.auth.service.CurrentUserService;
import com.app.medicalquality.evidence.service.FileStorageService;
import com.app.medicalquality.exception.AppException;
import com.app.medicalquality.finding.entity.Capa;
import com.app.medicalquality.finding.entity.Finding;
import com.app.medicalquality.finding.repository.CapaRepository;
import com.app.medicalquality.finding.repository.FindingRepository;
import com.app.medicalquality.gsr.entity.Audit;
import com.app.medicalquality.gsr.entity.AuditImport;
import com.app.medicalquality.gsr.repository.AuditCommentRepository;
import com.app.medicalquality.gsr.repository.AuditImportRepository;
import com.app.medicalquality.gsr.repository.AuditScoreRepository;
import com.app.medicalquality.auth.security.PermissionEvaluator;
import com.app.medicalquality.role.PermissionEnum;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditRemovalService {

    private final AuditAccess auditAccess;
    private final AuditScoreRepository scoreRepository;
    private final AuditCommentRepository commentRepository;
    private final FindingRepository findingRepository;
    private final CapaRepository capaRepository;
    private final AuditImportRepository importRepository;
    private final FileStorageService fileStorageService;
    private final AuditTimelineWriter timelineWriter;
    private final LifecycleLogWriter lifecycleLogWriter;
    private final CurrentUserService currentUserService;
    private final PermissionEvaluator permissionEvaluator;

    public AuditRemovalService(
            AuditAccess a_auditAccess,
            AuditScoreRepository a_scoreRepository,
            AuditCommentRepository a_commentRepository,
            FindingRepository a_findingRepository,
            CapaRepository a_capaRepository,
            AuditImportRepository a_importRepository,
            FileStorageService a_fileStorageService,
            AuditTimelineWriter a_timelineWriter,
            LifecycleLogWriter a_lifecycleLogWriter,
            CurrentUserService a_currentUserService,
            PermissionEvaluator a_permissionEvaluator) {
        this.auditAccess = a_auditAccess;
        this.scoreRepository = a_scoreRepository;
        this.commentRepository = a_commentRepository;
        this.findingRepository = a_findingRepository;
        this.capaRepository = a_capaRepository;
        this.importRepository = a_importRepository;
        this.fileStorageService = a_fileStorageService;
        this.timelineWriter = a_timelineWriter;
        this.lifecycleLogWriter = a_lifecycleLogWriter;
        this.currentUserService = a_currentUserService;
        this.permissionEvaluator = a_permissionEvaluator;
    }

    @Transactional
    public void remove(Long a_auditId, String a_mode, String a_confirm, boolean a_override, Authentication a_authentication) {
        requirePermission(a_mode, a_authentication);
        Audit audit = auditAccess.require(a_auditId, a_authentication);
        if (!"pdf".equals(a_mode) && !"DELETE".equals(a_confirm)) {
            throw new AppException("error.audit.confirm_delete");
        }
        if (activeCapa(a_auditId) && !a_override) {
            throw new AppException("error.audit.capa_active");
        }
        AuditImport imported = importRepository.findFirstByAuditIdAndDeletedAtIsNullOrderByCreatedAtDesc(a_auditId).orElse(null);
        if ("pdf".equals(a_mode)) {
            deletePdf(imported, false);
            lifecycleLogWriter.add(audit, "deleted_pdf", null, currentUserService.require(a_authentication));
            return;
        }
        scoreRepository.deleteByAuditId(a_auditId);
        commentRepository.deleteByAuditId(a_auditId);
        closeChildren(a_auditId);
        if ("everything".equals(a_mode)) {
            deletePdf(imported, false);
        } else if (imported != null) {
            fileStorageService.move("audits", "archive", imported.getStoredName());
            imported.setAudit(null);
        }
        audit.setDeletedAt(LocalDateTime.now());
        lifecycleLogWriter.add(audit, "deleted_" + a_mode, null, currentUserService.require(a_authentication));
        timelineWriter.add(audit, "deleted_" + a_mode, null, null);
    }

    private void requirePermission(String a_mode, Authentication a_authentication) {
        PermissionEnum permission = "pdf".equals(a_mode)
                ? PermissionEnum.DELETE_AUDIT_PDF
                : "everything".equals(a_mode) ? PermissionEnum.DELETE_AUDIT_EVERYTHING : PermissionEnum.DELETE_AUDIT_ONLY;
        if (!permissionEvaluator.hasPermission(a_authentication, permission.name())) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private boolean activeCapa(Long a_auditId) {
        return capaRepository.findByFinding_Audit_IdAndDeletedAtIsNull(a_auditId).stream()
                .anyMatch(a_capa -> !"Closed".equals(a_capa.getStatus()));
    }

    private void closeChildren(Long a_auditId) {
        List<Finding> findings = findingRepository.findByAuditIdAndDeletedAtIsNull(a_auditId);
        LocalDateTime now = LocalDateTime.now();
        for (Finding finding : findings) {
            for (Capa capa : capaRepository.findByFindingIdAndDeletedAtIsNull(finding.getId())) {
                capa.setDeletedAt(now);
            }
            finding.setDeletedAt(now);
        }
    }

    private void deletePdf(AuditImport a_import, boolean a_keepRow) {
        if (a_import == null) {
            return;
        }
        fileStorageService.delete("audits", a_import.getStoredName());
        if (!a_keepRow) {
            a_import.setDeletedAt(LocalDateTime.now());
        }
    }
}
