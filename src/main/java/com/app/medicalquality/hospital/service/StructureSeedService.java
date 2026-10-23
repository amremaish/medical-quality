package com.app.medicalquality.hospital.service;

import com.app.medicalquality.hospital.entity.Assessor;
import com.app.medicalquality.hospital.entity.Department;
import com.app.medicalquality.hospital.repository.AssessorRepository;
import com.app.medicalquality.hospital.repository.DepartmentRepository;
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

    private final AssessorRepository assessorRepository;
    private final DepartmentRepository departmentRepository;
    private final ObjectMapper objectMapper;

    public StructureSeedService(
            AssessorRepository a_assessorRepository,
            DepartmentRepository a_departmentRepository,
            ObjectMapper a_objectMapper) {
        this.assessorRepository = a_assessorRepository;
        this.departmentRepository = a_departmentRepository;
        this.objectMapper = a_objectMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments a_args) {
        if (assessorRepository.countByDeletedAtIsNull() > 0) {
            return;
        }
        JsonNode root = read();
        Map<String, Assessor> assessors = seedAssessors(root.get("assessors"));
        seedDepartments(root.get("departments"), assessors);
    }

    private Map<String, Assessor> seedAssessors(JsonNode a_rows) {
        Map<String, Assessor> saved = new HashMap<>();
        for (JsonNode row : a_rows) {
            Assessor assessor = new Assessor();
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

    private void seedDepartments(JsonNode a_rows, Map<String, Assessor> a_assessors) {
        int index = 1;
        for (JsonNode row : a_rows) {
            Department department = new Department();
            department.setCode("DEPT-" + String.format("%03d", index++));
            department.setNameAr(row.get("name_ar").asText());
            department.setNameEn(row.get("name_ar").asText());
            department.setDeptType(row.get("type").asText());
            department.setStatus("Active");
            department.setAssessor(a_assessors.get(row.get("assessor_id").asText()));
            departmentRepository.save(department);
        }
    }

    private JsonNode read() {
        try (InputStream in = new ClassPathResource("/catalog/seed_data.json").getInputStream()) {
            return objectMapper.readTree(in);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read hospital seed", ex);
        }
    }
}
