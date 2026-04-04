package com.example.scraping;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import com.example.scraping.cache.PageFetcher;
import com.example.scraping.config.DynamicConfig;
import com.example.scraping.config.ScrapeConfig;
import com.example.scraping.scraper.ScrapeEngine;
import com.example.scraping.utils.TestConfigLoader;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * Tests config-dnb-register.json against the ORIGINAL downloaded HTML files.
 *
 * NLBank_p1.html / NLBank_p2.html are the raw pages as downloaded from the browser.
 * They are "shell" pages: the &lt;app-root&gt; component is present but no register data
 * because the data is loaded dynamically by Angular/JS at runtime.
 *
 * nl_register.html is the SAME page as NLBank_p1.html but captured AFTER JS rendering,
 * so it contains the actual register-result articles and pagination.
 *
 * These tests document exactly what works and what fails with the original files,
 * showing what adjustments would be needed for offline fixture-based testing.
 */
public class DnbRegisterOriginalFilesTest {

    private static final Path FIXTURES = Paths.get("src/test/resources/fixtures").toAbsolutePath();

    private static String readFixture(String filename) throws IOException {
        return Files.readString(FIXTURES.resolve(filename), StandardCharsets.UTF_8);
    }

    // -----------------------------------------------------------------------
    // Test 1: NLBank_p1.html is a shell with NO register data
    //         → CardScraper throws because itemSelector finds nothing
    // -----------------------------------------------------------------------

    @Test
    void originalPage1_hasNoRegisterResults_throwsException() throws IOException {
        String html = readFixture("NLBank_p1.html");
        PageFetcher stub = url -> Jsoup.parse(html, url);

        ScrapeConfig config = TestConfigLoader.loadConfig("config-dnb-register.json");
        config.setEntranceUrl("https://example.com/register?p=1&l=20");
        config.setPagination(null);

        ScrapeEngine engine = new ScrapeEngine(config, stub);

        // The raw downloaded page has NO article.register-result elements.
        // CardScraper throws IllegalArgumentException when itemSelector finds nothing.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, engine::run);
        assertTrue(ex.getMessage().contains("No items found"),
                "Expected 'No items found' error because the shell page has no dynamic content");
    }

    // -----------------------------------------------------------------------
    // Test 2: NLBank_p2.html is also a shell — same problem
    // -----------------------------------------------------------------------

