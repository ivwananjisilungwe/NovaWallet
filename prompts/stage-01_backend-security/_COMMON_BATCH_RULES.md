# Common rules for every BE-1C batch prompt

Every Phase 1C batch prompt in this folder means:

1. Read root `AGENTS.md`, `novawallet-api/AGENTS.md`, `TODO.md`, `novawallet-api/docs/phase-1c/PHASE_1C_IMPLEMENTATION_PLAN.md` (§0 rules, §7 your batch, §8 test strategy, §9 migrations, §13 Batch Gate Protocol), `novawallet-api/docs/phase-1c/PHASE_1C_RISK_TRACEABILITY_MATRIX.md`, and `docs/DECISIONS.md`.
2. **Start with a mini-plan** in `novawallet-api/docs/phase-1c/PHASE_1C_BATCH_<N>_PLAN.md`: identifiers addressed, files to change (must match the plan's file list; explain any deviation), tests to add, decisions it depends on. If a blocking decision is not recorded in `docs/DECISIONS.md`, STOP after the plan.
3. Update `TODO.md` with the batch's stages as checkboxes (like Batch 1).
4. Implement stage by stage. After each stage: targeted tests.
5. Finish with the full **Batch Gate Protocol** (§13): git status/diff checks, targeted + full suite with counts, PostgreSQL 16 fresh + upgrade verification if migrations changed, traceability matrix updated **only with executable evidence**, canonical risk register updated, `novawallet-api/docs/phase-1c/PHASE_1C_BATCH_<N>_REPORT.md` (sections like the Batch 1 report: A–M).
6. Phase 1A/1B/1C-Batch-1 safeguards must remain intact; show the regression evidence.
7. Stop. Do not start the next batch. Do not commit.
