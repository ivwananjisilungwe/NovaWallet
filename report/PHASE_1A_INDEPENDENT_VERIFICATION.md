# Phase 1A — Independent Verification Report

**Repository:** `/home/ivwananji-silungwe/DEVELOPMENT/PROJECTS/NovaWallet/novawallet-api`
**Verifier role:** documentation-worker (independent verification, read-only)
**Task ID:** IV-04
**Report file:** `report/PHASE_1A_INDEPENDENT_VERIFICATION.md`
**Date of verification:** 2026-11-10

> Scope of this report: independent verification of the Phase 1A financial-guardrail changes against the repository state, source code, provided IV-01/IV-02/IV-03 evidence, and the Phase 1A implementation report. No application source, tests, migrations, configuration, documentation, or Git history were modified. No Maven build or test execution was performed by this verifier.

---

## 1. Repository and Git State

**Status: VERIFIED**

- Branch: `feature/phase-1a-financial-guardrails`
- HEAD: `60179fb5b09f0d5a11338f07ed2cb23ef393b1f5` (matches expected baseline)
- Tracked files modified: 15 (3 docs + 6 main + 6 tests), matching the Phase 1A change set
- Untracked files: 8 (`.mvn/wrapper/maven-wrapper.properties`, migration V12, and 6 new test files)
- Staged files: 0 (`git diff --cached --name-only` empty)
- `git diff --check`: passed (exit 0, no whitespace errors)
- No remotes configured; no push performed during Phase 1A
- Working tree contains only the already-known Phase 1A changes plus this report (which is ignored under `/report/`)
- `report/` directory is gitignored (`/report/` rule in `.gitignore`), so this report is not tracked

**Verification performed by IV-04 (read-only):**
- `git status`, `git rev-parse HEAD`, `git branch --show-current`
- `git diff --check`
- `git diff --cached --name-only`
- `git status --porcelain` count (22 entries: 15 modified + 7? — verified 15 modified + 8 untracked = 23 porcelain lines? Actually 15 + 8 = 23; status shows 15 modified + 8 untracked = 23 lines. The earlier count of 22 was a transient miscount; re-verification confirms 23 entries matching the documented change set.)

---

## 2. Exact Files Inspected

**Source (main):**
- `src/main/java/com/novawallet/novawallet_api/transaction/service/TransactionService.java`
- `src/main/java/com/novawallet/novawallet_api/transaction/controller/TransactionController.java`
- `src/main/java/com/novawallet/novawallet_api/transaction/repository/TransactionRepository.java`
- `src/main/java/com/novawallet/novawallet_api/transaction/service/TransactionReferenceGenerator.java`
- `src/main/java/com/novawallet/novawallet_api/payment/controller/WebhookController.java`
- `src/main/java/com/novawallet/novawallet_api/payment/service/WebhookService.java`
- `src/main/java/com/novawallet/novawallet_api/exception/GlobalExceptionHandler.java`
- `src/main/java/db/migration/V12__CreateTransactionReferenceSequence.java`
- `src/main/java/com/novawallet/novawallet_api/admin/service/AdminService.java` (out-of-scope observation check)
- `src/main/java/com/novawallet/novawallet_api/notification/schedule/BalanceRecalculationJob.java` (out-of-scope observation check)
- `src/main/java/com/novawallet/novawallet_api/wallet/service/AccountNumberGenerator.java` (out-of-scope observation check)

**Build/config:**
- `mvnw` (mode check)
- `.mvn/wrapper/maven-wrapper.properties`
- `.gitignore`

**Reports/evidence:**
- `report/PHASE_0_REPORT.md`
- `report/PHASE_1A_IMPLEMENTATION_REPORT.md`
- `/tmp/opencode/iv03-full-regression.log` (grep only)

**Git commands:**
- `git status`, `git status --porcelain`, `git rev-parse HEAD`, `git branch --show-current`
- `git diff --check`, `git diff --cached --name-only`, `git diff --stat`

**Not inspected in detail (relied on IV-01/IV-03 evidence):** the 12 named test files and the H2/PostgreSQL primitive verification scripts. No test source was re-read beyond the change set listing.

---

## 3. Findings for FIN-01 (Direct deposit containment)

**Status: VERIFIED**

**Source-code inspection findings:**

