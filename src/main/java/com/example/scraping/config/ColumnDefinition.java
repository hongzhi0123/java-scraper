package com.example.scraping.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ColumnDefinition {
    private String key;
    private Integer index;
    private String headerText;
    private String selector;
    @JsonProperty("isDetailLink")
    private boolean isDetailLink = false;
    private List<TransformerDef> transformers;
    
    public boolean isIsDetailLink() { return isDetailLink; }
}
