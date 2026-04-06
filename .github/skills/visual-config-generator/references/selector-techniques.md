# Visual Selector Verification Techniques

This document supplements the config-schema.md with Playwright-based techniques for building reliable selectors.

---

## The Problem with Manual Selector Inference

Traditional config generation asks the agent to:
1. Read raw HTML
2. Infer which selector targets a field
3. Hope it works

This fails because:
- HTML doesn't show visual hierarchy or layout
- The agent can't see which element is "the name" vs "the ID"
- Relative selectors are hard to mentally construct

## The Visual Approach

Instead, use Playwright to **query, verify, and derive** selectors:

---

## Technique 1: Find by Text, Then Extract Selector

```javascript
// Step 1: Find the element containing expected text
const element = Array.from(document.querySelectorAll('td, div, span'))
  .find(el => el.innerText.includes('Company Name'));

// Step 2: Derive a stable selector from that element
function deriveSelector(el) {
  // Prefer ID
  if (el.id) return '#' + el.id;
  
  // Prefer unique class
  if (el.className && !el.className.includes(' ')) {
    return el.tagName.toLowerCase() + '.' + el.className;
  }
  
  // Build path from parent
  const parent = el.parentElement;
  if (parent) {
    const siblings = Array.from(parent.children).filter(c => c.tagName === el.tagName);
    if (siblings.length > 1) {
      const index = siblings.indexOf(el) + 1;
      return deriveSelector(parent) + ' > ' + el.tagName.toLowerCase() + ':nth-child(' + index + ')';
    }
    return deriveSelector(parent) + ' > ' + el.tagName.toLowerCase();
  }
  
  return el.tagName.toLowerCase();
}

// Step 3: Verify it matches exactly one element
const selector = deriveSelector(element);
const matches = document.querySelectorAll(selector);
if (matches.length !== 1) {
  console.log('WARNING: Selector matches ' + matches.length + ' elements');
}
```

---

## Technique 2: Locate by Screen Position

When text search isn't reliable (e.g., "Status" appears multiple times):

```javascript
// Find the data container first
const container = document.querySelector('table#main-data');

// Get the nth row's cell at expected column index
const row = container.rows[1]; // Skip header
const cell = row.cells[2]; // 3rd column (0-indexed)

// Verify the cell is visible and contains expected content
const rect = cell.getBoundingClientRect();
if (rect.height > 0) {
  // Cell is visible
}

// Derive selector from container + row index + column index
const rowSelector = 'table#main-data > tbody > tr:nth-child(2)';
const cellSelector = rowSelector + ' > td:nth-child(3)';
```

---

## Technique 3: Semantic Element Discovery

```javascript
// Find elements by semantic attributes (more stable than CSS classes)
const selectors = [
  '[data-testid="company-name"]',  // Test-specific (very stable)
  '[aria-label="Company Name"]',    // Accessibility (stable)
  '[role="cell"].name',             // ARIA role + class
  'table th.name',                  // Table header + class
  'table td:first-child'            // Position (fallback)
];

// Try each selector until one matches exactly one element
for (const selector of selectors) {
  const matches = document.querySelectorAll(selector);
  if (matches.length === 1) {
    console.log('Found stable selector:', selector);
    break;
  }
}
```

---

## Technique 4: Relative Selector Building

Build selectors relative to a stable container:

```javascript
// 1. Find the stable container (has ID or unique class)
const container = document.querySelector('#company-list');

// 2. Find all potential items within container
const items = container.querySelectorAll('tr, div.card');

// 3. For each item, find fields relative to item
const firstItem = items[0];
const nameCell = firstItem.querySelector('td:first-child');
const statusCell = firstItem.querySelector('td:nth-child(3) a');

// 4. Derive selector from container (not from document root)
const itemSelector = '#company-list tr';

// Field selectors relative to item:
const fieldSelectors = {
  name: 'td:first-child',
  status: 'td:nth-child(3) a'
};
```

---

## Technique 5: Verify Before Committing

Always test selectors before adding to config:

```javascript
// Test function to simulate scraper behavior
function testSelector(selector, expectedText) {
  const el = document.querySelector(selector);
  if (!el) {
    return { success: false, error: 'No element found' };
  }
  
  const text = el.innerText?.trim();
  const href = el.tagName === 'A' ? el.href : el.querySelector('a')?.href;
  
  return {
    success: true,
    text: text,
    href: href,
    textMatches: expectedText ? text?.includes(expectedText) : true
  };
}

// Verify all expected items are matched
function testRepeatingSelector(selector, minCount) {
  const matches = document.querySelectorAll(selector);
  if (matches.length < minCount) {
    return { 
      success: false, 
      error: `Expected at least ${minCount} matches, found ${matches.length}` 
    };
  }
  
  // Check all matches have content
  const emptyCount = Array.from(matches).filter(m => !m.innerText?.trim()).length;
  if (emptyCount > 0) {
    return { 
      success: false, 
      error: `${emptyCount} matches have empty content` 
    };
  }
  
  return { success: true, count: matches.length };
}
```

---

## Technique 6: Dynamic Content Detection

For pages that load content via JavaScript:

```javascript
// Check if content exists in DOM
function hasContent(selector) {
  const el = document.querySelector(selector);
  return el && el.innerHTML.length > 100; // Arbitrary threshold
}

// Check if element is actually visible
function isVisible(selector) {
  const el = document.querySelector(selector);
  if (!el) return false;
  const rect = el.getBoundingClientRect();
  return rect.width > 0 && rect.height > 0;
}

// Wait for dynamic content
async function waitForContent(selector, timeout = 10000) {
  const start = Date.now();
  while (Date.now() - start < timeout) {
    if (hasContent(selector) && isVisible(selector)) {
      return true;
    }
    await new Promise(r => setTimeout(r, 500));
  }
  return false;
}
```

---

## Technique 7: Handling Complex Tables

Tables with merged cells, nested tables, or irregular structure:

```javascript
// Analyze table structure
function analyzeTable(table) {
  return {
    rowCount: table.rows.length,
    colCount: table.rows[0]?.cells.length || 0,
    hasHeader: table.rows[0]?.cells[0]?.tagName === 'TH',
    headers: Array.from(table.rows[0]?.cells || []).map(c => c.innerText.trim()),
    mergedCells: Array.from(table.querySelectorAll('[rowspan], [colspan]')).length
  };
}

// For tables with merged cells, extract from specific row/col
function extractCellValue(table, rowIndex, colIndex) {
  const row = table.rows[rowIndex];
  let col = 0;
  for (const cell of row.cells) {
    const colSpan = parseInt(cell.colSpan) || 1;
    if (col >= colIndex && col < colIndex + colSpan) {
      return cell.innerText.trim();
    }
    col += colSpan;
  }
  return null;
}
```

---

## Selector Stability Ranking

From most to least stable:

| Rank | Selector Type | Example | Stability |
|------|--------------|---------|-----------|
| 1 | ID | `#company-table` | Very stable (unique) |
| 2 | Data attribute | `[data-testid="name"]` | Stable (explicit) |
| 3 | ARIA attribute | `[aria-label="Company"]` | Stable (semantic) |
| 4 | Single class | `div.card` | Moderate |
| 5 | Tag + class + nth | `tr.data-row:nth-child(2)` | Moderate |
| 6 | Tag + nth-child | `tr:nth-child(3)` | Fragile |
| 7 | Tag only | `td` | Very fragile |

**Rule: Prefer selectors higher in the table.**
