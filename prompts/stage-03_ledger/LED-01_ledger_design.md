# LED-01 — Double-entry ledger: design only (no code)

**Area:** novawallet-api (docs) · **Needs:** BE-1D-08

---
Read root `AGENTS.md`, `novawallet-api/AGENTS.md`, `novawallet-api/docs/architecture/implementation-roadmap.md` (Phase 2), `payout-lifecycle.md`, `current-architecture.md`, `docs/architecture/financial-invariants.md`, `docs/v5/part-08/01_SYSTEM_ARCHITECTURE.md`, `docs/v5/part-08/03_MOBILE_MONEY_INTEGRATION.md` §5, the ledger diagram in `docs/v5/part-10/` (D24), and the current `TransactionService`, `PayoutService`, wallet and fee code.

Produce `docs/architecture/ledger-design.md`. **Do not modify source.**

Must contain:
1. **Chart of accounts:** customer wallet liabilities (one per wallet), provider clearing (per provider, per direction), settlement bank, fee income, card pool, savings/goal sub-accounts, chilimba pools, suspense/unknown-outcome, rewards expense, adjustments. Type (asset/liability/income/expense), normal balance, who may post.
2. **Schema:** `ledger_accounts`, `journal_entries` (immutable header: id, type, reference, idempotency key, created at, actor), `ledger_postings` (entry id, account, debit/credit, amount in minor units, currency), balance storage strategy (running balance per account with version vs. computed), constraints: every entry balances to zero, amounts > 0, append-only (DB-enforced), unique business reference.
3. **Posting recipes** for every flow, each as a debit/credit table for each stage: P2P transfer, payout stages A/B/C incl. reversal on definite failure and `PENDING_RECONCILIATION`, withdraw, top-up collection (pending → confirmed), fees, bill payment, goal deposit/withdraw, chilimba contribution/payout, card funding, refunds, manual adjustment (maker-checker).
4. **Holds:** how authorisation holds are represented (hold account vs. hold table) and released exactly once.
5. **Migration plan** (expand → migrate → contract): opening-balance entries from current wallet balances, dual-write period, reconciliation of wallet.balance vs ledger, cut-over, what happens to `balanceBefore/After` columns and the `BalanceRecalculationJob` (R-02).
6. **Invariants & proofs:** total debits = total credits; wallet liabilities = sum of wallet balances; the daily proof "wallets + card pool + fee income = provider clearing + settlement bank" stated precisely with signs.
7. **Concurrency:** lock order, isolation level, idempotency.
8. **Test plan** and **rollback plan**. Open questions as DEC items.

Stop. The owner must approve this document before LED-02.
