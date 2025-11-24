package com.example.scraping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.scraping.config.TransformerDef;
import com.example.scraping.scraper.transformer.ParseFloatTransformer;
import com.example.scraping.scraper.transformer.ReplaceTransformer;
import com.example.scraping.scraper.transformer.TrimTransformer;

public class TransformerTest {
    @Test
    public void testTransformers() {
        String input = " $1,299.99 ";
        List<TransformerDef> transformers = Arrays.asList(
                new TrimTransformer(),
                new ReplaceTransformer() {
                    {
                        setPattern("$");
                        setReplacement("");
                    }
                },
                new ReplaceTransformer() {
                    {
                        setPattern(",");
                        setReplacement("");
                    }
                },
                new ParseFloatTransformer());

        String result = input;
        for (TransformerDef t : transformers) {
            result = t.apply(result);
        }

        assertEquals("1299.99", result);
    }
}
