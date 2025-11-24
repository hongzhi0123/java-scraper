package com.example.scraping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.example.scraping.cache.FileBasedCache;
import com.example.scraping.cache.ResponseCache;
import com.example.scraping.config.CacheConfig;

public class CacheTest {
    @Test
    public void testCache() throws IOException {
        CacheConfig cfg = new CacheConfig();
        cfg.setEnabled(true);
        cfg.setCacheDir("target/test-cache");
        cfg.setTtlMinutes(10);

        ResponseCache cache = new FileBasedCache(cfg);
        String url = "https://test.com/page";
        String html = "<html><body>cached</body></html>";

        cache.put(url, html);
        assertEquals(html, cache.get(url));
    }
}
