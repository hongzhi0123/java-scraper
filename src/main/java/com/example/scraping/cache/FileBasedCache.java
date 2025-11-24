package com.example.scraping.cache;

import com.example.scraping.config.CacheConfig;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class FileBasedCache implements ResponseCache {
    private final CacheConfig config;
    private final Path cacheDir;

    public FileBasedCache(CacheConfig config) {
        this.config = config;
        this.cacheDir = Paths.get(config.getCacheDir());
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create cache dir: " + cacheDir, e);
        }
    }

    @Override
    public String get(String url) throws IOException {
        Path file;
        try {
            file = getCacheFile(url);
        } catch (NoSuchAlgorithmException e) {
            // Wrap in IOException so method signature stays compatible with the interface
            throw new IOException("Unable to compute cache filename (SHA-256)", e);
        }
        
        if (!Files.exists(file)) return null;

        // Check TTL
        if (config.getTtlMinutes() > 0) {
            Instant modified = Files.getLastModifiedTime(file).toInstant();
            Instant expiry = modified.plus(config.getTtlMinutes(), ChronoUnit.MINUTES);
            if (Instant.now().isAfter(expiry)) {
                Files.deleteIfExists(file);
                return null;
            }
        }

        return Files.readString(file);
    }

    @Override
    public void put(String url, String html) throws IOException {
        try {
            Path file = getCacheFile(url);
            Files.writeString(file, html);
        } catch (NoSuchAlgorithmException e) {
            // Wrap in IOException so method signature stays compatible with the interface
            throw new IOException("Unable to compute cache filename (SHA-256)", e);
        }
    }

    @Override
    public boolean isEnabled() {
        return config != null && config.isEnabled();
    }

    // Convert URL to safe filename: base64 or hash
    private Path getCacheFile(String url) throws NoSuchAlgorithmException {
        // Better: use hex encoding
        String hex = bytesToHex(java.security.MessageDigest.getInstance("SHA-256").digest(url.getBytes()));
        return cacheDir.resolve(hex + ".html");
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
