package com.example.scraping.utils;

import com.example.scraping.config.ScrapeConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class TestConfigLoader {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static ScrapeConfig loadConfig(String resourcePath) throws IOException {
        try (InputStream is = TestConfigLoader.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            return mapper.readValue(is, ScrapeConfig.class);
        }
    }
}