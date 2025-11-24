package com.example.scraping.config;

import java.util.List;

import lombok.Data;

@Data
public class CardDefinition {
    private String itemSelector; // e.g., "div.product-card"
    private List<FieldDefinition> fields;
    private RowFilterDefinition rowFilter;
}