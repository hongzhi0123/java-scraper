package com.example.scraping.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ColumnDefinition {
    private String key;
    private Integer index;
    private String headerText;
    private String selector;
    @JsonProperty("isDetailLink")
    private boolean isDetailLink = false;
    private List<TransformerDef> transformers;

    // getters & setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public Integer getIndex() { return index; }
    public void setIndex(Integer index) { this.index = index; }

    public String getHeaderText() { return headerText; }
    public void setHeaderText(String headerText) { this.headerText = headerText; }

    public String getSelector() { return selector; }
    public void setSelector(String selector) { this.selector = selector; }

    public boolean isIsDetailLink() { return isDetailLink; }
    public void setIsDetailLink(boolean isDetailLink) { this.isDetailLink = isDetailLink; }

    public List<TransformerDef> getTransformers() { return transformers; }
    public void setTransformers(List<TransformerDef> transformers) { this.transformers = transformers; }
}
