package com.example.scraping.scraper;

import com.example.scraping.cache.CachedJsoup;
import com.example.scraping.cache.FileBasedCache;
import com.example.scraping.cache.NoOpCache;
import com.example.scraping.cache.PageFetcher;
import com.example.scraping.cache.PlaywrightFetcher;
import com.example.scraping.cache.ResponseCache;
import com.example.scraping.config.ScrapeConfig;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ScrapeEngine {
    private final ScrapeConfig config;
    private final ResponseCache cache;
    private final PageFetcher staticFetcher;
    private final PageFetcher fetcherOverride; // non-null only in tests
    private final TableScraper tableScraper = new TableScraper();
    private final DetailPageScraper detailScraper = new DetailPageScraper();
    private final PaginationHandler paginationHandler = new PaginationHandler();

    public ScrapeEngine(ScrapeConfig config) {
        this.config = config;
        this.cache = config.getCache() != null && config.getCache().isEnabled()
                ? new FileBasedCache(config.getCache())
                : new NoOpCache();
        this.staticFetcher = new CachedJsoup(cache, config.getRateLimit());
        this.fetcherOverride = null;
    }

    // For testing: injects a custom PageFetcher, bypassing fetcher construction
    public ScrapeEngine(ScrapeConfig config, PageFetcher fetcher) {
        this.config = config;
        this.cache = new NoOpCache();
        this.staticFetcher = fetcher;
        this.fetcherOverride = fetcher;
    }

    public List<Map<String, String>> run() throws IOException {
        List<Map<String, String>> allResults = new ArrayList<>();
        String currentUrl = config.getEntranceUrl();

        if (fetcherOverride != null) {
            while (currentUrl != null) {
                Document doc = fetcherOverride.getDocument(currentUrl);
                currentUrl = processPage(doc, fetcherOverride, allResults);
            }
        } else if (config.isDynamic()) {
            try (PlaywrightFetcher dynamicFetcher = new PlaywrightFetcher(cache, config.getRateLimit(), config.getDynamicConfig())) {
                while (currentUrl != null) {
                    Document doc = dynamicFetcher.getDocument(currentUrl);
                    currentUrl = processPage(doc, dynamicFetcher, allResults);
                }
            }
        } else {
            while (currentUrl != null) {
                Document doc = staticFetcher.getDocument(currentUrl);
                currentUrl = processPage(doc, staticFetcher, allResults);
            }
        }

        return allResults;
    }

    private String processPage(Document doc, PageFetcher fetcher, List<Map<String, String>> allResults) throws IOException {
        // Scrape main table or cards
        List<Map<String, String>> items;
        if (config.getMainTable() != null) {
            items = tableScraper.scrapeTable(doc, config.getMainTable());
        } else if (config.getMainCard() != null) {
            CardScraper cardScraper = new CardScraper();
            items = cardScraper.scrapeCards(doc, config.getMainCard());
        } else {
            throw new IllegalStateException("No mainTable or mainCard defined in config");
        }

        // Scrape detail pages (if defined)
        if (config.getDetailPage() != null) {
            items = items.stream()
                    .map(item -> enrichWithDetail(item, fetcher))
                    .collect(Collectors.toList());
        }

        allResults.addAll(items);

        // Pagination
        if (config.getPagination() == null) return null;
        Optional<String> next = paginationHandler.getNextPageUrl(doc, config.getPagination());
        return next.orElse(null);
    }

    private Map<String, String> enrichWithDetail(Map<String, String> item, PageFetcher fetcher) {
        String detailUrlKey = null;
        for (String key : item.keySet()) {
            if (key.endsWith("Url")) {
                detailUrlKey = key;
                break;
            }
        }

        if (detailUrlKey == null || config.getDetailPage() == null) {
            return item;
        }

        String detailUrl = item.get(detailUrlKey);
        if (detailUrl == null || detailUrl.isEmpty()) {
            return item;
        }

        try {
            Document detailDoc = fetcher.getDocument(detailUrl);

            Map<String, String> detailData = detailScraper.scrapeDetailPage(detailDoc, config.getDetailPage());
            Map<String, String> merged = new LinkedHashMap<>(item);
            merged.putAll(detailData);
            return merged;

        } catch (Exception e) {
            System.err.println("⚠️ Failed to scrape detail page: " + detailUrl + " — " + e.getMessage());
            return item; // keep main data
        }
    }
}