- `TransactionController.deposit` route `POST /api/v1/wallets/{walletId}/deposit` remains registered (no endpoint removal; containment approach).
- `TransactionService.deposit(UUID, DepositRequest, UUID)` first calls `findActiveWalletWithLock(walletId, userId)`, which performs, in order:
  1. pessimistic-lock lookup (`findByIdWithLock`), throwing `ResourceNotFoundException` (404) if absent;
  2. ownership check (`wallet.getUserId().equals(userId)`), throwing `ForbiddenException` (403) on mismatch;
  3. active-status check (`WalletStatus.ACTIVE`), throwing `BadRequestException` (400) if inactive.
- After those checks it throws `ServiceUnavailableException("Direct wallet deposits are unavailable until verified payment collection is implemented")`.
- No `walletRepository.updateBalance`, no `Transaction` save, no `auditService.recordAction`, no `notificationService.sendBoth` occurs on this path.
- `GlobalExceptionHandler.handleServiceUnavailable` maps `ServiceUnavailableException` to HTTP 503 (`SERVICE_UNAVAILABLE`).
- `@CacheEvict(value = "walletBalances", key = "#walletId")` defaults `beforeInvocation = false`; because the method throws, the eviction does not run. `@Transactional` rolls back any read lock acquisition. No money is created.
- Source grep confirmed no alternative arbitrary-credit endpoint in `TransactionController` (only deposit/withdraw/transfer write endpoints).

**Tests actually executed (by IV-03, not by IV-04):**
- `TransactionEdgeCaseEndpointIntegrationTest` cases `directDeposit_isUnavailable_doesNotCredit`, `directDeposit_withIdempotencyKey_isUnavailable`, `directDeposit_nonOwner_isForbidden`, `directDeposit_unknownWallet_returnsNotFound`, `directDeposit_frozenWallet_returnsBadRequest` — included in the IV-03 full regression of 175 tests, 0 failures.

**Residual risk:** Direct deposits are contained, not resolved; a verified collection workflow does not yet exist.

---

## 4. Findings for FIN-02 (Webhook callback containment)

**Status: VERIFIED**

**Source-code inspection findings:**

- `WebhookController.verifySecret` uses `MessageDigest.isEqual` on byte arrays (constant-time comparison); returns 401-equivalent `UnauthorizedException` when the expected secret is blank/missing or the provided value mismatches. Missing header handled (`required = false` → null → `providedBytes` empty → mismatch).
- Swagger documents 401 for missing/invalid secret.
- `WebhookService.processCollectionCallback`:
  - validates `referenceId`/`status` (400 on malformed payload);
  - parses `NW:<walletId>:<uuid>` via `extractWalletId` (400 on malformed reference);
  - returns normally (controller → 200) for non-`Successful` statuses without credit;
  - normalizes amount (`normalizeAmount`, 400 if null/non-positive);
  - looks up wallet by id (`findById`, not `findByIdWithLock`), throwing `ResourceNotFoundException` (404) if unknown;
  - after lookup, logs a WARN and throws `ServiceUnavailableException` (503). No credit, mutation, transaction save, or audit occurs.
- No call to `transactionService.deposit` remains (grep of `WebhookService` shows only an unused import/field).
- Repeated 503 callbacks produce no DB-side side effects; only WARN log volume.

**Low findings confirmed by source inspection (consistent with IV-01):**
1. `transactionRepository` and `transactionService` fields are declared, injected, and stored but never used on the contained path (dead dependencies).
2. Stale Javadoc on `WebhookService` still describes the old credit/idempotency behavior (`depositDescription` helper remains unused).
3. On a `Successful` callback, amount validation (`normalizeAmount`) occurs before the unknown-wallet 404 lookup, so a malformed amount yields 400 even when the wallet is unknown.
4. `extractWalletId` validates segment count, prefix, and `segments[1]` UUID format, but does not validate that `segments[2]` is a UUID (third segment accepted unchecked).

**Tests actually executed (by IV-03):**
- `PaymentWebhookEndpointIntegrationTest` cases `successfulCallback_withValidSecret_isContained`, `successfulCallback_duplicateReplay_isContainedBothTimes`, `successfulCallback_withLargeAmount_isContained`, `successfulCallback_withZeroAmount_returnsBadRequest`, `successfulCallback_withNegativeAmount_returnsBadRequest`, `successfulCallback_forUnknownWallet_returnsNotFound`, `failedCallback_acknowledgedWithoutCredit`, `malformedReference_returnsBadRequest`, `duplicateCallback_isContained_idempotently`, `invalidSecret_returns401` — included in the 175/0/0 regression.

**Residual risk:** Callbacks are contained, not resolved; no collection initiation record, provider verification, authoritative amount/currency match, idempotency persistence, or replay protection beyond the static secret.

---

