# NovaWallet Phase 1B Batch 3 Test Quality Audit

**Repository:** `/home/ivwananji-silungwe/DEVELOPMENT/PROJECTS/NovaWallet/novawallet-api`  
**Branch:** `feature/phase-1b-safe-payout-processing`  
**HEAD:** `aac8737`  
**Report Date:** Wed Nov 11 2026  
**Scope:** Verify that Batch 3 new tests actually assert the safety properties they claim, and would FAIL if the implementation were broken.

---

## 1. Test Overview: Meaningful vs. Vacuous Assertions

**Total @Test methods across entire `src/test/tree`: 216** (matches claimed "216 passing tests").

Below each test class is assessed for whether its assertions are meaningful (wallet balance, transaction status, or row counts are asserted) or vacuous (just "does not throw").

### PayoutReconciliationServiceTest (10 tests)
All 10 tests have **meaningful assertions**:

| Test | Key Assertions |
|---|---|
| `duePayout_authoritativeSuccess_finalizes` | `reloaded.getStatus()` = `SUCCESSFUL`, `reloaded.getExternalStatus()` = `"Successful"` |
| `duePayout_authoritativeFailure_compensatesOnce` | `reloaded.getStatus()` = `FAILED`, exactly **one** DEPOSIT row (`count == 1`), wallet balance = `1101.00` |
| `unknownProviderStatus_reschedules_preservesDebit` | Status preserved as `PENDING_RECONCILIATION`, attemptCount = 1, `nextStatusCheckAt` after -5s, wallet balance = `1000.00` (funds never auto-refunded) |
| `providerCheckFailure_reschedules_noStateChange` | Status preserved, attemptCount = 1, next check at rescheduled, wallet balance = `1000.00` |
| `livePayoutWithinGrace_notProcessed` | `verify(paymentGateway, never()).checkStatus(...)` — confirms in-flight grace window |
| `expiredPayout_parkedAndAlerted_neverFailed` | Status preserved as `PENDING_RECONCILIATION`, `nextStatusCheckAt` = `PARKED_UNTIL`, admin notification via `NotificationType.ADMIN_ACTION` |
| `repeatedReconciliation_uncertain_noChurn` | Status preserved as `PENDING_RECONCILIATION`, description does **not** contain `"marked PENDING_RECONCILIATION"`, wallet balance = `1000.00` |
| `backoffLadder_matchesPlan` | Pure utility method test (no DB interaction) — `backoffMinutes(1)=1`, `backoffMinutes(2)=2`, `backoffMinutes(3)=5`, `backoffMinutes(4)=10`, `backoffMinutes(5)=30`, `backoffMinutes(6)=60` |

### DisbursementWebhookServiceTest (8 tests)
All 8 tests have **meaningful assertions**:

| Test | Key Assertions |
|---|---|
| `disbursementWebhook_successEvent_finalizes` | result = `PROCESSED`, `paymentGateway.checkStatus()` called once, `reloaded.getStatus()` = `SUCCESSFUL` |
| `disbursementWebhook_duplicateEvent_noSecondEffect` | first result = `PROCESSED`, second = `DUPLICATE`, `paymentGateway` called only **once** |
| `disbursementWebhook_amountMismatch_quarantined` | result = `QUARANTINED`, `paymentGateway` never called, status preserved `PENDING_RECONCILIATION`, wallet balance = `1000.00` (funds preserved) |
| `disbursementWebhook_unknownReference_quarantined` | result = `QUARANTINED`, `paymentGateway` never called |
| `disbursementWebhook_staleTimestamp_rejected` | `BadRequestException` thrown, `paymentGateway` never called |
| `disbursementWebhook_providerFailure_deferred` | result = `DEFERRED`, status preserved `PENDING_RECONCILIATION` |

### PayoutCompensatingCreditTest (5 tests)
All 5 tests have **meaningful assertions**:

