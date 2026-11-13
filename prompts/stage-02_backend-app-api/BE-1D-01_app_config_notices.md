# BE-1D-01 — App config & service notices

**Area:** novawallet-api · **Needs:** BE-1C-9 · **Screens:** A01, E05, P05, P06, X12

---
Follow `_COMMON_FEATURE_RULES.md`.

Endpoints:
- `GET /api/v1/app/config` (public, cacheable): minimum supported app version + latest version per platform, force-update flag (P06), maintenance mode + message + expected end (P05), feature flags (cards, bills, store, AI, chilimba, top-up), supported languages, support contacts, legal document versions (Q12).
- `GET /api/v1/app/notices` (auth optional): active service notices with severity, title, body, affected feature, start/end (E05).
- Admin CRUD for notices and config values (used by X12 later): create, schedule, end, list; changes audited.

Do:
1. Tables for notices and config (typed key/value with validation), next Flyway versions.
2. Maintenance mode: a filter returning `503` with error code `MAINTENANCE` and `Retry-After` for non-admin API calls when enabled; health and config endpoints stay up.
3. Version gate: requests with header `X-App-Version` below minimum get `426` with code `UPDATE_REQUIRED` (config endpoint exempt).
4. Feature flags evaluated server-side too: a disabled feature's endpoints return `FEATURE_DISABLED`.
5. `ETag`/`Cache-Control` on config.

Tests: maintenance on/off, version gate boundaries, flag enforcement, notice scheduling windows, no sensitive data in public config.
