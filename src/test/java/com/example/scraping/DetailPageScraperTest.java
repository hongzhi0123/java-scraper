package com.example.scraping;

import com.example.scraping.config.ColumnDefinition;
import com.example.scraping.config.DetailPageDefinition;
import com.example.scraping.config.FieldDefinition;
import com.example.scraping.config.ScrapeConfig;
import com.example.scraping.config.TableDefinition;
import com.example.scraping.scraper.DetailPageScraper;
import com.example.scraping.utils.TestConfigLoader;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DetailPageScraperTest {

    @Test
    public void testScrapeDetailPageWithSubTable() throws IOException {
        String html = new String(Files.readAllBytes(
                Paths.get("src/test/resources/fixtures/detail_p001.html")));
        Document doc = Jsoup.parse(html);

        // Load config from src/test/resources/config.json
        ScrapeConfig config = TestConfigLoader.loadConfig("config.json");

        // DetailPageDefinition def = new DetailPageDefinition();

        // FieldDefinition desc = new FieldDefinition();
        // desc.setKey("description"); desc.setSelector("div#description p");

        // FieldDefinition manu = new FieldDefinition();
        // manu.setKey("manufacturer"); manu.setSelector("dl.info dt:contains(Made by) + dd");

        // FieldDefinition stock = new FieldDefinition();
        // stock.setKey("inStock"); stock.setSelector("span.stock-status");

        // def.setFields(List.of(desc, manu, stock));

        // // Sub-table
        // TableDefinition subTable = new TableDefinition();
        // subTable.setSelector("table.specs");
        // subTable.setHasHeader(true);

        // var propCol = new ColumnDefinition(); propCol.setKey("specName"); propCol.setHeaderText("Property");
        // var valCol = new ColumnDefinition(); valCol.setKey("specValue"); valCol.setHeaderText("Value");
        // subTable.setColumns(List.of(propCol, valCol));

        // def.setSubTable(subTable);

        DetailPageScraper scraper = new DetailPageScraper();
        Map<String, String> result = scraper.scrapeDetailPage(doc, config.getDetailPage());

        assertEquals("Powerful laptop for developers.", result.get("description"));
        assertEquals("TechCorp", result.get("manufacturer"));
        assertEquals("", result.get("inStock")); // empty string preserved

        // Parse sub-table JSON
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, String>> subTableData = mapper.readValue(
                result.get("__subTable"), new TypeReference<>() {});

        assertEquals(2, subTableData.size());
        assertEquals("Weight", subTableData.get(0).get("specName"));
        assertEquals("1.5 kg", subTableData.get(0).get("specValue"));
    }
}