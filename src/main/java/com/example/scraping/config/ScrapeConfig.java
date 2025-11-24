package com.example.scraping.config;

import lombok.Data;

@Data
public class ScrapeConfig {
    private String entranceUrl;
    private TableDefinition mainTable;
    private CardDefinition mainCard;  // ← new: alternative to mainTable
    private DetailPageDefinition detailPage;
    private PaginationDefinition pagination;
    private RateLimitConfig rateLimit;
    private CacheConfig cache;
}