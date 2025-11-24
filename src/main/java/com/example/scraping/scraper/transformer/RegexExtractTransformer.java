package com.example.scraping.scraper.transformer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.scraping.config.TransformerDef;

import lombok.Data;

@Data
public class RegexExtractTransformer extends TransformerDef {
    private String pattern;
    private int group = 1; // capture group to extract

    @Override
    public String apply(String input) {
        if (input == null || pattern == null) return "";
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(input);
            if (m.find() && m.groupCount() >= group) {
                return m.group(group).trim();
            }
        } catch (Exception e) {
            // log.warn("Regex failed", e);
        }
        return "";
    }
}