## 5. Findings for FIN-06 (Transfer deadlock ordering)

**Status: VERIFIED**

**Source-code inspection findings:**

- `TransactionService.transfer`:
  - loads sender wallet via `findByUserId` (unlocked), checks active status, rejects self-transfer;
  - detaches the unlocked sender entity from the `EntityManager` before locking (avoids stale-version conflicts);
  - computes `firstLockId`/`secondLockId` by `senderWalletId.compareTo(receiverWalletId) < 0` (canonical ascending UUID order);
  - acquires both locks via `WalletRepository.findByIdWithLock` in that order;
  - maps `firstLockedWallet`/`secondLockedWallet` back to `lockedSenderWallet`/`lockedReceiverWallet` by id comparison;
  - rechecks `senderWallet.getStatus() == WalletStatus.ACTIVE` under lock; receiver active status checked;
  - atomic debit via `updateBalance(..., totalSenderDebit.negate())` with `updated == 0` → `BadRequestException` (insufficient balance);
  - atomic credit via `updateBalance(receiverWalletId, amount)` with `receiverUpdated == 0` → `IllegalStateException`;
  - re-reads both balances; persists debit/credit/fee transactions; `@Transactional` rollback preserved on exception.
- No `@Transactional` isolation downgrade introduced; method-level `@Transactional` (default isolation) retained.
- `@CacheEvict(value = "walletBalances", allEntries = true)` retains default `beforeInvocation = false`.

**Tests actually executed (by IV-03):**
- `TransactionServiceTransferLockOrderTest`, `ConcurrentTransferIntegrationTest`, `TransferRollbackIntegrationTest` — included in the 175/0/0 regression.

**Verification gap (not independently executed):** PostgreSQL row-level lock / deadlock behavior was not exercised; only H2 + JVM-level tests ran. Canonical ordering is verified at the code and H2-test level; equivalence of PG locking semantics is assumed, not demonstrated.

---

## 6. Findings for FIN-12 (DB-backed transaction references)

**Status: PARTIALLY VERIFIED**

**Source-code inspection findings:**

- `TransactionRepository.nextReferenceSequence()` executes `SELECT nextval('transaction_reference_seq')` (native query), returning `Long`.
- `TransactionReferenceGenerator.generate()`:
  - calls `nextReferenceSequence()`; throws `IllegalStateException` on null;
  - enforces `1 <= sequence <= 99,999,999`;
  - formats `NWTXyyyyMMdd` + 8-digit sequence.
- Removed `AtomicLong`, `@PostConstruct`, date-change reset, and `findMaxReferenceByPrefix` usage from the generator (grep confirms no `AtomicLong`/`PostConstruct` in the file).
- Migration `V12__CreateTransactionReferenceSequence`:
  - requires `autoCommit == false` else throws `IllegalStateException`;
  - sets and verifies nonzero fetch size (throws if `<= 0` or `setFetchSize` throws);
  - uses try-with-resources for `Statement` and `ResultSet`;
  - streams `SELECT reference FROM transactions` with `FETCH_SIZE = 1000`;
  - matches `^NWTX[0-9]{8}[0-9]{8}$`; computes max historical suffix; counts and warns on non-conforming rows;
  - fails closed if `maxSuffix >= 99,999,999`;
  - creates `transaction_reference_seq` with `START WITH 1 INCREMENT BY 1`; restarts to `maxSuffix + 1`;
  - does not rewrite historical rows.

**Tests actually executed (by IV-03):**
- `TransactionReferenceSequenceMigrationTest`, `V12MigrationStreamingGuardTest`, `TransactionReferenceConcurrencyIntegrationTest`, `TransactionReferenceGeneratorTest` — included in the 175/0/0 regression.

**Verification gap (not independently executed by IV-04):**
- PostgreSQL 16 production target was not verified (see Section 7).
- PostgreSQL 18.6 primitive verification (sequence restart, `nextval` 43/44, fail-closed duplicate `CREATE SEQUENCE`, `pg_typeof` bigint, regex exclusion) is reported by the implementation report but was not independently executed by IV-04. H2 2.3.232 was the only database actually exercised in the IV-03 regression.

Conclusion: implementation and H2 tests are verified; production-target (PG16) database behavior remains unverified, and the PG18 primitive checks are accepted on reported evidence only.

---

## 7. PostgreSQL 16 Verification Results

**Status: BLOCKED**

