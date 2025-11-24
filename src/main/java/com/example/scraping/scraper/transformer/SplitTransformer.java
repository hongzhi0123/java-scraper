package com.example.scraping.scraper.transformer;

import com.example.scraping.config.TransformerDef;

import lombok.Data;

@Data
public class SplitTransformer extends TransformerDef {
    private String delimiter;
    private int index; // which part to take (0-based); negative = from end

    @Override
    public String apply(String input) {
        if (input == null || delimiter == null) return "";
        String[] parts = input.split(delimiter);
        int idx = index >= 0 ? index : parts.length + index;
        return (idx >= 0 && idx < parts.length) ? parts[idx].trim() : "";
    }
}
