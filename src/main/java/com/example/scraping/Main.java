package com.example.scraping;

import com.example.eba.MyObject;
import com.example.scraping.config.ScrapeConfig;
import com.example.scraping.scraper.ScrapeEngine;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class Main {
    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                System.err.println("❌ Please provide the path to the config file as an argument.");
                return;
            }

            parseLargeFile(args[0]);
            // scraping(args[0]);

        } catch (IOException e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void scraping(String filepath) throws StreamReadException, DatabindException, IOException {
        // Load config
        ObjectMapper mapper = new ObjectMapper();
        ScrapeConfig config = mapper.readValue(new File(filepath), ScrapeConfig.class);

        // Run scraper
        ScrapeEngine engine = new ScrapeEngine(config);
        List<Map<String, String>> results = engine.run();

        // Output
        System.out.println("✅ Scraped " + results.size() + " items:");
        for (int i = 0; i < results.size(); i++) {
            System.out.println("\n--- Item " + (i + 1) + " ---");
            results.get(i).forEach((k, v) -> System.out.println(k + ": \"" + v + "\""));
        }
    }

    private static void parseLargeFile(String filepath) throws StreamReadException, DatabindException, IOException {
        List<MyObject> objects = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        try (JsonParser parser = mapper.getFactory().createParser(new File(filepath))) {
            // Expect START_ARRAY for the outer array
            JsonToken token = parser.nextToken();
            if (token != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected start of array");
            }

            // First token after START_ARRAY should be the disclaimer sub-array
            token = parser.nextToken();
            if (token == JsonToken.START_ARRAY) {
                parser.skipChildren(); // Skip the entire disclaimer array
            }

            // Next should be the data array
            token = parser.nextToken();
            if (token == JsonToken.START_ARRAY) {
                // Now ready to process the main data array
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    MyObject obj = parser.readValueAs(MyObject.class);
                    objects.add(obj);
                    // Process the object
                    // log.info(obj.toString());
                }
            }

            log.info("Total objects parsed: " + objects.size());
        }
    }
}