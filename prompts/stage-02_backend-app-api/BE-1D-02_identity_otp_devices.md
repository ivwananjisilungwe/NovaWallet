# BE-1D-02 — Identity: phone OTP, password reset, devices, security centre 🔒

**Area:** novawallet-api · **Needs:** BE-1D-01 · **Decisions:** DEC-13 (SMS provider), the sign-up conflict decision from S0-03 (phone+OTP vs email+password), SEC-07 · **Screens:** B01–B03, C01, C03–C06, O03, O04, Q10, Q11

---
Follow `_COMMON_FEATURE_RULES.md`. Use T5 plan-first: this changes authentication.

Endpoints (names from the designs):
- `POST /api/v1/auth/otp/send`, `POST /api/v1/auth/otp/verify` — phone verification for sign-up (B01–B02), new-device check (C05) and phone change (Q10).
- `POST /api/v1/password/forgot`, `POST /api/v1/password/reset` (C03–C04) — if they already exist under another path, keep one canonical path and report.
- `POST /api/v1/auth/device/verify`, `GET /api/v1/devices`, `DELETE /api/v1/devices/{id}` (C05, O04).
- `GET /api/v1/security/overview`, `POST /api/v1/security/lock` (O03: emergency lock of the account/wallet by the user).
- Change phone number flow (Q10) and verify email (Q11).

Do:
1. **OTP:** 6 digits, hashed at rest, 5-minute expiry, single use, max attempts, resend cooldown, per-phone and per-IP limits, purpose-bound (signup / device / phone-change / pin-reset), uniform responses (no enumeration). `SmsSender` interface with a logging/sandbox implementation; the real provider adapter only if its API docs are in `docs/providers/` (else `BLOCKED: DEC-13`). Never return the OTP in a response; in the `dev` profile only, expose it through a clearly dev-only mechanism.
2. **Registration** per the owner's decision on the sign-up conflict: support the designed phone-first flow (phone → OTP → details → PIN) while keeping email login working; migrate nothing destructively.
3. **Devices:** device id + name + platform + last seen bound to refresh tokens; new device requires OTP (C05); removing a device revokes its refresh tokens and access tokens (V16 store); notify the user on new device.
4. **Password reset:** hashed tokens, 1-hour expiry (existing), revoke all sessions on success.
5. **SEC-07:** if the owner approves closing it here, add email-verification token expiry + resend with tests; otherwise leave it and say so.
6. **Security lock:** user-initiated freeze that blocks money movement and logins from new devices until support/explicit unlock; audited; cannot be used to clear an admin/fraud freeze.

Tests: OTP lifecycle and abuse cases, device binding and revocation, session revocation on reset, enumeration checks, lock semantics.
