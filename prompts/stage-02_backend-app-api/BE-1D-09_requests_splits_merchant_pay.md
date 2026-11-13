# BE-1D-09 — Money requests, split bills, pay a shop 🔒

**Area:** novawallet-api · **Needs:** PAY-04 (fees/limits), LED-03 · **Screens:** G08, G09, H02, H03, Q05

---
Follow `_COMMON_FEATURE_RULES.md`. Read `docs/DIFFERENTIATOR-FEATURES.md` (Feature 3) and the screens' BUILD.md.

Endpoints (from the designs): `POST /api/v1/payment-requests` (+ list incoming/outgoing, pay, decline, cancel), `POST /api/v1/splits` (+ detail, remind, settle share), `POST /api/v1/merchant-payments` (H03 pay a shop from a scanned QR).

Do:
1. **Requests:** requester, payer (NovaWallet user by recipient token; non-users only if the design shows an invite path), amount, note, expiry; states PENDING → PAID | DECLINED | CANCELLED | EXPIRED. Paying a request (Q05) is a normal transfer (PIN, limits, fees, idempotency, ledger) linked to the request; a request can be paid at most once.
2. **Splits:** creator, total, participants with shares (equal/custom, must sum exactly in ngwee — define remainder rule), each share behaves as a payment request; progress shown in G09; creator's own share handled per the design; reminders rate-limited.
3. **Merchant payments:** resolve merchant QR (BE-1D-05 QR infrastructure; merchant registry minimal: id, name, category, status, settlement wallet) → pay to the merchant wallet with merchant fee rules (PAY-04). Merchant onboarding is admin-only for now.
4. Anti-harassment: block user, limit requests per day to non-contacts, report (feeds support cases later).
5. Notifications for each event with deep links.

Tests: state machines, pay-once under concurrency, share-sum rule, expiry job, blocked users, merchant QR forgery, ledger assertions.
