# ADM-06 — Limits & fees, notices, app config 🔒

**Area:** novawallet-admin · **Needs:** ADM-05, PAY-04, BE-1D-01

---
Follow `prompts/stage-06_admin-console/_COMMON_ADMIN_RULES.md` exactly.

Specifics:
- Screens: X11 limits and fees (versions with effective dates, simulate before submit, maker-checker approval, diff view), X12 notices (create/schedule/end service notices and maintenance mode, preview as the app shows it, feature flags).
- Production placeholder fees warning banner until DEC-15 values are set; every change audited with before/after.

Acceptance: listed screens pass the fidelity compare in light and dark, role-matrix tests green, gates green, flows verified in a browser against the sandbox backend.
