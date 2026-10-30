package com.app.medicalquality.policy.service;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.exception.AppException;
import com.app.medicalquality.exception.NotFoundException;
import com.app.medicalquality.finding.entity.Capa;
import com.app.medicalquality.finding.repository.CapaRepository;
import com.app.medicalquality.finding.repository.FindingRepository;
import com.app.medicalquality.gsr.entity.Audit;
import com.app.medicalquality.gsr.repository.AuditRepository;
import com.app.medicalquality.hospital.entity.Department;
import com.app.medicalquality.hospital.repository.DepartmentRepository;
import com.app.medicalquality.organization.entity.Organization;
import com.app.medicalquality.organization.repository.OrganizationRepository;
import com.app.medicalquality.role.repository.RoleRepository;
import com.app.medicalquality.policy.CapaSchedule;
import com.app.medicalquality.policy.PlanningRules;
import com.app.medicalquality.policy.PolicyDocuments;
import com.app.medicalquality.policy.QualityPolicyCatalog;
import com.app.medicalquality.policy.ResolvedPolicy;
import com.app.medicalquality.policy.entity.AuditAssessor;
import com.app.medicalquality.policy.entity.QualityPolicyChange;
import com.app.medicalquality.policy.entity.QualityPolicyGroupMember;
import com.app.medicalquality.policy.entity.QualityPolicyTemplate;
import com.app.medicalquality.policy.entity.QualityPolicyVersion;
import com.app.medicalquality.policy.entity.QualityPolicyViolation;
import com.app.medicalquality.policy.repository.AuditAssessorRepository;
import com.app.medicalquality.policy.repository.QualityPolicyChangeRepository;
import com.app.medicalquality.policy.repository.QualityPolicyGroupMemberRepository;
import com.app.medicalquality.policy.repository.QualityPolicyTemplateRepository;
import com.app.medicalquality.policy.repository.QualityPolicyVersionRepository;
import com.app.medicalquality.policy.repository.QualityPolicyViolationRepository;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QualityPolicyService {

    private static final Set<String> OPEN_AUDITS = Set.of("Draft", "In Progress", "Pending Review", "Rejected");
    private static final int FLAG_LIMIT = 50;

    private final QualityPolicyVersionRepository versionRepository;
    private final QualityPolicyChangeRepository changeRepository;
    private final QualityPolicyTemplateRepository templateRepository;
    private final QualityPolicyGroupMemberRepository memberRepository;
    private final QualityPolicyViolationRepository violationRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditRepository auditRepository;
    private final FindingRepository findingRepository;
    private final CapaRepository capaRepository;
    private final AuditAssessorRepository auditAssessorRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, ResolvedPolicy> snapshots = new ConcurrentHashMap<>();

    public QualityPolicyService(
            QualityPolicyVersionRepository a_versionRepository,
            QualityPolicyChangeRepository a_changeRepository,
            QualityPolicyTemplateRepository a_templateRepository,
            QualityPolicyGroupMemberRepository a_memberRepository,
            QualityPolicyViolationRepository a_violationRepository,
            OrganizationRepository a_organizationRepository,
            AuditRepository a_auditRepository,
            FindingRepository a_findingRepository,
            CapaRepository a_capaRepository,
            AuditAssessorRepository a_auditAssessorRepository,
            DepartmentRepository a_departmentRepository,
            RoleRepository a_roleRepository,
            ObjectMapper a_objectMapper) {
        this.versionRepository = a_versionRepository;
        this.changeRepository = a_changeRepository;
        this.templateRepository = a_templateRepository;
        this.memberRepository = a_memberRepository;
        this.violationRepository = a_violationRepository;
        this.organizationRepository = a_organizationRepository;
        this.auditRepository = a_auditRepository;
        this.findingRepository = a_findingRepository;
        this.capaRepository = a_capaRepository;
        this.auditAssessorRepository = a_auditAssessorRepository;
        this.departmentRepository = a_departmentRepository;
        this.roleRepository = a_roleRepository;
        this.objectMapper = a_objectMapper;
    }

    @Transactional(readOnly = true)
    public ResolvedPolicy current(Long a_organizationId) {
        return at(a_organizationId, LocalDate.now(), null);
    }

    @Transactional(readOnly = true)
    public ResolvedPolicy forFinding(com.app.medicalquality.finding.entity.Finding a_finding) {
        if (a_finding.getPolicyVersion() != null) {
            return snapshot(a_finding.getPolicyVersion());
        }
        Department department = a_finding.getDepartment();
        if (department == null || department.getOrganization() == null) {
            return new ResolvedPolicy(QualityPolicyCatalog.defaults(objectMapper));
        }
        LocalDate asOf = a_finding.getCreatedAt() == null
                ? LocalDate.now()
                : a_finding.getCreatedAt().toLocalDate();
        return at(department.getOrganization().getId(), asOf, a_finding.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public ResolvedPolicy forAudit(Audit a_audit) {
        if (a_audit.getPolicyVersion() != null) {
            return snapshot(a_audit.getPolicyVersion());
        }
        if (a_audit.getDepartment() == null || a_audit.getDepartment().getOrganization() == null) {
            return new ResolvedPolicy(QualityPolicyCatalog.defaults(objectMapper));
        }
        LocalDate asOf = a_audit.getAuditDate() == null ? LocalDate.now() : a_audit.getAuditDate();
        return at(a_audit.getDepartment().getOrganization().getId(), asOf, a_audit.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public QualityPolicyVersion applicable(Long a_organizationId, LocalDate a_date) {
        return choose(a_organizationId, a_date, null);
    }

    @Transactional(readOnly = true)
    public void assertAuditCreatable(Department a_department, LocalDate a_date) {
        if (a_department.getOrganization() == null || a_date == null) {
            return;
        }
        Long organizationId = a_department.getOrganization().getId();
        ResolvedPolicy policy = at(organizationId, a_date, null);
        LocalDate start = a_date.withDayOfMonth(1);
        long count = auditRepository
                .countByDepartment_Organization_IdAndDeletedAtIsNullAndAuditDateGreaterThanEqualAndAuditDateLessThan(
                        organizationId, start, start.plusMonths(1));
        LocalDate previous = null;
        if (policy.bool("planning.enforceFrequency")) {
            previous = auditRepository
                    .findFirstByDepartment_IdAndDeletedAtIsNullOrderByAuditDateDesc(a_department.getId())
                    .map(Audit::getAuditDate)
                    .orElse(null);
        }
        PlanningRules.check(policy, a_date, count, previous);
    }

    @Transactional(readOnly = true)
    public PolicyPage page(Organization a_organization, boolean a_canEdit) {
        Layers layers = layers(a_organization.getId());
        QualityPolicyVersion version = choose(a_organization.getId(), LocalDate.now(), null);
        ObjectNode effective = version == null ? layers.parent() : readObject(version.getDocumentJson());
        effective = PolicyDocuments.overlay(QualityPolicyCatalog.defaults(objectMapper), effective);
        ObjectNode overrides = version == null ? objectMapper.createObjectNode() : readObject(version.getOverridesJson());
        boolean drifted = version != null && drifted(layers.parent(), effective, overrides);
        List<PolicyPage.SettingView> settings = new ArrayList<>();
        for (QualityPolicyCatalog.Setting setting : QualityPolicyCatalog.settings()) {
            QualityPolicyCatalog.Group group = QualityPolicyCatalog.group(setting.group());
            settings.add(new PolicyPage.SettingView(
                    setting.key(),
                    setting.group(),
                    group.labelEn(),
                    group.labelAr(),
                    setting.labelEn(),
                    setting.labelAr(),
                    setting.helpEn(),
                    setting.helpAr(),
                    setting.type(),
                    setting.shape(),
                    effective.get(setting.key()),
                    QualityPolicyCatalog.defaultValue(objectMapper, setting.key()),
                    source(setting.key(), overrides, layers),
                    setting.min(),
                    setting.max()));
        }
        return new PolicyPage(
                version == null ? null : version.getId(),
                version == null ? null : version.getVersionNumber(),
                version == null ? "DEFAULT" : version.getStatus(),
                version == null ? null : version.getEffectiveFrom(),
                version != null,
                a_canEdit,
                drifted,
                effective.path("notifications.language").asText("en"),
                effective.path("notifications.calendar").asText("gregorian"),
                settings,
                departmentRepository.findByOrganizationIdAndDeletedAtIsNullOrderByCodeAsc(a_organization.getId()).stream()
                        .map(department -> new PolicyPage.DepartmentChoice(
                                department.getCode(), department.getNameEn(), department.getNameAr()))
                        .toList(),
                roleRepository.findByOrganizationIdAndDeletedAtIsNullOrderByNameAsc(a_organization.getId()).stream()
                        .map(role -> new PolicyPage.RoleChoice(role.getRoleCode(), role.getName()))
                        .toList());
    }

    @Transactional
    public PolicyPreview publish(Organization a_organization, User a_actor, PublishRequest a_request) {
        PolicyPreview preview = prepare(a_organization, a_request.getOverrides(), a_request.getEffectiveFrom(), a_request.getReason());
        if (!a_request.isConfirm()) {
            return preview;
        }
        if (!preview.warnings().isEmpty() && !a_request.isAcknowledgeWarnings()) {
            throw new AppException("error.policy.accreditation", preview.warnings());
        }
        return store(a_organization, a_actor, a_request.getOverrides(), a_request.getEffectiveFrom(), a_request.getReason(), preview);
    }

    @Transactional(readOnly = true)
    public List<HistoryEntry> history(Organization a_organization) {
        List<QualityPolicyVersion> versions =
                versionRepository.findByOrganization_IdAndDeletedAtIsNullOrderByVersionNumberAsc(a_organization.getId());
        List<HistoryEntry> entries = new ArrayList<>();
        for (QualityPolicyVersion version : versions) {
            List<ChangeView> changes = changeRepository.findByVersion_IdAndDeletedAtIsNullOrderByIdAsc(version.getId()).stream()
                    .map(a_change -> new ChangeView(
                            a_change.getSettingKey(),
                            readAny(a_change.getOldValue()),
                            readAny(a_change.getNewValue())))
                    .toList();
            entries.add(new HistoryEntry(
                    version.getId(),
                    version.getVersionNumber(),
                    version.getStatus(),
                    version.getEffectiveFrom(),
                    version.getEffectiveUntil(),
                    version.getReason(),
                    person(version.getCreatedBy()),
                    version.getCreatedAt(),
                    changes));
        }
        return entries;
    }

    @Transactional(readOnly = true)
    public ExportView export(Organization a_organization) {
        QualityPolicyVersion version = choose(a_organization.getId(), LocalDate.now(), null);
        if (version == null) {
            return new ExportView(0, null, "", objectMapper.createObjectNode());
        }
        return new ExportView(
                version.getVersionNumber(),
                version.getEffectiveFrom(),
                version.getReason(),
                readObject(version.getOverridesJson()));
    }

    @Transactional
    public PolicyPreview copy(Organization a_target, User a_actor, boolean a_superAdmin, CopyRequest a_request) {
        if (!a_superAdmin) {
            throw new AccessDeniedException("Access denied");
        }
        if (a_request.getSourceOrganizationId() == null
                || a_request.getSourceOrganizationId().equals(a_target.getId())) {
            throw new AppException("error.policy.copy_source");
        }
        Organization source = organizationRepository
                .findById(a_request.getSourceOrganizationId())
                .filter(a_organization -> a_organization.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("error.organization.not_found"));
        QualityPolicyVersion sourceVersion = choose(source.getId(), LocalDate.now(), null);
        JsonNode overrides = sourceVersion == null
                ? objectMapper.createObjectNode()
                : readObject(sourceVersion.getOverridesJson());
        PublishRequest publish = new PublishRequest();
        publish.setOverrides(overrides);
        publish.setEffectiveFrom(a_request.getEffectiveFrom());
        publish.setReason(a_request.getReason());
        publish.setConfirm(a_request.isConfirm());
        publish.setAcknowledgeWarnings(a_request.isAcknowledgeWarnings());
        return publish(a_target, a_actor, publish);
    }

    @Transactional
    public PolicyPreview approve(Organization a_organization, User a_actor, Long a_versionId) {
        QualityPolicyVersion version = version(a_organization.getId(), a_versionId);
        if (!"PENDING_APPROVAL".equals(version.getStatus())) {
            throw new AppException("error.policy.approve_state");
        }
        if (version.getCreatedBy().getId().equals(a_actor.getId())) {
            throw new AppException("error.policy.approver");
        }
        version.setApprovedBy(a_actor);
        version.setApprovedAt(LocalDateTime.now());
        activate(a_organization.getId(), version);
        versionRepository.save(version);
        ResolvedPolicy policy = snapshot(version);
        List<String> flags = flag(a_organization, version, policy);
        snapshots.remove(version.getId());
        return new PolicyPreview(
                List.of(),
                openAudits(a_organization.getId()),
                openFindings(a_organization.getId()),
                openCapas(a_organization.getId()),
                flags,
                List.of(),
                true,
                version.getId(),
                version.getVersionNumber(),
                version.getStatus(),
                version.getReason(),
                version.getEffectiveFrom(),
                version.getDocumentJson(),
                version.getOverridesJson());
    }

    @Transactional
    public void reject(Organization a_organization, User a_actor, Long a_versionId) {
        QualityPolicyVersion version = version(a_organization.getId(), a_versionId);
        if (!"PENDING_APPROVAL".equals(version.getStatus()) && !"SCHEDULED".equals(version.getStatus())) {
            throw new AppException("error.policy.approve_state");
        }
        version.setStatus("REJECTED");
        version.setApprovedBy(a_actor);
        version.setApprovedAt(LocalDateTime.now());
        versionRepository.save(version);
    }

    @Transactional(readOnly = true)
    public List<ViolationView> violations(Organization a_organization) {
        return violationRepository
                .findByOrganization_IdAndDeletedAtIsNullAndResolvedAtIsNullOrderByIdDesc(a_organization.getId())
                .stream()
                .map(a_row -> new ViolationView(
                        a_row.getId(),
                        a_row.getEntityType(),
                        a_row.getEntityId(),
                        a_row.getSettingKey(),
                        a_row.getDetail(),
                        a_row.getCreatedAt()))
                .toList();
    }

    @Transactional
    public GroupTemplateView createGroupTemplate(User a_actor, boolean a_superAdmin, String a_name, JsonNode a_document) {
        if (!a_superAdmin) {
            throw new AccessDeniedException("Access denied");
        }
        if (a_name == null || a_name.isBlank() || a_name.trim().length() > 200) {
            throw new AppException("error.policy.template_name");
        }
        ObjectNode document = objectNode(a_document);
        List<PolicyDocuments.FieldError> errors = PolicyDocuments.validate(document, PolicyDocuments.overlay(
                QualityPolicyCatalog.defaults(objectMapper), document));
        if (!errors.isEmpty()) {
            throw new AppException("error.policy.validation", errors);
        }
        QualityPolicyTemplate template = new QualityPolicyTemplate();
        template.setKind("GROUP");
        template.setName(a_name.trim());
        template.setDocumentJson(write(document));
        template.setCreatedBy(a_actor);
        templateRepository.save(template);
        return new GroupTemplateView(template.getId(), template.getName());
    }

    @Transactional
    public void assignGroup(boolean a_superAdmin, Long a_templateId, Long a_organizationId) {
        if (!a_superAdmin) {
            throw new AccessDeniedException("Access denied");
        }
        QualityPolicyTemplate template = templateRepository
                .findByIdAndDeletedAtIsNull(a_templateId)
                .filter(a_template -> "GROUP".equals(a_template.getKind()))
                .orElseThrow(() -> new NotFoundException("error.policy.template_missing"));
        Organization organization = organizationRepository
                .findById(a_organizationId)
                .filter(a_organization -> a_organization.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("error.organization.not_found"));
        QualityPolicyGroupMember member = memberRepository
                .findByOrganization_IdAndDeletedAtIsNull(a_organizationId)
                .orElseGet(QualityPolicyGroupMember::new);
        member.setOrganization(organization);
        member.setTemplate(template);
        member.setDeletedAt(null);
        memberRepository.save(member);
    }

    @Transactional
    public void removeGroup(boolean a_superAdmin, Long a_templateId, Long a_organizationId) {
        if (!a_superAdmin) {
            throw new AccessDeniedException("Access denied");
        }
        QualityPolicyGroupMember member = memberRepository
                .findByOrganization_IdAndDeletedAtIsNull(a_organizationId)
                .filter(a_member -> a_member.getTemplate().getId().equals(a_templateId))
                .orElseThrow(() -> new NotFoundException("error.policy.template_missing"));
        member.setDeletedAt(LocalDateTime.now());
    }

    private PolicyPreview prepare(Organization a_organization, JsonNode a_overrides, LocalDate a_effectiveFrom, String a_reason) {
        String reason = reason(a_reason);
        LocalDate effectiveFrom = effectiveFrom(a_effectiveFrom);
        ObjectNode overrides = objectNode(a_overrides);
        Layers layers = layers(a_organization.getId());
        ObjectNode effective = PolicyDocuments.overlay(layers.parent(), overrides);
        List<PolicyDocuments.FieldError> errors = PolicyDocuments.validate(overrides, effective);
        if (!errors.isEmpty()) {
            throw new AppException("error.policy.validation", errors);
        }
        QualityPolicyVersion current = choose(a_organization.getId(), LocalDate.now(), null);
        ObjectNode before = current == null
                ? layers.parent()
                : PolicyDocuments.overlay(QualityPolicyCatalog.defaults(objectMapper), readObject(current.getDocumentJson()));
        List<PolicyDocuments.Change> changes = PolicyDocuments.diff(before, effective);
        if (changes.isEmpty() && current != null) {
            throw new AppException("error.policy.unchanged");
        }
        pending(a_organization.getId());
        ResolvedPolicy policy = new ResolvedPolicy(effective);
        Impact impact = impact(a_organization.getId(), policy);
        return new PolicyPreview(
                changes.stream().map(a_change -> new ChangeView(a_change.key(), a_change.oldValue(), a_change.newValue())).toList(),
                impact.openAudits(),
                impact.openFindings(),
                impact.openCapas(),
                impact.flags(),
                PolicyDocuments.accreditationWarnings(effective),
                false,
                null,
                null,
                null,
                reason,
                effectiveFrom,
                write(effective),
                write(overrides));
    }

    private PolicyPreview store(
            Organization a_organization,
            User a_actor,
            JsonNode a_overrides,
            LocalDate a_effectiveFrom,
            String a_reason,
            PolicyPreview a_preview) {
        organizationRepository.lockById(a_organization.getId()).orElseThrow(
                () -> new NotFoundException("error.organization.not_found"));
        pending(a_organization.getId());
        List<QualityPolicyVersion> versions =
                versionRepository.findByOrganization_IdAndDeletedAtIsNullOrderByVersionNumberAsc(a_organization.getId());
        int next = versions.stream().mapToInt(QualityPolicyVersion::getVersionNumber).max().orElse(0) + 1;
        ObjectNode effective = readObject(a_preview.effectiveJson());
        boolean approval = effective.path("workflow.policyApprovalRequired").asBoolean(false);
        QualityPolicyVersion version = new QualityPolicyVersion();
        version.setOrganization(a_organization);
        version.setVersionNumber(next);
        version.setEffectiveFrom(a_preview.effectiveFrom());
        version.setDocumentJson(a_preview.effectiveJson());
        version.setOverridesJson(a_preview.overridesJson());
        version.setReason(a_preview.reason());
        version.setCreatedBy(a_actor);
        version.setStatus(approval ? "PENDING_APPROVAL" : "ACTIVE");
        if (!approval) {
            activate(versions, version);
        }
        versionRepository.save(version);
        for (ChangeView change : a_preview.changes()) {
            QualityPolicyChange row = new QualityPolicyChange();
            row.setVersion(version);
            row.setSettingKey(change.key());
            row.setOldValue(writeNullable(change.oldValue()));
            row.setNewValue(writeNullable(change.newValue()));
            changeRepository.save(row);
        }
        List<String> flags = List.of();
        if ("ACTIVE".equals(version.getStatus())) {
            flags = flag(a_organization, version, new ResolvedPolicy(effective));
        }
        snapshots.remove(version.getId());
        return new PolicyPreview(
                a_preview.changes(),
                a_preview.openAudits(),
                a_preview.openFindings(),
                a_preview.openCapas(),
                flags,
                a_preview.warnings(),
                true,
                version.getId(),
                version.getVersionNumber(),
                version.getStatus(),
                version.getReason(),
                version.getEffectiveFrom(),
                version.getDocumentJson(),
                version.getOverridesJson());
    }

    private void activate(Long a_organizationId, QualityPolicyVersion a_version) {
        activate(versionRepository.findByOrganization_IdAndDeletedAtIsNullOrderByVersionNumberAsc(a_organizationId), a_version);
    }

    private void activate(List<QualityPolicyVersion> a_versions, QualityPolicyVersion a_version) {
        if (a_version.getEffectiveFrom().isAfter(LocalDate.now())) {
            a_version.setStatus("SCHEDULED");
            return;
        }
        a_version.setStatus("ACTIVE");
        for (QualityPolicyVersion prior : a_versions) {
            if (prior.getId() != null && prior.getId().equals(a_version.getId())) {
                continue;
            }
            if (!PolicyDocuments.applicable(prior.getStatus())) {
                continue;
            }
            if (!prior.getEffectiveFrom().isAfter(a_version.getEffectiveFrom())) {
                prior.setStatus("SUPERSEDED");
                prior.setEffectiveUntil(a_version.getEffectiveFrom());
            }
        }
    }

    private void pending(Long a_organizationId) {
        boolean waiting = versionRepository.findByOrganization_IdAndDeletedAtIsNullOrderByVersionNumberAsc(a_organizationId).stream()
                .anyMatch(a_version -> "PENDING_APPROVAL".equals(a_version.getStatus()));
        if (waiting) {
            throw new AppException("error.policy.pending");
        }
    }

    private ResolvedPolicy at(Long a_organizationId, LocalDate a_asOf, LocalDateTime a_publishedNoLaterThan) {
        QualityPolicyVersion version = choose(a_organizationId, a_asOf, a_publishedNoLaterThan);
        if (version == null) {
            return new ResolvedPolicy(a_publishedNoLaterThan == null
                    ? layers(a_organizationId).parent()
                    : QualityPolicyCatalog.defaults(objectMapper));
        }
        return snapshot(version);
    }

    private QualityPolicyVersion choose(Long a_organizationId, LocalDate a_asOf, LocalDateTime a_publishedNoLaterThan) {
        List<PolicyDocuments.Window> windows = new ArrayList<>();
        for (QualityPolicyVersion version : versionRepository
                .findByOrganization_IdAndDeletedAtIsNullOrderByVersionNumberAsc(a_organizationId)) {
            if (a_publishedNoLaterThan != null
                    && version.getCreatedAt() != null
                    && version.getCreatedAt().isAfter(a_publishedNoLaterThan)) {
                continue;
            }
            windows.add(new PolicyDocuments.Window(
                    version.getId(),
                    version.getVersionNumber(),
                    version.getEffectiveFrom(),
                    version.getEffectiveUntil(),
                    version.getStatus(),
                    version.getDocumentJson()));
        }
        return PolicyDocuments.select(windows, a_asOf)
                .flatMap(a_window -> versionRepository.findByIdAndOrganization_IdAndDeletedAtIsNull(
                        a_window.id(), a_organizationId))
                .orElse(null);
    }

    private ResolvedPolicy snapshot(QualityPolicyVersion a_version) {
        return snapshots.computeIfAbsent(a_version.getId(), a_id -> new ResolvedPolicy(PolicyDocuments.overlay(
                QualityPolicyCatalog.defaults(objectMapper), readObject(a_version.getDocumentJson()))));
    }

    private Layers layers(Long a_organizationId) {
        ObjectNode system = templateRepository
                .findFirstByKindAndDeletedAtIsNull("SYSTEM")
                .map(a_template -> readObject(a_template.getDocumentJson()))
                .orElseGet(objectMapper::createObjectNode);
        ObjectNode group = memberRepository
                .findByOrganization_IdAndDeletedAtIsNull(a_organizationId)
                .map(QualityPolicyGroupMember::getTemplate)
                .filter(a_template -> a_template.getDeletedAt() == null)
                .map(a_template -> readObject(a_template.getDocumentJson()))
                .orElseGet(objectMapper::createObjectNode);
        ObjectNode parent = PolicyDocuments.overlay(QualityPolicyCatalog.defaults(objectMapper), system);
        parent = PolicyDocuments.overlay(parent, group);
        return new Layers(system, group, parent);
    }

    private boolean drifted(ObjectNode a_parent, ObjectNode a_snapshot, ObjectNode a_overrides) {
        for (QualityPolicyCatalog.Setting setting : QualityPolicyCatalog.settings()) {
            if (a_overrides.has(setting.key())) {
                continue;
            }
            JsonNode parent = a_parent.get(setting.key());
            JsonNode snap = a_snapshot.get(setting.key());
            if (parent != null && snap != null && !parent.equals(snap)) {
                return true;
            }
        }
        return false;
    }

    private String source(String a_key, ObjectNode a_overrides, Layers a_layers) {
        if (a_overrides.has(a_key)) {
            return "HOSPITAL";
        }
        if (a_layers.group().has(a_key)) {
            return "GROUP";
        }
        if (a_layers.system().has(a_key)) {
            return "SYSTEM";
        }
        return "DEFAULT";
    }

    private Impact impact(Long a_organizationId, ResolvedPolicy a_policy) {
        return new Impact(
                openAudits(a_organizationId),
                openFindings(a_organizationId),
                openCapas(a_organizationId),
                describe(a_organizationId, a_policy));
    }

    private long openAudits(Long a_organizationId) {
        return auditRepository.countByDepartment_Organization_IdAndDeletedAtIsNullAndStatusIn(
                a_organizationId, OPEN_AUDITS);
    }

    private long openFindings(Long a_organizationId) {
        return findingRepository.countByDepartment_Organization_IdAndDeletedAtIsNullAndStatus(a_organizationId, "Open");
    }

    private long openCapas(Long a_organizationId) {
        return capaRepository.countByFinding_Department_Organization_IdAndDeletedAtIsNullAndStatusNot(
                a_organizationId, "Closed");
    }

    private List<String> describe(Long a_organizationId, ResolvedPolicy a_policy) {
        List<String> flags = new ArrayList<>();
        for (Capa capa : capaRepository.findByFinding_Department_Organization_IdAndDeletedAtIsNullAndStatusNot(
                a_organizationId, "Closed")) {
            if (flags.size() >= FLAG_LIMIT || capa.getTargetDate() == null || capa.getFinding() == null) {
                continue;
            }
            try {
                String severity = CapaSchedule.severity(
                        a_policy, capa.getFinding().getCanonicalSeverity(), capa.getFinding().getRiskLevel());
                LocalDate opened = capa.getFinding().getCreatedAt() == null
                        ? LocalDate.now()
                        : capa.getFinding().getCreatedAt().toLocalDate();
                LocalDate due = CapaSchedule.addDays(a_policy, opened, a_policy.deadlineDays(severity));
                if (capa.getTargetDate().isAfter(due)) {
                    flags.add("CAPA " + capa.getId() + " is dated " + capa.getTargetDate()
                            + ", after the " + severity + " deadline " + due + ". It is kept and flagged.");
                }
            } catch (AppException ex) {
                flags.add("CAPA " + capa.getId() + " uses a severity this policy does not recognize. It is kept and flagged.");
            }
        }
        Map<Long, List<AuditAssessor>> teams = new HashMap<>();
        for (AuditAssessor member : auditAssessorRepository
                .findByAudit_Department_Organization_IdAndDeletedAtIsNull(a_organizationId)) {
            if (member.getAudit() == null || !OPEN_AUDITS.contains(member.getAudit().getStatus())) {
                continue;
            }
            teams.computeIfAbsent(member.getAudit().getId(), a_id -> new ArrayList<>()).add(member);
        }
        for (List<AuditAssessor> team : teams.values()) {
            if (flags.size() >= FLAG_LIMIT) {
                break;
            }
            Audit audit = team.get(0).getAudit();
            Department department = audit.getDepartment();
            if (department == null) {
                continue;
            }
            int cap = a_policy.maxAssessorsForDepartment(department.getNameEn(), department.getNameAr(), department.getCode());
            cap = Math.min(cap, a_policy.integer("assessors.maxPerAudit"));
            if (team.size() > cap) {
                String name = department.getNameEn() == null ? department.getCode() : department.getNameEn();
                flags.add("Audit " + audit.getId() + " in " + name + " has " + team.size()
                        + " assessors. The policy maximum is " + cap + ". Assignments are kept and flagged.");
            }
        }
        return flags;
    }

    private List<String> flag(Organization a_organization, QualityPolicyVersion a_version, ResolvedPolicy a_policy) {
        List<String> flags = describe(a_organization.getId(), a_policy);
        for (String flag : flags) {
            QualityPolicyViolation row = new QualityPolicyViolation();
            row.setOrganization(a_organization);
            row.setVersion(a_version);
            row.setEntityType(flag.startsWith("CAPA") ? "CAPA" : "AUDIT");
            row.setEntityId(idOf(flag));
            row.setSettingKey(flag.startsWith("CAPA") ? "capa.deadline.major" : "assessors.maxPerDepartmentByName");
            row.setDetail(flag.length() > 500 ? flag.substring(0, 500) : flag);
            violationRepository.save(row);
        }
        return flags;
    }

    private long idOf(String a_flag) {
        String[] parts = a_flag.split(" ");
        if (parts.length < 2) {
            return 0;
        }
        try {
            return Long.parseLong(parts[1]);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private QualityPolicyVersion version(Long a_organizationId, Long a_versionId) {
        return versionRepository
                .findByIdAndOrganization_IdAndDeletedAtIsNull(a_versionId, a_organizationId)
                .orElseThrow(() -> new NotFoundException("error.policy.version_missing"));
    }

    private ObjectNode objectNode(JsonNode a_node) {
        if (a_node == null || a_node.isNull()) {
            return objectMapper.createObjectNode();
        }
        if (!a_node.isObject()) {
            throw new AppException("error.policy.document");
        }
        return (ObjectNode) a_node;
    }

    private ObjectNode readObject(String a_json) {
        JsonNode node = readAny(a_json);
        if (node == null || !node.isObject()) {
            return objectMapper.createObjectNode();
        }
        return (ObjectNode) node;
    }

    private JsonNode readAny(String a_json) {
        if (a_json == null || a_json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(a_json);
        } catch (JsonProcessingException ex) {
            throw new AppException("error.policy.document");
        }
    }

    private String write(JsonNode a_node) {
        try {
            return objectMapper.writeValueAsString(a_node);
        } catch (JsonProcessingException ex) {
            throw new AppException("error.policy.document");
        }
    }

    private String writeNullable(JsonNode a_node) {
        if (a_node == null || a_node.isNull()) {
            return null;
        }
        return write(a_node);
    }

    private String reason(String a_reason) {
        if (a_reason == null || a_reason.isBlank()) {
            return "";
        }
        String trimmed = a_reason.trim();
        if (trimmed.length() > 1000) {
            throw new AppException("error.policy.reason");
        }
        return trimmed;
    }

    private LocalDate effectiveFrom(LocalDate a_date) {
        if (a_date == null) {
            return LocalDate.now();
        }
        LocalDate today = LocalDate.now();
        if (a_date.isBefore(today) || a_date.isAfter(today.plusYears(5))) {
            throw new AppException("error.policy.effective_date");
        }
        return a_date;
    }

    private String person(User a_user) {
        if (a_user == null) {
            return "";
        }
        String first = a_user.getFirstName() == null ? "" : a_user.getFirstName();
        String last = a_user.getLastName() == null ? "" : a_user.getLastName();
        String name = (first + " " + last).trim();
        return name.isEmpty() ? a_user.getUsername() : name;
    }

    private record Layers(ObjectNode system, ObjectNode group, ObjectNode parent) {}

    private record Impact(long openAudits, long openFindings, long openCapas, List<String> flags) {}

    public record ChangeView(String key, JsonNode oldValue, JsonNode newValue) {}

    public record PolicyPreview(
            List<ChangeView> changes,
            long openAudits,
            long openFindings,
            long openCapas,
            List<String> flags,
            List<String> warnings,
            boolean saved,
            Long versionId,
            Integer versionNumber,
            String status,
            String reason,
            LocalDate effectiveFrom,
            @JsonIgnore String effectiveJson,
            @JsonIgnore String overridesJson) {}

    public record PolicyPage(
            Long versionId,
            Integer versionNumber,
            String status,
            LocalDate effectiveFrom,
            boolean inForce,
            boolean canEdit,
            boolean templateChanged,
            String language,
            String calendar,
            List<SettingView> settings,
            List<DepartmentChoice> departments,
            List<RoleChoice> roles) {

        public record DepartmentChoice(String code, String nameEn, String nameAr) {}

        public record RoleChoice(String roleCode, String name) {}

        public record SettingView(
                String key,
                String group,
                String groupLabelEn,
                String groupLabelAr,
                String labelEn,
                String labelAr,
                String helpEn,
                String helpAr,
                String type,
                String shape,
                JsonNode value,
                JsonNode defaultValue,
                String source,
                Double min,
                Double max) {}
    }

    public record HistoryEntry(
            long id,
            int versionNumber,
            String status,
            LocalDate effectiveFrom,
            LocalDate effectiveUntil,
            String reason,
            String actor,
            LocalDateTime changedAt,
            List<ChangeView> changes) {}

    public record ExportView(int versionNumber, LocalDate effectiveFrom, String reason, JsonNode overrides) {}

    public record ViolationView(
            long id, String entityType, long entityId, String settingKey, String detail, LocalDateTime at) {}

    public record GroupTemplateView(long id, String name) {}

    @Getter
    @Setter
    public static class PublishRequest {
        private JsonNode overrides;
        private LocalDate effectiveFrom;
        private String reason;
        private boolean confirm;
        private boolean acknowledgeWarnings;
    }

    @Getter
    @Setter
    public static class CopyRequest {
        private Long sourceOrganizationId;
        private LocalDate effectiveFrom;
        private String reason;
        private boolean confirm;
        private boolean acknowledgeWarnings;
    }
}
