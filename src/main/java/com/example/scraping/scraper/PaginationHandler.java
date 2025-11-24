package com.example.scraping.scraper;

import com.example.scraping.config.PaginationDefinition;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Optional;

public class PaginationHandler {
    public Optional<String> getNextPageUrl(Document doc, PaginationDefinition def) {
        Element nextEl = doc.selectFirst(def.getNextButtonSelector());
        if (nextEl == null) return Optional.empty();

        String url;
        if (def.isUseHref()) {
            url = nextEl.attr("abs:href"); // JSoup resolves relative URLs if baseUri set
        } else {
            // Extendable: e.g., parse text like "Page 3"
            String text = nextEl.text().trim();
            url = computeFromText(text, def.getBaseUrl());
        }

        return url.isEmpty() ? Optional.empty() : Optional.of(url);
    }

    private String computeFromText(String text, String baseUrl) {
        // TODO: advanced logic (e.g., regex, increment page number)
        // For now, return empty → fallback to href-based
        return "";
    }
}