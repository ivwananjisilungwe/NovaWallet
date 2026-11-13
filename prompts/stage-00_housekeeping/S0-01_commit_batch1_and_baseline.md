# S0-01 — Verify Phase 1C Batch 1 and prepare the owner commit

**Area:** novawallet-api · **Needs:** — · **Agent files:** `AGENTS.md`, `novawallet-api/AGENTS.md`

---
Read root `AGENTS.md`, `novawallet-api/AGENTS.md`, `TODO.md`, `novawallet-api/docs/phase-1c/PHASE_1C_BATCH_1_REPORT.md`, `novawallet-api/docs/phase-1c/PHASE_1C_DECISION_LOG.md`.

Phase 1C Batch 1 (SEC-01, SEC-02, SEC-03, SEC-06, SEC-08, NEW-AUTH-01) is implemented but **uncommitted** on `feature/phase-1c-security-database-hardening` (about 33 modified + 20 new files, incl. V16). Your job is to prove it is safe for the owner to commit. Do not change behaviour.

Do:
1. `git status --short`, `git diff --stat`. List every changed/untracked file and classify: source, test, migration, docs, **unexpected** (e.g. `uploads/`, `target/`, `.env`, IDE files). Flag unexpected files; do not delete them.
2. Run `./mvnw -B clean test`. Expected 316 tests, 0 failures. Report the real numbers.
3. PostgreSQL 16 (disposable container / `pgtest` profile): fresh V1→V16 and upgrade V15→V16; `flyway validate`. Report.
4. `git diff --check`; secret scan of the full diff and untracked files (keys, passwords, tokens, JWT secrets, real phone numbers/emails).
5. Confirm `.gitignore` covers `target/`, `uploads/`, `.env`, `*.log`. If not, propose the lines (owner applies).
6. Write `reports/S0-01_REPORT.md` with: file classification, evidence, and the exact commit commands for the owner, e.g.:
   ```
   git add <explicit list or reviewed -A>
   git commit -m "Phase 1C Batch 1: auth & session hardening (SEC-01,02,03,06,08, NEW-AUTH-01)"
   ```
7. Stop. Do not commit.

Acceptance: evidence recorded; owner has a clean, explicit commit plan; any blocker clearly stated.
