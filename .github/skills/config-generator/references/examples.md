# Config Examples — Annotated

Real-world and test configurations demonstrating each layout type and feature.

---

## Example 1: Simple Card Layout

**Use case:** Page with repeated `<div>` containers, each holding a product card.  
**Source:** `src/test/resources/config-card.json`

```json
{
    "entranceUrl": "https://example.com/products",
    "mainCard": {
        "itemSelector": "div.product-card",
        "fields": [
            {
                "key": "id",
                "selector": "span.id",
                "optional": false
            },
            {
                "key": "name",
                "selector": "h3.title a"
            },
            {
                "key": "nameUrl",
                "selector": "h3.title a"
            },
            {
                "key": "price",
                "selector": "div.price"
            },
            {
                "key": "category",
                "selector": "span.category",
                "optional": true
            }
        ]
    },
    "detailPage": {
        "fields": [
            {
                "key": "description",
                "selector": "p.description"
            }
        ]
    },
    "pagination": {
        "nextButtonSelector": "a.next"
    }
}
```

**Key points:**
- `"nameUrl"` — key ending with `Url` extracts the `href` from the `<a>` element. The scraper also uses this URL to navigate to the detail page.
- `"optional": false` on `id` — items without an `id` element are skipped entirely.
- `"optional": true` on `category` — missing category is tolerated.
- `detailPage.fields` — enriches each card with data from its linked detail page.
- Simple `pagination` — follows the `<a class="next">` link.

---

## Example 2: Table with Detail Page and SubTable

**Use case:** HTML table listing companies, each row links to a detail page with additional fields and a nested permissions table.  
**Source:** based on `config/b_c.json` (BaFin company register)

```json
{
    "entranceUrl": "https://portal.mvp.bafin.de/database/InstInfo/",
    "mainTable": {
        "selector": "#institut",
        "hasHeader": true,
        "columns": [
            { "headerText": "Unternehmen", "key": "name", "isDetailLink": true },
            { "headerText": "Gattung", "key": "gattung" }
        ]
    },
    "detailPage": {
        "fields": [
            { "key": "name", "selector": "div#content p:nth-of-type(1)" },
            { "key": "address", "selector": "div#content p:nth-of-type(2)" },
            { "key": "bafinId", "selector": "dl.docData:nth-of-type(2) dd:nth-of-type(1)" },
            { "key": "id", "selector": "dl.docData:nth-of-type(2) dd:nth-of-type(2)" },
            { "key": "regNr", "selector": "dl.docData:nth-of-type(2) dd:nth-of-type(3)" }
        ],
        "subTable": {
            "selector": "#erlaubnis",
            "hasHeader": true,
            "columns": [
                { "index": 0, "key": "permission" },
                { "index": 1, "key": "notBefore" },
                { "index": 2, "key": "notAfter" },
                { "index": 3, "key": "reason" }
            ]
        }
    },
    "pagination": {
        "nextButtonSelector": "a:contains(vor)"
    }
}
```

**Key points:**
- `hasHeader: true` + `headerText` — columns matched by header text, not position.
- `"isDetailLink": true` — the first column's `<a>` href is the detail page URL.
- `detailPage.subTable` — nested table on the detail page, stored as JSON under `__subTable`.
- `subTable` columns use `index` (position-based) since the sub-table headers may vary.

---

## Example 3: Dynamic Page with Playwright

**Use case:** JavaScript-rendered page (SPA) that requires browser execution. Uses URL pattern pagination.  
**Source:** `src/test/resources/config-dnb-register.json`

```json
{
    "entranceUrl": "https://www.dnb.nl/en/public-register/?p=1&l=20",
    "dynamic": true,
    "dynamicConfig": {
        "waitForSelector": "ul.register-search__results__list",
        "timeout": 15000
    },
    "mainCard": {
        "itemSelector": "article.register-result",
        "fields": [
            {
                "key": "statutoryName",
                "selector": "div.register-result__statutory-name .register-result__title a",
                "optional": false
            },
            {
                "key": "tradeName",
                "selector": "div.register-result__trade-name .register-result__value",
                "optional": true
            },
            {
                "key": "register",
                "selector": "div.register-result__register .register-result__value",
                "optional": false
            },
            {
                "key": "country",
                "selector": "div.register-result__country .register-result__value",
                "optional": false
            }
        ]
    },
    "pagination": {
        "nextButtonSelector": "li.pagination__item--next-page button.pagination__link",
        "useHref": false,
        "urlPattern": "https://www.dnb.nl/en/public-register/?p={page}&l=20",
        "activePageSelector": "li.pagination__item--is-active button.pagination__link"
    }
}
```

