package com.example.scraping.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TableDefinition {
    private String selector;
    @JsonProperty("hasHeader")
    private boolean hasHeader = true;
    private List<ColumnDefinition> columns;

    public String getSelector() { return selector; }
    public void setSelector(String selector) { this.selector = selector; }

    public boolean isHasHeader() { return hasHeader; }
    public void setHasHeader(boolean hasHeader) { this.hasHeader = hasHeader; }

    public List<ColumnDefinition> getColumns() { return columns; }
    public void setColumns(List<ColumnDefinition> columns) { this.columns = columns; }
}