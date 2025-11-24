package com.example.scraping.scraper.transformer;

import com.example.scraping.config.TransformerDef;

import lombok.Data;

@Data
public class DefaultIfEmptyTransformer extends TransformerDef {
    private String defaultValue = "";

    @Override
    public String apply(String input) {
        return (input == null || input.trim().isEmpty()) ? defaultValue : input;
    }
}
