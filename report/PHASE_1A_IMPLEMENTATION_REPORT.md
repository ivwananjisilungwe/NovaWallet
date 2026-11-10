# Phase 1A Implementation Report — Financial Guardrails

**Repository:** `/home/ivwananji-silungwe/DEVELOPMENT/PROJECTS/NovaWallet/novawallet-api`
**Branch:** `feature/phase-1a-financial-guardrails`
**Baseline commit:** `60179fb5b09f0d5a11338f07ed2cb23ef393b1f5`
**Report file:** `report/PHASE_1A_IMPLEMENTATION_REPORT.md` (ignored by repository policy under `/report/`; created as a local deliverable)
**Scope:** Financial guardrails only. No Phase 1B, payment orchestration, double-entry ledger, modular-monolith, event-driven/outbox, or microservice extraction work was performed.

---

## A. Executive Summary

Phase 1A implemented and verified five financial/build guardrails on the existing Spring Boot monolith:

1. **FIN-01 — Direct deposit containment:** `POST /api/v1/wallets/{walletId}/deposit` remains registered and still performs ownership/active-wallet checks, but `TransactionService.deposit` now throws `ServiceUnavailableException` (HTTP 503) before any balance update, transaction write, audit record, or notification. No money is created.
2. **FIN-02 — Webhook callback containment:** Lipila collection callbacks still authenticate the static `X-Webhook-Secret` header with constant-time comparison and still return 200 for non-Successful statuses, but a Successful callback now throws `ServiceUnavailableException` (HTTP 503) before crediting. No `transactionService.deposit` call remains.
3. **FIN-06 — Transfer deadlock ordering:** `TransactionService.transfer` now acquires pessimistic locks in ascending UUID order, maps locked rows back to sender/receiver, rechecks sender active status under lock, and preserves atomic debit/credit plus rollback. No isolation downgrade was introduced.
4. **FIN-12 — DB-backed transaction references:** `TransactionReferenceGenerator` now allocates from `transaction_reference_seq` via `nextval`, with a `1..99,999,999` guard. Migration V12 scans all references, ignores non-conforming rows with a warning, fails closed at capacity, restarts to `max+1`, and never rewrites historical rows. Streaming was fixed with a nonzero fetch size, autocommit guard, and try-with-resources.
5. **OPS-01 — Maven Wrapper restoration:** Maven Wrapper 3.3.4 script-only configuration was restored with Maven 3.9.16 and a pinned SHA-256 derived from the official ASF SHA-512. Clean-cache bootstrap and tampered-checksum failure were verified.

Final regression result: **175 tests, 0 failures, 0 errors, 0 skipped** on H2 with JDK 17 via `./mvnw clean test`. Baseline before Phase 1A was 144 tests with 8 failures caused by duplicate transaction references.

Security review result: **PASS WITH ISSUES**, with three low findings only. No high or critical findings. Scope was compliant.

---

## B. Initial Repository State

- **Branch:** `feature/phase-1a-financial-guardrails`
- **Baseline commit:** `60179fb5b09f0d5a11338f07ed2cb23ef393b1f5`
- **Initial test state:** 144 tests, 8 failures (duplicate transaction references sharing DB state).
- **Initial risk state:** FIN-01 and FIN-02 were confirmed critical vulnerabilities; FIN-06 was a confirmed high-severity deadlock vulnerability; FIN-12 and OPS-01 were confirmed defects from TASK-H.
- **No commits or pushes were performed during Phase 1A.**

---

## C. Task-by-Task Implementation Details

### C.1 FIN-01 — Direct unverified wallet deposit containment

**Files changed:**

- `src/main/java/com/novawallet/novawallet_api/transaction/controller/TransactionController.java`
- `src/main/java/com/novawallet/novawallet_api/transaction/service/TransactionService.java`

**Implementation:**

- `TransactionController.deposit` documentation now states that direct deposits are temporarily disabled and never credit a wallet. Swagger responses include HTTP 503.
- `TransactionService.deposit(UUID, DepositRequest, UUID)` still calls `findActiveWalletWithLock(walletId, userId)`, preserving ownership and active-wallet checks.
- After those checks, it throws `ServiceUnavailableException("Direct wallet deposits are unavailable until verified payment collection is implemented")`.
- No `walletRepository.updateBalance`, `Transaction` save, audit record, or notification occurs.
- `ServiceUnavailableException` was imported into `TransactionService`.

**Tests:**