    @Test
    void originalPage2_hasNoRegisterResults_throwsException() throws IOException {
        String html = readFixture("NLBank_p2.html");
        PageFetcher stub = url -> Jsoup.parse(html, url);

        ScrapeConfig config = TestConfigLoader.loadConfig("config-dnb-register.json");
        config.setEntranceUrl("https://example.com/register?p=2&l=20");
        config.setPagination(null);

        ScrapeEngine engine = new ScrapeEngine(config, stub);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, engine::run);
        assertTrue(ex.getMessage().contains("No items found"),
                "Page 2 shell also has no dynamic content");
    }

    // -----------------------------------------------------------------------
    // Test 3: nl_register.html HAS rendered data → works with stub fetcher
    //         This proves the config is correct once the HTML contains data
    // -----------------------------------------------------------------------

    @Test
    void renderedPage_nl_register_scrapesSuccessfully() throws IOException {
        String html = readFixture("nl_register.html");
        PageFetcher stub = url -> Jsoup.parse(html, url);

        ScrapeConfig config = TestConfigLoader.loadConfig("config-dnb-register.json");
        config.setEntranceUrl("https://example.com/register?p=1&l=20");
        config.setPagination(null); // only test page 1 data

        ScrapeEngine engine = new ScrapeEngine(config, stub);
        List<Map<String, String>> results = engine.run();

        // nl_register.html contains 20 register entries
        assertEquals(20, results.size(), "nl_register.html should have 20 register entries");

        // Verify first entry
        assertEquals("\"Easy Payment Services\" OOD", results.get(0).get("statutoryName"));
        assertEquals("\"Easy Payment Services\" OOD", results.get(0).get("tradeName"));
        assertEquals("Electronic money institutions", results.get(0).get("register"));
        assertEquals("BULGARIJE", results.get(0).get("country"));

        // Verify some entries with missing optional tradeName
        // Entry 2: "EIG Re" EAD — no trade name in the HTML
        assertEquals("\"EIG Re\" EAD", results.get(1).get("statutoryName"));
        assertEquals("", results.get(1).get("tradeName"), "tradeName should be empty when missing");
        assertEquals("Insurers", results.get(1).get("register"));

        // Entry 11: 12Budget B.V. — has a trade name with multiple values
        assertEquals("12Budget B.V.", results.get(10).get("statutoryName"));
        assertEquals("12Budget B.V., Knip, Hypotheek24", results.get(10).get("tradeName"));
        assertEquals("Payment service providers", results.get(10).get("register"));
        assertEquals("NEDERLAND", results.get(10).get("country"));

        // Last entry: AB "Mano bankas"
        assertEquals("AB \"Mano bankas\"", results.get(19).get("statutoryName"));
    }

    // -----------------------------------------------------------------------
    // Test 4: nl_register.html has pagination — verify active page detection
    // -----------------------------------------------------------------------

    @Test
    void renderedPage_nl_register_hasPaginationWithActivePage1() throws IOException {
        String html = readFixture("nl_register.html");
        PageFetcher stub = url -> Jsoup.parse(html, url);

        ScrapeConfig config = TestConfigLoader.loadConfig("config-dnb-register.json");
        config.setEntranceUrl("https://example.com/register?p=1&l=20");
        // Keep pagination enabled — it should detect page 1 as active and compute page 2 URL
        config.getPagination().setUrlPattern("https://example.com/register?p={page}&l=20");

        // Stub returns nl_register for page 1, then empty for page 2 to stop pagination
        String emptyPage2 = """
                <html><body>
                <ul class="register-search__results__list">
                  <li><article class="register-result">
                    <div class="register-result__statutory-name">
                      <h3 class="register-result__title"><a href="/d/1">Page2 Entry</a></h3>
                    </div>
                    <div class="register-result__trade-name"><p class="register-result__value"></p></div>
                    <div class="register-result__register"><p class="register-result__value">Banks</p></div>
                    <div class="register-result__country"><p class="register-result__value">NEDERLAND</p></div>
                  </article></li>
                </ul>
                <ul class="pagination">
                  <li class="pagination__item"><button class="pagination__link">1</button></li>
                  <li class="pagination__item pagination__item--is-active">
                    <button class="pagination__link">2</button>
                  </li>
                </ul>
                </body></html>
                """;

        PageFetcher paginatedStub = url -> {
            if (url.contains("p=2")) return Jsoup.parse(emptyPage2, url);
            return Jsoup.parse(html, url);
        };

        ScrapeEngine engine = new ScrapeEngine(config, paginatedStub);
        List<Map<String, String>> results = engine.run();

        // 20 from nl_register.html (page 1) + 1 from page 2 stub
        assertEquals(21, results.size(), "Should have 20 from page 1 + 1 from page 2");
        assertEquals("\"Easy Payment Services\" OOD", results.get(0).get("statutoryName")); // first from page 1
        assertEquals("Page2 Entry", results.get(20).get("statutoryName")); // from page 2
    }

    // -----------------------------------------------------------------------
    // Test 5: Use NLBank_p1.html + NLBank_p2.html as fixture comparison
    //         Shows both shells are structurally identical (no data)
    // -----------------------------------------------------------------------

    @Test
    void bothOriginalPages_areShellsWithNoData() throws IOException {
        String p1 = readFixture("NLBank_p1.html");
        String p2 = readFixture("NLBank_p2.html");

        org.jsoup.nodes.Document doc1 = Jsoup.parse(p1);
        org.jsoup.nodes.Document doc2 = Jsoup.parse(p2);

        // Both shells have app-root but no dynamic content
        assertFalse(doc1.select("app-root").isEmpty(), "NLBank_p1 should have app-root");
        assertFalse(doc2.select("app-root").isEmpty(), "NLBank_p2 should have app-root");

        // Neither has register data
        assertTrue(doc1.select("article.register-result").isEmpty(),
                "NLBank_p1 has no register-result articles (JS not rendered)");
        assertTrue(doc2.select("article.register-result").isEmpty(),
                "NLBank_p2 has no register-result articles (JS not rendered)");

        // Neither has the results list
        assertTrue(doc1.select("ul.register-search__results__list").isEmpty(),
                "NLBank_p1 has no results list (JS not rendered)");
        assertTrue(doc2.select("ul.register-search__results__list").isEmpty(),
                "NLBank_p2 has no results list (JS not rendered)");

        // Neither has pagination
        assertTrue(doc1.select("li.pagination__item--is-active").isEmpty(),
                "NLBank_p1 has no pagination (JS not rendered)");
    }

    // -----------------------------------------------------------------------
    // Test 6: nl_register.html (rendered) vs NLBank_p1.html (shell) comparison
    //         Shows what the rendered version adds
    // -----------------------------------------------------------------------

    @Test
    void renderedVsShell_showsWhatJsAdds() throws IOException {
        String shell = readFixture("NLBank_p1.html");
        String rendered = readFixture("nl_register.html");

        org.jsoup.nodes.Document shellDoc = Jsoup.parse(shell);
        org.jsoup.nodes.Document renderedDoc = Jsoup.parse(rendered);

        // Shell: app-root present but empty (Angular hasn't bootstrapped)
        assertFalse(shellDoc.select("app-root").isEmpty(), "Shell has app-root");
        assertEquals(0, shellDoc.select("article.register-result").size(),
                "Shell has 0 register results");
        assertEquals(0, shellDoc.select("ul.register-search__results__list").size(),
                "Shell has 0 results lists");

        // Rendered: app-root contains full content
        assertFalse(renderedDoc.select("app-root").isEmpty(), "Rendered has app-root");
        assertEquals(20, renderedDoc.select("article.register-result").size(),
                "Rendered has 20 register results");
        assertEquals(1, renderedDoc.select("ul.register-search__results__list").size(),
                "Rendered has 1 results list");
        assertTrue(renderedDoc.select("li.pagination__item--next-page").size() > 0,
                "Rendered has next-page button");
        assertTrue(renderedDoc.select("li.pagination__item--is-active").size() > 0,
                "Rendered has active page indicator");
    }

    // -----------------------------------------------------------------------
    // Test 7: WireMock serves the ORIGINAL NLBank_p1.html shell (with Angular
    //         scripts replaced by a simple fetch script), plus an API endpoint.
    //         Playwright loads the page, JS fetches the API, DOM gets populated,
    //         and ScrapeEngine extracts register data using the config.
    //
    //         This simulates the real-world flow: shell page + dynamic API data.
    // -----------------------------------------------------------------------

    private static final String REGISTER_API_JSON = """
            [
              {
                "statutoryName": "\\"Easy Payment Services\\" OOD",
                "tradeName": "\\"Easy Payment Services\\" OOD",
                "register": "Electronic money institutions",
                "country": "BULGARIJE",
                "detailUrl": "/en/public-register/information-detail/?registerCode=WFTEG&relationNumber=R135587"
              },
              {
                "statutoryName": "\\"EIG Re\\" EAD",
                "tradeName": "",
                "register": "Insurers",
                "country": "BULGARIJE",
                "detailUrl": "/en/public-register/information-detail/?registerCode=WFTVE&relationNumber=R166515"
              },
              {
                "statutoryName": "ABN AMRO Bank N.V.",
                "tradeName": "ABN AMRO",
                "register": "Banks",
                "country": "NEDERLAND",
                "detailUrl": "/en/public-register/information-detail/?registerCode=BANK&relationNumber=R100123"
              }
            ]
            """;

    /**
     * Script injected into the shell page to replace Angular.
     * Fetches register data from /api/register and builds
     * the same DOM structure that the real Angular app produces.
     */
    private static final String INJECT_SCRIPT = """
            <script>
            fetch('/api/register')
              .then(r => r.json())
              .then(items => {
                const appRoot = document.querySelector('app-root');
                let html = '<ul class="register-search__results__list">';
                items.forEach(item => {
                  html += '<li class="register-search__results__listitem">'
                    + '<article class="register-result">'
                    + '<div class="register-result__statutory-name">'
                    + '  <span class="sr-only register-result__label">Statutory name</span>'
                    + '  <h3 class="register-result__title">'
                    + '    <a href="' + item.detailUrl + '">' + item.statutoryName + '</a>'
                    + '  </h3>'
                    + '</div>';
                  if (item.tradeName) {
                    html += '<div class="register-result__trade-name">'
                      + '  <span class="register-result__label">Trade name</span>'
                      + '  <p class="register-result__value">' + item.tradeName + '</p>'
                      + '</div>';
                  }
                  html += '<div class="register-result__register">'
                    + '  <span class="register-result__label">Register</span>'
                    + '  <p class="register-result__value">' + item.register + '</p>'
                    + '</div>'
                    + '<div class="register-result__country">'
                    + '  <span class="register-result__label">Country</span>'
                    + '  <p class="register-result__value">' + item.country + '</p>'
                    + '</div>'
                    + '</article></li>';
                });
                html += '</ul>';
                appRoot.innerHTML = html;
              });
            </script>
            """;

    @Test
    void playwright_withOriginalShell_andMockApi_scrapesRegisterData() throws IOException {
        // 1. Read the original downloaded shell page
        String shellHtml = readFixture("NLBank_p1.html");

        // 2. Replace Angular scripts with our fetch script.
        //    Remove the Angular module scripts that would fail to load,
        //    and inject our simple API-fetching script instead.
        String modifiedHtml = shellHtml
                .replaceAll("<script[^>]*src=\"/assets/ng/SearchApp/[^\"]*\"[^>]*></script>", "")
                .replace("</body>", INJECT_SCRIPT + "\n</body>");

        // 3. Start WireMock
        WireMockServer wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        try {
            String baseUrl = "http://localhost:" + wireMock.port();

            // Serve the modified shell page
            wireMock.stubFor(get(urlEqualTo("/en/public-register/"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "text/html; charset=utf-8")
                            .withBody(modifiedHtml)));

            // Serve the register API data
            wireMock.stubFor(get(urlEqualTo("/api/register"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(REGISTER_API_JSON)));

            // 4. Configure ScrapeEngine with the WireMock URL, no pagination
            ScrapeConfig config = TestConfigLoader.loadConfig("config-dnb-register.json");
            config.setEntranceUrl(baseUrl + "/en/public-register/");
            config.setPagination(null);

            DynamicConfig dynConfig = new DynamicConfig();
            dynConfig.setWaitForSelector("ul.register-search__results__list");
            dynConfig.setTimeout(15000);
            config.setDynamicConfig(dynConfig);

            // 5. Run the scraper (uses Playwright because dynamic=true)
            ScrapeEngine engine = new ScrapeEngine(config);
            List<Map<String, String>> results = engine.run();

            // 6. Verify results
            assertEquals(3, results.size(), "Should scrape 3 register entries from API");

            assertEquals("\"Easy Payment Services\" OOD", results.get(0).get("statutoryName"));
            assertEquals("\"Easy Payment Services\" OOD", results.get(0).get("tradeName"));
            assertEquals("Electronic money institutions", results.get(0).get("register"));
            assertEquals("BULGARIJE", results.get(0).get("country"));

            assertEquals("\"EIG Re\" EAD", results.get(1).get("statutoryName"));
            assertEquals("", results.get(1).get("tradeName"), "Missing tradeName → empty");
            assertEquals("Insurers", results.get(1).get("register"));
            assertEquals("BULGARIJE", results.get(1).get("country"));

            assertEquals("ABN AMRO Bank N.V.", results.get(2).get("statutoryName"));
            assertEquals("ABN AMRO", results.get(2).get("tradeName"));
            assertEquals("Banks", results.get(2).get("register"));
            assertEquals("NEDERLAND", results.get(2).get("country"));

            // 7. Verify WireMock received both calls
            wireMock.verify(1, getRequestedFor(urlEqualTo("/en/public-register/")));
            wireMock.verify(1, getRequestedFor(urlEqualTo("/api/register")));
        } finally {
            wireMock.stop();
        }
    }
}
