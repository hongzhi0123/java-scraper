package com.example.scraping.scraper;

import com.example.scraping.config.ScrapeConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ScrapeEngine {
    private final ScrapeConfig config;
    private final TableScraper tableScraper = new TableScraper();
    private final DetailPageScraper detailScraper = new DetailPageScraper();
    private final PaginationHandler paginationHandler = new PaginationHandler();

    public ScrapeEngine(ScrapeConfig config) {
        this.config = config;
    }

    public List<Map<String, String>> run() throws IOException {
        List<Map<String, String>> allResults = new ArrayList<>();
        String currentUrl = config.getEntranceUrl();

        while (currentUrl != null) {
            Document doc;
            applyRateLimit();
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
            Document detailDoc = Jsoup.connect(detailUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            Map<String, String> detailData = detailScraper.scrapeDetailPage(detailDoc, config.getDetailPage());
            Map<String, String> merged = new LinkedHashMap<>(item);
            merged.putAll(detailData);
            return merged;

        } catch (Exception e) {
            System.err.println("⚠️ Failed to scrape detail page: " + detailUrl + " — " + e.getMessage());
            return item; // keep main data
        }
    }

    private void applyRateLimit() {
        if (config.getRateLimit() == null)
            return;

        int rpm = config.getRateLimit().getRequestsPerMinute();
        if (rpm <= 0)
            return;

        long baseDelay = 60_000L / rpm; // ms per request
        long jitter = config.getRateLimit().isRandomize() ? (long) (baseDelay * 0.5 * Math.random()) : 0L;

        try {
            Thread.sleep(baseDelay + jitter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}