- `TransactionEdgeCaseEndpointIntegrationTest`:
  - `directDeposit_isUnavailable_doesNotCredit`
  - `directDeposit_withIdempotencyKey_isUnavailable`
  - `directDeposit_nonOwner_isForbidden`
  - `directDeposit_unknownWallet_returnsNotFound`
  - `directDeposit_frozenWallet_returnsBadRequest`
- These tests verify HTTP 503, unchanged balance, and zero transaction rows.

**Residual risk:** Direct deposits are contained, not resolved. A verified collection workflow does not yet exist.

### C.2 FIN-02 — Webhook collection callback containment

**Files changed:**

- `src/main/java/com/novawallet/novawallet_api/payment/controller/WebhookController.java`
- `src/main/java/com/novawallet/novawallet_api/payment/service/WebhookService.java`

**Implementation:**

- `WebhookController` Swagger response text now states that HTTP 503 also covers contained collection callbacks.
- `WebhookService.processCollectionCallback` still:
  - validates `status`, `amount`, and `referenceId`;
  - parses `NW:<walletId>:<uuid>`;
  - returns 200 without credit for non-Successful statuses;
  - normalizes the amount;
  - looks up the wallet, returning 404 for an unknown wallet.
- After wallet lookup, it logs a warning and throws `ServiceUnavailableException("Collection callbacks are temporarily disabled until verified collection initiation is implemented")`.
- The previous `transactionRepository.existsByDescription` deduplication and `transactionService.deposit` credit path were removed.
- `DepositRequest` and `TransactionService` imports were removed from `WebhookService`.
- Wallet lookup changed from `findByIdWithLock` to `findById` because no credit occurs and no lock is needed.
- Secret authentication remains the static `X-Webhook-Secret` header with constant-time comparison.

**Tests:**

- `PaymentWebhookEndpointIntegrationTest`:
  - `successfulCallback_withValidSecret_isContained`
  - `successfulCallback_duplicateReplay_isContainedBothTimes`
  - `successfulCallback_withLargeAmount_isContained`
  - `successfulCallback_withZeroAmount_returnsBadRequest`
  - `successfulCallback_withNegativeAmount_returnsBadRequest`
  - `successfulCallback_forUnknownWallet_returnsNotFound`
  - `failedCallback_acknowledgedWithoutCredit`
  - `malformedReference_returnsBadRequest`
  - `duplicateCallback_isContained_idempotently`
  - `invalidSecret_returns401`
- These tests verify 503 containment, no balance change, no transaction rows, and unchanged secret behavior.

**Residual risk:** Callbacks are contained, not resolved. There is no collection initiation record, provider verification, authoritative amount/currency match, idempotency persistence, or replay protection beyond the existing static secret.

### C.3 FIN-06 — Canonical transfer lock ordering

**Files changed:**

- `src/main/java/com/novawallet/novawallet_api/transaction/service/TransactionService.java`

**Implementation:**

- The transfer path now detaches the unlocked sender entity through `EntityManager` before acquiring locks, avoiding stale-version conflicts under concurrency.
- It computes `firstLockId` and `secondLockId` by comparing sender and receiver wallet UUIDs.
- It locks the lower UUID first and the higher UUID second using `WalletRepository.findByIdWithLock`.
- It maps `firstLockedWallet` and `secondLockedWallet` back to `lockedSenderWallet` and `lockedReceiverWallet`.
- It reassigns `senderWallet` and `receiverWallet` to the locked entities.
- It rechecks `senderWallet.getStatus() == WalletStatus.ACTIVE` under lock.
- Receiver active status remains checked.
- Atomic debit/credit and rollback behavior are preserved.
- No `@Transactional` isolation level was downgraded.

**Tests:**

- `TransactionServiceTransferLockOrderTest`
- `ConcurrentTransferIntegrationTest`
- `TransferRollbackIntegrationTest`

**Residual risk:** Canonical ordering is implemented; an automatic deadlock retry/backoff policy is intentionally not part of Phase 1A.

### C.4 FIN-12 — DB-backed transaction reference sequence

**Files changed:**

- `src/main/java/com/novawallet/novawallet_api/transaction/repository/TransactionRepository.java`
- `src/main/java/com/novawallet/novawallet_api/transaction/service/TransactionReferenceGenerator.java`
- `src/main/java/db/migration/V12__CreateTransactionReferenceSequence.java`

**Repository change:**

- Added `nextReferenceSequence()`, implemented as `SELECT nextval('transaction_reference_seq')`.

