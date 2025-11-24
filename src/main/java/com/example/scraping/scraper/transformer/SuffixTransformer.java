package com.example.scraping.scraper.transformer;

import com.example.scraping.config.TransformerDef;

import lombok.Data;

@Data
public class SuffixTransformer extends TransformerDef {
    private String delimiter = "-";

    @Override
    public String apply(String input) {
        if (input == null)
            return "";
        int idx = input.lastIndexOf(delimiter);
        return idx >= 0 && idx < input.length() - delimiter.length() ? input.substring(idx + delimiter.length()) : "";
    }
}