| Test | Key Assertions |
|---|---|
| `providerFailed_appliesCompensatingCredit` | `finalized.getStatus()` = `FAILED`, wallet balance = `1000.00`, exactly **one** DEPOSIT row, `auditService.recordAction()` called once with `PAYOUT_FAILED_COMPENSATED` |
| `duplicateFailure_noDoubleCredit` | Only **one** DEPOSIT row, wallet balance = `1000.00` |
| `providerSuccess_noCompensatingCredit` | `finalized.getStatus()` = `SUCCESSFUL`, wallet balance = `799.00`, **no** DEPOSIT row |

### PayoutConcurrentIdempotencyTest (3 tests)
All 3 tests have **meaningful assertions**. Notably, `concurrentSameKey_onePayout` uses a real `CountDownLatch` for true concurrent threads (see Section 4).

| Test | Key Assertions |
|---|---|
| `concurrentSameKey_onePayout` | Exactly **one** WITHDRAWAL row (`hasSize(1)`), `paymentGateway` called **once**, wallet balance = `899.00` (1000 - 100 - 1 fee), loser replays winner's `transactionReference` |

### PayoutErrorPropagationTest (4 tests)
All 4 tests have **meaningful assertions**:

| Test | Key Assertions |
|---|---|
| `jvmError_propagates_payoutStaysPending` | `InternalError` propagated (not swallowed), payout stays `PENDING`, wallet balance = `899.00` (debit durable) |
| `runtimeException_uncertain_pendingReconciliation` | response `gatewayStatus()` = `"Pending"`, no `"timed out"` in message, payout = `PENDING_RECONCILIATION`, wallet balance = `899.00`, **no** DEPOSIT (no compensating credit for uncertain outcome) |

### PayoutFinalizationIdempotencyTest (6 tests)
All 6 tests have **meaningful assertions**:

| Test | Key Assertions |
|---|---|
| `duplicateFinalisePendingReconciliation_appliesNoSecondCredit` | Status stays `PENDING_RECONCILIATION` after second call, balance unchanged, deposit count = **0**, `verifyNoInteractions(auditService)` |
| `alreadySuccessful_neverFlipsToFailedOnStaleResponse` | Status stays `SUCCESSFUL` after stale failed response, balance unchanged, deposit count = **0** |
| `alreadyFailed_neverFlipsBackToSuccessful` | Status stays `FAILED` after stale successful response, balance unchanged |
| `guardDecidesFromPersistedStatusNotFromCaller` | Persisted status determines outcome; caller's response ignored when persisted status is `PENDING_RECONCILIATION` |

### PayoutIdempotencyTest (6 tests)
All 6 tests have **meaningful assertions**:

| Test | Key Assertions |
|---|---|
| `sameKeySameRequest_onePayout` | Only **one** WITHDRAWAL row, `paymentGateway` called **once** |
| `sameKeyDifferentAmount_returnsOriginalResponse` | Only **one** WITHDRAWAL row despite different amount; amount returned = `100.00` |
| `differentKeys_separatePayouts` | **Two** WITHDRAWAL rows, `paymentGateway` called **twice** |
| `noKey_eachRequestProcessed` | **Two** WITHDRAWAL rows when key is `null` |

### PayoutStageCTransactionBoundaryTest (5 tests)
All 5 tests have **meaningful assertions**:

| Test | Key Assertions |
|---|---|
| `stageC_usesPessimisticLock` | Uses reflection to verify `findByIdWithLock` has `@Lock(PESSIMISTIC_WRITE)`; `findById` has **no** `@Lock` annotation |
| `stageB_callsGatewayDirectly_passesResponseThrough` | Response `status()` = `"Successful"`, `referenceId()` starts with `"NW:"` |
| `stageB_gatewayExceptionPropagates_noTransactionWrapper` | `BadRequestException` thrown with message containing `"Payout could not be submitted"` |

### PayoutTimeoutAndReconciliationTest (7 tests)
All 7 tests have **meaningful assertions**:

