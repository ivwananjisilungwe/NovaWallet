# PAY-05 — Bills, ZESCO tokens, airtime, data bundles, digital store 🔒 ⛔

**Area:** novawallet-api · **Needs:** PAY-04 · **Blocked by:** DEC-16 (aggregator contract) for real adapters · **Screens:** K00–K06, Q04

---
Follow `_COMMON_FEATURE_RULES.md`. Read each K-screen BUILD.md and Q04.

Endpoints (from the designs): `GET /api/v1/billers`, `GET /billers/saved` (+ save/remove), `POST /api/v1/bills/validate`, `POST /api/v1/bills/pay`, airtime & data bundle catalogue + purchase, `GET /api/v1/store/products`, `POST /api/v1/store/orders`.

Do:
1. Catalogue: billers by category with required fields (meter number, account number, smart-card number), validation patterns, logos (category tiles in `docs/v5/part-06`), amounts/bundles per network.
2. **Validate before pay** (K02): returns the customer name/account details from the provider so the user confirms the right meter/account; a short-lived validation token must accompany `pay`.
3. Pay = a `BILL`/`AIRTIME` payment intent through the orchestration engine: hold → provider → success with **token/voucher delivery** (K03 "Token ready": store the ZESCO token/voucher encrypted, show in the receipt and notification list, never in push payloads), failed → release, unknown → reconciliation; **no refund without a definite failure**.
4. Digital store (K05/K06): products, price, stock/availability from the provider, order → fulfilment code delivery; same intent mechanics.
5. Saved billers and recents per user.
6. **Providers:** everything runs on a `BillsProvider` SPI with the sandbox simulator. Real adapter only with the aggregator's API documents in `docs/providers/` — otherwise `BLOCKED: DEC-16` and feature flags `bills`, `airtime`, `store` stay off in production.
7. Fees/commissions via PAY-04; ledger recipes from `ledger-design.md` (add recipes if missing and get them approved).

Tests: validation token required, delivery of token exactly once, unknown-outcome handling, duplicate submit, catalogue caching, flags.
