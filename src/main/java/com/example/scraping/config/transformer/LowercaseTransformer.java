package com.example.scraping.config.transformer;

import com.example.scraping.config.TransformerDef;

public class LowercaseTransformer extends TransformerDef {
    @Override
    public String apply(String input) {
        return input != null ? input.toLowerCase() : "";
    }
}