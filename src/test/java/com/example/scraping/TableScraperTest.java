package com.example.scraping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import com.example.scraping.config.ScrapeConfig;
import com.example.scraping.scraper.TableScraper;
import com.example.scraping.utils.TestConfigLoader;

public class TableScraperTest {

    @Test
    public void testScrapeNoHeaderWithNegativeIndex() throws IOException {
        String html = new String(Files.readAllBytes(
                Paths.get("src/test/resources/fixtures/entrance.html")));
        Document doc = Jsoup.parse(html);
        doc.setBaseUri("https://example.com"); // critical for abs:href

        // Load config from src/test/resources/config-table.json
        ScrapeConfig config = TestConfigLoader.loadConfig("config-table.json");

        TableScraper scraper = new TableScraper();
        List<Map<String, String>> results = scraper.scrapeTable(doc, config.getMainTable());

        assertEquals(2, results.size());
        Map<String, String> first = results.get(0);
        assertEquals("P001", first.get("id"));
        assertEquals("Laptop", first.get("name"));
        assertEquals("$999", first.get("price"));
        assertEquals("Laptop", first.get("product"));
        assertTrue(first.containsKey("productUrl"));
        assertTrue(first.get("productUrl").endsWith("/product/001"));
    }
}