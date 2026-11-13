# BE-1D-07 — Admin: staff accounts, roles, TOTP, metrics, alerts 🔒

**Area:** novawallet-api · **Needs:** BE-1D-06 · **Decisions:** DEC-03 · **Screens:** X01, X02, X14, X15 (+ permissions used by X03–X13, X16)

---
Follow `_COMMON_FEATURE_RULES.md`. Use T5 plan-first. Read `docs/v5/part-07/docs/ADMIN_GUIDE.md` and every `docs/v5/part-07/screens/X*/BUILD.md` to extract the permission list.

Do:
1. **Staff identity separate from customers:** staff table, invite by email, first sign-in forces password set + TOTP enrolment (X01). `POST /api/v1/admin/auth/login` (password → TOTP), short admin sessions, separate JWT audience for admin tokens, IP allow-list option.
2. **Roles & permissions** (DEC-03): SUPER_ADMIN, SUPPORT, KYC_REVIEWER, FINANCE, COMPLIANCE/read-only auditor — as granular permissions (e.g. `kyc.review`, `wallet.freeze`, `fees.edit`, `staff.manage`, `pii.reveal`) mapped to roles; method-level enforcement on every admin endpoint; migrate existing ADMIN users to SUPER_ADMIN.
3. **Maker-checker** framework for sensitive actions (fee/limit change, manual adjustment, role grant, provider kill switch): pending approval records; approver ≠ requester; expiry; audited.
4. **Step-up:** sensitive actions require a fresh TOTP.
5. Reason required on every mutating admin action; stored in audit (X14: searchable, immutable per BE-1C-4).
6. `GET /api/v1/admin/metrics/overview` and `GET /api/v1/admin/alerts` for X02 (users, KYC queue size, volumes, success rates, pending reconciliation, float indicators — only numbers the system really has).
7. Staff management endpoints for X15 (invite, change role with maker-checker, deactivate, reset TOTP).
8. PII reveal endpoint: returns unmasked field only with permission + reason; audited.

Tests: permission matrix test (every admin route × every role), TOTP flows (wrong code, replay, clock skew), maker-checker rules, customer token cannot reach admin routes and vice versa.
