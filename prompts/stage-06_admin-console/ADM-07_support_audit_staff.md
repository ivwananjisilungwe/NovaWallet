# ADM-07 — Support, audit log, staff and roles

**Area:** novawallet-admin · **Needs:** ADM-06, BE-1D-12

---
Follow `prompts/stage-06_admin-console/_COMMON_ADMIN_RULES.md` exactly.

Specifics:
- Screens: X13 support cases (queue, filters, assignment, thread with customer, internal notes, canned replies, links to user/transaction, SLA timers), X14 audit log (search by actor/action/entity/date, immutable view, export permission-gated), X15 staff and roles (invite, role change via maker-checker, deactivate, reset TOTP).
- Role matrix e2e: every role can/cannot reach each screen and action exactly per the permission table.

Acceptance: listed screens pass the fidelity compare in light and dark, role-matrix tests green, gates green, flows verified in a browser against the sandbox backend.
