package com.example.scraping;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.scraping.cache.NoOpCache;
import com.example.scraping.cache.PageFetcher;
import com.example.scraping.cache.PlaywrightFetcher;
import com.example.scraping.config.DynamicConfig;
import com.example.scraping.config.ScrapeConfig;
import com.example.scraping.scraper.ScrapeEngine;
import com.example.scraping.utils.TestConfigLoader;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * Tests the config-dnb-register.json configuration against the DNB public register page structure.
 *
 * WireMock serves:
 *   GET /en/public-register/?p=1&l=20  → HTML shell + JS that fetches /api/register?page=1
 *   GET /en/public-register/?p=2&l=20  → HTML shell + JS that fetches /api/register?page=2
 *   GET /api/register?page=1           → JSON with page 1 register data
 *   GET /api/register?page=2           → JSON with page 2 register data
 *
 * The JS renders article.register-result cards and pagination matching the real DNB structure.
 * Playwright waits for ul.register-search__results__list, then ScrapeEngine extracts data.
 */
public class DnbRegisterScraperTest {

    private WireMockServer wireMock;
    private String baseUrl;

    /**
     * HTML shell: fetches register data from API and builds the DOM structure
     * that matches config-dnb-register.json selectors.
     * The {PAGE_NUM} placeholder is replaced per page.
     */
    private static final String PAGE_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="en">
            <body>
              <div id="app"></div>
              <script>
                const currentPage = {PAGE_NUM};
                fetch('/api/register?page=' + currentPage)
                  .then(r => r.json())
                  .then(data => {
                    let html = '<ul class="register-search__results__list">';
                    data.items.forEach(item => {
                      html += '<li class="register-search__results__listitem">'
                        + '<article class="register-result">'
                        + '<div class="register-result__statutory-name">'
                        + '  <h3 class="register-result__title">'
                        + '    <a href="' + item.detailUrl + '">' + item.statutoryName + '</a>'
                        + '  </h3>'
                        + '</div>'
                        + '<div class="register-result__trade-name">'
                        + '  <p class="register-result__value">' + (item.tradeName || '') + '</p>'
                        + '</div>'
                        + '<div class="register-result__register">'
                        + '  <p class="register-result__value">' + item.register + '</p>'
                        + '</div>'
                        + '<div class="register-result__country">'
                        + '  <p class="register-result__value">' + item.country + '</p>'
                        + '</div>'
                        + '</article></li>';
                    });
                    html += '</ul>';

                    // Pagination
                    html += '<nav class="pagination-wrapper"><ul class="pagination">';
                    for (let i = 1; i <= data.totalPages; i++) {
                      const activeClass = (i === currentPage) ? ' pagination__item--is-active' : '';
                      html += '<li class="pagination__item' + activeClass + '">'
                        + '<button type="button" class="pagination__link">' + i + '</button></li>';
                    }
                    if (currentPage < data.totalPages) {
                      html += '<li class="pagination__item pagination__item--next-page">'
                        + '<button type="button" class="pagination__link">Next page</button></li>';
                    }
                    html += '</ul></nav>';

                    document.getElementById('app').innerHTML = html;
                  });
              </script>
            </body>
            </html>
            """;

    private static final String PAGE1_JSON = """
            {
              "totalPages": 2,
              "items": [
                {
                  "statutoryName": "\\"Easy Payment Services\\" OOD",
                  "tradeName": "\\"Easy Payment Services\\" OOD",
                  "register": "Electronic money institutions",
                  "country": "BULGARIJE",
                  "detailUrl": "/en/public-register/information-detail/?registerCode=WFTEG&relationNumber=R135587"
                },
                {
                  "statutoryName": "ABN AMRO Bank N.V.",
                  "tradeName": "ABN AMRO",
                  "register": "Banks",
                  "country": "NEDERLAND",
                  "detailUrl": "/en/public-register/information-detail/?registerCode=BANK&relationNumber=R100123"
                },
                {
                  "statutoryName": "Adyen N.V.",
                  "tradeName": "",
                  "register": "Electronic money institutions",
                  "country": "NEDERLAND",
                  "detailUrl": "/en/public-register/information-detail/?registerCode=WFTEG&relationNumber=R150456"
                }
              ]
            }
            """;

    private static final String PAGE2_JSON = """
            {
              "totalPages": 2,
              "items": [
                {
                  "statutoryName": "Bunq B.V.",
                  "tradeName": "bunq",
                  "register": "Banks",
                  "country": "NEDERLAND",
                  "detailUrl": "/en/public-register/information-detail/?registerCode=BANK&relationNumber=R200789"
                },
                {
                  "statutoryName": "ING Bank N.V.",
                  "tradeName": "ING",
                  "register": "Banks",
                  "country": "NEDERLAND",
                  "detailUrl": "/en/public-register/information-detail/?registerCode=BANK&relationNumber=R100456"
                }
              ]
            }
            """;

    @BeforeEach
    void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        baseUrl = "http://localhost:" + wireMock.port();

        // Page 1 HTML shell
        wireMock.stubFor(get(urlEqualTo("/en/public-register/?p=1&l=20"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html; charset=utf-8")
                        .withBody(PAGE_TEMPLATE.replace("{PAGE_NUM}", "1"))));

        // Page 2 HTML shell
        wireMock.stubFor(get(urlEqualTo("/en/public-register/?p=2&l=20"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html; charset=utf-8")
                        .withBody(PAGE_TEMPLATE.replace("{PAGE_NUM}", "2"))));

        // API endpoint: page 1 data
        wireMock.stubFor(get(urlEqualTo("/api/register?page=1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PAGE1_JSON)));

        // API endpoint: page 2 data
        wireMock.stubFor(get(urlEqualTo("/api/register?page=2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PAGE2_JSON)));
    }

    @AfterEach
    void stopWireMock() {
        wireMock.stop();
    }

    // -----------------------------------------------------------------------
    // Test 1: Jsoup cannot see dynamically rendered register data
    // -----------------------------------------------------------------------

    @Test
    void jsoup_cannotSeeDynamicRegisterContent() throws IOException {
        org.jsoup.nodes.Document doc = Jsoup.connect(baseUrl + "/en/public-register/?p=1&l=20").get();

        assertTrue(doc.select("article.register-result").isEmpty(),
                "Jsoup should find no register results — JS was never executed");
        assertTrue(doc.select("ul.register-search__results__list").isEmpty());
    }

    // -----------------------------------------------------------------------
    // Test 2: Playwright renders page 1 and ScrapeEngine extracts 3 items
    // -----------------------------------------------------------------------

    @Test
    void playwright_scrapesPage1WithConfig() throws IOException {
        ScrapeConfig config = TestConfigLoader.loadConfig("config-dnb-register.json");
        // Point to WireMock instead of real DNB
        config.setEntranceUrl(baseUrl + "/en/public-register/?p=1&l=20");
        // Override pagination URL pattern to use WireMock base
        config.getPagination().setUrlPattern(baseUrl + "/en/public-register/?p={page}&l=20");
        // Disable pagination so we only scrape page 1
        config.setPagination(null);

        DynamicConfig dynConfig = new DynamicConfig();
        dynConfig.setWaitForSelector("ul.register-search__results__list");
        dynConfig.setTimeout(15000);
        config.setDynamicConfig(dynConfig);

        ScrapeEngine engine = new ScrapeEngine(config);
        List<Map<String, String>> results = engine.run();

        assertEquals(3, results.size(), "Page 1 should have 3 register entries");

        // First entry
        assertEquals("\"Easy Payment Services\" OOD", results.get(0).get("statutoryName"));
        assertEquals("\"Easy Payment Services\" OOD", results.get(0).get("tradeName"));
        assertEquals("Electronic money institutions", results.get(0).get("register"));
        assertEquals("BULGARIJE", results.get(0).get("country"));

        // Second entry
        assertEquals("ABN AMRO Bank N.V.", results.get(1).get("statutoryName"));
        assertEquals("ABN AMRO", results.get(1).get("tradeName"));
        assertEquals("Banks", results.get(1).get("register"));
        assertEquals("NEDERLAND", results.get(1).get("country"));

        // Third entry (no trade name)
        assertEquals("Adyen N.V.", results.get(2).get("statutoryName"));
        assertEquals("", results.get(2).get("tradeName"));
        assertEquals("Electronic money institutions", results.get(2).get("register"));
        assertEquals("NEDERLAND", results.get(2).get("country"));
    }

    // -----------------------------------------------------------------------
    // Test 3: Playwright + pagination scrapes both pages (5 items total)
    // -----------------------------------------------------------------------

    @Test
    void playwright_scrapesMultiplePagesWithPagination() throws IOException {
        ScrapeConfig config = TestConfigLoader.loadConfig("config-dnb-register.json");
        config.setEntranceUrl(baseUrl + "/en/public-register/?p=1&l=20");
        config.getPagination().setUrlPattern(baseUrl + "/en/public-register/?p={page}&l=20");

        DynamicConfig dynConfig = new DynamicConfig();
        dynConfig.setWaitForSelector("ul.register-search__results__list");
        dynConfig.setTimeout(15000);
        config.setDynamicConfig(dynConfig);

        ScrapeEngine engine = new ScrapeEngine(config);
        List<Map<String, String>> results = engine.run();

        assertEquals(5, results.size(), "Should have 3 from page 1 + 2 from page 2");

        // Verify page 1 items
        assertEquals("\"Easy Payment Services\" OOD", results.get(0).get("statutoryName"));
        assertEquals("ABN AMRO Bank N.V.", results.get(1).get("statutoryName"));
        assertEquals("Adyen N.V.", results.get(2).get("statutoryName"));

        // Verify page 2 items
        assertEquals("Bunq B.V.", results.get(3).get("statutoryName"));
        assertEquals("bunq", results.get(3).get("tradeName"));
        assertEquals("Banks", results.get(3).get("register"));
        assertEquals("NEDERLAND", results.get(3).get("country"));

        assertEquals("ING Bank N.V.", results.get(4).get("statutoryName"));
        assertEquals("ING", results.get(4).get("tradeName"));
        assertEquals("Banks", results.get(4).get("register"));
        assertEquals("NEDERLAND", results.get(4).get("country"));
    }

    // -----------------------------------------------------------------------
    // Test 4: WireMock verifies the browser made API calls
    // -----------------------------------------------------------------------

    @Test
    void playwright_triggersApiCallsToBackend() throws IOException {
        DynamicConfig cfg = new DynamicConfig();
        cfg.setWaitForSelector("ul.register-search__results__list");
        cfg.setTimeout(15000);

        try (PlaywrightFetcher fetcher = new PlaywrightFetcher(new NoOpCache(), null, cfg)) {
            fetcher.getDocument(baseUrl + "/en/public-register/?p=1&l=20");
        }

        wireMock.verify(1, getRequestedFor(urlEqualTo("/en/public-register/?p=1&l=20")));
        wireMock.verify(1, getRequestedFor(urlEqualTo("/api/register?page=1")));
    }

    // -----------------------------------------------------------------------
    // Test 5: Unit test with stub fetcher (no browser needed) — page 1 only
    // -----------------------------------------------------------------------

    @Test
    void stubFetcher_scrapesPreRenderedHtml() throws IOException {
        // Pre-rendered HTML matching the DNB register structure
        String html = """
                <html><body>
                <ul class="register-search__results__list">
                  <li><article class="register-result">
                    <div class="register-result__statutory-name">
                      <h3 class="register-result__title">
                        <a href="/detail/1">"Easy Payment Services" OOD</a>
                      </h3>
                    </div>
                    <div class="register-result__trade-name">
                      <p class="register-result__value">"Easy Payment Services" OOD</p>
                    </div>
                    <div class="register-result__register">
                      <p class="register-result__value">Electronic money institutions</p>
                    </div>
                    <div class="register-result__country">
                      <p class="register-result__value">BULGARIJE</p>
                    </div>
                  </article></li>
                  <li><article class="register-result">
                    <div class="register-result__statutory-name">
                      <h3 class="register-result__title">
                        <a href="/detail/2">ABN AMRO Bank N.V.</a>
                      </h3>
                    </div>
                    <div class="register-result__trade-name">
                      <p class="register-result__value">ABN AMRO</p>
                    </div>
                    <div class="register-result__register">
                      <p class="register-result__value">Banks</p>
                    </div>
                    <div class="register-result__country">
                      <p class="register-result__value">NEDERLAND</p>
                    </div>
                  </article></li>
                </ul>
                </body></html>
                """;

        PageFetcher stub = url -> Jsoup.parse(html, url);

        ScrapeConfig config = TestConfigLoader.loadConfig("config-dnb-register.json");
        config.setEntranceUrl("https://example.com/register");
        config.setPagination(null);

        ScrapeEngine engine = new ScrapeEngine(config, stub);
        List<Map<String, String>> results = engine.run();

        assertEquals(2, results.size());
        assertEquals("\"Easy Payment Services\" OOD", results.get(0).get("statutoryName"));
        assertEquals("\"Easy Payment Services\" OOD", results.get(0).get("tradeName"));
        assertEquals("Electronic money institutions", results.get(0).get("register"));
        assertEquals("BULGARIJE", results.get(0).get("country"));

        assertEquals("ABN AMRO Bank N.V.", results.get(1).get("statutoryName"));
        assertEquals("ABN AMRO", results.get(1).get("tradeName"));
        assertEquals("Banks", results.get(1).get("register"));
        assertEquals("NEDERLAND", results.get(1).get("country"));
    }

    // -----------------------------------------------------------------------
    // Test 6: Stub fetcher with pagination across two pages
    // -----------------------------------------------------------------------

    @Test
    void stubFetcher_paginationAcrossTwoPages() throws IOException {
        String page1Html = """
                <html><body>
                <ul class="register-search__results__list">
                  <li><article class="register-result">
                    <div class="register-result__statutory-name">
                      <h3 class="register-result__title"><a href="/d/1">Bank A</a></h3>
                    </div>
                    <div class="register-result__trade-name"><p class="register-result__value">Trade A</p></div>
                    <div class="register-result__register"><p class="register-result__value">Banks</p></div>
                    <div class="register-result__country"><p class="register-result__value">NEDERLAND</p></div>
                  </article></li>
                </ul>
                <ul class="pagination">
                  <li class="pagination__item pagination__item--is-active">
                    <button type="button" class="pagination__link">1</button>
                  </li>
                  <li class="pagination__item">
                    <button type="button" class="pagination__link">2</button>
                  </li>
                  <li class="pagination__item pagination__item--next-page">
                    <button type="button" class="pagination__link">Next page</button>
                  </li>
                </ul>
                </body></html>
                """;

        String page2Html = """
                <html><body>
                <ul class="register-search__results__list">
                  <li><article class="register-result">
                    <div class="register-result__statutory-name">
                      <h3 class="register-result__title"><a href="/d/2">Bank B</a></h3>
                    </div>
                    <div class="register-result__trade-name"><p class="register-result__value">Trade B</p></div>
                    <div class="register-result__register"><p class="register-result__value">Insurers</p></div>
                    <div class="register-result__country"><p class="register-result__value">BELGIE</p></div>
                  </article></li>
                </ul>
                <ul class="pagination">
                  <li class="pagination__item">
                    <button type="button" class="pagination__link">1</button>
                  </li>
                  <li class="pagination__item pagination__item--is-active">
                    <button type="button" class="pagination__link">2</button>
                  </li>
                </ul>
                </body></html>
                """;

        PageFetcher stub = url -> {
            if (url.contains("p=2")) return Jsoup.parse(page2Html, url);
            return Jsoup.parse(page1Html, url);
        };

        ScrapeConfig config = TestConfigLoader.loadConfig("config-dnb-register.json");
        config.setEntranceUrl("https://example.com/register?p=1&l=20");
        config.getPagination().setUrlPattern("https://example.com/register?p={page}&l=20");

        ScrapeEngine engine = new ScrapeEngine(config, stub);
        List<Map<String, String>> results = engine.run();

        assertEquals(2, results.size());
        assertEquals("Bank A", results.get(0).get("statutoryName"));
        assertEquals("Trade A", results.get(0).get("tradeName"));
        assertEquals("Banks", results.get(0).get("register"));
        assertEquals("NEDERLAND", results.get(0).get("country"));

        assertEquals("Bank B", results.get(1).get("statutoryName"));
        assertEquals("Trade B", results.get(1).get("tradeName"));
        assertEquals("Insurers", results.get(1).get("register"));
        assertEquals("BELGIE", results.get(1).get("country"));
    }

    // -----------------------------------------------------------------------
    // Test 7: Optional trade name field returns empty when missing
    // -----------------------------------------------------------------------

    @Test
    void stubFetcher_missingOptionalTradeNameReturnsEmpty() throws IOException {
        String html = """
                <html><body>
                <ul class="register-search__results__list">
                  <li><article class="register-result">
                    <div class="register-result__statutory-name">
                      <h3 class="register-result__title"><a href="/d/1">Some Institution</a></h3>
                    </div>
                    <!-- trade-name div is present but value element is missing -->
                    <div class="register-result__register">
                      <p class="register-result__value">Payment service providers</p>
                    </div>
                    <div class="register-result__country">
                      <p class="register-result__value">DUITSLAND</p>
                    </div>
                  </article></li>
                </ul>
                </body></html>
                """;

        PageFetcher stub = url -> Jsoup.parse(html, url);

        ScrapeConfig config = TestConfigLoader.loadConfig("config-dnb-register.json");
        config.setEntranceUrl("https://example.com/register");
        config.setPagination(null);

        ScrapeEngine engine = new ScrapeEngine(config, stub);
        List<Map<String, String>> results = engine.run();

        assertEquals(1, results.size());
        assertEquals("Some Institution", results.get(0).get("statutoryName"));
        assertEquals("", results.get(0).get("tradeName")); // optional → empty
        assertEquals("Payment service providers", results.get(0).get("register"));
        assertEquals("DUITSLAND", results.get(0).get("country"));
    }
}
