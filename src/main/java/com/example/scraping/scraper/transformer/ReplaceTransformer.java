package com.example.scraping.scraper.transformer;

import com.example.scraping.config.TransformerDef;

import lombok.Data;

@Data
public class ReplaceTransformer extends TransformerDef {
    private String pattern;
    private String replacement;

    @Override
    public String apply(String input) {
        if (input == null)
            return "";
        return input.replaceAll(pattern, replacement);
    }
}
