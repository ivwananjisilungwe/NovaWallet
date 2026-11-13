# LED-03 — Route every money flow through the ledger 🔒

**Area:** novawallet-api · **Needs:** LED-02

---
Follow `_COMMON_FEATURE_RULES.md`. Work one flow at a time; full gates after each; stop if any invariant test fails.

Order: (1) P2P transfer → (2) fees → (3) payout stages A/B/C + reconciliation outcomes + disbursement webhook → (4) withdraw → (5) admin adjustments/freeze effects → (6) balance reads.

Do, per flow:
1. Implement the posting recipe from `ledger-design.md` inside the same DB transaction as the existing wallet/transaction update (**dual-write phase**). The existing public API and responses do not change.
2. Add a per-flow reconciliation assertion test: after N randomised operations (incl. failures, retries, concurrent requests, unknown provider outcomes), wallet balances equal ledger wallet-account balances and the trial balance is zero.
3. Payouts: holds/suspense follow the three-stage design; a reconciled failure reverses exactly once; `PENDING_RECONCILIATION` money sits in the designed suspense/clearing account.

Then:
4. Opening-balance migration job: one journal entry per existing wallet (idempotent, re-runnable, report totals). Dry-run mode first.
5. Drift detector job: compares wallet balance vs ledger balance; any drift → alert record + admin metric; never auto-corrects.
6. Cut-over switch (config): balance reads come from the ledger; the wallet balance column becomes a cache updated only by the ledger service. Resolve R-02 (`BalanceRecalculationJob`) per the design — it must not rewrite history.
7. Update `payout-lifecycle.md` and `financial-invariants.md`.

Tests: all existing money tests still green + the reconciliation assertions on PostgreSQL 16 + crash-in-the-middle tests (exception after ledger post → whole transaction rolls back).
