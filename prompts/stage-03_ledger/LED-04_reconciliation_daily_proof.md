# LED-04 — Reconciliation & daily proof 🔒

**Area:** novawallet-api · **Needs:** LED-03 · **Screens:** X09 (admin reconciliation), X02 metrics

---
Follow `_COMMON_FEATURE_RULES.md`. Read `docs/v5/part-08/03_MOBILE_MONEY_INTEGRATION.md` §5 and `docs/v5/part-07/screens/X09_admin_reconciliation/BUILD.md`.

Do:
1. **Daily proof job** (Africa/Lusaka business day): trial balance = 0; customer liabilities = Σ wallet accounts; "wallets + card pool + fee income = provider clearing + settlement bank" per the signed definition in `ledger-design.md`. Stores a dated proof record (pass/fail, figures, differences).
2. **Provider statement reconciliation:** import a provider settlement/transaction file (format per provider docs in `docs/providers/`; if absent build the importer against a documented internal CSV format and mark the Lipila mapping `BLOCKED`). Match by provider reference + amount; classify: matched, missing in ledger, missing at provider, amount mismatch, duplicate.
3. **Exception queue** for X09: list, filter, assign, resolve with a reason; resolutions that move money create maker-checker adjustment entries (BE-1D-07), never direct edits.
4. Continuous checks: stuck pending payouts/collections beyond SLA, suspense account ageing, drift detector results.
5. Admin endpoints for X09 and the metrics used by X02; CSV export permission-gated and audited.
6. Alerts as records + log events now (wired to real alerting in OPS-05).

Tests: proof passes on clean data and fails on injected imbalance; each reconciliation class detected; resolution creates balanced entries; time-zone boundary tests.
