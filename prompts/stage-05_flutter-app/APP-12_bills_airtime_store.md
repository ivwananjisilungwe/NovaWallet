# APP-12 — Payments hub, bills, ZESCO token, airtime & data, digital store (K00–K06, Q04) 🔒

**Area:** novawallet-app · **Needs:** APP-11, PAY-05 done · **Screens:** K00, K01, K02, K03, K04, K05, K06, Q04

---
Follow `prompts/stage-05_flutter-app/_COMMON_SCREEN_RULES.md` exactly (design fidelity protocol, states, fidelity gate, report).

Screens to build (open each folder via `docs/v5/SCREEN_INDEX.md`): K00 · K01 · K02 · K03 · K04 · K05 · K06 · Q04.

Endpoints: GET /billers, /billers/saved, POST /bills/validate, POST /bills/pay, airtime/data catalogue + purchase, GET /store/products, POST /store/orders.

Specifics:
- K00 hub; K01 biller categories with tiles; K02 ZESCO prepaid: validate meter → show account name → amount → review → PIN; K03 token ready (copy/share, stored in receipt, never in push); K04 airtime and data; Q04 data bundles; K05/K06 store and product detail.
- Unknown outcome follows the same P03 pattern; saved billers; feature flags `bills`, `airtime`, `store`.

Integration: ZESCO validate + pay + token, airtime purchase, failed validation.

Acceptance: every listed screen passes the fidelity compare in both themes (table in the report), all states built, gates green, flow verified on an emulator against the sandbox backend.
