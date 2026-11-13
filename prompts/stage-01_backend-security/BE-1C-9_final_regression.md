# BE-1C-9 — Phase 1C Batch 9: Final security regression & acceptance 🔒

**Area:** novawallet-api · **Needs:** BE-1C-8 (BE-1C-5 may remain BLOCKED)

---
Follow `_COMMON_BATCH_RULES.md` for Batch 9. This batch adds no features. It proves Phase 1C.

Do:
1. Re-run every Phase 1A, 1B and 1C safeguard test; list them by safeguard in `novawallet-api/docs/phase-1c/PHASE_1C_FINAL_VERIFICATION_LOG.md` with command, counts, log path.
2. Full suite on H2 **and** the PostgreSQL 16 `pgtest` tier; fresh V1→head and upgrade from the pre-1C head; `flyway validate`.
3. Walk the plan's §14 Definition of Done and the 20 acceptance criteria one by one: met / not met / blocked, each with evidence.
4. Traceability matrix: every identifier has a final status backed by evidence. Collections security (SEC-05) is either verified against a real contract or explicitly `BLOCKED: BLK-01` with the 503 guard still in place (prove with a test).
5. Dependency check: `./mvnw dependency:tree`, list known-vulnerable dependencies (use OWASP dependency-check if it can run; otherwise say it could not) and propose upgrades — do not upgrade major versions in this batch.
6. Secret scan of the whole repo history-independent working tree.
7. Produce: `PHASE_1C_FINAL_ACCEPTANCE_REPORT.md`, `PHASE_1C_FINAL_REMAINING_RISKS.md`, `PHASE_1C_FINAL_FILE_CHANGE_INVENTORY.md`. Final decision is exactly one of ACCEPT / CONDITIONAL ACCEPT / REJECT, with **no** claim of certification, regulatory approval, live-money or production readiness.
8. Update `TODO.md` and `novawallet-api/docs/architecture/implementation-roadmap.md` status for Phase 1C.

Stop. The owner decides whether to merge the branch.
