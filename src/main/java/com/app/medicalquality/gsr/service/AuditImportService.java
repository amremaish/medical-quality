package com.app.medicalquality.gsr.service;

import com.app.medicalquality.auth.service.CurrentUserService;
import com.app.medicalquality.evidence.service.FileStorageService;
import com.app.medicalquality.exception.AppException;
import com.app.medicalquality.exception.NotFoundException;
import com.app.medicalquality.finding.service.FindingService;
import com.app.medicalquality.gsr.dto.GsrRequests;
import com.app.medicalquality.gsr.entity.AuditImport;
import com.app.medicalquality.gsr.entity.GsrElement;
import com.app.medicalquality.gsr.entity.GsrStandard;
import com.app.medicalquality.gsr.entity.Audit;
import com.app.medicalquality.gsr.repository.AuditImportRepository;
import com.app.medicalquality.gsr.repository.AuditRepository;
import com.app.medicalquality.gsr.repository.GsrElementRepository;
import com.app.medicalquality.gsr.repository.GsrStandardRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AuditImportService {

    private final AuditImportRepository importRepository;
    private final AuditRepository auditRepository;
    private final GsrStandardRepository standardRepository;
    private final GsrElementRepository elementRepository;
    private final AuditScoreService auditScoreService;
    private final AuditCommandService auditCommandService;
    private final FindingService findingService;
    private final FileStorageService fileStorageService;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    public AuditImportService(
            AuditImportRepository a_importRepository,
            AuditRepository a_auditRepository,
            GsrStandardRepository a_standardRepository,
            GsrElementRepository a_elementRepository,
            AuditScoreService a_auditScoreService,
            AuditCommandService a_auditCommandService,
            FindingService a_findingService,
            FileStorageService a_fileStorageService,
            CurrentUserService a_currentUserService,
            ObjectMapper a_objectMapper) {
        this.importRepository = a_importRepository;
        this.auditRepository = a_auditRepository;
        this.standardRepository = a_standardRepository;
        this.elementRepository = a_elementRepository;
        this.auditScoreService = a_auditScoreService;
        this.auditCommandService = a_auditCommandService;
        this.findingService = a_findingService;
        this.fileStorageService = a_fileStorageService;
        this.currentUserService = a_currentUserService;
        this.objectMapper = a_objectMapper;
    }

    @Transactional
    public ImportView upload(MultipartFile a_file, Authentication a_authentication) {
        FileStorageService.Stored stored = fileStorageService.store(a_file, "audits");
        String text = readText(stored.storedName());
        if (text.isBlank()) {
            throw new AppException("error.import.empty");
        }
        AuditImport row = new AuditImport();
        row.setOriginalName(stored.originalName());
        row.setStoredName(stored.storedName());
        row.setMimeType(stored.mimeType());
        row.setSizeBytes(stored.sizeBytes());
        row.setStatus("parsed");
        row.setExtractedJson(preview(text));
        row.setUploadedBy(currentUserService.require(a_authentication));
        importRepository.save(row);
        return new ImportView(row.getId(), row.getOriginalName(), row.getStatus(), row.getExtractedJson());
    }

    @Transactional
    public Long confirm(Long a_importId, Long a_auditId, Authentication a_authentication) {
        AuditImport row = importRepository
                .findByIdAndDeletedAtIsNull(a_importId)
                .orElseThrow(() -> new NotFoundException("error.import.not_found"));
        Long auditId = a_auditId == null ? createAudit(row, a_authentication) : a_auditId;
        apply(auditId, row.getExtractedJson(), a_authentication);
        Audit audit = auditRepository
                .findByIdAndDeletedAtIsNull(auditId)
                .orElseThrow(() -> new NotFoundException("error.audit.not_found"));
        row.setAudit(audit);
        row.setStatus("confirmed");
        return auditId;
    }

    @Transactional
    public ImportView updatePreview(Long a_importId, String a_json) {
        AuditImport row = importRepository
                .findByIdAndDeletedAtIsNull(a_importId)
                .orElseThrow(() -> new NotFoundException("error.import.not_found"));
        try {
            if (!objectMapper.readTree(a_json).has("standards")) {
                throw new AppException("error.validation.failed");
            }
        } catch (IOException ex) {
            throw new AppException("error.validation.failed");
        }
        row.setExtractedJson(a_json);
        return new ImportView(row.getId(), row.getOriginalName(), row.getStatus(), row.getExtractedJson());
    }

    public record ImportView(Long id, String originalName, String status, String extractedJson) {}

    private Long createAudit(AuditImport a_row, Authentication a_authentication) {
        try {
            JsonNode json = objectMapper.readTree(a_row.getExtractedJson());
            GsrRequests.Create request = new GsrRequests.Create();
            request.setDepartmentName(text(json, "departmentName", "Imported department"));
            request.setAuditorName(text(json, "auditorName", "Imported auditor"));
            request.setAuditDate(date(json));
            return auditCommandService.create(request, a_authentication).id();
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read import preview", ex);
        }
    }

    private void apply(Long a_auditId, String a_json, Authentication a_authentication) {
        try {
            for (JsonNode standard : objectMapper.readTree(a_json).get("standards")) {
                applyStandard(a_auditId, standard, a_authentication);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot apply import", ex);
        }
    }

    private void applyStandard(Long a_auditId, JsonNode a_standard, Authentication a_authentication) {
        if (!a_standard.hasNonNull("status")) {
            return;
        }
        GsrStandard standard = standardRepository
                .findByCodeAndDeletedAtIsNull(a_standard.get("gsr").asText())
                .orElse(null);
        if (standard == null) {
            return;
        }
        String status = a_standard.get("status").asText();
        String comment = a_standard.path("comment").asText("");
        for (GsrElement element : elementRepository.findByStandardIdAndDeletedAtIsNullOrderBySortOrderAsc(standard.getId())) {
            auditScoreService.saveScore(a_auditId, element.getId(), status, a_authentication);
            if (!comment.isBlank()) {
                auditScoreService.saveComment(a_auditId, element.getId(), comment, a_authentication);
            }
        }
        if ("partial".equals(status) || "not".equals(status)) {
            FindingService.Create finding = new FindingService.Create();
            finding.setAuditId(a_auditId);
            finding.setGsrCode(standard.getCode());
            finding.setElementIndex(0);
            finding.setDescription(standard.getCode() + " imported as " + status);
            finding.setRiskLevel("not".equals(status) ? "High" : "Medium");
            finding.setCategory("Imported");
            findingService.open(finding, a_authentication);
        }
    }

    private String preview(String a_text) {
        GsrTextExtractor.Header header = GsrTextExtractor.header(a_text);
        List<String> codes = standardRepository.findByDeletedAtIsNullOrderBySortOrderAsc().stream()
                .map(GsrStandard::getCode)
                .toList();
        ObjectNode root = objectMapper.createObjectNode();
        root.put("departmentName", header.departmentName());
        root.put("auditorName", header.auditorName());
        root.put("auditDate", header.auditDate());
        ArrayNode standards = root.putArray("standards");
        for (GsrTextExtractor.Hit hit : GsrTextExtractor.standards(a_text, codes)) {
            ObjectNode node = standards.addObject();
            node.put("gsr", hit.code());
            if (hit.status() != null) {
                node.put("status", hit.status());
            } else {
                node.putNull("status");
            }
            node.put("comment", "");
        }
        return root.toString();
    }

    private String readText(String a_storedName) {
        try (PDDocument document = Loader.loadPDF(fileStorageService.load("audits", a_storedName).getFile())) {
            return new PDFTextStripper().getText(document);
        } catch (IOException ex) {
            throw new AppException("error.import.empty");
        }
    }

    private String text(JsonNode a_json, String a_field, String a_fallback) {
        String value = a_json.path(a_field).asText("");
        return value.isBlank() ? a_fallback : value;
    }

    private LocalDate date(JsonNode a_json) {
        String value = a_json.path("auditDate").asText("");
        return value.isBlank() ? LocalDate.now() : LocalDate.parse(value);
    }
}
