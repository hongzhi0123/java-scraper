package com.example.scraping;

import com.example.scraping.cache.PageFetcher;
import com.example.scraping.config.DynamicConfig;
import com.example.scraping.config.ScrapeConfig;
import com.example.scraping.scraper.ScrapeEngine;
import com.example.scraping.utils.TestConfigLoader;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ScrapeEngineDynamicTest {

    private static String fixtureUrl(String filename) {
        return Paths.get("src/test/resources/fixtures/" + filename).toAbsolutePath().toUri().toString();
    }

    // ---------- unit tests: stub fetcher, no browser ----------

    @Test
    void dynamicMode_stubFetcher_scrapesTwoCards() throws IOException {
        String html = "<html><body>"
                + "<div class='product-card'><span class='id'>D001</span>"
                + "<h3 class='title'><a href='/d001'>Tablet</a></h3>"
                + "<div class='price'>$499</div>"
                + "<span class='category'>Tech</span></div>"
                + "<div class='product-card'><span class='id'>D002</span>"
                + "<h3 class='title'><a href='/d002'>Headphones</a></h3>"
                + "<div class='price'>$149</div>"
                + "<span class='category'>Audio</span></div>"
                + "</body></html>";

        PageFetcher stub = url -> Jsoup.parse(html, url);

        ScrapeConfig config = TestConfigLoader.loadConfig("config-dynamic.json");
        config.setEntranceUrl("https://example.com/list");

        ScrapeEngine engine = new ScrapeEngine(config, stub);
        List<Map<String, String>> results = engine.run();

        assertEquals(2, results.size());
        assertEquals("D001", results.get(0).get("id"));
        assertEquals("Tablet", results.get(0).get("name"));
        assertEquals("$499", results.get(0).get("price"));
        assertEquals("Tech", results.get(0).get("category"));
        assertEquals("D002", results.get(1).get("id"));
        assertEquals("Headphones", results.get(1).get("name"));
    }

    @Test
    void dynamicMode_stubFetcher_missingOptionalFieldIsEmpty() throws IOException {
        String html = "<html><body>"
                + "<div class='product-card'><span class='id'>D003</span>"
                + "<h3 class='title'><a href='/d003'>Keyboard</a></h3>"
                + "<div class='price'>$89</div>"
                + "<!-- no category --></div>"
                + "</body></html>";

        PageFetcher stub = url -> Jsoup.parse(html, url);

        ScrapeConfig config = TestConfigLoader.loadConfig("config-dynamic.json");
        config.setEntranceUrl("https://example.com/list");

        ScrapeEngine engine = new ScrapeEngine(config, stub);
        List<Map<String, String>> results = engine.run();

        assertEquals(1, results.size());
        assertEquals("", results.get(0).get("category")); // optional → empty string
    }

    @Test
    void dynamicMode_stubFetcher_paginationFollowsNextPage() throws IOException {
        String page1 = "<html><body>"
                + "<div class='product-card'><span class='id'>P1</span>"
                + "<h3 class='title'><a href='/p1'>Item1</a></h3>"
                + "<div class='price'>$10</div></div>"
                + "<a href='https://example.com/page2' class='next'>Next</a>"
                + "</body></html>";

        String page2 = "<html><body>"
                + "<div class='product-card'><span class='id'>P2</span>"
                + "<h3 class='title'><a href='/p2'>Item2</a></h3>"
                + "<div class='price'>$20</div></div>"
                + "</body></html>";

        PageFetcher stub = url -> {
            if (url.contains("page2")) return Jsoup.parse(page2, "https://example.com/page2");
            return Jsoup.parse(page1, "https://example.com/page1");
        };

        ScrapeConfig config = TestConfigLoader.loadConfig("config-dynamic.json");
        config.setEntranceUrl("https://example.com/page1");

        // Add pagination so the engine follows the "next" link
        com.example.scraping.config.PaginationDefinition pagination = new com.example.scraping.config.PaginationDefinition();
        pagination.setNextButtonSelector("a.next");
        config.setPagination(pagination);

        ScrapeEngine engine = new ScrapeEngine(config, stub);
        List<Map<String, String>> results = engine.run();

        assertEquals(2, results.size());
        assertEquals("P1", results.get(0).get("id"));
        assertEquals("P2", results.get(1).get("id"));
    }

    // ---------- integration test: real Playwright browser ----------

    @Test
    void dynamicMode_realBrowser_scrapesDynamicFixture() throws IOException {
        ScrapeConfig config = TestConfigLoader.loadConfig("config-dynamic.json");
        config.setEntranceUrl(fixtureUrl("dynamic.html"));

        DynamicConfig dynConfig = new DynamicConfig();
        dynConfig.setWaitForSelector("div.product-card");
        config.setDynamicConfig(dynConfig);

        ScrapeEngine engine = new ScrapeEngine(config);
        List<Map<String, String>> results = engine.run();

        assertEquals(2, results.size());

        Map<String, String> first = results.get(0);
        assertEquals("D001", first.get("id"));
        assertEquals("Tablet", first.get("name"));
        assertEquals("$499", first.get("price"));
        assertEquals("Tech", first.get("category"));

        Map<String, String> second = results.get(1);
        assertEquals("D002", second.get("id"));
        assertEquals("Headphones", second.get("name"));
        assertEquals("$149", second.get("price"));
        assertEquals("Audio", second.get("category"));
    }
}
