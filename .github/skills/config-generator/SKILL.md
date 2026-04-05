---
name: config-generator
description: "Generate scraper configuration JSON for a new web page source. Use when: adding a new scraping source, analyzing web page structure, creating config JSON, testing scraper config. Fetches the page via Playwright, analyzes HTML structure, generates valid config, and validates it."
argument-hint: "URL of the web page to scrape (e.g. https://example.com/register)"
---

# Scraper Config Generator

Generate a scraper configuration JSON by analyzing a target web page's HTML structure.

## When to Use

- Adding a new data source to the scraper
- Creating a JSON config for a web page you haven't scraped before
- Regenerating or fixing a config that doesn't extract data correctly

## Prerequisites

- The Playwright MCP server must be running (configured in `.vscode/mcp.json`)
- The Java scraper project must be buildable (`mvn compile` succeeds)

## Procedure

Follow these steps in order. Do NOT skip steps.

### Step 1 — Fetch and Inspect the Page

1. Use the Playwright MCP tools to navigate to the target URL:
   - Call `browser_navigate` with the URL
   - Call `browser_snapshot` to get the accessibility tree
   - If the snapshot shows no data content (empty lists/tables), the page likely requires JavaScript rendering — note this for `dynamic: true`

2. Also get the raw HTML for CSS selector analysis:
   - Call `browser_run_code` with `document.querySelector('body').innerHTML` or use `fetch_webpage` for static pages

3. Determine if the page is **dynamic** (needs JS rendering):
   - If the initial HTML has no data but the rendered page does → `dynamic: true`
   - If data is present in the raw HTML → `dynamic: false` (static)

### Step 2 — Analyze the HTML Structure

Read the [config schema reference](./references/config-schema.md) for all available properties.

Analyze the page to determine:

1. **Layout type** — Is the data in a `<table>` or in repeated `<div>`/`<article>` containers?
   - `<table>` → use `mainTable`
   - Repeated containers → use `mainCard`

2. **Item container** (for cards):
   - Find the CSS selector that matches each individual item
   - Look for repeating elements: `div.card`, `article.result`, `li.item`, etc.
   - Verify the selector matches ALL items and ONLY items (not headers/footers)

3. **Table structure** (for tables):
   - Find the `<table>` CSS selector (id or class)
   - Check if there's a header row (`<thead>` or first `<tr>` with `<th>`)
   - Count columns, identify what each contains

4. **Fields to extract** from each item:
   - Identify the key data points (name, ID, status, address, etc.)
   - For each field, find the CSS selector **relative to the item container**
   - Note which fields contain links (`<a>` tags) — these may be detail page links
   - Note which fields might be missing on some items → mark as `optional: true`

5. **Detail page links**:
   - Check if items contain links to detail/profile pages
   - If cards: use the `*Url` key naming convention (e.g. `"nameUrl"` extracts `href`)
   - If table: mark the column with `"isDetailLink": true`

6. **Pagination**:
   - Look for "Next" buttons, page number links, or URL patterns
   - Find the CSS selector for the next-page trigger
   - Determine if it's href-based, URL-pattern-based, or click-based

7. **Detail page structure** (if detail page links were found in step 5):

   Navigate to the **first** detail page link using Playwright and analyze it:

   a. **Fetch the detail page:**
      - Extract the href from the first item's detail link (identified in step 5)
      - Call `browser_navigate` with that URL
      - Call `browser_snapshot` to get the rendered content
      - Also capture the raw HTML via `browser_run_code` with `document.querySelector('body').innerHTML`
      - **Save this HTML** — it will be needed for the test fixture in Step 4

   b. **Identify standalone fields** on the detail page:
      - Look for labeled data: `<dt>`/`<dd>` pairs, `<th>`/`<td>` pairs, `label: value` patterns
      - Look for sections with company info, addresses, registration numbers, dates, status
      - For each field, determine the CSS selector **from the document root** (not relative to a container)
      - Test selectors mentally: would `document.querySelector("your-selector")` return exactly one element?
      - Common patterns:
        - Definition lists: `"dl.info dt:contains(Name) ~ dd"`
        - Headed sections: `"div.detail h4:contains(Address) + p"`
        - Nth-child for positional: `"div#content p:nth-of-type(2)"`

   c. **Identify nested tables** (`subTable`) on the detail page:
      - Look for `<table>` elements containing structured data (permissions, licenses, activities, branches)
      - Determine the table's CSS selector
      - Check if the table has a header row (`<thead>` or `<th>` elements)
      - Identify which columns contain useful data and their positions or header text
      - Check if only certain rows should be kept — look for icons, checkmarks, or status indicators:
        - If rows have a visual indicator (e.g., checkmark image, colored icon), configure a `rowFilter`:
          ```json
          "rowFilter": {
            "selector": "td:first-child img",
            "attr": "alt",
            "value": "Ticked activity"
          }
          ```
        - For text-based filtering: omit `attr` to match on element text content
        - Use `"contains": true` for substring matching, `"not": true` to invert

   d. **Verify selectors** against the detail page HTML:
      - Each `detailPage.fields[].selector` must match exactly one element on the detail page
      - Each `subTable` selector must match exactly one `<table>`
      - If a field might be absent on some detail pages, mark it `"optional": true`

