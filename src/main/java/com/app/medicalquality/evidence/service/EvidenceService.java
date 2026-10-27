package com.app.medicalquality.evidence.service;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.service.CurrentUserService;
import com.app.medicalquality.evidence.entity.DocumentFile;
import com.app.medicalquality.evidence.entity.Evidence;
import com.app.medicalquality.evidence.repository.DocumentFileRepository;
import com.app.medicalquality.evidence.repository.EvidenceRepository;
import com.app.medicalquality.gsr.entity.Audit;
import com.app.medicalquality.gsr.repository.AuditRepository;
import com.app.medicalquality.hospital.entity.Department;
import com.app.medicalquality.hospital.repository.DepartmentRepository;
import com.app.medicalquality.hospital.service.DepartmentScope;
import java.util.List;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EvidenceService {

    private final EvidenceRepository evidenceRepository;
    private final DocumentFileRepository documentFileRepository;
    private final AuditRepository auditRepository;
    private final DepartmentRepository departmentRepository;
    private final FileStorageService fileStorageService;
    private final CurrentUserService currentUserService;
    private final DepartmentScope departmentScope;

    public EvidenceService(
            EvidenceRepository a_evidenceRepository,
            DocumentFileRepository a_documentFileRepository,
            AuditRepository a_auditRepository,
            DepartmentRepository a_departmentRepository,
            FileStorageService a_fileStorageService,
            CurrentUserService a_currentUserService,
            DepartmentScope a_departmentScope) {
        this.evidenceRepository = a_evidenceRepository;
        this.documentFileRepository = a_documentFileRepository;
        this.auditRepository = a_auditRepository;
        this.departmentRepository = a_departmentRepository;
        this.fileStorageService = a_fileStorageService;
        this.currentUserService = a_currentUserService;
        this.departmentScope = a_departmentScope;
    }

    @Transactional(readOnly = true)
    public List<EvidenceView> list(Authentication a_authentication) {
        Set<Long> departmentIds = visibleDepartments(a_authentication);
        return evidenceRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .filter(a_evidence -> visible(a_evidence, departmentIds))
                .map(this::view)
                .toList();
    }

    @Transactional
    public EvidenceView upload(MultipartFile a_file, Long a_auditId, Long a_departmentId, Authentication a_authentication) {
        Set<Long> departmentIds = visibleDepartments(a_authentication);
        Audit audit = null;
        Department department = null;
        if (a_auditId != null) {
            audit = auditRepository.findById(a_auditId).orElse(null);
            if (audit == null
                    || audit.getDepartment() == null
                    || !departmentIds.contains(audit.getDepartment().getId())) {
                throw new AccessDeniedException("Access denied");
            }
        }
        if (a_departmentId != null) {
            department = departmentRepository.findById(a_departmentId).orElse(null);
            if (department == null || !departmentIds.contains(department.getId())) {
                throw new AccessDeniedException("Access denied");
            }
        }
        FileStorageService.Stored stored = fileStorageService.store(a_file, "evidence");
        DocumentFile document = new DocumentFile();
        document.setCategory("evidence");
        document.setOriginalName(stored.originalName() == null ? "file" : stored.originalName());
        document.setStoredName(stored.storedName());
        document.setMimeType(stored.mimeType());
        document.setSizeBytes(stored.sizeBytes());
        document.setUploadedBy(currentUserService.require(a_authentication));
        documentFileRepository.save(document);
        Evidence evidence = new Evidence();
        evidence.setDocument(document);
        evidence.setAudit(audit);
        evidence.setDepartment(department);
        evidenceRepository.save(evidence);
        return view(evidence);
    }

    private Set<Long> visibleDepartments(Authentication a_authentication) {
        User user = currentUserService.require(a_authentication);
        return departmentScope.departmentIds(a_authentication, user);
    }

    private boolean visible(Evidence a_evidence, Set<Long> a_departmentIds) {
        if (a_evidence.getDepartment() != null && a_departmentIds.contains(a_evidence.getDepartment().getId())) {
            return true;
        }
        return a_evidence.getAudit() != null
                && a_evidence.getAudit().getDepartment() != null
                && a_departmentIds.contains(a_evidence.getAudit().getDepartment().getId());
    }

    @Transactional(readOnly = true)
    public Resource download(Long a_documentId) {
        DocumentFile document = documentFileRepository
                .findByIdAndDeletedAtIsNull(a_documentId)
                .orElseThrow(() -> new com.app.medicalquality.exception.NotFoundException("error.element.not_found"));
        return fileStorageService.load("evidence", document.getStoredName());
    }

    private EvidenceView view(Evidence a_evidence) {
        DocumentFile document = a_evidence.getDocument();
        Long auditId = a_evidence.getAudit() == null ? null : a_evidence.getAudit().getId();
        return new EvidenceView(a_evidence.getId(), document.getId(), document.getOriginalName(), document.getSizeBytes(), auditId);
    }

    public record EvidenceView(Long id, Long documentId, String originalName, long sizeBytes, Long auditId) {}
}
