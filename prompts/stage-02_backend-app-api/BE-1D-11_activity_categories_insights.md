# BE-1D-11 — Activity search, categories, insights, statements

**Area:** novawallet-api · **Needs:** BE-1D-10 · **Screens:** E02, J01–J05, J07, M01

---
Follow `_COMMON_FEATURE_RULES.md`.

Endpoints: `GET /wallets/{walletId}/transactions?page=&size=&type=&from=&to=&q=` (J01/J02 filters + search), `GET /transactions/{reference}` enriched (J03), `PATCH /api/v1/transactions/{reference}/category`, `GET /api/v1/insights/summary?month=` (M01), statements (J05).

Do:
1. **Unified activity feed:** one list covering transfers, payouts, top-ups, bills, airtime, goals, chilimba, card transactions, fees — each item with type, direction, counterparty display name, status, category, icon key (matching the category tiles in `docs/v5/part-06`), reference. Stable keyset pagination; indexes for the filters; search by name/reference/amount.
2. **Transaction detail** fields for J03: status timeline, fee, total, counterparty, method, provider reference, linked items (request, split, bill token), actions allowed (report a problem, repeat, share receipt).
3. **Categories:** fixed catalogue matching the design tiles; rule-based auto-categorisation (type, biller, merchant category, keywords) — AI improves it later (AI-01); user override stored and respected; override teaches a per-user rule.
4. **Insights summary:** money in/out, by category, top merchants/recipients, comparison with the previous month, budget progress — computed by SQL aggregation with caching; includes cash entries separately flagged.
5. **Statements:** month list + summary; PDF generation is in BE-1D-12.
6. Performance: seed 100k transactions for one wallet; record query plans and timings; all list endpoints < 200 ms at p95 locally.

Tests: filter combinations, pagination stability under inserts, category override, summary arithmetic exact in ngwee, time-zone month boundaries.
