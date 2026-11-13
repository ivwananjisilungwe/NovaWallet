# BE-1D-10 — Budgets, savings goals, round-ups, cash entries, Chilimba 🔒

**Area:** novawallet-api · **Needs:** BE-1D-09 · **Screens:** M02–M09, Q06–Q09

---
Follow `_COMMON_FEATURE_RULES.md`. Use T5 plan-first for Chilimba. Read `docs/DIFFERENTIATOR-FEATURES.md` (Feature 1) and every listed BUILD.md. Use the designs' spelling: **Chilimba**.

Endpoints (from the designs): `GET /api/v1/budgets?month=`, `POST /budgets`; `GET /api/v1/goals`, `POST /goals`, `POST /goals/{id}/deposit` and withdraw (Q08/Q09); round-ups settings (M07); `POST /api/v1/cash-entries` (M08); Chilimba groups (M09, Q06, Q07).

Do:
1. **Budgets** (no money movement): per category per month, spent computed from categorised transactions (BE-1D-11), alerts at thresholds.
2. **Goals** (money moves): each goal has a ledger sub-account; deposit/withdraw are internal transfers (PIN per design, idempotent, limits); target amount/date, progress; optional lock rules only if designed. Closing a goal returns funds to the wallet.
3. **Round-ups** (M07): rule (round to K1/K5/K10 or multiplier) applied after eligible outgoing payments, posted as a goal deposit; never causes overdraft; daily cap; pause/resume; idempotent per source transaction.
4. **Cash entries** (M08): manual, non-ledger records of cash income/spend for insights only, clearly marked as not wallet money.
5. **Chilimba** (rotating group savings): group, members (invite/accept), contribution amount, cycle length, payout order (fixed/random per design), states per cycle. Contributions go to a group pool ledger account; payout to the cycle's recipient when the cycle completes per the designed rule. Define and test: missed contribution, member leaving mid-rotation, dispute/pause by admin, group closure with exact refunds. Write the rules in `docs/product/chilimba-rules.md` and flag legal questions (collective savings) as a DEC for counsel. Feature flag `chilimba`.

Tests: goal ledger assertions, round-up idempotency/no-overdraft, chilimba full rotation simulation with failures, concurrency on pool payouts, sums exact in ngwee.
