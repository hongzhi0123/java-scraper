package com.example.scraping.config.transformer;

import com.example.scraping.config.TransformerDef;

public class DefaultIfEmptyTransformer extends TransformerDef {
    private String defaultValue = "";

    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

    @Override
    public String apply(String input) {
        return (input == null || input.trim().isEmpty()) ? defaultValue : input;
    }
}
