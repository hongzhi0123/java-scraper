package com.example.scraping.config;

public class RowFilterDefinition {
    private String selector;        // selector inside row/card (e.g., "td.status img")
    private String attr;            // attribute to check (e.g., "alt", "class", "data-status")
    private String value;           // expected value (exact match)
    private Boolean contains;       // if true: substring match (case-insensitive)
    private Boolean not = false;    // if true: invert condition

    // Getters & setters
    public String getSelector() { return selector; }
    public void setSelector(String selector) { this.selector = selector; }

    public String getAttr() { return attr; }
    public void setAttr(String attr) { this.attr = attr; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public Boolean getContains() { return contains; }
    public void setContains(Boolean contains) { this.contains = contains; }

    public Boolean getNot() { return not; }
    public void setNot(Boolean not) { this.not = not; }
}
