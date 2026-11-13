# LED-02 — Ledger schema & posting engine 🔒

**Area:** novawallet-api · **Needs:** LED-01 approved by the owner (check `docs/DECISIONS.md`)

---
Follow `prompts/stage-02_backend-app-api/_COMMON_FEATURE_RULES.md`. Implement exactly `docs/architecture/ledger-design.md`; if you must deviate, stop and ask.

Do:
1. Migrations for accounts, journal entries, postings, constraints and indexes. DB-level protections: append-only (no UPDATE/DELETE for the app role), CHECK amount > 0, a deferred constraint or trigger proving each entry balances, unique (type, business reference).
2. `LedgerService.post(entryRequest)`: validates balance-to-zero, currency, account status; idempotent by key; locks accounts in deterministic order; updates running balances atomically; returns the entry. No other code may write ledger tables.
3. Account provisioning: system accounts seeded by migration; a wallet account created with each wallet (idempotent).
4. Read API (internal): account balance, statement by account, entry by reference.
5. **No business flow uses the ledger yet** — this prompt only builds and proves the engine.
6. Property/invariant tests: random valid entries keep global debit = credit; unbalanced entries rejected; concurrent posts to the same accounts keep balances correct (PostgreSQL 16 tier); replay returns the same entry; negative-balance rules per account type.
7. Performance sanity: 10k postings benchmark recorded (time, no deadlocks).

Report with invariant evidence.
