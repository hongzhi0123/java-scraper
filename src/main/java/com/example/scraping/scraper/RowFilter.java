package com.example.scraping.scraper;

import com.example.scraping.config.RowFilterDefinition;

import java.util.regex.Pattern;

import org.jsoup.nodes.Element;

public class RowFilter {
    public static boolean shouldKeepRow(Element row, RowFilterDefinition filter) {
        if (filter == null)
            return true; // no filter → keep all

        Element target = row.selectFirst(filter.getSelector());
        if (target == null) {
            return invert(false, filter.getNot()); // element not found → reject
        }

        String actual;
        if (filter.getAttr() != null && !filter.getAttr().isEmpty()) {
            actual = target.attr(filter.getAttr()).trim();
        } else {
            actual = target.text().trim();
        }

        boolean matches;
        if (filter.getContains() != null && filter.getContains()) {
            // Word-boundary match (case-insensitive)
            String escapedValue = Pattern.quote(filter.getValue());
            Pattern pattern = Pattern.compile("\\b" + escapedValue + "\\b", Pattern.CASE_INSENSITIVE);
            matches = pattern.matcher(actual).find();
        } else {
            matches = actual.equals(filter.getValue());
        }

        return invert(matches, filter.getNot());
    }

    private static boolean invert(boolean value, Boolean not) {
        return Boolean.TRUE.equals(not) ? !value : value;
    }
}