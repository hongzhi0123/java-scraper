package com.example.scraping.scraper.transformer;

import com.example.scraping.config.TransformerDef;

public class ParseIntTransformer extends TransformerDef {
    @Override
    public String apply(String input) {
        if (input == null) return "";
        try {
            return String.valueOf(Integer.parseInt(input.trim()));
        } catch (NumberFormatException e) {
            return ""; // or "0", or throw — your choice
        }
    }
}
