# PAY-04 — Fees & limits engine 🔒

**Area:** novawallet-api · **Needs:** PAY-03 · **Decisions:** DEC-15 (real values) · **Screens:** F03, F04, G03, I02, D02, X11

---
Follow `_COMMON_FEATURE_RULES.md`.

Do:
1. **Fee rules:** by operation type, provider/network, amount band, KYC level; fixed + percentage with min/max; effective-dated versions (a quote uses the version valid at quote time); rounding rule documented (ngwee, half-up unless the owner says otherwise). `GET /fees/estimate` returns fee, total, and a short-lived signed **quote id**; money endpoints accept the quote id and reject if expired/changed (so the review screen total is the charged total).
2. **Limits:** per transaction, daily, weekly, monthly, balance cap, count-based; per KYC level and operation; time windows in Africa/Lusaka; atomic check-and-reserve inside the money transaction (no race between check and debit). Error codes carry which limit and the remaining allowance (F03 "over limit").
3. `GET` endpoint for the user's own limits and usage (D02, O01 if shown).
4. Fees post to fee-income accounts in the ledger; provider costs can be recorded separately (config) for margin reporting.
5. Admin (X11): view/edit fee and limit versions with **maker-checker**, future-dated activation, full audit, and a "simulate" endpoint (given amount/level → fee) used by the UI before approval.
6. Seed placeholder values clearly marked `PLACEHOLDER — OWNER TO SET (DEC-15)`; the system refuses to start in the production profile while placeholders are active.

Tests: band boundaries, rounding, version switch at midnight Lusaka time, quote expiry/tamper, limit races under concurrency (PostgreSQL 16), maker-checker.
