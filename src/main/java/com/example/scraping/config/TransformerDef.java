package com.example.scraping.config;

import com.example.scraping.config.transformer.DefaultIfEmptyTransformer;
import com.example.scraping.config.transformer.LowercaseTransformer;
import com.example.scraping.config.transformer.ParseFloatTransformer;
import com.example.scraping.config.transformer.ParseIntTransformer;
import com.example.scraping.config.transformer.RegexExtractTransformer;
import com.example.scraping.config.transformer.ReplaceTransformer;
import com.example.scraping.config.transformer.SplitTransformer;
import com.example.scraping.config.transformer.SubstringTransformer;
import com.example.scraping.config.transformer.TrimTransformer;
import com.example.scraping.config.transformer.UppercaseTransformer;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TrimTransformer.class, name = "trim"),
        @JsonSubTypes.Type(value = LowercaseTransformer.class, name = "lowercase"),
        @JsonSubTypes.Type(value = UppercaseTransformer.class, name = "uppercase"),
        @JsonSubTypes.Type(value = ParseIntTransformer.class, name = "parseInt"),
        @JsonSubTypes.Type(value = ParseFloatTransformer.class, name = "parseFloat"),
        @JsonSubTypes.Type(value = ReplaceTransformer.class, name = "replace"),
        @JsonSubTypes.Type(value = SplitTransformer.class, name = "split"),
        @JsonSubTypes.Type(value = SubstringTransformer.class, name = "substring"),
        @JsonSubTypes.Type(value = RegexExtractTransformer.class, name = "regexExtract"),
        @JsonSubTypes.Type(value = DefaultIfEmptyTransformer.class, name = "defaultIfEmpty")
})
public abstract class TransformerDef {
    public abstract String apply(String input);
}