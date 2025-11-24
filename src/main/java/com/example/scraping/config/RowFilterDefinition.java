package com.example.scraping.config;

import lombok.Data;

@Data
public class RowFilterDefinition {
    private String selector;        // selector inside row/card (e.g., "td.status img")
    private String attr;            // attribute to check (e.g., "alt", "class", "data-status")
    private String value;           // expected value (exact match)
    private Boolean contains;       // if true: substring match (case-insensitive)
    private Boolean not = false;    // if true: invert condition
}
