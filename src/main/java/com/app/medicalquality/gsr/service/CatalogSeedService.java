package com.app.medicalquality.gsr.service;

import com.app.medicalquality.gsr.entity.GsrChapter;
import com.app.medicalquality.gsr.entity.GsrElement;
import com.app.medicalquality.gsr.entity.GsrStandard;
import com.app.medicalquality.gsr.repository.GsrChapterRepository;
import com.app.medicalquality.gsr.repository.GsrElementRepository;
import com.app.medicalquality.gsr.repository.GsrStandardRepository;
import com.app.medicalquality.record.entity.MrItem;
import com.app.medicalquality.record.entity.MrSection;
import com.app.medicalquality.record.repository.MrItemRepository;
import com.app.medicalquality.record.repository.MrSectionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Order(3)
public class CatalogSeedService implements ApplicationRunner {

    private final GsrChapterRepository chapterRepository;
    private final GsrStandardRepository standardRepository;
    private final GsrElementRepository elementRepository;
    private final MrSectionRepository sectionRepository;
    private final MrItemRepository itemRepository;
    private final ObjectMapper objectMapper;

    public CatalogSeedService(
            GsrChapterRepository a_chapterRepository,
            GsrStandardRepository a_standardRepository,
            GsrElementRepository a_elementRepository,
            MrSectionRepository a_sectionRepository,
            MrItemRepository a_itemRepository,
            ObjectMapper a_objectMapper) {
        this.chapterRepository = a_chapterRepository;
        this.standardRepository = a_standardRepository;
        this.elementRepository = a_elementRepository;
        this.sectionRepository = a_sectionRepository;
        this.itemRepository = a_itemRepository;
        this.objectMapper = a_objectMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments a_args) {
        if (chapterRepository.count() == 0) {
            seedGsr(read("/catalog/gsr_data.json"));
        }
        if (sectionRepository.count() == 0) {
            seedMr(read("/catalog/mr_catalog.json"));
        }
    }

    private void seedGsr(JsonNode a_root) {
        int standardOrder = 1;
        for (JsonNode node : a_root) {
            GsrChapter chapter = chapter(node.get("cat").asText());
            GsrStandard standard = new GsrStandard();
            standard.setChapter(chapter);
            standard.setCode(node.get("gsr").asText());
            standard.setTitle(node.get("title").asText());
            standard.setHandbookRef(node.get("ref").asText());
            standard.setIntent(node.get("std_title").asText());
            standard.setSortOrder(standardOrder++);
            standardRepository.save(standard);
            saveElements(standard, node.get("eoc"));
        }
    }

    private GsrChapter chapter(String a_name) {
        return chapterRepository.findAll().stream()
                .filter(a_chapter -> a_name.equals(a_chapter.getName()))
                .findFirst()
                .orElseGet(() -> saveChapter(a_name));
    }

    private GsrChapter saveChapter(String a_name) {
        GsrChapter chapter = new GsrChapter();
        chapter.setName(a_name);
        chapter.setSortOrder((int) chapterRepository.count() + 1);
        return chapterRepository.save(chapter);
    }

    private void saveElements(GsrStandard a_standard, JsonNode a_elements) {
        int order = 1;
        for (JsonNode element : a_elements) {
            GsrElement row = new GsrElement();
            row.setStandard(a_standard);
            row.setSortOrder(order++);
            row.setBody(element.asText());
            elementRepository.save(row);
        }
    }

    private void seedMr(JsonNode a_root) {
        int sectionOrder = 1;
        for (JsonNode sectionNode : a_root) {
            MrSection section = new MrSection();
            section.setName(sectionNode.get("name").asText());
            section.setSortOrder(sectionOrder++);
            sectionRepository.save(section);
            saveItems(section, sectionNode.get("items"));
        }
    }

    private void saveItems(MrSection a_section, JsonNode a_items) {
        int order = 1;
        for (JsonNode item : a_items) {
            MrItem row = new MrItem();
            row.setSection(a_section);
            row.setStandardCode(item.get("std").asText());
            row.setEocNo(item.get("eoc").asText());
            row.setBody(item.get("text").asText());
            row.setSortOrder(order++);
            itemRepository.save(row);
        }
    }

    private JsonNode read(String a_path) {
        try (InputStream in = new ClassPathResource(a_path).getInputStream()) {
            return objectMapper.readTree(in);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read " + a_path, ex);
        }
    }
}
