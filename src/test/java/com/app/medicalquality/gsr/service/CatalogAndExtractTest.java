package com.app.medicalquality.gsr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogAndExtractTest {

    @Test
    void catalogsMatchTheChecklistSizes() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode gsr = mapper.readTree(resource("/catalog/gsr_data.json"));
        int elements = 0;
        for (JsonNode standard : gsr) {
            elements += standard.get("eoc").size();
        }
        assertEquals(29, gsr.size());
        assertEquals(147, elements);

        int items = 0;
        for (JsonNode section : mapper.readTree(resource("/catalog/mr_catalog.json"))) {
            items += section.get("items").size();
        }
        assertEquals(61, items);
    }

    @Test
    void extractorReadsStatusNextToEachStandard() {
        String text = "Department: Pharmacy\nAuditor Name: Sara\n2026-03-01\nGSR.01 identification Met\nGSR.02 orders Not Met";
        assertEquals("Pharmacy", GsrTextExtractor.header(text).departmentName());
        assertEquals("2026-03-01", GsrTextExtractor.header(text).auditDate());
        List<GsrTextExtractor.Hit> hits = GsrTextExtractor.standards(text, List.of("GSR.01", "GSR.02", "GSR.03"));
        assertEquals("met", hits.get(0).status());
        assertEquals("not", hits.get(1).status());
        assertEquals(null, hits.get(2).status());
    }

    private InputStream resource(String a_path) {
        return getClass().getResourceAsStream(a_path);
    }
}
