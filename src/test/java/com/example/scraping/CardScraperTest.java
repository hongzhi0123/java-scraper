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

public class CardScraperTest {

    @Test
    public void testScrapeCardLayout() throws IOException {
        String html = new String(Files.readAllBytes(
                Paths.get("src/test/resources/fixtures/cards.html")));
        Document doc = Jsoup.parse(html, "https://example.com"); // ← baseUri for abs:href

        // Load config from src/test/resources/config.json
        ScrapeConfig config = TestConfigLoader.loadConfig("config-card.json");

        CardScraper scraper = new CardScraper();
        List<Map<String, String>> results = scraper.scrapeCards(doc, config.getMainCard());

        assertEquals(2, results.size());

        Map<String, String> laptop = results.get(0);
        assertEquals("P001", laptop.get("id"));
        assertEquals("Laptop", laptop.get("name"));
        assertEquals("https://example.com/product/001", laptop.get("nameUrl")); // abs:href
        assertEquals("$999", laptop.get("price"));
        assertEquals("Electronics", laptop.get("category"));

        Map<String, String> mouse = results.get(1);
        assertEquals("P002", mouse.get("id"));
        assertEquals("Mouse", mouse.get("name"));
        assertEquals("", mouse.get("category")); // optional → empty string
    }

    @Test
    public void testEndToEndWithCardConfig() throws IOException {
        String html = new String(Files.readAllBytes(
                Paths.get("src/test/resources/fixtures/cards.html")));
        Document doc = Jsoup.parse(html, "https://example.com"); // ← baseUri for abs:href        
        ScrapeConfig config = TestConfigLoader.loadConfig("config-card.json");

        CardScraper cardScraper = new CardScraper();
        List<Map<String, String>> results = cardScraper.scrapeCards(doc, config.getMainCard());

        assertEquals(2, results.size());
        assertTrue(results.get(0).containsKey("nameUrl"));
    }
}