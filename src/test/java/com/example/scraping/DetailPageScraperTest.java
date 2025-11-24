package com.example.scraping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import com.example.scraping.config.ScrapeConfig;
import com.example.scraping.scraper.DetailPageScraper;
import com.example.scraping.utils.TestConfigLoader;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DetailPageScraperTest {

    @Test
    public void testScrapeDetailPageWithSubTable() throws IOException {
        String html = new String(Files.readAllBytes(
                Paths.get("src/test/resources/fixtures/detail_p001.html")));
        Document doc = Jsoup.parse(html);

        // Load config from src/test/resources/config-table.json
        ScrapeConfig config = TestConfigLoader.loadConfig("config-table.json");

        DetailPageScraper scraper = new DetailPageScraper();
        Map<String, String> result = scraper.scrapeDetailPage(doc, config.getDetailPage());

        assertEquals("Powerful laptop for developers.", result.get("description"));
        assertEquals("TechCorp", result.get("manufacturer"));
        assertEquals("", result.get("inStock")); // empty string preserved
        assertEquals("1299.99", result.get("price"));

        // Parse sub-table JSON
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, String>> subTableData = mapper.readValue(
                result.get("__subTable"), new TypeReference<>() {});

        assertEquals(2, subTableData.size());

        var firstRow = subTableData.get(0);
        assertEquals("Weight", firstRow.get("specName"));
        assertEquals("1.5 kg", firstRow.get("specValue"));
    }
}