- No PostgreSQL 16 binary, cluster, or container is available in this environment. `pg_lsclusters` shows only cluster `18/main` (port 5432, online). Docker has only the `postgres:latest` (PG18) image; no PG16 image or running container.
- IV-02 concluded BLOCKED for the same reason and did not execute any of the 10 checklist items.
- All 10 PostgreSQL 16 verification items (sequence existence/type, `nextval` behavior, migration seeding, fail-closed capacity, non-conforming-row handling, streaming guards, concurrency, etc.) remain NOT EXECUTED.
- H2 (2.3.232) and PostgreSQL 18.6 results do **not** constitute PostgreSQL 16 verification. H2 in particular does not implement PostgreSQL sequences/Flyway Java migrations identically.
- **No repository changes, no Maven, no Flyway run, and no service start** were performed by IV-02 or IV-04.

---

## 8. OPS-01 Verification (Maven Wrapper restoration)

**Status: VERIFIED**

**IV-04 read-only checks:**
- `mvnw` file mode: `755` (confirmed via `stat`)
- `.mvn/wrapper/maven-wrapper.properties` contents:
  - `distributionUrl=...apache-maven-3.9.16-bin.zip`
  - `wrapperVersion=3.3.4`
  - `distributionType=only-script`
  - `distributionSha256Sum=5af3b743dd8b876b5c45da33b676251e5f1687712644abb4ee519ca56e1d89ce`
- `.gitignore` ignores `.mvn/wrapper/maven-wrapper.jar` and tracks only `maven-wrapper.properties` (via `!.mvn/wrapper/maven-wrapper.properties`)
- No `maven-wrapper.jar` references found in the tracked tree
- `mvnw.cmd` present

**Evidence from IV-03 (executed, not re-run by IV-04):**
- `./mvnw --version` passed on JDK 17, reporting Maven 3.9.16
- Index mode `100755`
- Clean-cache bootstrap passed
- Tampered-checksum failure verified
- Wrapper properties untracked and not ignored; jar ignored

**No Maven execution by IV-04** (per absolute rules). OPS-01 verification is accepted on IV-03 evidence plus IV-04 file/mode inspection.

---

## 9. Baseline Test Discrepancy Explanation

**Status: EXPLAINED (not conclusively reconcilable)**

- Phase 0 reported 144 tests, 143 passed, 1 failed: `TransactionFlowIntegrationTest$TransactionLifecycle.shouldCompleteDepositWithdrawAndTransfer` with duplicate reference `NWTX2026111000000018`.
- Pristine baseline run reported 144 tests, 8 failures: `AuditAssertionEndpointIntegrationTest` (1) + `IdempotencyEndpointIntegrationTest` (7), with the named lifecycle test passing.
- Both reports describe the same underlying defect: duplicate transaction references colliding on the DB unique constraint. The differing failure selection is attributed to order- or timing-dependent behavior from shared H2 state / test-context caching (H2 mem testdb with `DB_CLOSE_DELAY=-1`, no parallel Surefire config, Surefire 3.5.3).
- Original Phase 0 Surefire reports are unavailable, so the discrepancy cannot be conclusively reconciled.
- IV-03 full regression: 175 tests, 0 failures, 0 errors, 0 skipped (exit 0), confirming the FIN-12 fix removed the collision. Net +31 tests versus the 144 baseline.

**IV-04 did not independently rerun the baseline or the regression** (Maven prohibited); this explanation is carried from provided evidence.

---

## 10. Full Test Evidence

**Status: VERIFIED (on reported evidence)**

- Command: `./mvnw clean test`
- Database: H2 2.3.232 (mem testdb, `DB_CLOSE_DELAY=-1`)
- JDK: 17.0.13; Maven 3.9.16; Surefire 3.5.3; Spring Boot 3.5.3
- Result: **175 tests, 0 failures, 0 errors, 0 skipped**
- Exit code: 0
- Wall-clock: 83.19 sec
- Log: `/tmp/opencode/iv03-full-regression.log` (IV-04 grepped the summary line and `BUILD SUCCESS`; file size 2,397,799 bytes)
- Baseline comparison: 144 tests → 175 tests (+31); 8 failures → 0
- No parallel Surefire configuration; no tests skipped

**IV-04 test execution:** none. Per absolute rules, no Maven was run and no services were started. All test results above are attributed to IV-03.

**Additional verification artifacts:**
- `git diff --check`: passed (IV-04)
- `git diff --cached --name-only`: empty (IV-04)

---

## 11. Security Findings

**Result: PASS WITH ISSUES (no high or critical)**

Findings confirmed by IV-04 source inspection:

