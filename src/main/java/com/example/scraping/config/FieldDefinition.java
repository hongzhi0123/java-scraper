package com.example.scraping.config;

import java.util.List;

public class FieldDefinition {
    private String key;
    private String selector;
    private boolean optional = true;
    private List<TransformerDef> transformers;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getSelector() { return selector; }
    public void setSelector(String selector) { this.selector = selector; }

    public boolean isOptional() { return optional; }
    public void setOptional(boolean optional) { this.optional = optional; }

    public List<TransformerDef> getTransformers() { return transformers; }
    public void setTransformers(List<TransformerDef> transformers) { this.transformers = transformers; }    
}