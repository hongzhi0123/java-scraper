package com.example.scraping.config;

import lombok.Data;

@Data
public class CacheConfig {
    private boolean enabled = false;       // default: off
    private int ttlMinutes = 60;           // 0 = no expiry
    private String cacheDir = "target/cache"; // Maven-friendly
}