**Generator change:**

- Removed `AtomicLong`, `@PostConstruct`, date-change reset logic, and use of `findMaxReferenceByPrefix`.
- `generate()` now calls `transactionRepository.nextReferenceSequence()`.
- It throws `IllegalStateException` if the database returns null.
- It enforces `1 <= sequence <= 99,999,999`.
- It formats the reference as `NWTXyyyyMMdd` plus an 8-digit sequence.
- The database unique constraint on `transactions.reference` remains the final duplicate guard.

**Migration V12 behavior:**

- Streams `SELECT reference FROM transactions`.
- Uses `FETCH_SIZE = 1000`.
- Requires `autoCommit == false`; otherwise throws `IllegalStateException`.
- Requires a nonzero fetch size; otherwise throws `IllegalStateException`.
- Uses try-with-resources for `Statement` and `ResultSet`.
- Matches references against `^NWTX[0-9]{8}[0-9]{8}$`.
- Computes the maximum historical suffix.
- Ignores non-conforming references and logs a warning with the count.
- Fails closed if `maxSuffix >= 99,999,999`.
- Creates `transaction_reference_seq` with `START WITH 1 INCREMENT BY 1`.
- Restarts the sequence to `maxSuffix + 1`.
- Does not rewrite historical rows.

**Tests:**

- `TransactionReferenceSequenceMigrationTest`:
  - seeds sequence above historical maximum suffix;
  - empty database first `nextval` is 1;
  - scale test with 20,000 valid + 20,000 ignored rows.
- `V12MigrationStreamingGuardTest`:
  - fails when autocommit is true;
  - fails when fetch size is non-positive;
  - fails when `setFetchSize` throws;
  - validates nonzero fetch size before SELECT.
- `TransactionReferenceConcurrencyIntegrationTest` and `TransactionReferenceGeneratorTest`.

**Verification evidence:**

- Scale test runtime: 1967 ms.
- First `nextval` after 20,000 valid rows: 20001.
- PostgreSQL primitive verification on PostgreSQL 18.6 passed: sequence restart, `nextval` values 43 and 44, fail-closed duplicate `CREATE SEQUENCE`, `pg_typeof` bigint, and regex exclusion.
- PostgreSQL 16 production target was not verified.

**Residual risk:** Sequence capacity remains finite. Production-target PostgreSQL 16 verification remains open.

### C.5 OPS-01 — Maven Wrapper restoration

**Files changed:**

- `.mvn/wrapper/maven-wrapper.properties` (untracked by repository policy)
- `mvnw`
- `mvnw.cmd`

**Implementation:**

- Maven Wrapper 3.3.4 script-only configuration.
- Maven 3.9.16.
- SHA-256 pinned from the official ASF SHA-512.
- `mvnw` mode `755`.
- Wrapper properties remain untracked/ignored by repository policy.

**Verification:**

- Clean-cache bootstrap run passed.
- Tampered-checksum failure was verified.
- Full `./mvnw clean test` on JDK 17 passed 175/175 tests with exit code 0.

---

## D. Database Changes

### D.1 Migration V12

- **Migration file:** `src/main/java/db/migration/V12__CreateTransactionReferenceSequence.java`
- **Database object created:** `transaction_reference_seq`
- **Sequence definition:** `CREATE SEQUENCE transaction_reference_seq START WITH 1 INCREMENT BY 1`
- **Seeding:** `ALTER SEQUENCE transaction_reference_seq RESTART WITH maxSuffix + 1`
- **Historical data impact:** None. Historical `transactions` rows are scanned but not rewritten. Only the maximum suffix among conforming references affects sequence seeding.
- **Non-conforming rows:** Ignored and counted; a warning is logged.
- **Fail-closed behavior:** If the maximum conforming suffix is `>= 99,999,999`, migration throws `IllegalStateException` and does not create/seed the sequence.

### D.2 Schema constraints

- No new table, foreign key, or check constraint was added in Phase 1A.
- The existing unique constraint on `transactions.reference` remains the final duplicate guard.
- Migration V12 does not alter `transactions`.

### D.3 PostgreSQL verification

- Primitive verification passed on PostgreSQL 18.6:
  - sequence restart behavior;
  - `nextval` values 43 and 44;
  - fail-closed duplicate `CREATE SEQUENCE`;
  - `pg_typeof` bigint;
  - regex exclusion of non-conforming references.
- Production target is PostgreSQL 16; this was not verified.

