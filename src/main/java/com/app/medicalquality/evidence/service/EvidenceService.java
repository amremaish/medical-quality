package com.app.medicalquality.evidence.service;

import com.app.medicalquality.auth.service.CurrentUserService;
import com.app.medicalquality.evidence.entity.DocumentFile;
import com.app.medicalquality.evidence.entity.Evidence;
import com.app.medicalquality.evidence.repository.DocumentFileRepository;
import com.app.medicalquality.evidence.repository.EvidenceRepository;
import com.app.medicalquality.gsr.repository.AuditRepository;
import com.app.medicalquality.hospital.repository.DepartmentRepository;
import java.util.List;
import org.springframework.core.io.Resource;
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

    public EvidenceService(
            EvidenceRepository a_evidenceRepository,
            DocumentFileRepository a_documentFileRepository,
            AuditRepository a_auditRepository,
            DepartmentRepository a_departmentRepository,
            FileStorageService a_fileStorageService,
            CurrentUserService a_currentUserService) {
        this.evidenceRepository = a_evidenceRepository;
        this.documentFileRepository = a_documentFileRepository;
        this.auditRepository = a_auditRepository;
        this.departmentRepository = a_departmentRepository;
        this.fileStorageService = a_fileStorageService;
        this.currentUserService = a_currentUserService;
    }

    @Transactional(readOnly = true)
    public List<EvidenceView> list() {
        return evidenceRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .map(this::view)
                .toList();
    }

    @Transactional
    public EvidenceView upload(MultipartFile a_file, Long a_auditId, Long a_departmentId, Authentication a_authentication) {
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
        if (a_auditId != null) {
            evidence.setAudit(auditRepository.findById(a_auditId).orElse(null));
        }
        if (a_departmentId != null) {
            evidence.setDepartment(departmentRepository.findById(a_departmentId).orElse(null));
        }
        evidenceRepository.save(evidence);
        return view(evidence);
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
