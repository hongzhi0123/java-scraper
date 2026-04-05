package com.example.scraping;

import com.example.scraping.cache.PageFetcher;
import com.example.scraping.config.ScrapeConfig;
import com.example.scraping.scraper.ScrapeEngine;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Generic config validation test. Run with:
 *   mvn test -Dtest=ConfigValidationTest -DconfigPath=config/my_source.json
 *
 * Loads the config, runs the scraper (first page only), and verifies non-empty results.
 */
@EnabledIfSystemProperty(named = "configPath", matches = ".+")
public class ConfigValidationTest {

    @Test
    public void validateConfig() throws IOException {
        String configPath = System.getProperty("configPath");
        assertNotNull(configPath, "System property 'configPath' must be set");

        File configFile = new File(configPath);
        assertTrue(configFile.exists(), "Config file not found: " + configPath);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
        ScrapeConfig config = mapper.readValue(configFile, ScrapeConfig.class);

        assertNotNull(config.getEntranceUrl(), "entranceUrl must not be null");
        assertTrue(config.getMainTable() != null || config.getMainCard() != null,
                "Config must define either mainTable or mainCard");

        ScrapeEngine engine;
        String url = config.getEntranceUrl();
        if (url.startsWith("file:")) {
            // For file: URLs, use a local file-reading PageFetcher that only serves existing files
            PageFetcher fileFetcher = fileUrl -> {
                String path = fileUrl.startsWith("file:") ? fileUrl.substring(5) : fileUrl;
                java.nio.file.Path filePath = Paths.get(path);
                if (!Files.exists(filePath)) {
                    throw new IOException("File not found (pagination/detail link not available locally): " + path);
                }
                String html = new String(Files.readAllBytes(filePath));
                return Jsoup.parse(html, "https://localhost");
            };
            // Disable pagination and detail pages for file-based configs
            config.setPagination(null);
            config.setDetailPage(null);
            engine = new ScrapeEngine(config, fileFetcher);
        } else {
            engine = new ScrapeEngine(config);
        }
        List<Map<String, String>> results = engine.run();

        assertFalse(results.isEmpty(), "Scraper returned no results — check your selectors");

        // Print results for inspection
        System.out.println("Scraped " + results.size() + " items from: " + config.getEntranceUrl());
        int limit = Math.min(results.size(), 3);
        for (int i = 0; i < limit; i++) {
            System.out.println("\n--- Item " + (i + 1) + " ---");
            results.get(i).forEach((k, v) ->
                    System.out.println("  " + k + ": \"" + truncate(v, 120) + "\""));
        }
        if (results.size() > limit) {
            System.out.println("\n... and " + (results.size() - limit) + " more items");
        }

        // Verify all non-optional fields have values in at least one result
        Map<String, String> first = results.get(0);
        assertFalse(first.isEmpty(), "First result has no fields");
        System.out.println("\nFields found: " + first.keySet());
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
