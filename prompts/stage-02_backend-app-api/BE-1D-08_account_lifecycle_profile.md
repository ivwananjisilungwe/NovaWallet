# BE-1D-08 — Profile, preferences, referrals, privacy export, close account

**Area:** novawallet-api · **Needs:** BE-1D-07 · **Decisions:** DEC-08 (retention), DEC-21, DEC-22 · **Screens:** A06, O01, O02, O08, O09, O12, O13, Q12

---
Follow `_COMMON_FEATURE_RULES.md`.

Endpoints: `GET/PUT /users/me` extended (language A06, appearance O08, hide-balance default, discoverability), `GET /api/v1/referrals/me` (O09: code, share link, counts, rewards status), `POST /api/v1/privacy/export` (O12), `POST /api/v1/users/me/close` (O13), legal documents + acceptance records (Q12: version, accepted at).

Do:
1. Preferences persisted server-side so they follow the user across devices.
2. **Referrals:** unique code per user, attribution at sign-up, anti-abuse (self-referral, device/phone reuse), reward rules read from config and **disabled by default** until the owner sets them (rewards move money → must go through the ledger later; until LED-03 only track eligibility).
3. **Privacy export:** asynchronous job producing the user's data (profile, transactions, KYC metadata — not other users' data), delivered through an authenticated, expiring download; rate limited; audited.
4. **Close account:** preconditions (zero balance or guided withdrawal, no pending transactions/chilimba obligations), PIN + confirmation, cooling-off window where the design shows it, then deactivation. **No hard deletion**: retention follows DEC-08; record the request and state. Sessions revoked.
5. Terms/Privacy versions: require re-acceptance when a new version is published (app config exposes current versions).

Tests: preference round-trip, referral abuse cases, export only own data, close-account preconditions and state machine.
