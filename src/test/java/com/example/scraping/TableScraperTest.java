package com.example.scraping;

import com.example.scraping.config.ColumnDefinition;
import com.example.scraping.config.ScrapeConfig;
import com.example.scraping.config.TableDefinition;
import com.example.scraping.scraper.TableScraper;
import com.example.scraping.utils.TestConfigLoader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TableScraperTest {

    @Test
    public void testScrapeNoHeaderWithNegativeIndex() throws IOException {
        String html = new String(Files.readAllBytes(
                Paths.get("src/test/resources/fixtures/entrance.html")));
        Document doc = Jsoup.parse(html);
        doc.setBaseUri("https://example.com"); // critical for abs:href

        // Load config from src/test/resources/config.json
        ScrapeConfig config = TestConfigLoader.loadConfig("config.json");

        // TableDefinition def = new TableDefinition();
        // def.setSelector("table.product-list");
        // def.setHasHeader(false);

        // ColumnDefinition idCol = new ColumnDefinition();
        // idCol.setKey("id"); idCol.setIndex(0);

        // ColumnDefinition nameCol = new ColumnDefinition();
        // nameCol.setKey("name"); nameCol.setIndex(1); nameCol.setSelector("a");

        // ColumnDefinition priceCol = new ColumnDefinition();
        // priceCol.setKey("price"); priceCol.setIndex(-1); // last column

        // ColumnDefinition linkCol = new ColumnDefinition();
        // linkCol.setKey("product"); linkCol.setIndex(1); linkCol.setIsDetailLink(true);

        // def.setColumns(List.of(idCol, nameCol, priceCol, linkCol));

        TableScraper scraper = new TableScraper();
        List<Map<String, String>> results = scraper.scrapeTable(doc, config.getMainTable());

        assertEquals(2, results.size());
        Map<String, String> first = results.get(0);
        assertEquals("P001", first.get("id"));
        assertEquals("Laptop", first.get("name"));
        assertEquals("$999", first.get("price"));
        assertEquals("Laptop", first.get("product"));
        assertTrue(first.containsKey("productUrl"));
        assertTrue(first.get("productUrl").endsWith("/product/001"));
    }
}