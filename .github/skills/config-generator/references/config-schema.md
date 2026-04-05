# Scraper Configuration JSON Schema

This document describes every property accepted by the scraper's configuration JSON.  
The Java model classes live in `src/main/java/com/example/scraping/config/`.

---

## Root — `ScrapeConfig`

| Property | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `entranceUrl` | string | **yes** | — | Starting URL (`https://…` or `file:path/to/file.html`) |
| `mainTable` | object | one of † | — | Table-layout extraction rules |
| `mainCard` | object | one of † | — | Card/div-layout extraction rules |
| `detailPage` | object | no | — | Extraction rules for linked detail pages |
| `pagination` | object | no | — | How to navigate to the next page |
| `dynamic` | boolean | no | `false` | `true` → use Playwright (JavaScript rendering) |
| `dynamicConfig` | object | no | — | Playwright-specific settings (only when `dynamic: true`) |
| `rateLimit` | object | no | — | Request throttling |
| `cache` | object | no | — | Response caching |

† Exactly **one** of `mainTable` or `mainCard` must be present.

---

## Card Layout — `CardDefinition` (`mainCard`)

Use when the page renders a list of repeated containers (divs, articles, list items).

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `itemSelector` | string | **yes** | CSS selector matching each item container (e.g. `"div.product-card"`, `"article.result"`) |
| `fields` | FieldDefinition[] | **yes** | Fields to extract from each container |
| `rowFilter` | object | no | Filter out items that don't match a condition |

---

## Table Layout — `TableDefinition` (`mainTable`)

Use when data is inside an HTML `<table>`.

| Property | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `selector` | string | **yes** | — | CSS selector for the `<table>` element |
| `hasHeader` | boolean | no | `true` | Whether the first `<tr>` is a header row |
| `columns` | ColumnDefinition[] | **yes** | — | Column extraction rules |
| `rowFilter` | object | no | — | Filter rows |

---

## Field Extraction — `FieldDefinition` (used in `mainCard.fields` and `detailPage.fields`)

| Property | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `key` | string | **yes** | — | Output field name. **Convention:** if key ends with `Url` → extract `href` attribute instead of text |
| `selector` | string | **yes** | — | CSS selector relative to the item container |
| `optional` | boolean | no | `true` | If `false`, the item is skipped when this field is missing |
| `rawHtml` | boolean | no | `false` | If `true`, extract inner HTML instead of text |
| `transformers` | TransformerDef[] | no | — | Post-extraction text transformations (applied in order) |

### Key naming conventions

- `"key": "nameUrl"` — the suffix `Url` tells the scraper to extract the `href` attribute from the matched `<a>` element. The scraper also uses this key to find the detail page link.
- `"key": "someField"` — plain keys extract the text content of the matched element.

---

## Column Extraction — `ColumnDefinition` (used in `mainTable.columns`)

| Property | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `key` | string | **yes** | — | Output field name |
| `index` | integer | no † | — | Column position (0-based). Negative values count from end (`-1` = last column) |
| `headerText` | string | no † | — | Match column by header text (used when `hasHeader: true`). Takes priority over `index` |
| `selector` | string | no | — | Nested CSS selector within the cell (e.g. `"a"` to get anchor text) |
| `isDetailLink` | boolean | no | `false` | If `true`, extract `href` from the cell's `<a>` as the detail page URL |
| `transformers` | TransformerDef[] | no | — | Post-extraction text transformations |

† At least one of `index` or `headerText` must be provided.

---

## Detail Page — `DetailPageDefinition` (`detailPage`)

Fetched for each item that has a detail URL (`*Url` field or `isDetailLink` column).

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `fields` | FieldDefinition[] | no | Fields to extract from the detail page |
| `subTable` | TableDefinition | no | A nested table on the detail page (results stored as JSON under `__subTable` key) |

The `subTable` follows the same `TableDefinition` schema above, including optional `rowFilter`.

---

## Row Filter — `RowFilterDefinition` (`rowFilter`)

Filters items (cards or table rows) based on a condition within each item.

