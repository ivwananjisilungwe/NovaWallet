## Report — BE-1C-2

Done:
- Authorization and ownership hardening (Phase 1C Batch 2) verified and documented: ownership on walletId, reference, payout and KYC document routes (F1, proven by tests, no service change); paging bounds (F2); URL-level `/api/v1/admin/**` ADMIN rule (F3); multipart limit 10 MB with 413 mapping (F4, not proven end-to-end); unknown JSON fields rejected (F5/F9); DTO bounds, fee estimate and KYC tier validation (F6–F8).
- Six authz gaps closed by `AuthorizationGapIntegrationTest` (notifications scoping ×3 rows, PIN change, KYC upload anonymous, admin cross-user document).
- Batch report written: `novawallet-api/docs/phase-1c/PHASE_1C_BATCH_2_REPORT.md` (sections A–M).
- Authz map gap rows updated to cite the test methods; traceability matrix updated for SEC-10 (IN PROGRESS, amount bounds only), plus §3.1 evidence rows for IDOR, route authentication and admin route protection. No risk marked CLOSED.
- Single-ADMIN risk (SEC-13, DEC-03) was recorded earlier in commit `fac6f61`; not changed here.
- No Java, README.md, StartHere.md, TODO.md or HANDOFF.md edits. No maven or git write commands run.

Files changed: 3 (+1 new)
- `novawallet-api/docs/phase-1c/PHASE_1C_BATCH_2_REPORT.md` (new)
- `reports/PHASE_1C_AUTHZ_MAP.md` (6 gap rows and Gaps section)
- `novawallet-api/docs/phase-1c/PHASE_1C_RISK_TRACEABILITY_MATRIX.md` (SEC-10 row, §3.1, §8 summary, §9 change log)
- `reports/opencode/BE-1C-2_REPORT.md` (new; this file)

Evidence (supplied by the owner from runs in this session; not re-run by this agent, since maven was not allowed):
- `cd novawallet-api && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw -B clean test`: exit 0, Tests run 361, Failures 0, Errors 0, Skipped 0 (baseline 316). Runtime JDK 21.
- Combined full run `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw -B clean test` (2026-11-14, after the gap test was added): exit 0, **Tests run 366, Failures 0, Errors 0, Skipped 0** (observed).
- No migrations added (head V16); PG16 migration check not required and not run.
- `git diff --check` clean on batch files; secret scan of batch files found nothing.
- Read-only check by this agent: the six gap rows are closed in the authz map (0 GAP markers remain); `git diff --check` clean on the two edited docs.

Blocked / not done:
- BLOCKED: Java 17 runtime verification (BLK-04). Only JDK 21 was used.
- NOT DONE: combined full-suite run including `AuthorizationGapIntegrationTest` (needs an owner or reviewer run).
- NOT DONE: end-to-end multipart 413 check (MockMvc does not enforce the servlet limit).
- NOT DONE: SEC-10 narration length bound.
- NOT DONE: pass over inbound/outbound DTOs for the global `fail-on-unknown-properties` setting.
- Left as-is in `PHASE_1C_AUTHZ_MAP.md`: the "Method" note still says tests were not run and that `InputBoundsIntegrationTest` was absent; both are stale now (kept per instruction to change only the gap rows).
- Git finding: F2–F8 source changes are not in separate batch commits; `git log --stat` attributes them to the monorepo import `6653046`. Not verified against the working tree.

Decisions needed:
- DEC-03 remains in force (single ADMIN). Split roles are deferred to BE-1D-07; no new decision requested now.
- Decide whether the global Jackson `fail-on-unknown-properties: true` should stay for all inbound JSON, or be limited to request bodies (recommendation: keep for request bodies; audit webhook DTOs before relying on it).

Risks noticed (not fixed):
- `getMonthlyStatement` totals only the first 100 transactions → BE-1D-12.
- `GET /api/v1/transactions/{reference}` returns 403 vs 404, revealing existence → BE-99.
- `ChangePasswordRequest.newPassword` max 100 vs 128 elsewhere (no change).
- `FeeController` Javadoc says no auth; route is authenticated (wrong comment, no change).
- `/api/v1/webhooks/lipila/**` is permitAll; future routes under it are public by default.
- KYC document access is streamed, not signed URLs (contract A10) → BE-1C-6.
- SEC-10 narration length unbounded (still OPEN).

Next: T1 independent review of BE-1C-2 (`prompts/templates/T1_independent_review.md`), then BE-1C-3.
