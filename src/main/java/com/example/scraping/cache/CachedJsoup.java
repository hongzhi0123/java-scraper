package com.example.scraping.cache;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.example.scraping.config.RateLimitConfig;

import java.io.IOException;

public class CachedJsoup {
    private final ResponseCache cache;
    private RateLimitConfig config;

    public CachedJsoup(ResponseCache cache, RateLimitConfig config) {
        this.cache = cache;
        this.config = config;
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
        System.out.println("🌐 Fetching: " + url);
        Connection connection = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(10000);

        Connection.Response response = connection.execute();
        String html = response.body();

        if (cache.isEnabled()) {
            cache.put(url, html);
        }

        return response.parse();
    }

    private void applyRateLimit() {
        if (config == null)
            return;

        int rpm = config.getRequestsPerMinute();
        if (rpm <= 0)
            return;

        long baseDelay = 60_000L / rpm; // ms per request
        long jitter = config.isRandomize() ? (long) (baseDelay * 0.5 * Math.random()) : 0L;

        try {
            Thread.sleep(baseDelay + jitter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
