package com.example.scraping.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class TableDefinition {
    private String selector;
    @JsonProperty("hasHeader")
    private boolean hasHeader = true;
    private RowFilterDefinition rowFilter;
    private List<ColumnDefinition> columns;
}