# BE-1D-06 — Notifications: categories, read state, preferences, push

**Area:** novawallet-api · **Needs:** BE-1D-05 · **Decisions:** DEC-14 (FCM) · **Screens:** A07, E06, E07, O07

---
Follow `_COMMON_FEATURE_RULES.md`.

Endpoints: `GET /api/v1/notifications?category=` (E06 tabs/filters), `PUT /notifications/{id}/read`, existing `read-all` and `unread-count`, `GET/PUT /api/v1/notifications/preferences` (O07: per category × channel), `POST/DELETE` push-token registration bound to the device from BE-1D-02.

Do:
1. Notification model: category (money, security, KYC, promos, system), title, body, deep-link target (screen + id), created/read. Bodies never contain tokens, OTPs, full phone/ID numbers.
2. Preferences: security and transaction-critical notifications cannot be disabled; marketing is opt-in.
3. Push: `PushSender` interface; FCM implementation only if DEC-14 is approved and credentials are supplied by env (never in the repo). **Push payload carries no amounts, names or balances** — a generic line + notification id; the app fetches the details after unlock. Invalid tokens are pruned.
4. Sending is asynchronous and never blocks or fails a money operation (prepare for the outbox in ARC-02: write notification intents in the same transaction as the business change).
5. Email templates kept; SMS only for OTP.

Tests: ownership on read, category filter, preference enforcement, payload redaction test, token pruning, sender failure does not affect the transaction.