8. **Data transformations needed**:
   - Currency symbols to strip (e.g. `$`, `€`)
   - Whitespace issues → `trim`
   - IDs embedded in strings → `regexExtract`
   - Default values needed → `defaultIfEmpty`

### Step 3 — Generate the Config JSON

Read [examples](./references/examples.md) for patterns to follow.

Generate the JSON following these rules:

1. **Start with the template** from `./assets/config-template.json` and fill in the values
2. **Set `entranceUrl`** to the target URL
3. **Choose `mainCard` or `mainTable`** based on Step 2 analysis — never both
4. **Apply naming conventions:**
   - For card fields linking to detail pages: use key ending in `Url` (e.g. `"nameUrl"`)
   - For table columns linking to detail pages: set `"isDetailLink": true`
5. **Mark optional fields** with `"optional": true` for data that may not be present on every item
6. **Add transformers** where text needs cleaning (see schema reference for all 12 types)
7. **Configure pagination** if the page has multiple pages
8. **Set `dynamic: true`** and add `dynamicConfig` if the page requires JavaScript rendering
9. **Add rate limiting** with sensible defaults:
   ```json
   "rateLimit": { "requestsPerMinute": 12, "randomize": true }
   ```
10. **Enable caching** so the scraper caches HTTP responses locally for testing and re-runs:
    ```json
    "cache": { "enabled": true, "ttlMinutes": 120, "cacheDir": "target/test-cache" }
    ```
    This avoids hitting the live site on every test iteration.

### Step 4 — Save the Config and Test Fixtures

1. **Save the generated JSON** to the `config/` directory with a descriptive filename:
   - Use the pattern: `{source_abbreviation}_{register_type}.json`
   - Example: `fma_credit.json`, `dnb_register.json`, `bafin_payment.json`

2. Validate the JSON is syntactically correct

3. Cross-check: for each CSS selector in the config, verify it exists in the fetched HTML

4. **Cache the fetched HTML as local test fixtures** — this is mandatory so tests can run offline without hitting the live site:
   - Save the entrance page HTML (obtained in Step 1) to `src/test/resources/fixtures/{source_name}.html`
   - If there's a detail page, save the detail page HTML (obtained in Step 2.7a) to `src/test/resources/fixtures/{source_name}_detail.html`
   - Create a **test config variant** in `src/test/resources/config-{source_name}.json`:
     - Copy the generated config
     - Replace `entranceUrl` with `file:src/test/resources/fixtures/{source_name}.html`
     - Remove `dynamic`, `dynamicConfig`, `rateLimit`, and `cache` (not needed for local files)
     - Remove `pagination` (local fixture is single-page)
     - **Keep `detailPage`** if you saved the detail fixture — the `ConfigValidationTest` handles `file:` URLs with a local file fetcher that serves both entrance and detail pages
     - Ensure any detail link hrefs in the entrance fixture HTML will resolve to the saved detail fixture path (or accept that detail enrichment is skipped for file-based configs)

### Step 5 — Test the Configuration

**First, test with the local fixture** (fast, offline, repeatable):

```bash
mvn test -Dtest=com.example.scraping.ConfigValidationTest -DconfigPath=src/test/resources/config-{source_name}.json -pl .
```

This validates your CSS selectors against the cached HTML without any network requests.

**Then, test with the live config** (if the local test passes):

```bash
mvn test -Dtest=com.example.scraping.ConfigValidationTest -DconfigPath=config/{filename}.json -pl .
```

The live config has `cache.enabled: true`, so the first run fetches from the network and caches responses in `target/test-cache/`. Subsequent re-runs reuse the cache — no repeated downloads.

**Evaluate the results:**
- If the test **passes** and prints extracted data → config is valid, report the results
- If the test **fails** with empty results → selectors are wrong, go back to Step 2
- If the test **fails** with an error → check the error message, fix the config, re-run

**Iterate up to 3 times** on failures. After 3 failed attempts, report the issue and suggest the user inspect the page manually.

## Output

When complete, report:
1. The generated config file path
2. Number of items extracted
3. Sample of first 2-3 items (field names and values)
4. Any warnings about optional fields or potential issues