1. **Unused fields in `WebhookService`** — `transactionRepository` and `transactionService` are injected and stored but unused on the contained path. (Low)
2. **Stale Javadoc in `WebhookService`** — still describes the old credit/idempotency workflow; `depositDescription` helper unused. (Low)
3. **Amount validation before unknown-wallet 404** — `normalizeAmount` runs before `findById` on `Successful` callbacks, so a bad amount returns 400 even for an unknown wallet. (Low)
4. **Third reference segment not validated** — `extractWalletId` does not validate `segments[2]` as a UUID. (Low, per IV-01; confirmed by code read)

No high or critical findings. No arbitrary-credit endpoint exists (FIN-01/FIN-02 contained).

**Out-of-scope observations confirmed by IV-04 (not part of Phase 1A):**
- `AdminService.toggleUserDeletedStatus` (lines 72–108): modifies `User` and the associated `Wallet` status without a pessimistic lock (`findByIdIncludingDeleted` / `findByUserId` are unlocked). Concurrent admin toggles or concurrent wallet transactions can race. Confirmed by source read.
- `BalanceRecalculationJob` + `TransactionRepository.recalculateMissingBalances`: stamps current wallet balances onto historical rows where snapshots are null. Do not run in production as-is. Confirmed by source read.
- `AccountNumberGenerator`: 10 random digits, 10 retries with existence check; TOCTOU under concurrency; possible unique-constraint violation; insufficient retries under load. Confirmed by source read.

---

## 12. Scope Compliance

**Status: VERIFIED**

- Modified tracked files (15) exactly match the Phase 1A change set listed in the implementation report (3 docs + 6 main + 6 tests).
- Untracked files (8) exactly match the documented new files.
- No excluded Phase 1B+ work found: no payment orchestration/provider workflow, no double-entry ledger, no modular-monolith boundary enforcement, no event-driven/outbox, no microservice extraction, no historical transaction rewrites, no production deployment.
- No commit or push performed; HEAD unchanged at baseline.
- No commits, resets, cleans, staging, stashing, or branch switches performed by IV-04.
- IV-04 created/updated only `report/PHASE_1A_INDEPENDENT_VERIFICATION.md`.

---

## 13. Remaining Risks

1. **FIN-01/FIN-02 are contained, not resolved** — no verified collection workflow, initiation record, provider verification, authoritative amount/currency match, idempotency persistence, or replay protection exists yet.
2. **FIN-12 production-target PG16 verification open** — H2/PG18 evidence only; PG16 behavior unverified (BLOCKED).
3. **FIN-06 PostgreSQL concurrency not demonstrated** — canonical ordering verified on H2/JVM only; PG row-lock/deadlock behavior assumed equivalent.
4. **Four low security findings** in `WebhookService` (unused fields, stale Javadoc, amount-before-404 ordering, unvalidated third reference segment).
5. **Out-of-scope races/risks** — `AdminService.toggleUserDeletedStatus` status race, `BalanceRecalculationJob` historical snapshot stamping, `AccountNumberGenerator` TOCTOU/uniqueness.
6. **Sequence capacity** — `transaction_reference_seq` is finite (99,999,999); generation fails closed when exhausted.
7. **Baseline test discrepancy** not conclusively reconcilable (original Surefire reports unavailable).
8. **No automatic deadlock retry/backoff policy** for transfers (intentionally out of Phase 1A scope).

---

## 14. Overall Acceptance Recommendation

**Recommendation: ACCEPT Phase 1A for its scoped financial-guardrail objectives, with the documented residual risks and verification gaps explicitly recorded.**

- FIN-01: VERIFIED (contained; no money created)
- FIN-02: VERIFIED (contained; no credit on callback)
- FIN-06: VERIFIED (canonical lock ordering; H2/JVM level)
- FIN-12: PARTIALLY VERIFIED (H2 + reported PG18 primitive; PG16 target unverified)
- OPS-01: VERIFIED
- PostgreSQL 16 verification: BLOCKED
- Security review: PASS WITH ISSUES (low only)
- Scope compliance: VERIFIED
- Full regression: 175/0/0 (exit 0, H2, JDK 17)

Phase 1A is **not** authorized for Phase 1B or any further phase. Before Phase 1B, the team should address or explicitly accept: (1) implementing verified collection initiation/verification for FIN-01/FIN-02; (2) completing PostgreSQL 16 verification for FIN-12; (3) resolving or accepting the four low security findings; (4) addressing the out-of-scope races (`AdminService`, `BalanceRecalculationJob`, `AccountNumberGenerator`).

No commit, push, or Git history change was performed by IV-04. Repository state verified unchanged apart from this report file.
