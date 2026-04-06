---
name: visual-config-generator
description: "Generate scraper configuration JSON by visually inspecting the rendered page. Use when: adding a new scraping source, analyzing a web page structure, creating a config JSON. Uses screenshots and Playwright interaction to understand the page, then derives reliable CSS selectors programmatically."
argument-hint: "URL of the web page to scrape (e.g. https://example.com/register)"
---

# Visual Config Generator

Generate a scraper configuration JSON by **visually inspecting** the rendered page using Playwright. Instead of inferring selectors from raw HTML, this skill uses screenshots and interactive DOM exploration to build reliable, stable selectors.

## When to Use

- Adding a new data source to the scraper
- Creating a JSON config for a web page you haven't scraped before
- Regenerating or fixing a config that doesn't extract data correctly

## Prerequisites

- The Playwright MCP server must be running (configured in `.vscode/mcp.json`)
- The Java scraper project must be buildable (`mvn compile` succeeds)

## Core Principle: See Before You Extract

**Never read raw HTML to infer selectors.** Always use:
1. **Screenshots** — see what the page actually looks like
2. **Element inspection** — query Playwright for element info, not raw HTML
3. **Selector verification** — test selectors before committing them to the config

---

## Procedure

### Step 1 — Visual Reconnaissance

**Take a screenshot first.** This gives you context before exploring.

```
1. Call browser_navigate with the URL
2. Call mcp_playwright_screenshot to capture the rendered page
3. Examine the screenshot to understand the page layout:
   - Is this a table, a card list, or something else?
   - Where is the main data on the page?
   - What fields are visible in each item?
   - Are there pagination controls?
```

If the screenshot shows no data, the page likely requires JavaScript rendering. Note `dynamic: true` for later.

**Wait for content to load** (for dynamic pages):
```
browser_evaluate: "document.querySelector('body').innerHTML.length"  // check if content exists
// If length is small or 0, use browser_evaluate to wait:
//   await new Promise(r => setTimeout(r, 2000))
```

### Step 2 — Locate the Data Container

Instead of reading HTML, **ask Playwright to find the container**:

```
browser_evaluate: 
  // Find all table elements
  JSON.stringify(
    Array.from(document.querySelectorAll('table')).map(t => ({
      tag: 'table',
      id: t.id,
      classes: t.className,
      rows: t.rows.length,
      position: t.getBoundingClientRect()
    }))
  )
```

Look at the results to identify which container holds the data. Check `position` (is it visible? does it span the main area?) and `rows` (count).

For card layouts, query for containers:
```
browser_evaluate:
  JSON.stringify(
    Array.from(document.querySelectorAll('div, article, li, section'))
      .filter(el => el.children.length > 2 && el.children.length < 20)
      .map(el => ({
        tag: el.tagName,
        id: el.id,
        class: el.className.substring(0, 50),
        childCount: el.children.length,
        position: el.getBoundingClientRect()
      }))
      .filter(el => el.position.height > 50 && el.position.width > 100)
  )
```

**Choose the best candidate** by:
- Reasonable child count (not too few, not too many)
- Visible on page (`position.height` > 0)
- Matches expected layout type

### Step 3 — Identify Items Within the Container

Once you have the container, find the individual items:

```
browser_evaluate:
  // Assuming we found table at index 0
  const container = document.querySelectorAll('table')[0];
  JSON.stringify({
    containerSelector: getContainerSelector(container),
    itemCount: container.rows.length,
    firstItem: getElementSnapshot(container.rows[0]),
    lastItem: getElementSnapshot(container.rows[container.rows.length - 1])
  })
```

Where `getElementSnapshot` is a helper that captures meaningful info:
```javascript
function getElementSnapshot(el) {
  return {
    html: el.outerHTML.substring(0, 300),
    text: el.innerText.substring(0, 100),
    children: Array.from(el.children).map(c => ({
      tag: c.tagName,
      class: c.className.substring(0, 30),
      text: c.innerText.substring(0, 50)
    }))
  };
}

function getContainerSelector(el) {
  if (el.id) return '#' + el.id;
  if (el.className && !el.className.includes(' ')) return el.tagName.toLowerCase() + '.' + el.className;
  return el.tagName.toLowerCase();
}
```

### Step 4 — Extract Fields with Selector Verification

For each field you want to extract:

**1. Locate the field visually:**
```
browser_evaluate:
  // Find elements that contain the expected text
  JSON.stringify(
    Array.from(document.querySelectorAll('td, th, div, span, a'))
      .filter(el => el.innerText.includes('expected_text'))
      .map(el => ({
        tag: el.tagName,
        text: el.innerText.substring(0, 100),
        selector: getSelector(el),
        rect: el.getBoundingClientRect()
      }))
  )
```

**2. Build a stable selector:**
```
browser_evaluate:
  // Get the element's stable selector (id > class > nth-child)
  function getSelector(el) {
    if (el.id) return '#' + el.id;
    const parent = el.parentElement;
    if (parent) {
      const siblings = Array.from(parent.children).filter(c => c.tagName === el.tagName);
      if (siblings.length > 1) {
        const index = siblings.indexOf(el) + 1;
        return getSelector(parent) + ' > ' + el.tagName.toLowerCase() + ':nth-child(' + index + ')';
      }
    }
    return el.tagName.toLowerCase() + (el.className ? '.' + el.className.split(' ')[0] : '');
  }
  getSelector(document.querySelector('your_selector'))
```

