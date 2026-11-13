# BE-1D-12 — Receipts, statements PDF, support cases, help centre

**Area:** novawallet-api · **Needs:** BE-1D-11 · **Decisions:** DEC-23 (support channels) · **Screens:** J04–J06, O10, O11, X13

---
Follow `_COMMON_FEATURE_RULES.md`.

Endpoints: `GET /api/v1/transactions/{reference}/receipt.pdf` (J04), statement PDF (J05), `POST /api/v1/support/cases` + list/detail/messages (J06 "Report a problem", O11 support chat), `GET /api/v1/help/articles` (O10), admin case endpoints (X13).

Do:
1. **PDF receipts/statements:** server-side generation with the brand assets from `docs/v5/part-06/logo`; owner-only access; includes reference, date (Africa/Lusaka), amounts, fee, counterparty (masked as in the app), status; bill tokens included; a verification code/QR that resolves to a public "receipt is genuine" check without exposing details. Size/time limits; cached.
2. **Support cases:** category, linked transaction (optional), description, attachments (reuse hardened upload from BE-1C-6), status OPEN → IN_PROGRESS → WAITING_CUSTOMER → RESOLVED → CLOSED, message thread between user and staff, SLA timers, satisfaction rating. A case about a transaction snapshots its key facts.
3. **Support chat (O11):** implement as case messages with polling/long-polling per the design; no third-party chat SDK unless the owner decides.
4. **Help articles:** categories, articles (markdown), search, versioned, admin CRUD; seed the articles implied by the designs' help topics (titles only + `CONTENT: OWNER TO WRITE`).
5. **Admin (X13):** queue, filters, assignment, internal notes (never visible to the user), canned replies, link to user/transaction, escalate to reconciliation exception.
6. Notifications on staff reply.

Tests: receipt ownership + content, PDF contains no unmasked PII beyond the owner's own, case state machine, internal notes isolation, attachment security.
