package com.example.scraping;

import com.example.scraping.config.RowFilterDefinition;
import com.example.scraping.scraper.RowFilter;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RowFilterTest {

    @Test
    public void testAltFilter() {
        String html = """
                <table>
                  <tr><td><img alt="Ticked activity"></td><td>Task 1</td></tr>
                  <tr><td><img alt="Pending"></td><td>Task 2</td></tr>
                  <tr><td>No icon</td><td>Task 3</td></tr>
                </table>
                """;
        var doc = Jsoup.parse(html);

        var filter = new RowFilterDefinition();
        filter.setSelector("td:first-child img");
        filter.setAttr("alt");
        filter.setValue("Ticked activity");

        var rows = doc.select("tr");
        assertTrue(RowFilter.shouldKeepRow(rows.get(0), filter)); // ✅
        assertFalse(RowFilter.shouldKeepRow(rows.get(1), filter)); // ❌
        assertFalse(RowFilter.shouldKeepRow(rows.get(2), filter)); // ❌ (no img)
    }

    @Test
    public void testContainsText() {
        var filter = new RowFilterDefinition();
        filter.setSelector("span.status");
        filter.setContains(true);
        filter.setValue("active"); // case-insensitive word match

        // Test cases
        var active1 = Jsoup.parse("<span class='status'>Active</span>").body().child(0);
        assertTrue(RowFilter.shouldKeepRow(active1, filter), "Exact 'Active'");

        var active2 = Jsoup.parse("<span class='status'>Active Now</span>").body().child(0);
        assertTrue(RowFilter.shouldKeepRow(active2, filter), "Contains 'Active' as word");

        var inactive = Jsoup.parse("<span class='status'>Inactive</span>").body().child(0);
        assertFalse(RowFilter.shouldKeepRow(inactive, filter), "'Inactive' should not match 'active'");

        var reactive = Jsoup.parse("<span class='status'>Re-active</span>").body().child(0);
        assertTrue(RowFilter.shouldKeepRow(reactive, filter), "'Re-active' contains word 'active'");

        var cPlusPlus = Jsoup.parse("<span class='status'>Use C++</span>").body().child(0);
        // Test escaping
        var cppFilter = new RowFilterDefinition();
        cppFilter.setSelector("span.status");
        cppFilter.setContains(true);
        cppFilter.setValue("C++");

        // Since '+' is not a word char, C++ should not match as a whole word
        assertFalse(RowFilter.shouldKeepRow(cPlusPlus, cppFilter), "Escaped regex for 'C++'");
    }

    @Test
    public void testNotFilter() {
        var filter = new RowFilterDefinition();
        filter.setSelector("td");
        filter.setValue("SKIP");
        filter.setNot(true);

        var keep = Jsoup.parse("<table><tr><td>Process</td></tr></table>").selectFirst("tr");
        assertTrue(RowFilter.shouldKeepRow(keep, filter));

        var skip = Jsoup.parse("<table><tr><td>SKIP</td></tr></table>").body().selectFirst("tr");
        assertFalse(RowFilter.shouldKeepRow(skip, filter));
    }
}
