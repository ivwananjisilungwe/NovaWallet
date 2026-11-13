# S0-03 — Decisions register + API contract target

**Area:** docs · **Needs:** S0-02

---
Read root `AGENTS.md`, `novawallet-api/docs/phase-1c/PHASE_1C_DECISION_LOG.md`, `novawallet-api/docs/phase-1c/PHASE_1C_IMPLEMENTATION_PLAN.md` §11–§12, `prompts/02_DECISIONS_AND_BLOCKERS.md`, `reports/API_ENDPOINT_INVENTORY.md`, `docs/v5/SCREEN_INDEX.md`.

**Part A — `docs/DECISIONS.md`** (the one place agents check for owner decisions)
```
| ID | Question | Options | Recommendation | Owner decision | Date | Blocks |
```
1. Copy every DEC/BLK from the Phase 1C plan and decision log with its real status (DEC-01, DEC-02, DEC-10, BLK-03 are decided/implemented — cite the log). Do not change the log itself; it stays canonical for Phase 1C.
2. Add DEC-13 … DEC-24 from `prompts/02_DECISIONS_AND_BLOCKERS.md` with empty "Owner decision".
3. Add "External dependencies" with status (Lipila collections spec, live onboarding, counsel, Play account, pilot partner, fonts).

**Part B — `docs/api/CONTRACT_TARGET.md`** (what "backend complete" means)
1. From `docs/v5/SCREEN_INDEX.md` and each screen's BUILD.md "Data and backend" section, build a table: endpoint · method · screens that use it · exists today? (check controllers, cite file) · backend prompt that will deliver it (use the mapping in `prompts/01_MASTER_ORDER.md`).
2. Add endpoints the admin screens X01–X16 need (read each `docs/v5/part-07/screens/X*/BUILD.md`).
3. Flag conflicts between the designs and the current API (e.g. designs sign up by **phone + OTP** (B01–B02) while the API registers by email + password; designs use `/collections` for top-up while `/deposit` is disabled). List each conflict with options and a recommendation; these become owner decisions (DEC-25+). Do not resolve them yourself.
4. Count: endpoints existing / missing / conflicting.

No source code changes. Stop and report the open decisions ordered by the first prompt they block.
