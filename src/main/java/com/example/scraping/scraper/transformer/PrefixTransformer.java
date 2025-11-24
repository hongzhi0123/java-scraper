package com.example.scraping.scraper.transformer;

import com.example.scraping.config.TransformerDef;

public class PrefixTransformer extends TransformerDef {
    private String delimiter = "-";

    // getters/setters (required for Jackson property binding)
    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    @Override
    public String apply(String input) {
        if (input == null)
            return "";
        int idx = input.indexOf(delimiter);
        return idx > 0 ? input.substring(0, idx) : "";
    }
}
