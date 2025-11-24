package com.example.scraping.scraper.transformer;

import com.example.scraping.config.TransformerDef;

public class UppercaseTransformer extends TransformerDef {
    @Override
    public String apply(String input) {
        return input != null ? input.toUpperCase() : "";
    }
}
