# BE-1D-04 — KYC levels, document types, rejection reasons

**Area:** novawallet-api · **Needs:** BE-1D-03 · **Decisions:** DEC-15 (limits per level) · **Screens:** D01–D09, E01, X05, X06

---
Follow `_COMMON_FEATURE_RULES.md`.

Endpoints:
- `GET /api/v1/kyc/tiers` — the account levels shown on D02: name, requirements, limits (per transaction, daily, monthly, balance cap), what the user unlocks.
- `GET /api/v1/kyc/status` extended: current level, per-document status, review state (D07), rejection reason code + user message + which document to redo (D09), next step.
- Admin: rejection reason catalogue; reject with reason code(s); request re-upload of one document.

Do:
1. Level/limit values come from a config table (placeholders flagged `OWNER TO SET` until DEC-15); the limits engine in PAY-04 will read the same table.
2. D03 "Choose your ID" → document types allowed per level (NRC, passport, driver's licence if the design lists it — follow the BUILD.md).
3. State machine documented and enforced: NOT_STARTED → IN_PROGRESS → SUBMITTED/IN_REVIEW → VERIFIED | ACTION_NEEDED → …; illegal transitions rejected.
4. Notifications on state change (in-app now; push later).
5. Keep everything from BE-1C-6 (ownership, audit, upload hardening).

Tests: tier listing, status mapping for every state, reject-with-reason → status shows it, illegal transitions, limits reflect level.
