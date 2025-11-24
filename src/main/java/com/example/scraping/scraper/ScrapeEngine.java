package com.example.scraping.scraper;

import com.example.scraping.cache.CachedJsoup;
import com.example.scraping.cache.FileBasedCache;
import com.example.scraping.cache.NoOpCache;
import com.example.scraping.cache.ResponseCache;
import com.example.scraping.config.ScrapeConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ScrapeEngine {
    private final ScrapeConfig config;
    private final ResponseCache cache;
    private final CachedJsoup cachedJsoup;
    private final TableScraper tableScraper = new TableScraper();
    private final DetailPageScraper detailScraper = new DetailPageScraper();
    private final PaginationHandler paginationHandler = new PaginationHandler();

    public ScrapeEngine(ScrapeConfig config) {
        this.config = config;
        this.cache = config.getCache() != null && config.getCache().isEnabled()
                ? new FileBasedCache(config.getCache())
                : new NoOpCache();
        this.cachedJsoup = new CachedJsoup(cache, config.getRateLimit());
    }

    public List<Map<String, String>> run() throws IOException {
        List<Map<String, String>> allResults = new ArrayList<>();
        String currentUrl = config.getEntranceUrl();

        while (currentUrl != null) {
            // ✅ Use cached fetch
            Document doc = cachedJsoup.getDocument(currentUrl);
            
            try {
                doc = Jsoup.connect(currentUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .timeout(10000)
                        .get();
            } catch (IOException e) {
                System.err.println("❌ Failed to fetch page: " + currentUrl + " — " + e.getMessage());
                break;
            }

            // Scrape main table
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
                        .map(item -> enrichWithDetail(item))
                        .collect(Collectors.toList());
            }

            allResults.addAll(items);

            // Pagination
            Optional<String> next = paginationHandler.getNextPageUrl(doc, config.getPagination());
            currentUrl = next.orElse(null);
        }

        return allResults;
    }

    private Map<String, String> enrichWithDetail(Map<String, String> item) {
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
            Document detailDoc =  cachedJsoup.getDocument(detailUrl);

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