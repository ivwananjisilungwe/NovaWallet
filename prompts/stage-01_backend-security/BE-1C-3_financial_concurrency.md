# BE-1C-3 — Phase 1C Batch 3: Wallet, account & financial concurrency 🔒 (financially critical)

**Area:** novawallet-api · **Needs:** BE-1C-2 · **Decisions:** DEC-04, DEC-05, DEC-12

---
Follow `_COMMON_BATCH_RULES.md` for Batch 3.

Scope (plan §7 Batch 3): R-01, R-02, R-03, FIN-09, FIN-11, NEW-FIN-01. Read each identifier's definition in the traceability matrix and risk register before touching code.

Do:
1. For each identifier: write the failing test that demonstrates the defect **first** (concurrency tests use real threads/latches against PostgreSQL via the `pgtest` profile where H2 can't reproduce locking).
2. Fix at the application level (the DB constraint comes in Batch 4):
   - idempotent wallet/account creation (concurrent KYC approval must not create two wallets; second attempt returns the existing one or a controlled retry, never a 500),
   - deterministic lock ordering for every multi-wallet operation,
   - balance checks and debits under the correct lock (`SELECT … FOR UPDATE` / pessimistic lock), no read-modify-write races,
   - DEC-12: restoring a soft-deleted user must not clear a fraud freeze; store freeze reason; require explicit admin unfreeze.
3. Verify the payout three-stage flow and reconciliation remain correct under concurrent payouts from the same wallet (no overdraft, no double debit, holds released exactly once).
4. Document invariants in `docs/architecture/financial-invariants.md`: e.g. balance ≥ 0, sum of holds ≤ balance, one wallet per user, each idempotency key → at most one financial effect.

Tests: concurrency suites (N parallel transfers/payouts/approvals), invariant assertions after each, PostgreSQL 16 run recorded.