| Property | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `selector` | string | **yes** | — | CSS selector within the row/card to inspect |
| `attr` | string | no | — | Attribute to check. If `null`, the element's text content is used |
| `value` | string | **yes** | — | Expected value |
| `contains` | boolean | no | `false` | `true` → substring match (case-insensitive). `false` → exact match |
| `not` | boolean | no | `false` | `true` → invert the condition (keep items that do NOT match) |

---

## Pagination — `PaginationDefinition` (`pagination`)

| Property | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `nextButtonSelector` | string | **yes** | — | CSS selector for the "next page" link/button |
| `useHref` | boolean | no | `true` | Extract `href` from the element for the next page URL |
| `urlPattern` | string | no | — | URL template with `{page}` placeholder (e.g. `"https://site.com/list?p={page}&l=20"`) |
| `activePageSelector` | string | no | — | Selector for the current active page element (used with `urlPattern` to determine current page number) |
| `baseUrl` | string | no | — | Base URL for resolving relative hrefs |

### Pagination strategies

1. **Href-based** (`useHref: true`, default): Extract `href` from `nextButtonSelector` element.
2. **URL pattern** (`urlPattern` set): Compute next page URL by incrementing the page number found in `activePageSelector`.
3. **Click-based** (`useHref: false`, no `urlPattern`): Used with dynamic pages; the button is clicked.

---

## Dynamic Page — `DynamicConfig` (`dynamicConfig`)

Only relevant when `"dynamic": true`. Uses Playwright for JavaScript-rendered pages.

| Property | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `waitForSelector` | string | no | — | CSS selector to wait for before scraping (page is ready when this element appears) |
| `timeout` | integer | no | `30000` | Timeout in milliseconds for page operations |

---

## Rate Limiting — `RateLimitConfig` (`rateLimit`)

| Property | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `requestsPerMinute` | integer | no | `10` | Maximum requests per minute |
| `randomize` | boolean | no | `true` | Add random jitter to request intervals |
| `respectRetryAfter` | boolean | no | `true` | Honor `Retry-After` response headers |

---

## Caching — `CacheConfig` (`cache`)

| Property | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `enabled` | boolean | no | `false` | Enable response caching |
| `ttlMinutes` | integer | no | `60` | Cache time-to-live in minutes (`0` = no expiry) |
| `cacheDir` | string | no | `"target/cache"` | Directory for cached responses |

---

## Transformers — `TransformerDef`

Applied in sequence to the extracted text. Specified as a JSON array on `fields[].transformers` or `columns[].transformers`.

| Type | Parameters | Example Input → Output |
|------|-----------|----------------------|
| `trim` | — | `"  hello  "` → `"hello"` |
| `lowercase` | — | `"HELLO"` → `"hello"` |
| `uppercase` | — | `"hello"` → `"HELLO"` |
| `parseInt` | — | `"42.99"` → `"42"` |
| `parseFloat` | — | `"99.99"` → `"99.99"` |
| `replace` | `pattern` (string), `replacement` (string) | `"$99"` with `pattern:"$"`, `replacement:""` → `"99"` |
| `regexExtract` | `pattern` (regex), `group` (int) | `"SKU: ELEC-42"` with `pattern:"(\\w+)-(\\d+)"`, `group:2` → `"42"` |
| `split` | `delimiter` (string), `index` (int) | `"a,b,c"` with `delimiter:","`, `index:1` → `"b"` |
| `substring` | `start` (int), `end` (int) | `"hello"` with `start:0`, `end:3` → `"hel"` |
| `defaultIfEmpty` | `defaultValue` (string) | `""` → `"N/A"` |
| `getPrefix` | `delimiter` (string) | `"user_name"` with `delimiter:"_"` → `"user"` |
| `getSuffix` | `delimiter` (string) | `"user_name"` with `delimiter:"_"` → `"name"` |

### Transformer JSON example

```json
"transformers": [
  { "type": "trim" },
  { "type": "replace", "pattern": "$", "replacement": "" },
  { "type": "replace", "pattern": ",", "replacement": "" },
  { "type": "parseFloat" },
  { "type": "defaultIfEmpty", "defaultValue": "0.0" }
]
```
