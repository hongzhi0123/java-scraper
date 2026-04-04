package com.example.scraping;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.scraping.cache.NoOpCache;
import com.example.scraping.cache.PlaywrightFetcher;
import com.example.scraping.config.DynamicConfig;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * Demonstrates the difference between Jsoup (static HTTP) and Playwright (JS-rendered).
 *
 * WireMock serves two endpoints:
 *   GET /           → HTML shell with empty #app and a <script> that fetches /api/products
 *   GET /api/products → JSON array of products
 *
 * The browser executes the script, fills the DOM, and Playwright captures the result.
 * Jsoup fetches only the raw HTML — it never runs the script, so it sees an empty #app.
 */
public class DynamicPageScrapingTest {

    private WireMockServer wireMock;
    private String baseUrl;

    // HTML page: starts empty, JS fetch fills in product cards
    private static final String PAGE_HTML = """
            <!DOCTYPE html>
            <html>
            <body>
              <div id="app"></div>
              <script>
                fetch('/api/products')
                  .then(r => r.json())
                  .then(data => {
                    document.getElementById('app').innerHTML = data.products
                      .map(p =>
                        '<div class="product-card">' +
                          '<span class="id">' + p.id + '</span>' +
                          '<h3 class="title"><a href="' + p.url + '">' + p.name + '</a></h3>' +
                          '<div class="price">' + p.price + '</div>' +
                          '<span class="category">' + p.category + '</span>' +
                        '</div>'
                      ).join('');
                  });
              </script>
            </body>
            </html>
            """;

    private static final String PRODUCTS_JSON = """
            {
              "products": [
                {"id": "D001", "name": "Tablet",     "url": "/d001", "price": "$499", "category": "Tech"},
                {"id": "D002", "name": "Headphones", "url": "/d002", "price": "$149", "category": "Audio"}
              ]
            }
            """;

    @BeforeEach
    void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        baseUrl = "http://localhost:" + wireMock.port();

        wireMock.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html; charset=utf-8")
                        .withBody(PAGE_HTML)));

        wireMock.stubFor(get(urlEqualTo("/api/products"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PRODUCTS_JSON)));
    }

    @AfterEach
    void stopWireMock() {
        wireMock.stop();
    }

    // -----------------------------------------------------------------------
    // Test 1: proves Jsoup CANNOT handle dynamic content
    // -----------------------------------------------------------------------

    @Test
    void jsoup_cannotSeeJavaScriptRenderedContent() throws IOException {
        Document doc = Jsoup.connect(baseUrl + "/").get();

        // #app is empty because Jsoup never executed the <script>
        assertTrue(doc.select("div.product-card").isEmpty(),
                "Jsoup should find no product cards — JS was never executed");
        assertEquals("", doc.select("#app").text());
    }

    // -----------------------------------------------------------------------
    // Test 2: proves Playwright CAN handle dynamic content
    // -----------------------------------------------------------------------

    @Test
    void playwright_seesJavaScriptRenderedContent() throws IOException {
        DynamicConfig cfg = new DynamicConfig();
        cfg.setWaitForSelector("div.product-card"); // wait until JS has injected the cards

        try (PlaywrightFetcher fetcher = new PlaywrightFetcher(new NoOpCache(), null, cfg)) {
            Document doc = fetcher.getDocument(baseUrl + "/");

            List<org.jsoup.nodes.Element> cards = doc.select("div.product-card");
            assertEquals(2, cards.size(), "Playwright should find both JS-rendered cards");

            assertEquals("D001",        cards.get(0).select("span.id").text());
            assertEquals("Tablet",      cards.get(0).select("h3.title a").text());
            assertEquals("$499",        cards.get(0).select("div.price").text());
            assertEquals("Tech",        cards.get(0).select("span.category").text());

            assertEquals("D002",        cards.get(1).select("span.id").text());
            assertEquals("Headphones",  cards.get(1).select("h3.title a").text());
            assertEquals("$149",        cards.get(1).select("div.price").text());
            assertEquals("Audio",       cards.get(1).select("span.category").text());
        }
    }

    // -----------------------------------------------------------------------
    // Test 3: verifies WireMock recorded the API call the browser made
    // -----------------------------------------------------------------------

    @Test
    void playwright_triggersApiCallToBackend() throws IOException {
        DynamicConfig cfg = new DynamicConfig();
        cfg.setWaitForSelector("div.product-card");

        try (PlaywrightFetcher fetcher = new PlaywrightFetcher(new NoOpCache(), null, cfg)) {
            fetcher.getDocument(baseUrl + "/");
        }

        // WireMock verifies the browser actually fetched the JSON endpoint
        wireMock.verify(1, getRequestedFor(urlEqualTo("/api/products")));
    }

    // -----------------------------------------------------------------------
    // Test 4: caching — second call must not hit WireMock again
    // -----------------------------------------------------------------------

    @Test
    void playwright_secondCallServedFromCache() throws IOException {
        // Use an in-memory stub cache
        java.util.Map<String, String> store = new java.util.HashMap<>();
        com.example.scraping.cache.ResponseCache cache = new com.example.scraping.cache.ResponseCache() {
            public boolean isEnabled() { return true; }
            public String get(String url) { return store.get(url); }
            public void put(String url, String html) { store.put(url, html); }
        };

        DynamicConfig cfg = new DynamicConfig();
        cfg.setWaitForSelector("div.product-card");

        try (PlaywrightFetcher fetcher = new PlaywrightFetcher(cache, null, cfg)) {
            fetcher.getDocument(baseUrl + "/"); // first: hits WireMock
            fetcher.getDocument(baseUrl + "/"); // second: served from cache
        }

        // WireMock should only have received a single request for the page
        wireMock.verify(1, getRequestedFor(urlEqualTo("/")));
    }
}
