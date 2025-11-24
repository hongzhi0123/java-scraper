package com.example.scraping.config;

import lombok.Data;

@Data
public class RateLimitConfig {
    private int requestsPerMinute = 10; // default: gentle
    private boolean randomize = true; // add jitter
    private boolean respectRetryAfter = true;
}