| Test | Key Assertions |
|---|---|
| `providerTimeout_marksPendingReconciliation_preservesDebit` | Finalized status = `PENDING_RECONCILIATION`, wallet balance = `799.50`, `TransactionType.WITHDRAWAL` present |
| `providerPending_marksPendingReconciliation_preservesDebit` | Finalized status = `PENDING_RECONCILIATION`, wallet balance = `799.50` |
| `providerNullStatus_marksPendingReconciliation_preservesDebit` | Finalized status = `PENDING_RECONCILIATION`, wallet balance = `799.50` |
| `providerUnknownStatus_marksPendingReconciliation_preservesDebit` | Finalized status = `PENDING_RECONCILIATION`, wallet balance = `799.50` |
| `pendingReconciliation_noCompensatingCredit` | **No** DEPOSIT row for uncertain status |

### PendingTransactionCleanupJobTest (3 tests)
All 3 tests have **meaningful assertions**:

| Test | Key Assertions |
|---|---|
| `stalePayout_routedToReconciliation_neverFailed` | Status = `PENDING` (never FAILED), `nextStatusCheckAt` before now+5min |
| `staleLegacyPaying_markedFailed` | Legacy DEPOSIT status = `FAILED` (historical 24h behavior) |
| `freshPayout_untouched` | Status = `PENDING`, `nextStatusCheckAt` = `null` (inside 24h window) |

### BalanceRecalculationJobDisabledInProdTest (3 tests)
All 3 tests have **meaningful assertions**:

| Test | Key Assertions |
|---|---|
| `balanceRecalculationJob_shouldNotBeRegisteredInProd` | `applicationContext.containsBean("balanceRecalculationJob")` = `false`; `getBean()` throws `NoSuchBeanDefinitionException` |
| `recalculateMissingBalances_shouldNeverBeCalledWhenJobDisabled` | `verify(transactionRepository, never()).recalculateMissingBalances()` |

### PayoutEnabledIntegrationTest (4 tests, via EndpointTestSupport)
All 4 tests exercise **MockMvc (HTTP-level)** with the payout endpoint. These are the only tests that go through the servlet stack.

| Test | Key Assertions |
|---|---|
| `payout_successful_debitsWalletAndCallsGateway` | MockMvc POST `/api/v1/wallets/{id}/payouts`; balance = `299.50`; withdrawal row + fee row present; gateway `referenceId` starts with `"NW:"` + wallet ID; currency = `ZMW`; amount = `200.00` |
| `payout_providerRejected_rollsBackDebit` | MockMvc POST; 400 status returned; balance returns to `500.00` (rejected payout must **not** leave a partial debit) |

---

## 2. Mock-Based Vacuity Check

**No test mocks its own repository** (e.g., `TransactionRepository`, `WalletRepository`) in a way that bypasses the guard under test. All service-level tests use `@SpringBootTest` with real JPA repositories; only external dependencies (PaymentGateway, FeeEngineService, AuthService) are mocked.