---

## E. Test Results

### E.1 Full regression

- Command: `./mvnw clean test`
- JDK: 17
- Database: H2
- Result: **175 tests, 0 failures, 0 errors, 0 skipped**
- Exit code: 0

### E.2 Baseline comparison

- Baseline: 144 tests, 8 failures.
- Final: 175 tests, 0 failures.
- Net change: +31 tests.

### E.3 Targeted test areas

Targeted suites passed in their respective runs. Relevant test files include:

- `TransactionEdgeCaseEndpointIntegrationTest`
- `PaymentWebhookEndpointIntegrationTest`
- `TransactionServiceTransferLockOrderTest`
- `ConcurrentTransferIntegrationTest`
- `TransferRollbackIntegrationTest`
- `TransactionReferenceSequenceMigrationTest`
- `V12MigrationStreamingGuardTest`
- `TransactionReferenceConcurrencyIntegrationTest`
- `TransactionReferenceGeneratorTest`
- `TransactionFlowIntegrationTest`
- `IdempotencyEndpointIntegrationTest`
- `AuditAssertionEndpointIntegrationTest`

No exact combined targeted count is claimed beyond the verified full-regression count of 175.

### E.4 Maven Wrapper verification

- Clean-cache bootstrap: passed.
- Tampered-checksum failure: verified.
- Wrapper mode: `755`.
- Maven version: 3.9.16.

### E.5 Concurrency verification

- Opposite-direction transfer tests passed.
- Canonical UUID lock ordering was verified by unit/integration tests.
- No deadlock was observed in the verified transfer test runs.

---

## F. Security and Financial-Integrity Review

**Review result:** PASS WITH ISSUES.

**Findings:**

- Three low findings:
  1. Unused webhook fields / `transactionRepository` / `transactionService` in `WebhookService`.
  2. Stale Javadoc.
  3. Amount validation occurs before unknown-wallet 404 on Successful callbacks.
- No high or critical findings.
- Scope compliance: compliant.

**Additional out-of-scope observations:**

- `AdminService.toggleUserDeletedStatus` has a status race without a pessimistic lock.
- `BalanceRecalculationJob` naming/scope remains an observation.
- `AccountNumberGenerator` uniqueness was not reviewed.
- Cosmetic audit-message fix is not part of Phase 1A.

**Financial-integrity assessment:**

- FIN-01 no longer allows arbitrary self-crediting.
- FIN-02 no longer credits unverified callback amounts.
- FIN-06 deadlock ordering is mitigated and verified.
- FIN-12 reference collisions are mitigated by a DB-backed sequence.
- OPS-01 build reproducibility is restored.

---

## G. Scope Compliance

Phase 1A did not perform:

- Phase 1B work.
- Payment orchestration or provider workflow implementation.
- Double-entry ledger implementation.
- Modular monolith boundary enforcement.
- Event-driven architecture or outbox implementation.
- Microservice extraction.
- Historical transaction data rewrites.
- Production deployment or commit/push.

Modified source, test, and migration files are part of the Phase 1A implementation tasks, not this documentation task. This documentation task modified only documentation and report files.

---

## H. Git Verification

### H.1 Branch and baseline

- Branch: `feature/phase-1a-financial-guardrails`
- HEAD: `60179fb5b09f0d5a11338f07ed2cb23ef393b1f5`
- No commits or pushes were performed.

### H.2 Tracked files modified

- `docs/architecture/backend-risk-register.md`
- `docs/architecture/current-architecture.md`
- `docs/architecture/implementation-roadmap.md`
- `src/main/java/com/novawallet/novawallet_api/payment/controller/WebhookController.java`
- `src/main/java/com/novawallet/novawallet_api/payment/service/WebhookService.java`
- `src/main/java/com/novawallet/novawallet_api/transaction/controller/TransactionController.java`
- `src/main/java/com/novawallet/novawallet_api/transaction/repository/TransactionRepository.java`
- `src/main/java/com/novawallet/novawallet_api/transaction/service/TransactionReferenceGenerator.java`
- `src/main/java/com/novawallet/novawallet_api/transaction/service/TransactionService.java`
- `src/test/java/com/novawallet/novawallet_api/endpoint/AuditAssertionEndpointIntegrationTest.java`
- `src/test/java/com/novawallet/novawallet_api/endpoint/IdempotencyEndpointIntegrationTest.java`
- `src/test/java/com/novawallet/novawallet_api/endpoint/PaymentWebhookEndpointIntegrationTest.java`
- `src/test/java/com/novawallet/novawallet_api/endpoint/TransactionEdgeCaseEndpointIntegrationTest.java`
- `src/test/java/com/novawallet/novawallet_api/transaction/controller/ConcurrentTransferIntegrationTest.java`
- `src/test/java/com/novawallet/novawallet_api/transaction/controller/TransactionFlowIntegrationTest.java`

