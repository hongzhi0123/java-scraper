package com.example.scraping.config;

import com.example.scraping.scraper.transformer.DefaultIfEmptyTransformer;
import com.example.scraping.scraper.transformer.LowercaseTransformer;
import com.example.scraping.scraper.transformer.ParseFloatTransformer;
import com.example.scraping.scraper.transformer.ParseIntTransformer;
import com.example.scraping.scraper.transformer.PrefixTransformer;
import com.example.scraping.scraper.transformer.RegexExtractTransformer;
import com.example.scraping.scraper.transformer.ReplaceTransformer;
import com.example.scraping.scraper.transformer.SplitTransformer;
import com.example.scraping.scraper.transformer.SubstringTransformer;
import com.example.scraping.scraper.transformer.SuffixTransformer;
import com.example.scraping.scraper.transformer.TrimTransformer;
import com.example.scraping.scraper.transformer.UppercaseTransformer;
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
        @JsonSubTypes.Type(value = DefaultIfEmptyTransformer.class, name = "defaultIfEmpty"),
        @JsonSubTypes.Type(value = PrefixTransformer.class, name = "getPrefix"),
        @JsonSubTypes.Type(value = SuffixTransformer.class, name = "getSuffix")
})
public abstract class TransformerDef {
    public abstract String apply(String input);
}