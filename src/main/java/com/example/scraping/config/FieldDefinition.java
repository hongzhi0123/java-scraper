package com.example.scraping.config;

import java.util.List;

import lombok.Data;

@Data
public class FieldDefinition {
    private String key;
    private String selector;
    private boolean optional = true;
    private boolean rawHtml = false;
    private List<TransformerDef> transformers;
}