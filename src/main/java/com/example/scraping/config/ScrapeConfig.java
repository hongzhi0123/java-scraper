package com.example.scraping.config;

public class ScrapeConfig {
    private String entranceUrl;
    private TableDefinition mainTable;
    private DetailPageDefinition detailPage;
    private PaginationDefinition pagination;

    // getters & setters
    public String getEntranceUrl() { return entranceUrl; }
    public void setEntranceUrl(String entranceUrl) { this.entranceUrl = entranceUrl; }

    public TableDefinition getMainTable() { return mainTable; }
    public void setMainTable(TableDefinition mainTable) { this.mainTable = mainTable; }

    public DetailPageDefinition getDetailPage() { return detailPage; }
    public void setDetailPage(DetailPageDefinition detailPage) { this.detailPage = detailPage; }

    public PaginationDefinition getPagination() { return pagination; }
    public void setPagination(PaginationDefinition pagination) { this.pagination = pagination; }
}