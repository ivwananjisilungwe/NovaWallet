# ADM-02 — Dashboard and users

**Area:** novawallet-admin · **Needs:** ADM-01

---
Follow `prompts/stage-06_admin-console/_COMMON_ADMIN_RULES.md` exactly.

Specifics:
- Screens: X02 dashboard (metrics overview, alerts, pending queues, charts as drawn), X03 users list (server-side search/filter/pagination, masked PII), X04 user detail (profile, KYC state, wallet summary, devices, limits, recent activity, support cases, risk flags; actions: freeze/unfreeze, security lock, reset device, PII reveal with permission + reason, deactivate/restore respecting DEC-12).
- Every action: confirm dialog with exact effect + mandatory reason + step-up when required; result toasts; audit visible in X14.

Acceptance: listed screens pass the fidelity compare in light and dark, role-matrix tests green, gates green, flows verified in a browser against the sandbox backend.