### H.3 Untracked files

- `.mvn/wrapper/maven-wrapper.properties`
- `src/main/java/db/migration/V12__CreateTransactionReferenceSequence.java`
- `src/test/java/com/novawallet/novawallet_api/transaction/controller/TransferRollbackIntegrationTest.java`
- `src/test/java/com/novawallet/novawallet_api/transaction/migration/TransactionReferenceSequenceMigrationTest.java`
- `src/test/java/com/novawallet/novawallet_api/transaction/migration/V12MigrationStreamingGuardTest.java`
- `src/test/java/com/novawallet/novawallet_api/transaction/service/TransactionReferenceConcurrencyIntegrationTest.java`
- `src/test/java/com/novawallet/novawallet_api/transaction/service/TransactionReferenceGeneratorTest.java`
- `src/test/java/com/novawallet/novawallet_api/transaction/service/TransactionServiceTransferLockOrderTest.java`

### H.4 Diff summary

- Tracked diff stat: 15 files changed, 652 insertions, 258 deletions after documentation updates.
- `git diff --check`: passed (no whitespace errors).
- No Maven build was run for this documentation task.

### H.5 Mirror verification

The three architecture documents were mirrored under `/home/ivwananji-silungwe/DEVELOPMENT/PROJECTS/NovaWallet/docs/architecture/` and verified byte-for-byte identical with `cmp` and SHA-256:

- `backend-risk-register.md`: `d34e9545f1e3371fe8b9cbd426e7266855b959766fb621c2520ae8b46dee59a7`
- `current-architecture.md`: `6b34259baa0764020c167681fe3f8c396a3027c49d6f6c7c06cf2b2f19a26614`
- `implementation-roadmap.md`: `996a93901701d70a79ea6c73e72298a0ae2a0e2be5392d7725a166c78f9fcc3e`

The report file is intentionally ignored by the repository's `/report/` rule and is not tracked.

---

## I. Phase 1A Acceptance Assessment

| Risk ID | Status | Assessment |
|---|---|---|
| FIN-01 | TEMPORARILY DISABLED | Direct deposit no longer credits funds. Ownership and active checks remain. No admin/public equivalent exists. Remaining risk: verified collection workflow not implemented. |
| FIN-02 | TEMPORARILY DISABLED | Callback credit path is contained. Secret auth remains unchanged. Non-Successful callbacks still return 200. No credit occurs. Remaining risk: no collection initiation/verification workflow or amount/currency match. |
| FIN-06 | VERIFIED | Canonical UUID lock ordering is implemented and tested. Sender active status is rechecked under lock. Atomic debit/credit and rollback are preserved. No isolation downgrade. Residual retry/backoff policy is not part of Phase 1A. |
| FIN-12 | MITIGATED | DB-backed sequence is implemented and tested. Migration streams references, ignores non-conforming rows, fails closed at capacity, and preserves historical rows. PostgreSQL 16 production target was not verified; PostgreSQL 18.6 primitive verification passed. |
| OPS-01 | VERIFIED | Wrapper bootstrap and checksum pinning were restored and verified. Full regression passed. |

Overall Phase 1A acceptance: **accepted for independent review**, with the documented residual risks and verification gaps.

---

## J. Recommendation

Phase 1A is complete for its scoped financial-guardrail objectives and is ready for independent review. It is **not** authorized for Phase 1B or any further phase. Before Phase 1B, the team should address or explicitly accept:

1. FIN-01/FIN-02 remain contained, not resolved; verified collection initiation and verification are still required.
2. FIN-12 production-target PostgreSQL 16 verification remains open.
3. Low security findings from review: unused webhook fields/repository/service, stale Javadoc, and amount-validation ordering before unknown-wallet 404.
4. Out-of-scope observations: `AdminService.toggleUserDeletedStatus` status race, `BalanceRecalculationJob` naming/scope, and `AccountNumberGenerator` uniqueness.
5. Cosmetic audit-message fix is outside Phase 1A scope.

No commit or push was performed.
EOF
echo written
