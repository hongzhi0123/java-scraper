# Config Schema Reference

The configuration JSON format is identical to the original [config-generator skill](../config-generator/references/config-schema.md).

For full schema documentation, see: [`../config-generator/references/config-schema.md`](../config-generator/references/config-schema.md)

## Quick Reference

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `entranceUrl` | string | yes | Starting URL |
| `mainTable` | object | one-of | Table layout extraction rules |
| `mainCard` | object | one-of | Card/div layout extraction rules |
| `detailPage` | object | no | Detail page extraction rules |
| `pagination` | object | no | Pagination configuration |
| `dynamic` | boolean | no | Use Playwright for JS rendering |
| `rateLimit` | object | no | Request throttling |
| `cache` | object | no | Response caching |

## Transformers Available

| Type | Description |
|------|-------------|
| `trim` | Remove leading/trailing whitespace |
| `lowercase` | Convert to lowercase |
| `uppercase` | Convert to uppercase |
| `parseInt` | Parse as integer |
| `parseFloat` | Parse as float |
| `replace` | Replace text (pattern + replacement) |
| `regexExtract` | Extract via regex capture group |
| `split` | Split by delimiter, get index |
| `substring` | Extract substring by position |
| `defaultIfEmpty` | Use default if empty |
| `getPrefix` | Get part before delimiter |
| `getSuffix` | Get part after delimiter |

## Key Naming Convention

- `name` → extract text content
- `nameUrl` → extract `href` attribute (for detail links)
- `isDetailLink: true` → table column is a detail link
