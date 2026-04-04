package com.example.scraping;

import com.example.scraping.cache.PlaywrightFetcher;
import com.example.scraping.cache.ResponseCache;
import com.example.scraping.config.DynamicConfig;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PlaywrightFetcherTest {

    // ---------- in-memory stub cache ----------

    static class StubCache implements ResponseCache {
        private final Map<String, String> store = new HashMap<>();

        @Override public boolean isEnabled() { return true; }
        @Override public String get(String url) { return store.get(url); }
        @Override public void put(String url, String html) { store.put(url, html); }
    }

    // ---------- helpers ----------

    private static String fixtureUrl(String filename) {
        return Paths.get("src/test/resources/fixtures/" + filename).toAbsolutePath().toUri().toString();
    }

    // ---------- tests ----------

    @Test
    void cacheHit_returnsDocumentWithoutLaunchingBrowser() throws IOException {
        StubCache cache = new StubCache();
        cache.put("https://example.com/page", "<html><body><p class='msg'>cached</p></body></html>");

        // No browser is launched because the cache has a hit
        try (PlaywrightFetcher fetcher = new PlaywrightFetcher(cache, null, null)) {
            Document doc = fetcher.getDocument("https://example.com/page");
            assertEquals("cached", doc.select("p.msg").text());
        }
    }

    @Test
    void cacheMiss_fetchesLocalFileAndReturnsDocument() throws IOException {
        StubCache cache = new StubCache();
        String url = fixtureUrl("dynamic.html");

        try (PlaywrightFetcher fetcher = new PlaywrightFetcher(cache, null, null)) {
            Document doc = fetcher.getDocument(url);
            assertEquals(2, doc.select("div.product-card").size());
            assertEquals("Tablet", doc.select("div.product-card:first-child h3.title a").text());
        }
    }

    @Test
    void cacheMiss_htmlStoredInCacheAfterFetch() throws IOException {
        StubCache cache = new StubCache();
        String url = fixtureUrl("dynamic.html");
        assertNull(cache.get(url), "cache should be empty before fetch");

        try (PlaywrightFetcher fetcher = new PlaywrightFetcher(cache, null, null)) {
            fetcher.getDocument(url);
        }

        assertNotNull(cache.get(url), "cache should contain html after fetch");
        assertTrue(cache.get(url).contains("product-card"));
    }

    @Test
    void cacheHit_secondCallReturnsCachedDocument() throws IOException {
        StubCache cache = new StubCache();
        String url = fixtureUrl("dynamic.html");

        try (PlaywrightFetcher fetcher = new PlaywrightFetcher(cache, null, null)) {
            // First call hits the browser and populates the cache
            fetcher.getDocument(url);
            // Second call must return from cache (same result, no additional browser page)
            Document doc = fetcher.getDocument(url);
            assertEquals(2, doc.select("div.product-card").size());
        }
    }

    @Test
    void waitForSelector_waitsForElementAndReturnsDocument() throws IOException {
        StubCache cache = new StubCache();
        String url = fixtureUrl("dynamic.html");

        DynamicConfig dynamicConfig = new DynamicConfig();
        dynamicConfig.setWaitForSelector("div.product-card");

        try (PlaywrightFetcher fetcher = new PlaywrightFetcher(cache, null, dynamicConfig)) {
            Document doc = fetcher.getDocument(url);
            assertFalse(doc.select("div.product-card").isEmpty());
            assertEquals("$499", doc.select("div.product-card:first-child div.price").text());
        }
    }

    @Test
    void networkIdle_usedWhenNoSelectorConfigured() throws IOException {
        StubCache cache = new StubCache();
        String url = fixtureUrl("dynamic.html");

        DynamicConfig dynamicConfig = new DynamicConfig(); // no waitForSelector → NETWORKIDLE

        try (PlaywrightFetcher fetcher = new PlaywrightFetcher(cache, null, dynamicConfig)) {
            Document doc = fetcher.getDocument(url);
            assertEquals(2, doc.select("div.product-card").size());
        }
    }

    @Test
    void close_canBeCalledMultipleTimes() {
        PlaywrightFetcher fetcher = new PlaywrightFetcher(new StubCache(), null, null);
        assertDoesNotThrow(() -> {
            fetcher.close();
            fetcher.close(); // second close must not throw
        });
    }
}