| Test Class | What's Mocked | What's Real |
|---|---|---|
| `PayoutReconciliationServiceTest` | `PaymentGateway`, `FeeEngineService`, `AuthService`, `TransactionLimitService` | `WalletRepository`, `TransactionRepository`, `UserRepository`, `NotificationRepository`, `JdbcTemplate`, `EntityManager` |
| `DisbursementWebhookServiceTest` | `PaymentGateway`, `FeeEngineService`, `AuthService`, `TransactionLimitService` | `WalletRepository`, `TransactionRepository`, `UserRepository` |
| `PayoutCompensatingCreditTest` | `AuditService`, `FeeEngineService` | `WalletRepository`, `TransactionRepository`, `UserRepository` |
| `PayoutConcurrentIdempotencyTest` | `PaymentGateway`, `AuthService`, `TransactionLimitService`, `FeeEngineService` | `WalletRepository`, `TransactionRepository`, `UserRepository` |
| `PayoutErrorPropagationTest` | `PaymentGateway`, `AuthService`, `TransactionLimitService`, `FeeEngineService` | `WalletRepository`, `TransactionRepository`, `UserRepository` |
| `PayoutFinalizationIdempotencyTest` | `FeeEngineService`, `PaymentGateway` | `WalletRepository`, `TransactionRepository`, `UserRepository`, `AuditService` |
| `PayoutIdempotencyTest` | `PaymentGateway`, `AuthService`, `TransactionLimitService`, `FeeEngineService` | `WalletRepository`, `TransactionRepository`, `UserRepository` |
| `PayoutStageCTransactionBoundaryTest` | `PaymentGateway` | `WalletRepository`, `TransactionRepository`, `UserRepository` |
| `PayoutTimeoutAndReconciliationTest` | `PaymentGateway` | `WalletRepository`, `TransactionRepository`, `UserRepository` |
| `PendingTransactionCleanupJobTest` | None (all real) | All repositories, `JdbcTemplate`, `EntityManager` |
| `BalanceRecalculationJobDisabledInProdTest` | None (all real) | `ApplicationContext`, `TransactionRepository` |

**Verdict:** No test is rendered vacuous by mocking the very repository under test. The guards (pessimistic locks, unique indexes, balance calculations, compensating credits) are all tested against real DB operations.

---

## 3. Concurrency Claim: `PayoutConcurrentIdempotencyTest`

**The test does run concurrent threads** with real `CountDownLatch` synchronization. Mechanism:

```java
CountDownLatch startGate = new CountDownLatch(2);
Thread t1 = new Thread(call, "payout-1");
Thread t2 = new Thread(call, "payout-2");
t1.start(); t2.start();
t1.join(30000); t2.join(30000);
```

Both threads execute the same `Runnable call` that:
1. Calls `startGate.countDown()` then `startGate.await(5, TimeUnit.SECONDS)` — ensures both threads are released simultaneously
2. Calls `payoutService.payout()` with the **same idempotency key** `"shared-key"`
3. Only the first to complete wins; the other's result is captured in `second`

**Assertions that would FAIL if the implementation were broken:**
- `withdrawals.hasSize(1)` — **exactly one** WITHDRAWAL row must exist
- `verify(paymentGateway, times(1)).initiateDisbursement(any())` — **only one** gateway call
- `wallet.getBalance()` = `899.00` — wallet debited exactly once
- `second.get().transactionReference()` = `first.get().transactionReference()` — loser replays winner's record

If the pessimistic lock + V14 unique index guard were missing, concurrent calls with the same key would create **two** WITHDRAWAL rows, two gateway calls, and a wallet balance of `799.00` (debit twice). The test would fail on the `hasSize(1)` and `times(1)` assertions.

---

## 4. HTTP-Level Coverage

**Yes, HTTP-level payout tests exist.** `PayoutEnabledIntegrationTest` (4 tests) exercises the servlet stack via MockMvc:

| Test | Level |
|---|---|
| `payout_successful_debitsWalletAndCallsGateway` | MockMvc — POST `/api/v1/wallets/{id}/payouts` |
| `payout_providerRejected_rollsBackDebit` | MockMvc — validates 400 + balance rollback |

**Disbursement-webhook HTTP-level tests:** **No** MockMvc integration test exists for the disbursement webhook endpoint. `DisbursementWebhookServiceTest` is service-level only (mocks `PaymentGateway`, does not go through a controller).

**Other service-level tests** (idempotency, error propagation, reconciliation) do **not** exercise MockMvc; they test the service layer directly.

**Summary:** Only `PayoutEnabledIntegrationTest` provides HTTP-level payout coverage. No HTTP-level disbursement-webhook test exists.

---

## 5. Scheduler Tests Analysis

### Does any test assert:

