package com.example.scraping.config.transformer;

import com.example.scraping.config.TransformerDef;

public class ReplaceTransformer extends TransformerDef {
    private String pattern;
    private String replacement;

    // getters & setters (required for Jackson)
    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

    @Override
    public String apply(String input) {
        if (input == null)
            return "";
        return input.replace(pattern, replacement);
    }
}
