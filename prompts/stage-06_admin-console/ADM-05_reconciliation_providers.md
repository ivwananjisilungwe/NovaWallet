# ADM-05 — Reconciliation and providers (kill switch) 🔒

**Area:** novawallet-admin · **Needs:** ADM-04, LED-04, PAY-01

---
Follow `prompts/stage-06_admin-console/_COMMON_ADMIN_RULES.md` exactly.

Specifics:
- Screens: X09 reconciliation (daily proof status, exceptions queue, provider statement import, matching classes, resolve with reason through maker-checker), X10 providers (health, success rates, per-operation kill switch with maker-checker and a typed-confirmation dialog, recent failures).
- Dangerous-action UX: red confirm, shows customers affected, requires reason + step-up; kill switch changes appear instantly on X02 and are audited.

Acceptance: listed screens pass the fidelity compare in light and dark, role-matrix tests green, gates green, flows verified in a browser against the sandbox backend.