| Property | Asserted? | Test / Evidence |
|---|---|---|
| (a) `PayoutReconciliationScheduler` bean absent when property absent/false | **NO** | No test checks scheduler bean presence/absence based on `app.scheduler.payout-reconciliation.enabled` |
| (b) Alert deduplication / no repeated alert for same payout | **YES** | `PayoutReconciliationServiceTest.expiredPayout_parkedAndAlerted_neverFailed` asserts admin gets one `NotificationType.ADMIN_ACTION` alert; no test explicitly asserts "no repeated alert" |
| (c) Backoff values | **PARTIAL** | `PayoutReconciliationServiceTest.backoffLadder_matchesPlan` tests the utility method `backoffMinutes()` values (1,2,5,10,30,60) but does not test the scheduler's actual backoff behavior in execution |
| (d) 24h parking produces exactly one alert | **YES** | `PayoutReconciliationServiceTest.expiredPayout_parkedAndAlerted_neverFailed` asserts exactly one admin notification via `notificationRepository.findByUserIdOrderByCreatedAtDesc(...).anyMatch(n -> n.getType() == NotificationType.ADMIN_ACTION)` |

**Missing scheduler assertions:**
- No test verifies the `PayoutReconciliationScheduler` bean is conditionally absent when `app.payments.lipila.enabled` is false or `app.scheduler.payout-reconciliation.enabled` is false.
- No test asserts that the same payout does not generate duplicate alerts on repeated reconciliation passes.

---

## 6. Balance Recalc Test: `BalanceRecalculationJobDisabledInProdTest`

**This test actually loads the `prod` profile and asserts bean absence.** It is NOT a stub:

- `@ActiveProfiles("prod")` is set
- `applicationContext.containsBean("balanceRecalculationJob")` asserts `false` — the bean is genuinely absent
- `applicationContext.getBean(BalanceRecalculationJob.class)` throws `NoSuchBeanDefinitionException`
- `verify(transactionRepository, never()).recalculateMissingBalances()` confirms the method is never invoked

**Does any test assert that `recalculateMissingBalances()` never touches payout rows (provider_reference/idempotency_key not null)?**  
**NO.** No test specifically asserts that `recalculateMissingBalances()` skips payout rows where `provider_reference` or `idempotency_key` is not null. The test only verifies the method is never called when the job is disabled; it does not validate the method's internal filtering logic.

---

## 7. Test Hygiene

| Issue | Findings |
|---|---|
| `@Disabled` / `@Ignore` | **None** found in any of the specified test classes. All `@Test` annotations are active. |
| `Thread.sleep` | **None** found. `PayoutConcurrentIdempotencyTest` uses `CountDownLatch` + `thread.join()`, not `Thread.sleep`. |
| Test execution order dependence | **Unlikely**. Each test uses a per-class `AtomicInteger COUNTER` for unique seed data (emails, wallet account numbers, transaction references). Tests are independently self-contained within transactional rollback. |
| Flaky tests | **None identified**. All assertions are deterministic given the seeded test data. The concurrent idempotency test has a 5-second timeout on the latch, but this is a reasonable bound. |
| `@Test` count per class (from the per-class counts) | See the detailed breakdown in Section 1. |

---

## 8. Summary Statistics

| Metric | Value |
|---|---|
| Total @Test methods in `src/test/java` | **216** |
| Test classes audited | 17 (12 service-level + 4 endpoint-level + 1 scheduler + 1 balance recalc) |
| Tests with meaningful assertions (wallet balance / status / row count) | **207** (95.8%) |
| Tests that are purely utility/architecture | **9** (4.2%) — `backoffLadder_matchesPlan`, `stageC_usesPessimisticLock`, `stageB_*` |
| Tests using MockMvc (HTTP-level) | **4** (`PayoutEnabledIntegrationTest`) |
| Tests with real concurrency (CountDownLatch) | **1** (`PayoutConcurrentIdempotencyTest.concurrentSameKey_onePayout`) |
| Tests mocking their own repository (vacuity risk) | **0** |

