package com.example.scraping.cache;

public class NoOpCache implements ResponseCache {
    @Override public String get(String url) { return null; }
    @Override public void put(String url, String html) {}
    @Override public boolean isEnabled() { return false; }
}
