# PAY-06 — Cards: waitlist now, issuing module when a partner exists 🔒 ⛔

**Area:** novawallet-api · **Needs:** PAY-05 · **Blocked by:** DEC-18 (card partner) for everything except Part A · **Screens:** L01–L07, X16

---
Follow `_COMMON_FEATURE_RULES.md`. Read `docs/v5/part-08/04_VIRTUAL_CARDS.md` and `docs/CARD-ISSUING-RESEARCH.md`.

**Part A — build now (no partner needed):**
1. `POST /api/v1/cards/waitlist` + status (L01): join, position/eligibility, notify-me preference. Feature flag `cards` = waitlist mode.
2. Admin view of waitlist size and export (X16, permission-gated).

**Part B — only when the partner's API docs are in `docs/providers/cards/` and DEC-18 is recorded. Otherwise STOP after Part A and report `BLOCKED: DEC-18`.**
3. Card module per `04_VIRTUAL_CARDS.md` §5: `GET /cards`, `POST /cards` (L02 design choice), `GET /cards/{id}/transactions`, `POST /cards/{id}/freeze` (L06), `PATCH /cards/{id}/controls` (L05), `POST /cards/{id}/fund` (L07).
4. **PCI scope stays tiny (§4):** the API never receives, stores or logs PAN/CVV/expiry. Card details (L04) are displayed through the partner's secure element/tokenised reveal; our DB stores only partner card id, last4, brand, status.
5. Funding model per the partner (prefunded card pool vs just-in-time): ledger accounts `card pool`, recipes approved in `ledger-design.md`; authorisation/settlement/refund/chargeback webhooks through the generic webhook pipeline with signature verification.
6. Controls: freeze, limits, online/ATM toggles as the partner supports.
7. Admin X16: programme status, float/pool balance, card counts, disputes list.

Tests: waitlist idempotency; (Part B) simulator for authorisation/settlement/reversal with ledger assertions, no PAN in logs test, webhook verification.
