package com.example.scraping.scraper.transformer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.scraping.config.TransformerDef;

public class RegexExtractTransformer extends TransformerDef {
    private String pattern;
    private int group = 1; // capture group to extract

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
    public int getGroup() { return group; }
    public void setGroup(int group) { this.group = group; }

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
