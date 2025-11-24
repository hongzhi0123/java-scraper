package com.example.scraping.config.transformer;

import com.example.scraping.config.TransformerDef;

public class SubstringTransformer extends TransformerDef {
    private Integer start; // inclusive
    private Integer end;   // exclusive (optional)

    public Integer getStart() { return start; }
    public void setStart(Integer start) { this.start = start; }
    public Integer getEnd() { return end; }
    public void setEnd(Integer end) { this.end = end; }

    @Override
    public String apply(String input) {
        if (input == null) return "";
        int len = input.length();
        int s = start != null ? Math.max(0, start) : 0;
        int e = end != null ? Math.min(len, end) : len;
        return (s < e) ? input.substring(s, e) : "";
    }
}
