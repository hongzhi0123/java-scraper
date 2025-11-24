package com.example.scraping.config.transformer;

import com.example.scraping.config.TransformerDef;

public class ParseFloatTransformer extends TransformerDef {
    @Override
    public String apply(String input) {
        if (input == null) return "";
        try {
            return String.valueOf(Float.parseFloat(input.trim()));
        } catch (NumberFormatException e) {
            return "";
        }
    }
}