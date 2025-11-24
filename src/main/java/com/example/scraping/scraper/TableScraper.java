package com.example.scraping.scraper;

import com.example.scraping.config.ColumnDefinition;
import com.example.scraping.config.TableDefinition;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.stream.Collectors;

public class TableScraper {

    public List<Map<String, String>> scrapeTable(Document doc, TableDefinition tableDef) {
        Element table = doc.selectFirst(tableDef.getSelector());
        if (table == null) {
            throw new IllegalArgumentException("Table not found with selector: " + tableDef.getSelector());
        }

        Elements allRows = table.select("tbody tr, tr");
        List<String> headers = extractHeaders(table, tableDef);

        // Remove header row if present
        Elements rows = allRows;
        if (tableDef.isHasHeader() && !allRows.isEmpty() && headers.size() > 0) {
            rows = new Elements(allRows.subList(1, allRows.size()));
        }

        return rows.stream()
                .map(row -> parseRow(row, tableDef, headers))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<String> extractHeaders(Element table, TableDefinition def) {
        if (!def.isHasHeader())
            return Collections.emptyList();

        Element headerRow = table.selectFirst("thead tr, tr:has(th)");
        if (headerRow == null)
            return Collections.emptyList();

        return headerRow.select("th, td").stream()
                .map(el -> el.text().trim())
                .collect(Collectors.toList());
    }

    private Map<String, String> parseRow(Element row, TableDefinition def, List<String> headers) {
        Elements cells = row.select("td, th");
        int totalCols = cells.size();

        Map<String, String> record = new LinkedHashMap<>(); // preserve order
        for (ColumnDefinition col : def.getColumns()) {
            String value = "";
            String linkUrl = null;

            // Step 1: Determine column index
            Integer resolvedIndex = null;

            // Prefer headerText matching (if headers available)
            if (!headers.isEmpty() && col.getHeaderText() != null && !col.getHeaderText().isEmpty()) {
                String targetHeader = col.getHeaderText().trim();
                for (int i = 0; i < headers.size(); i++) {
                    if (headers.get(i).equalsIgnoreCase(targetHeader)) {
                        resolvedIndex = i;
                        break;
                    }
                }
                // Optionally: warn if not found
                // if (resolvedIndex == null) System.err.println("⚠️ Header '" + targetHeader + "' not found");
            }

            // Fallback to explicit index (supports negative)
            if (resolvedIndex == null && col.getIndex() != null) {
                resolvedIndex = resolveIndex(col.getIndex(), totalCols);
            }

            // If still unresolved → skip (or use empty)
            if (resolvedIndex == null || resolvedIndex < 0 || resolvedIndex >= totalCols) {
                // Put empty string (your preference)
                record.put(col.getKey(), "");
                continue;
            }            

            // Step 2: Extract cell content
            Element cell = cells.get(resolvedIndex);

            if (col.getSelector() != null) {
                Element selected = cell.selectFirst(col.getSelector());
                if (selected != null) {
                    value = selected.text().trim();
                    if (col.isIsDetailLink()) {
                        linkUrl = selected.attr("abs:href");
                    }
                } else {
                    value = ""; // selector didn’t match
                }
            } else {
                value = cell.text().trim();
                if (col.isIsDetailLink()) {
                    Element link = cell.selectFirst("a[href]");
                    if (link != null) {
                        value = link.text().trim();     // display text
                        linkUrl = link.attr("abs:href"); // detail URL
                    }
                }
            }

            record.put(col.getKey(), value);
            if (linkUrl != null) {
                record.put(col.getKey() + "Url", linkUrl);
            }
        }

        return record;
    }

    private int resolveIndex(Integer index, int total) {
        if (index == null)
            return -1;
        if (index >= 0)
            return index;
        return total + index; // e.g., -1 → total-1
    }
}