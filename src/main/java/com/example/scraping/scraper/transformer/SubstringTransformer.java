package com.example.scraping.scraper.transformer;

import com.example.scraping.config.TransformerDef;

import lombok.Data;

@Data
public class SubstringTransformer extends TransformerDef {
    private Integer start; // inclusive
    private Integer end;   // exclusive (optional)

    @Override
    public String apply(String input) {
        if (input == null) return "";
        int len = input.length();
        int s = start != null ? Math.max(0, start) : 0;
        int e = end != null ? Math.min(len, end) : len;
        return (s < e) ? input.substring(s, e) : "";
    }
}
