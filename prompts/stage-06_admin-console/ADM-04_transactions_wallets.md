# ADM-04 — Transactions, transaction detail, wallets 🔒

**Area:** novawallet-admin · **Needs:** ADM-03

---
Follow `prompts/stage-06_admin-console/_COMMON_ADMIN_RULES.md` exactly.

Specifics:
- Screens: X07 transactions (filters: reference, status, type, provider, amount, date, wallet/user; saved filters; CSV export permission-gated and audited), X08 detail (status timeline, provider attempts and raw status codes, ledger entries for the transaction, related items, risk decision, customer-visible text).
- Actions from X08: retry status query, escalate to reconciliation (X09), add internal note, request manual adjustment (maker-checker) — never edit balances directly.

Acceptance: listed screens pass the fidelity compare in light and dark, role-matrix tests green, gates green, flows verified in a browser against the sandbox backend.