**3. Verify the selector is reliable:**
```
browser_evaluate:
  // Count how many elements match
  document.querySelectorAll('your_selector').length
  // Should be 1 for a unique field, or N for a repeated field
```

**4. Test extraction:**
```
browser_evaluate:
  // Simulate what the scraper will extract
  const el = document.querySelector('your_selector');
  JSON.stringify({
    text: el ? el.innerText.trim() : null,
    href: el ? (el.tagName === 'A' ? el.href : el.querySelector('a')?.href) : null
  })
```

### Step 5 — Analyze Detail Pages Visually

Navigate to a detail page and **take another screenshot** to understand its structure.

```
1. Extract a detail link URL:
   browser_evaluate: document.querySelector('your_detail_link_selector').href

2. Navigate to the detail page:
   browser_navigate: <detail_url>

3. Take a screenshot:
   mcp_playwright_screenshot

4. Analyze the structure programmatically:
   browser_evaluate:
     // Find all dt/dd pairs, th/td pairs, labeled sections
     JSON.stringify({
       definitions: Array.from(document.querySelectorAll('dt')).map(dt => ({
         term: dt.innerText,
         def: dt.nextElementSibling?.innerText?.substring(0, 100)
       })),
       tables: Array.from(document.querySelectorAll('table')).map(t => ({
         id: t.id,
         rows: t.rows.length,
         headers: Array.from(t.rows[0]?.cells || []).map(c => c.innerText)
       }))
     })
```

### Step 6 — Handle Pagination

**Look for pagination in the screenshot first.** Then verify programmatically:

```
browser_evaluate:
  JSON.stringify({
    nextButtons: Array.from(document.querySelectorAll('a, button'))
      .filter(el => /next|weiter|suivant|page/i.test(el.innerText))
      .map(el => ({
        text: el.innerText,
        selector: getSelector(el),
        href: el.href || el.onclick?.toString()
      })),
    pageLinks: Array.from(document.querySelectorAll('a'))
      .filter(el => /\d+/.test(el.innerText))
      .map(el => ({
        text: el.innerText,
        href: el.href
      }))
  })
```

Determine pagination strategy:
- **Href-based**: Next button has an `href` attribute
- **URL-pattern**: Page numbers in links, no obvious next button
- **Click-based**: Next button triggers JavaScript without URL change

### Step 7 — Generate Config with Verified Selectors

Now generate the JSON using only **verified selectors** from Step 4:

```json
{
    "entranceUrl": "https://example.com/source",
    "dynamic": true,
    "dynamicConfig": {
        "waitForSelector": "table#data, div.card-container",
        "timeout": 15000
    },
    "mainTable": {
        "selector": "#data",
        "hasHeader": true,
        "columns": [
            { "headerText": "Name", "key": "name", "isDetailLink": true },
            { "headerText": "Status", "key": "status" }
        ]
    },
    "detailPage": {
        "fields": [
            { "key": "address", "selector": "dl.info dd:nth-of-type(2)" },
            { "key": "phone", "selector": "dl.info dd:nth-of-type(4)" }
        ]
    },
    "pagination": {
        "nextButtonSelector": "a.next",
        "useHref": true
    },
    "rateLimit": {
        "requestsPerMinute": 12,
        "randomize": true
    },
    "cache": {
        "enabled": true,
        "ttlMinutes": 120,
        "cacheDir": "target/test-cache"
    }
}
```

**Rule: Only include selectors you verified in Step 4.** If you didn't test it, don't add it.

### Step 8 — Save Fixtures and Test

**Cache the page HTML for offline testing:**

```
browser_evaluate: document.documentElement.outerHTML
```

Save to `src/test/resources/fixtures/{source_name}.html`. Create a test config variant that uses `file:` URLs and remove dynamic/rateLimit settings.

**Run validation:**
```
mvn test -Dtest=ConfigValidationTest -DconfigPath=config/{source}.json
```

---

## Selector Derivation Strategies

### From Text Content
```javascript
// Find element by its text
Array.from(document.querySelectorAll('*'))
  .find(el => el.innerText.trim() === 'Exact Label Name')
```

### From Position
```javascript
// Get element at specific screen position (useful for finding items by row)
document.elementFromPoint(x, y)
```

### From Semantic Role
```javascript
// Find by aria-label, role, or data attributes
document.querySelector('[role="button"][aria-label="Next"]')
```

### Stable Selectors (in priority order)
1. `#id` — unique identifier
2. `[data-testid="value"]` — test-specific attribute
3. `.specific-class` — non-generic class
4. `tag.class:nth-child(n)` — position-based
5. Avoid: `:nth-of-type(n)` alone — fragile without parent context

---

## Common Pitfalls

| Pitfall | Solution |
|---------|----------|
| Selector matches multiple elements | Add parent context or use `:nth-child()` |
| Selector matches zero elements | Check if content is in iframe, shadow DOM, or loaded dynamically |
| Detail page links are relative | Always resolve against `window.location.origin` |
| Pagination button is hidden | Check `display: none`, `visibility: hidden`, or inside scrollable container |
| Table has merged cells | Use `rowSpan`/`colSpan` awareness, or extract from nested structure |

---

## Output

When complete, report:
1. The generated config file path
2. Number of items extracted (from test run)
3. Sample of first 2-3 items (field names and values)
4. Any selectors that were difficult to verify (flag as needing review)
5. Whether dynamic rendering was required
