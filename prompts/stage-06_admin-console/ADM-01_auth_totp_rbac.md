# ADM-01 — Staff sign-in, TOTP, session, role-aware navigation 🔒

**Area:** novawallet-admin · **Needs:** ADM-00, BE-1D-07

---
Follow `prompts/stage-06_admin-console/_COMMON_ADMIN_RULES.md` exactly.

Specifics:
- Screen: X01. Flow: email + password → TOTP code; first sign-in forces password change + TOTP enrolment (QR + manual key, recovery codes shown once).
- Session handling per ADMIN_AGENTS rule 8 (record the decision: httpOnly cookie vs memory token); idle timeout 15 min with warning; step-up re-auth dialog for sensitive actions; sign-out everywhere.
- Permission context from the token/profile; navigation items and routes hidden/blocked by permission; 403 page; deep links preserved after login.
- Tests: wrong password/TOTP, lockout messaging, expired session, role matrix for navigation, no token in localStorage.

Acceptance: listed screens pass the fidelity compare in light and dark, role-matrix tests green, gates green, flows verified in a browser against the sandbox backend.
