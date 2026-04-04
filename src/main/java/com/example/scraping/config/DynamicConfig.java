package com.example.scraping.config;

import lombok.Data;

@Data
public class DynamicConfig {
    /** CSS selector to wait for before capturing the page. Takes priority over waitForNetworkIdle. */
    private String waitForSelector;

    /** Timeout in milliseconds for page operations (default 30000). */
    private int timeout = 30000;
}
