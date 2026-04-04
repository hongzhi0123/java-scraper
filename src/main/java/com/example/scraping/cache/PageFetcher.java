package com.example.scraping.cache;

import org.jsoup.nodes.Document;

import java.io.IOException;

public interface PageFetcher {
    Document getDocument(String url) throws IOException;
}
