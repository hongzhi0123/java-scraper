package com.example.scraping.scraper;

import com.example.scraping.config.FieldDefinition;
import com.example.scraping.config.TransformerDef;
import com.example.scraping.config.CardDefinition;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.stream.Collectors;

public class CardScraper {

    public List<Map<String, String>> scrapeCards(Document doc, CardDefinition cardDef) {
        Elements items = doc.select(cardDef.getItemSelector());
        if (items.isEmpty()) {
            throw new IllegalArgumentException("No items found with selector: " + cardDef.getItemSelector());
        }

        return items.stream()
                .filter(row -> RowFilter.shouldKeepRow(row, cardDef.getRowFilter()))
                .map(item -> parseItem(item, cardDef.getFields()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<String, String> parseItem(Element item, List<FieldDefinition> fields) {
        Map<String, String> record = new LinkedHashMap<>();

        for (FieldDefinition field : fields) {
            String value = "";
            String linkUrl = null;

            Element el = item.selectFirst(field.getSelector());
            if (el != null) {
                // Extract text by default
                value = field.isRawHtml() ? el.html() : el.text().trim();

                // Special: if this field is meant to be a detail link
                // (we repurpose `optional` flag or add new `isDetailLink` — but let's extend
                // FieldDefinition)
                // For now, use a convention: key ends with "Url" → extract href
                if (field.getKey().endsWith("Url") || isDetailLinkField(field)) {
                    Element link = el.tagName().equals("a") ? el : el.selectFirst("a[href]");
                    if (link != null) {
                        value = link.text().trim(); // display text
                        linkUrl = link.attr("abs:href"); // resolved URL
                    }
                }
            } else if (!field.isOptional()) {
                // Fail fast if required field missing
                return null;
            }

            if (field.getTransformers() != null) {
                for (TransformerDef t : field.getTransformers()) {
                    value = t.apply(value);
                }
            }
            record.put(field.getKey(), value);
            if (linkUrl != null) {
                // Store URL under key (e.g., "productUrl" if key="product")
                String baseKey = field.getKey().replaceFirst("Url$", "");
                record.put(baseKey + "Url", linkUrl);
            }
        }

        return record;
    }

    // Optional: allow explicit marking (extend FieldDefinition if needed)
    private boolean isDetailLinkField(FieldDefinition field) {
        // Convention: selector targets <a>, or contains 'href'
        return field.getSelector() != null &&
                (field.getSelector().contains("a") || field.getSelector().contains("[href]"));
    }
}