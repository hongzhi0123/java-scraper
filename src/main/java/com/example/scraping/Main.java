package com.example.scraping;

import com.example.scraping.config.ScrapeConfig;
import com.example.scraping.scraper.ScrapeEngine;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        try {
            // Load config
            ObjectMapper mapper = new ObjectMapper();
            ScrapeConfig config = mapper.readValue(new File("config.json"), ScrapeConfig.class);

            // Run scraper
            ScrapeEngine engine = new ScrapeEngine(config);
            List<Map<String, String>> results = engine.run();

            // Output
            System.out.println("✅ Scraped " + results.size() + " items:");
            for (int i = 0; i < results.size(); i++) {
                System.out.println("\n--- Item " + (i + 1) + " ---");
                results.get(i).forEach((k, v) -> System.out.println(k + ": \"" + v + "\""));
            }

        } catch (IOException e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}