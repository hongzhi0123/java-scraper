package com.example.scraping.config;

public class PaginationDefinition {
    private String nextButtonSelector;
    private boolean useHref = true;
    private String baseUrl;

    public String getNextButtonSelector() { return nextButtonSelector; }
    public void setNextButtonSelector(String nextButtonSelector) { this.nextButtonSelector = nextButtonSelector; }

    public boolean isUseHref() { return useHref; }
    public void setUseHref(boolean useHref) { this.useHref = useHref; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
}