package com.example.scraping.scraper;

import com.example.scraping.config.DetailPageDefinition;
import com.example.scraping.config.FieldDefinition;
import com.example.scraping.config.TransformerDef;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

public class DetailPageScraper {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, String> scrapeDetailPage(Document doc, DetailPageDefinition def) {
        Map<String, String> data = new LinkedHashMap<>();

        // Scrape fields
        if (def.getFields() != null) {
            for (FieldDefinition field : def.getFields()) {
                Element el = doc.selectFirst(field.getSelector());
                String value = (el != null) ? el.text().trim() : "";

                if (field.getTransformers() != null) {
                    for (TransformerDef t : field.getTransformers()) {
                        value = t.apply(value);
                    }
                }

                data.put(field.getKey(), value);
            }
        }

        // Scrape sub-table
        if (def.getSubTable() != null) {
            TableScraper tableScraper = new TableScraper();
            List<Map<String, String>> subRows = tableScraper.scrapeTable(doc, def.getSubTable());
            try {
                data.put("__subTable", objectMapper.writeValueAsString(subRows));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize sub-table", e);
            }
        }

        return data;
    }
}