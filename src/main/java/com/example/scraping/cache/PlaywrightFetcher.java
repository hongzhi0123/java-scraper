package com.example.scraping.cache;

import com.example.scraping.config.DynamicConfig;
import com.example.scraping.config.RateLimitConfig;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

/**
 * Fetches pages using a headless Chromium browser via Playwright, enabling
 * scraping of JavaScript-rendered content. Shares the same ResponseCache and
 * RateLimitConfig as CachedJsoup so caching and rate limiting are consistent.
 *
 * Must be closed after use (implements AutoCloseable) to release browser resources.
 */
public class PlaywrightFetcher implements PageFetcher, AutoCloseable {

    private final ResponseCache cache;
    private final RateLimitConfig rateLimitConfig;
    private final DynamicConfig dynamicConfig;

    private Playwright playwright;
    private Browser browser;

    public PlaywrightFetcher(ResponseCache cache, RateLimitConfig rateLimitConfig, DynamicConfig dynamicConfig) {
        this.cache = cache;
        this.rateLimitConfig = rateLimitConfig;
        this.dynamicConfig = dynamicConfig != null ? dynamicConfig : new DynamicConfig();
    }

    public Document getDocument(String url) throws IOException {
        if (cache.isEnabled()) {
            String cached = cache.get(url);
            if (cached != null) {
                System.out.println("✅ Cache hit: " + url);
                return Jsoup.parse(cached, url);
            }
        }

        applyRateLimit();
        System.out.println("🌐 Fetching (dynamic): " + url);

        ensureBrowserOpen();

        String html;
        try (Page page = browser.newPage()) {
            int timeout = dynamicConfig.getTimeout();
            page.setDefaultTimeout(timeout);
            page.navigate(url);

            if (dynamicConfig.getWaitForSelector() != null && !dynamicConfig.getWaitForSelector().isBlank()) {
                page.waitForSelector(dynamicConfig.getWaitForSelector());
            } else {
                page.waitForLoadState(LoadState.NETWORKIDLE);
            }

            html = page.content();
        } catch (PlaywrightException e) {
            throw new IOException("Playwright failed to fetch: " + url, e);
        }

        if (cache.isEnabled()) {
            cache.put(url, html);
        }

        return Jsoup.parse(html, url);
    }

    private void ensureBrowserOpen() {
        if (playwright == null) {
            playwright = Playwright.create();
        }
        if (browser == null || !browser.isConnected()) {
            browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
        }
    }

    private void applyRateLimit() {
        if (rateLimitConfig == null) return;
        int rpm = rateLimitConfig.getRequestsPerMinute();
        if (rpm <= 0) return;

        long baseDelay = 60_000L / rpm;
        long jitter = rateLimitConfig.isRandomize() ? (long) (baseDelay * 0.5 * Math.random()) : 0L;

        try {
            Thread.sleep(baseDelay + jitter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        if (browser != null) {
            browser.close();
            browser = null;
        }
        if (playwright != null) {
            playwright.close();
            playwright = null;
        }
    }
}
