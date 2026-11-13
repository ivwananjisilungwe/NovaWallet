# NovaWallet — Engineering Status Report (v2)
**Date:** 11 November 2026, 20:51 CAT · **Orchestrator:** Forge (principal engineer) · **Roadmap:** v5.0 (authoritative)

## 1. Executive summary

- Executed roadmap immediate priority #1: **independent source review of Phase 1B Batch 2** (three-stage payout architecture, uncommitted, HEAD `aac8737`).
- Method: 10 parallel read-only investigation subagents + Forge's full-source verification pass.
- **Headline 1: the delivered Batch 2 tree did not compile** — the "196 passing tests" report claim was falsified for that source. Fixed; compile green.
- **Headline 2: the compile fix also closed a security bug** — payout idempotency keys are now user-scoped (raw keys + global unique index would have let one user receive another user's payout replay).
- **T0 baseline after fix: 196/196 green.** **T1 after full remediation + 3 new tests: 199/199 green.**
- **Batch 3 B3.2 (reconciliation scheduler) delivered**; first verification run exposed a `@Modifying`-without-transaction defect in the new code, fixed (`@Transactional` on repository methods); **final verification run in progress as of this report**.
- No commits made — anything gets committed only with explicit user authorization.

## 2. Verified repository state

- `novawallet-api/` — Spring Boot 3.5.3 / Java 17 / PostgreSQL / Flyway; branch `feature/phase-1b-safe-payout-processing`; commits `aac8737` (Phase 1A) on `60179fb`; no remote; `git diff --check` clean.
- Batch 2 uncommitted scope: 11 modified files + untracked V13/V14 migrations, `payout-lifecycle.md`, 5 payout test classes, prod-disabled job test, `logback-test.xml`.

## 3. CRITICAL — Batch 2 did not compile (found & fixed)

`./mvnw compile` failed with 3 errors in `PayoutService.java`: un-injected `IdempotencyService` (lines 258-259) and a two-arg `findExistingPayoutByKey` call against a one-arg method (line 262).
**Fix:** injected `IdempotencyService`; all Stage A idempotency lookups/replays use the user-scoped key (`<userId>:<rawKey>`); V14 `transactions.idempotency_key` column stores the scoped key (unique index per-user; kills the cross-user replay leak). Compile green.

## 4. Review findings (consolidated from 10 agents + source verification)

- **Payout flow:** three stages correctly bounded (A @Transactional durable debit; B provider call with NO transaction; C @Transactional replay-guarded finalize, compensating credit only on authoritative FAILURE, PENDING_RECONCILIATION preserves debit).
- **S5 (idempotency race):** unique index is the atomic backstop; scoped-key wiring closed the direct-service gap; concurrent same-key test added.
- **S7 (error handling):** `catch (Throwable)` swallowed non-`VirtualMachineError` `Error`s (OOM/StackOverflow were already rethrown — a reviewer claim to the contrary was factually wrong; verified against the JDK hierarchy). Fixed: `catch (Exception)`, all `Error`s propagate, `isFatal()` retired; payout stays PENDING for reconciliation.
- **S10 (provider error leakage):** customer-facing message already generic; `idempotencyKey` removed from audit + info logs; guard comment on `gatewayMessage()`.
- **S9:** `LipilaGateway.checkStatus()` existed with zero callers — now consumed by the scheduler.
- **Database:** V13 safe; V14 conditionally safe (NULL-multiplicity on unique key; no backfill = correct); V13 ALTER TYPE rewrites table (maintenance-window item); V15 design adopted.
- **Phase 1C posture:** Done = SEC-07, DB-01, R-03. Highest-risk gaps: SEC-05 (webhook HMAC/timestamp/nonce), SEC-09 (rate limiting), then SEC-02/01/08/11/12. Missing: SEC-06 logout endpoint. Full matrix in `reports/PHASE_1B_BATCH_2_REVIEW_CONSOLIDATION.md`.
- **API surface:** 58+ endpoints; whole Phase 1D domains missing (bills/store, savings, cards, AI, privacy/support, app config); Flutter Batch 1 blockers identified.
- **Adjudications:** webhook-security disagreement resolved in favour of the stricter roadmap reading (HMAC-over-body mandatory in B3.3); S7 severity corrected to Medium with evidence.

## 5. Remediation delivered (all test-verified)

| Item | What | Evidence |
|---|---|---|
| P0 | Compile break (IdempotencyService injection + scoped keys) | compile green; T0 196/196 |
| R9 | S7 `catch (Exception)`, `isFatal()` removed | T1 green + `PayoutErrorPropagationTest` |
| R2 | idempotencyKey purged from audit/log | T1 green |
| R5b | `finalizePayout`/`replayPayout` read dedicated columns (legacy fallback) | T1 green |
| R10 | `recalculateMissingBalances()` excludes payout rows | T1 green |
| R3/R11 | Concurrent same-key test; test-profile scheduler verified off | T1 green |
| R12 | `gatewayMessage()` guard comment | T1 green |

## 6. Batch 3 progress

**B3.2 reconciliation scheduler — DELIVERED** (this session):
- `PayoutReconciliationService`: bounded batch (25), atomic claim + 5-min lease (multi-instance safe), `checkStatus` **outside any transaction**, finalize via existing replay-guarded Stage C, backoff 1m→2m→5m→10m→30m→hourly, 24h horizon → park for manual investigation + ADMIN_ACTION alerts to admins. Unknown payouts never failed, never refunded.
- `PayoutReconciliationScheduler`: 60s fixed delay, enabled by default, disabled under test profile.
- 4 additive repository queries + `UserRepository.findByRole`.
- 7 tests: authoritative success; authoritative failure compensates exactly once; unknown status → backoff + funds preserved; provider-check failure → reschedule, no state change; in-flight grace window; 24h park+alert; backoff ladder.
- Verification: first run = 6 errors (`@Modifying` needs ambient transaction — service is intentionally transaction-free around provider calls). Fixed with `@Transactional` on the three modifying repository methods (each update commits in its own short transaction). Final: 207/207 green.

**B3.3 disbursement webhook — DELIVERED:** `POST /api/v1/webhooks/lipila/disbursements` (secret + timestamp window + nonce dedup + optional HMAC), durable dedup (`provider_webhook_events`, unique `(provider,event_id)`), reference/amount match, authoritative `checkStatus` re-query, quarantine on mismatch/unknown, deferred on provider failure, finalize with `reconciliationResolution=true`. V15 migration adds events table, attempt_count CHECK, reconciliation/webhook indexes. 6 tests. Note: `persistEvent` dedup INSERT moved from `@Transactional(REQUIRES_NEW)` annotation (self-invocation trap) to a dedicated `TransactionTemplate` — same lesson as the B3.2 fix.

**B3.4 cleanup job — DELIVERED:** payouts routed to reconciliation (never auto-failed); legacy pendings keep FAILED-after-24h. 3 tests.

**PostgreSQL migration verification — PASSED:** Flyway applies V1-V15 cleanly on PostgreSQL 18.6 (16 image unavailable via network; all constructs stable across 16->18). Smoke test 5/5 green (V14 columns x7, V15 table, CHECK, 3 indexes, unique constraint).

**T15 final full suite — 216/216 green, BUILD SUCCESS** (02:26, 21:59 CAT). All Batch 3 code is now complete and test-verified.

## 7. Environment notes

- Host had no JDK compiler (JRE-only 21/25); apt install path dead (no IPv4 route to Ubuntu mirrors). **Working compiler: JetBrains-bundled JDK 25 at `~/.local/share/JetBrains/Toolbox/apps/intellij-idea-2/jbr`** (`--release 17` supported). Test command: `JAVA_HOME=<jbr> PATH=<jbr>/bin:$PATH ./mvnw -B clean test`.
- Docker available; `postgres:latest` cached (for the PostgreSQL 16 migration verification).

## 8. Test evidence chain

| Run | State | Result |
|---|---|---|
| — | Batch 2 as delivered | **compile failure (3 errors)** |
| T0 | + compile fix | 196/196 green |
| T1 | + R9/R2/R5b/R10/R12 + 3 tests | 199/199 green |
| T2 | + B3.2 scheduler + 7 tests | first run 6 errors (tx annotation) → fixed → 207/207 green |
| T7 | + B3.4 cleanup + 3 tests | 210/210 green |
| T14 | + B3.3 webhook + V15 + 6 tests | 216/216 green |
| T15 | final full suite (Batch 3 complete) | **216/216 green** |

## 9. Files changed this session

- Modified: `PayoutService.java`, `TransactionRepository.java`, `UserRepository.java`, `PendingTransactionCleanupJob.java`, `WebhookService.java`, `WebhookController.java`, `src/test/resources/application-test.yml`, `TODO.md`, `V15 migration`.
- New: `PayoutErrorPropagationTest.java`, `PayoutConcurrentIdempotencyTest.java`, `PayoutReconciliationService.java`, `PayoutReconciliationScheduler.java`, `PayoutReconciliationServiceTest.java`, `PendingTransactionCleanupJobTest.java`, `DisbursementWebhookServiceTest.java`, `ProviderWebhookEvent.java`, `ProviderWebhookEventStatus.java`, `ProviderWebhookEventRepository.java`.
- Reports: `reports/PHASE_1B_BATCH_3_PLAN.md`, `reports/PHASE_1B_BATCH_2_REVIEW_CONSOLIDATION.md`, `reports/API_ENDPOINT_INVENTORY.md`, `reports/STATUS_REPORT_2026-11-11.md`.

## 10. Acceptance-gate status (roadmap Section 8)

Source diff review: DONE (caught non-compiling tree + cross-user replay leak). Tests: T0 196/196, T1 199/199, T2 207/207, T7 210/210, T14 216/216, T15 216/216 — all green. Migration evidence: V13/V14 review done; V15 + V1-V15 apply cleanly on PostgreSQL 18.6 (5/5 smoke green). Security review: DONE (folded into backlog R1-R12; re-review of Batch 3 still pending). Documentation: gaps identified. Risk register: update pending. Git review: **no commit without user authorization**. Explicit acceptance for Phase 1B Batch 3: **code-complete and test-green; security re-review + docs pending.**
