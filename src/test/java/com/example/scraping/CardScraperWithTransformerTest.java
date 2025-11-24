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
import com.example.scraping.scraper.CardScraper;
import com.example.scraping.utils.TestConfigLoader;

public class CardScraperWithTransformerTest {
    @Test
    public void testEndToEndWithCardConfig() throws IOException {
        String html = new String(Files.readAllBytes(
                Paths.get("src/test/resources/fixtures/transform.html")));
        Document doc = Jsoup.parse(html, "https://example.com"); // ← baseUri for abs:href        
        ScrapeConfig config = TestConfigLoader.loadConfig("config-card-transformers.json");

        CardScraper cardScraper = new CardScraper();
        List<Map<String, String>> results = cardScraper.scrapeCards(doc, config.getMainCard());

        assertEquals(2, results.size());
        var first = results.get(0);
        assertEquals("LAP", first.get("type")); //  "SKU: LAP-001"
        assertEquals("001", first.get("id")); //  "SKU: LAP-001"
        assertEquals("laptop-pro-15", first.get("titleSlug")); // "Laptop Pro 15"
        assertEquals("1299.99", first.get("price")); // " $1,299.99 "
        assertEquals("electronics", first.get("tags")); // "Electronics"
        assertEquals("Electronics", first.get("category")); // "Electronics"

        var second = results.get(1);
        assertEquals("MUS", second.get("type")); //  "SKU: MUS-888"
        assertEquals("888", second.get("id")); //  "SKU: MUS-888"
        assertEquals("gaming-mouse", second.get("titleSlug")); // "Gaming Mouse"
        assertEquals("45.0", second.get("price")); // "$45"
        assertEquals("accessories", second.get("tags")); // "accessories"
        assertEquals("Uncategorized", second.get("category")); // <!-- no category -->
    }    
}
