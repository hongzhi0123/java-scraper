package com.example.scraping.config;

import java.util.List;

import lombok.Data;

@Data
public class DetailPageDefinition {
    private List<FieldDefinition> fields;
    private TableDefinition subTable;
}