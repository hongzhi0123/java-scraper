package com.example.scraping.config;

import java.util.List;

public class CardDefinition {
    private String itemSelector; // e.g., "div.product-card"
    private List<FieldDefinition> fields;
    private RowFilterDefinition rowFilter;

    public String getItemSelector() {
        return itemSelector;
    }

    public void setItemSelector(String itemSelector) {
        this.itemSelector = itemSelector;
    }

    public List<FieldDefinition> getFields() {
        return fields;
    }

    public void setFields(List<FieldDefinition> fields) {
        this.fields = fields;
    }

    public RowFilterDefinition getRowFilter() { return rowFilter; }
    public void setRowFilter(RowFilterDefinition rowFilter) { this.rowFilter = rowFilter; }        
}