**Key points:**
- `"dynamic": true` — tells the scraper to use Playwright instead of JSoup.
- `dynamicConfig.waitForSelector` — waits for the results list to render before extracting.
- `urlPattern` with `{page}` — pagination computes next URL by incrementing the page number.
- `activePageSelector` — finds the current page number to calculate the next one.

---

## Example 4: Card with Transformers

**Use case:** Extracted text needs cleaning, parsing, or reformatting.  
**Source:** `src/test/resources/config-card-transformers.json`

```json
{
    "entranceUrl": "https://example.com/products",
    "mainCard": {
        "itemSelector": "div.product",
        "fields": [
            {
                "key": "type",
                "selector": "span.sku",
                "transformers": [
                    { "type": "trim" },
                    { "type": "regexExtract", "pattern": "SKU:\\s*(\\w+)", "group": 1 }
                ]
            },
            {
                "key": "id",
                "selector": "span.sku",
                "transformers": [
                    { "type": "trim" },
                    { "type": "regexExtract", "pattern": "SKU:\\s*(\\w+)-(\\d+)", "group": 2 }
                ]
            },
            {
                "key": "price",
                "selector": "span.price",
                "transformers": [
                    { "type": "trim" },
                    { "type": "replace", "pattern": "$", "replacement": "" },
                    { "type": "replace", "pattern": ",", "replacement": "" },
                    { "type": "parseFloat" },
                    { "type": "defaultIfEmpty", "defaultValue": "0.0" }
                ]
            },
            {
                "key": "titleSlug",
                "selector": "h3",
                "transformers": [
                    { "type": "trim" },
                    { "type": "lowercase" },
                    { "type": "replace", "pattern": " ", "replacement": "-" },
                    { "type": "substring", "start": 0, "end": 20 }
                ]
            },
            {
                "key": "category",
                "selector": "span.cat",
                "transformers": [
                    { "type": "defaultIfEmpty", "defaultValue": "Uncategorized" }
                ]
            }
        ]
    }
}
```

**Key points:**
- Transformers are applied **in sequence** — output of one feeds into the next.
- `regexExtract` — pull specific capture groups from text (e.g., extract ID from SKU string).
- Price cleaning pipeline: trim → remove `$` → remove `,` → parse as float → default if empty.
- `titleSlug` pipeline: trim → lowercase → replace spaces with hyphens → truncate to 20 chars.

---

## Example 5: Table with SubTable and Row Filter

**Use case:** Detail page has a permissions table; only rows marked with a specific icon should be kept.  
**Source:** based on `config/r_a.json` (REGAFI register)

```json
{
    "entranceUrl": "https://www.regafi.fr/spip.php?page=results",
    "mainTable": {
        "selector": ".table",
        "hasHeader": true,
        "columns": [
            { "headerText": "REGAFI identifier", "key": "id", "isDetailLink": true },
            { "headerText": "Name", "key": "name" },
            { "headerText": "Status", "key": "status" },
            { "headerText": "Category", "key": "category" }
        ]
    },
    "detailPage": {
        "subTable": {
            "selector": "#zone_en_europe table[summary^='List of activities']:first",
            "hasHeader": true,
            "rowFilter": {
                "selector": "td:first-child img",
                "attr": "alt",
                "value": "Ticked activity"
            },
            "columns": [
                { "index": -1, "key": "permission" }
            ]
        }
    },
    "pagination": {
        "nextButtonSelector": "a:contains(Next page)"
    }
}
```

**Key points:**
- `rowFilter` on `subTable` — only keeps rows where `td:first-child img[alt]` equals `"Ticked activity"`.
- `"index": -1` — extracts the **last** column (flexible when column count varies).
- No `detailPage.fields` — only the subTable is extracted from the detail page.