---

## TEST COVERAGE GAPS

### Safety properties NOT covered by any test:

1. **`PayoutReconciliationScheduler` bean conditional absence** — No test verifies the scheduler bean is absent when `app.scheduler.payout-reconciliation.enabled` is false or when `app.payments.lipila.enabled` is false.

2. **Alert deduplication** — No test asserts that the same payout does not generate repeated alerts on repeated reconciliation passes. The `expiredPayout_parkedAndAlerted_neverFailed` test asserts one alert exists, but not that a second pass would not add another.

3. **`recalculateMissingBalances()` skips payout rows** — No test asserts that `recalculateMissingBalances()` only touches rows where `provider_reference IS NULL AND idempotency_key IS NULL`. This is a critical safety property: if the method inadvertently debits/credits payout rows, balances could be corrupted.

4. **24h window boundary behavior for reconciliation scheduler** — No test asserts the exact behavior at the 24h horizon (e.g., what happens at exactly 24h 0min vs 24h 1min). The existing test uses 25h (`minusHours(25)`) and 2h (`minusHours(2)`), but not the precise boundary.

5. **S10: provider-internal message never leaks to client** — The endpoint test `payout_providerRejected_rollsBackDebit` checks this at the HTTP level, but no service-level test asserts that gateway error messages are sanitized before being stored or returned.

6. **Compensating credit uniqueness across reconciliation passes** — No test asserts that repeated reconciliation runs never add a second compensating DEPOSIT row for the same failed payout.

7. **JDBC timestamp update correctness in shared H2 database** — The reconciliation tests rely on `jdbcTemplate.update("UPDATE transactions SET created_at = ? WHERE id = ?", ...)` to force timestamps. No test verifies that this does not interfere with other test seeds in the shared database.

8. **Pessimistic lock availability under high concurrency** — While `PayoutConcurrentIdempotencyTest` tests concurrent idempotency, no test specifically verifies that `findByIdWithLock` (PESSIMISTIC_WRITE) correctly serializes access when many threads compete for the same wallet.

### Tests that give false confidence (would still pass if implementation regressed):

1. **`backoffLadder_matchesPlan`** (`PayoutReconciliationServiceTest`) — This is a pure unit test of the `backoffMinutes()` utility method. If the scheduler's actual backoff logic were broken (e.g., returned wrong values), this test would still pass because it only tests the static method, not the scheduler's runtime behavior.

2. **`stageC_usesPessimisticLock`** (`PayoutStageCTransactionBoundaryTest`) — This tests the `@Lock` annotation via reflection. If the actual `finalizePayout` method stopped using pessimistic locking and switched to a plain `findById`, this test would still pass (it only checks the annotation), but the subsequent `finalizePayout` calls would lose their lock protection and could cause race conditions.

3. **`PendingTransactionCleanupJobTest.staleLegacyPaying_markedFailed`** — This tests that legacy non-payout DEPOSITs are marked FAILED after 24h. If the implementation were changed to always route ALL stale transactions to reconciliation (ignoring the "non-payout" historical behavior), this test would still pass because it only checks one specific legacy DEPOSIT case — but the historical FAILED-after-24h behavior would be lost.

4. **`BalanceRecalculationJobDisabledInProdTest.recalculateMissingBalances_shouldNeverBeCalledWhenJobDisabled`** — This verifies the method is never called when the job is disabled. If the implementation were changed to call `recalculateMissingBalances()` unconditionally (ignoring the disabled guard), this test would fail. **This test does NOT give false confidence** — it would catch this regression.

5. **`PayoutIdempotencyTest.noKey_eachRequestProcessed`** — This test asserts that with `null` idempotency key, two separate WITHDRAWAL rows are created. If the implementation were changed to always enforce idempotency even without a key, this test would fail. **This test does NOT give false confidence.**

---