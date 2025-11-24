package com.example.scraping.config;

import lombok.Data;

@Data
public class PaginationDefinition {
    private String nextButtonSelector;
    private boolean useHref = true;
    private String baseUrl;
}