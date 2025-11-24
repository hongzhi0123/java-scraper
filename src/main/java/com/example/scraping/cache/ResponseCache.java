package com.example.scraping.cache;

import java.io.IOException;

public interface ResponseCache {
    String get(String url) throws IOException;
    void put(String url, String html) throws IOException;
    boolean isEnabled();
}