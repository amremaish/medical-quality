package com.app.medicalquality.hospital.service;

import com.app.medicalquality.hospital.entity.Assessor;
import com.app.medicalquality.hospital.entity.Department;
import com.app.medicalquality.hospital.entity.DepartmentType;
import com.app.medicalquality.hospital.repository.AssessorRepository;
import com.app.medicalquality.hospital.repository.DepartmentRepository;
import com.app.medicalquality.hospital.repository.DepartmentTypeRepository;
import com.app.medicalquality.organization.FacilityType;
import com.app.medicalquality.organization.entity.Organization;
import com.app.medicalquality.organization.repository.OrganizationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Order(4)
public class StructureSeedService implements ApplicationRunner {

    private static final Map<String, String> TYPE_CODES = Map.ofEntries(
            Map.entry("Pharmacy", "PHARMACY"),
            Map.entry("Diagnostic", "DIAGNOSTIC"),
            Map.entry("Inpatient", "INPATIENT"),
            Map.entry("Inpatient Wards", "INPATIENT"),
            Map.entry("Day Care", "OUTPATIENT"),
            Map.entry("Outpatient", "OUTPATIENT"),
            Map.entry("Laboratory", "LABORATORY"),
            Map.entry("Radiology", "RADIOLOGY"),
            Map.entry("Operating Rooms", "SURGICAL"),
            Map.entry("Cath Lab", "SURGICAL"),
            Map.entry("Plastic Surgery", "SURGICAL"),
            Map.entry("ER", "EMERGENCY"),
            Map.entry("ICU", "CRITICAL_CARE"),
            Map.entry("NICU", "CRITICAL_CARE"),
            Map.entry("Rehabilitation", "REHABILITATION"),
            Map.entry("Speech Therapy", "REHABILITATION"),
            Map.entry("Cardiology", "CLINICAL"),
            Map.entry("Neurosciences", "CLINICAL"),
            Map.entry("Tropical Medicine", "CLINICAL"),
            Map.entry("Blood Bank", "BLOOD_BANK"),
            Map.entry("Transplant", "TRANSPLANT"),
            Map.entry("CSSD", "CSSD"));

    private final AssessorRepository assessorRepository;
    private final DepartmentRepository departmentRepository;
    private final DepartmentTypeRepository departmentTypeRepository;
    private final OrganizationRepository organizationRepository;
    private final ObjectMapper objectMapper;

    public StructureSeedService(
            AssessorRepository a_assessorRepository,
            DepartmentRepository a_departmentRepository,
            DepartmentTypeRepository a_departmentTypeRepository,
            OrganizationRepository a_organizationRepository,
            ObjectMapper a_objectMapper) {
        this.assessorRepository = a_assessorRepository;
        this.departmentRepository = a_departmentRepository;
        this.departmentTypeRepository = a_departmentTypeRepository;
        this.organizationRepository = a_organizationRepository;
        this.objectMapper = a_objectMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments a_args) {
        if (assessorRepository.countByDeletedAtIsNull() > 0) {
            return;
        }
        JsonNode root = read();
        Organization organization = hospital(root.get("hospital"));
        Map<String, Assessor> assessors = seedAssessors(root.get("assessors"), organization);
        seedDepartments(root.get("departments"), assessors, organization);
    }

    private Organization hospital(JsonNode a_hospital) {
        String name = a_hospital.get("name_en").asText();
        return organizationRepository.findByNameAndDeletedAtIsNull(name).orElseGet(() -> {
            Organization organization = new Organization();
            organization.setName(name);
            organization.setOrgType(FacilityType.HOSPITAL);
            organization.setStatus("Active");
            return organizationRepository.save(organization);
        });
    }

    private Map<String, Assessor> seedAssessors(JsonNode a_rows, Organization a_organization) {
        Map<String, Assessor> saved = new HashMap<>();
        for (JsonNode row : a_rows) {
            Assessor assessor = new Assessor();
            assessor.setOrganization(a_organization);
            assessor.setCode(row.get("id").asText());
            assessor.setNameAr(row.get("name_ar").asText());
            assessor.setNameEn(row.get("name_en").asText());
            assessor.setNote(row.hasNonNull("note") ? row.get("note").asText() : null);
            assessor.setActive(!row.get("departments").isEmpty());
            assessorRepository.save(assessor);
            saved.put(assessor.getCode(), assessor);
        }
        return saved;
    }

    private void seedDepartments(JsonNode a_rows, Map<String, Assessor> a_assessors, Organization a_organization) {
        int index = 1;
        for (JsonNode row : a_rows) {
            Department department = new Department();
            department.setOrganization(a_organization);
            department.setCode("DEPT-" + String.format("%03d", index++));
            department.setNameAr(row.get("name_ar").asText());
            department.setNameEn(row.get("name_ar").asText());
            department.setDepartmentType(departmentType(row.get("type").asText()));
            department.setStatus("Active");
            department.setAssessor(a_assessors.get(row.get("assessor_id").asText()));
            departmentRepository.save(department);
        }
    }

    private DepartmentType departmentType(String a_label) {
        String code = TYPE_CODES.getOrDefault(a_label, a_label);
        return departmentTypeRepository
                .findById(code)
                .orElseThrow(() -> new IllegalStateException("Unknown department type: " + a_label));
    }

    private JsonNode read() {
        try (InputStream in = new ClassPathResource("/catalog/seed_data.json").getInputStream()) {
            return objectMapper.readTree(in);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read hospital seed", ex);
        }
    }
}
