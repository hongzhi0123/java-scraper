package com.example.scraping.config;

import java.util.List;

public class DetailPageDefinition {
    private List<FieldDefinition> fields;
    private TableDefinition subTable;

    public List<FieldDefinition> getFields() { return fields; }
    public void setFields(List<FieldDefinition> fields) { this.fields = fields; }

    public TableDefinition getSubTable() { return subTable; }
    public void setSubTable(TableDefinition subTable) { this.subTable = subTable; }
}