package com.example.scraping.scraper.transformer;

import com.example.scraping.config.TransformerDef;

import lombok.Data;

@Data
public class PrefixTransformer extends TransformerDef {
    private String delimiter = "-";

    @Override
    public String apply(String input) {
        if (input == null)
            return "";
        int idx = input.indexOf(delimiter);
        return idx > 0 ? input.substring(0, idx) : "";
    }
}
