package com.example.scraping.config;

import lombok.Data;

@Data
public class PaginationDefinition {
    private String nextButtonSelector;
    private boolean useHref = true;
    private String baseUrl;
    /** URL template for index-based pagination, e.g. "https://example.com/list?p={page}&l=20" */
    private String urlPattern;
    /** Selector for the active/current page element whose text contains the current page number */
    private String activePageSelector;
}