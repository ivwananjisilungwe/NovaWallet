# Phase 1C — Batch 3 Plan: Wallet, Account & Financial Concurrency

Date: 2026-11-14 · Prompt: BE-1C-3 · Template: T5 (plan first) · Style model: `PHASE_1C_BATCH_2_PLAN.md`

**Status: PLAN ONLY.** No source, test, migration, or documentation file was changed. No Maven or git write command was run. Implementation must not start until the decisions in §6 marked **BLOCKING** are answered.

Identifiers in scope (BE-1C-3): R-01, R-02, R-03, FIN-09, FIN-11, NEW-FIN-01. Decisions: DEC-04 (OPEN), DEC-05 (DECIDED), DEC-12 (DECIDED). DEC-11 (OPEN) is needed for the PostgreSQL proof and is not listed in the prompt; it is included here because the prompt's test requirement depends on it.

---

## 0. Summary

1. **R-01 is confirmed.** `AdminService.toggleUserDeletedStatus` reads the wallet without a lock, sets it to `ACTIVE` on restore regardless of why it was frozen, and leaves `freeze_reason` set (`AdminService.java:88-93`).
2. **R-03 is confirmed as a design defect.** The account-number check is advisory (`AccountNumberGenerator.java:31`). The unique index is authoritative (`V3:15`). A collision surfaces at the wallet insert, outside the generator, with no retry.
3. **NEW-FIN-01 needs correction before implementation.** `User` has `@Version` (`User.java:116`), so a losing concurrent approval should fail at its user-row update and roll back its wallet insert. The likely real defects are an uncontrolled failure instead of a clean 409, and the absence of a wallet-level guard. A RED test must prove which before the register wording is kept.
4. **FIN-09 is confirmed.** The percentage fee is rounded to 2 dp before the flat fee is added (`FeeEngineService.java:54-56`). Existing tests do not cover the double-rounding case.
5. **FIN-11 is blocked.** The code exempts KYC-unapproved users from the daily cap (`TransactionLimitService.java:110-112`), but no owner decision chooses the replacement policy. The prompt's "approved policy" does not exist in `docs/DECISIONS.md`.
6. **FIN-06 (lock order) is already fixed** (`TransactionService.java:217-228`). The existing proof is Mockito-only or H2-only; neither is PostgreSQL evidence.
7. **There is a new SQL precedence bug** in the recalc backfill (`TransactionRepository.java:159-160`). Payout exclusion applies only to one OR branch. The job is contained, so this is latent.
8. **Transfer and withdraw have no service-level idempotency.** Only payouts do (`PayoutService.java:233-257`). The filter's header is optional and its key ignores endpoint and body.
9. **The `pgtest` profile does not exist in `novawallet-api/src`.** It is mentioned only in docs. Creating it is part of this batch and depends on DEC-11.
10. **No migration is required for Batch 3 by default.** The fixes fit existing columns. Any new idempotency fingerprint column is a decision (D-4) and would take V17 from Batch 4.

---

## 1. What I Read and What It Shows

### 1.1 Documents and sources read

- Root `AGENTS.md`, `novawallet-api/AGENTS.md`, `docs/DECISIONS.md`, `prompts/templates/T5_plan_first.md`, `prompts/stage-01_backend-security/_COMMON_BATCH_RULES.md`, `prompts/stage-01_backend-security/BE-1C-3_financial_concurrency.md`.
- `novawallet-api/docs/phase-1c/`: `PHASE_1C_BATCH_2_PLAN.md`, `PHASE_1C_IMPLEMENTATION_PLAN.md` (§5-§13), `PHASE_1C_RISK_TRACEABILITY_MATRIX.md`, `PHASE_1C_DECISION_LOG.md`.
- `novawallet-api/docs/architecture/backend-risk-register.md` (lines 1-420 read; §5.1 at lines 689-699 read directly; the remainder was not needed for this plan).
- Source read in full or in the cited ranges: `AdminService`, `AdminKycService`, `WalletService`, `AccountNumberGenerator`, `WalletRepository`, `Wallet`, `TransactionService`, `TransactionRepository`, `TransactionLimitService`, `FeeEngineService`, `BalanceRecalculationJob`, `TransactionController`, `IdempotencyFilter`, `IdempotencyService`, `PayoutService` (ranges 140-160, 226-262, 440-560, 680-690), `FreezeReason`, `UserRepository`, `User` (`@SQLRestriction`, `@Version`), `KycConfig`, `KycExpiryJob` (ranges only).
- Migrations read: `V3`, `V9`, `V14`, `V16`; `V15` checked by grep only; `V6` not read in this pass. `V12` is Java-based at `src/main/java/db/migration/V12__CreateTransactionReferenceSequence.java`. Head is V16.
- Tests read: `FeeEngineServiceTest` (182-200), `TransactionServiceTransferLockOrderTest` (50-104), `BalanceRecalculationJobDisabledInProdTest` (lines 30-73), `ConcurrentTransferIntegrationTest` (header and lines 88-130).

### 1.2 Verified findings

Status key: **CONFIRMED** (defect present, read in source) · **CORRECTED** (prior docs overstate it) · **NEW** (not in any register; proposed ID) · **VERIFIED-OK** (control present).

| # | Finding | Evidence (verified by reading) | Status |
|---|---|---|---|
| F1 | Admin restore unconditionally sets wallet `ACTIVE`; the wallet is looked up with no lock; the reason is not cleared. | `AdminService.java:88-93` (`findByUserId`, `setStatus(ACTIVE)`, no `freezeReason` change). `WalletService.unfreezeWallet` clears the reason at `:95`, so restore leaves `status=ACTIVE` with a stale `freeze_reason`. | CONFIRMED (R-01, DEC-12) |
| F2 | Deactivation freezes the wallet with no reason and no lock. | `AdminService.java:100-105`. `FreezeReason` has no value for user deactivation (`FreezeReason.java:10-15`: `SUSPICIOUS_ACTIVITY`, `ADMIN_ACTION`, `USER_REQUEST`). | CONFIRMED |
| F3 | The admin path does not lock the user row first. The documented lock order (user row, then wallet rows) is therefore not implemented on this path. | `AdminService.java:77` (`findByIdIncludingDeleted`, plain select). `UserRepository.java:32-33` is a native query, which is needed to bypass `@SQLRestriction`. `User.java:26` is `@SQLRestriction("deleted = false")`. | CONFIRMED |
| F4 | Wallet-per-user is not enforced. `wallets.user_id` has only a non-unique index. The entity Javadoc claims "exactly one wallet" and that negatives are blocked at DB level; both are untrue at DB level. | `V3:12` (FK), `V3:16` (`idx_wallets_user_id`, non-unique). `Wallet.java:16` and `:18` (Javadoc). `WalletRepository.java:23` returns `Optional<Wallet>` from `findByUserId`; Spring Data throws `IncorrectResultSizeDataAccessException` when more than one row matches (standard behaviour; to be proven by test). Callers: `TransactionService.java:195`, `AdminService.java:88,100`, `WalletService.java:44`. | CONFIRMED |
| F5 | Approval race (NEW-FIN-01). A check-then-act on `PENDING` exists, but the user row carries `@Version`, so a losing concurrent approval should fail at its user UPDATE and roll back its wallet INSERT. The duplicate-wallet outcome is therefore **unproven**. The likely defect is an uncontrolled failure (no 409), plus no wallet-level guard. | `AdminKycService.java:106-109` (load and `PENDING` check), `:126` (user save), `:138-145` (wallet build and save), `User.java:116` (`@Version`). `GlobalExceptionHandler.java:158` catch-all `Exception` handler. Status code for optimistic-lock and integrity failures to be proven by RED test. | **CORRECTED** (NEW-FIN-01 wording) |
| F6 | Account number is checked before insert, not enforced at insert; collisions surface as an unhandled integrity error. Exhaustion throws a generic `IllegalStateException`. | `AccountNumberGenerator.java:31` (advisory `existsByAccountNumber`), `:36` (`IllegalStateException`), `:20` (`MAX_ATTEMPTS=10`). `V3:15` (unique index, authoritative). Only caller: `AdminKycService.java:140`. Test wiring only: `TestConfig.java:17-20`. | CONFIRMED (R-03) |
| F7 | PostgreSQL aborts a transaction after a failed statement. A retry after an insert violation therefore needs a savepoint or a separate transaction. A naive "catch and retry" inside the same `@Transactional` will not work. | PostgreSQL behaviour (general knowledge of PG semantics; to be proven by the R-03 PostgreSQL test). Applies to `AccountNumberGenerator` callers at `AdminKycService.java:140,145`. | DESIGN CONSTRAINT |
| F8 | FIN-06 (lock order) is fixed: the lower UUID is locked first, then the higher, and the sender status is re-checked after the lock. | `TransactionService.java:217-221` (ordering), `:223-228` (locks), `:238-244` (post-lock status checks). | VERIFIED-OK |
| F9 | Existing lock-order test is Mockito-based. It verifies call order on mocks, not database lock behaviour. The existing concurrency test runs on H2 with `MODE=PostgreSQL`. Neither is PostgreSQL evidence. | `TransactionServiceTransferLockOrderTest.java:32` (`MockitoExtension`), `:53-89` (`runDirection`), `:91-104`. `ConcurrentTransferIntegrationTest.java:34-35` (`@SpringBootTest`); `application-test.yml:4` (H2 URL). | CONFIRMED (gap) |
| F10 | Pre-lock sender read uses `findByUserId` and a status check before the lock. The post-lock re-check is the one that counts. The pre-lock read also throws on duplicate wallets (F4). | `TransactionService.java:195-200`; post-lock `:238`. | CONFIRMED (partly relevant) |
| F11 | Non-negative balance is enforced only by a conditional UPDATE. No migration has a balance `CHECK`. The only CHECK in the migration set is `attempt_count >= 0`. | `WalletRepository.java:56` (`AND balance + :amount >= 0`). `V15:27` (the only CHECK). | CONFIRMED (NEW-DB-01 at app layer) |
| F12 | FIN-09: the percentage is rounded to 2 dp before the flat fee is added and before clamping. Existing test only covers 0.3333% of 100.00. | `FeeEngineService.java:54-55` (`divide(...,2,HALF_EVEN)`), `:56` (`add(flatFee)`), `:59-64` (clamp), `:66` (`setScale(2)`). `FeeEngineServiceTest.java:182-200`. Hand-computed counter-example: amount 10.00, percentage 0.05, flat 0.01. Current result 0.01; single rounding gives 0.02. **To be confirmed by the RED test.** | CONFIRMED |
| F13 | FIN-11: KYC-unapproved users skip the daily cap and the tier wallet cap. Velocity caps still apply to everyone. Whether a KYC-unapproved user can hold a wallet depends on the expiry path (F17). | `TransactionLimitService.java:110-112` (early return when `kycStatus != APPROVED` or `tier < 1`). Velocity: `:73-98` (count `:76`, sum `:87`). | CONFIRMED |
| F14 | Velocity and daily caps are check-then-act with no serialisation across wallets. The velocity check is per user across all wallets; the daily check is per wallet. Both are safe only while each user has exactly one wallet (F4). | `TransactionLimitService.java:76,87` (per user), `:117-118` (per wallet). | NEW (proposed NEW-FIN-05) |
| F15 | Recalc backfill SQL precedence bug. The `WHERE` clause is `A OR B AND C AND D`, so the payout exclusion (`C`, `D`) applies only to the `B` branch. Payout rows with a NULL `balance_before` are still rewritten. The Javadoc says payouts are excluded. | `TransactionRepository.java:143-146` (Javadoc), `:159-160` (WHERE). Job is contained: `BalanceRecalculationJob.java:17-21` (`matchIfMissing=false`); `application-prod.yml:148-149` (`enabled: false`). | NEW (proposed NEW-FIN-02); latent |
| F16 | R-02 containment is tested only under the `prod` profile. Absence under `dev`, `test`, and no profile is untested. | `BalanceRecalculationJobDisabledInProdTest.java:30` (`@ActiveProfiles("prod")`). `application.yml:5-6` sets `profiles.active: dev` (SEC-04, Batch 8). | CONFIRMED (test gap) |
| F17 | KYC expiry may leave a wallet live while KYC is not APPROVED. `KycExpiryJob` sets approved users older than one year to `NOT_SUBMITTED`. Whether it touches the wallet was not read beyond these lines. | `KycExpiryJob.java:31-40`. Wallet creation only on approval: `AdminKycService.java:138-145`. | UNVERIFIED (read `KycExpiryJob` in full at step S1) |
| F18 | Tier 0 cannot be approved (only tier 1 is visible in config). `getTier` throws for unknown tiers, which `approveKyc` maps to 400. | `KycConfig.java:25-29`. `application.yml:123-126` (tier 1 "Basic" visible; full list to confirm). `AdminKycService.java:114-118`. | VERIFIED-OK (partial) |
| F19 | Transfer and withdraw have no service-level idempotency. The controller Javadoc claims "all financial endpoints accept an Idempotency-Key". The header is optional at the filter. | `TransactionController.java:37` (Javadoc), `:84-89` (withdraw, no header), `:101-105` (transfer, no header). `TransactionService.java:110` and `:193` (no key parameter). `IdempotencyFilter.java:109-113` (no header means pass through). | NEW (proposed NEW-FIN-04) |
| F20 | Filter replay ignores endpoint and body. A key reused on another endpoint or with a different body returns the first cached response. | `IdempotencyFilter.java:130` (`scopedKey(userId, rawKey)`, no endpoint, no body). `IdempotencyService.java:70-78` (`method`, `endpoint`, `userId` stored; no body hash). | NEW (proposed NEW-FIN-03) |
| F21 | Crash window between commit and cache write. The business transaction commits inside `chain.doFilter`; the filter writes the cache afterwards. A crash in between leaves a `PROCESSING` row; retries get 409 until TTL. No double debit, but a stuck key. | `IdempotencyFilter.java:144-157` (`chain.doFilter`, then `complete` at `:157`). `IdempotencyService.java:77` (`expiresAt`, TTL default 24 h per `@Value` at `:36`). | NEW (proposed NEW-FIN-06, low) |
| F22 | PIN verification and velocity queries run while the wallet row lock is held. | `TransactionService.java:111` (lock), `:118` (velocity), `:120` (PIN); transfer `:223-226` (lock), `:251` (velocity), `:253` (PIN). | NEW (proposed NEW-FIN-07, low) |
| F23 | Payout idempotency is the precedent to follow: scoped `userId:key`, stored on `transactions.idempotency_key`, unique, checked after the wallet lock. | `PayoutService.java:242` (scoped key), `:257` (lookup), `:178` (catch of `DataIntegrityViolationException`). `V14:11` (column), `V14:18` (unique index). | VERIFIED-OK (precedent) |
| F24 | Test coverage gaps for the Batch 3 targets. No test references `toggleUserDeletedStatus` or `enforceDailySendLimit`. `approveKyc` appears only in `InputBoundsIntegrationTest`. `AccountNumberGenerator` appears only as a test bean. No PostgreSQL-backed test exists. | Targeted grep of `src/test/java`. | CONFIRMED |
| F25 | `pgtest` profile is absent from the code. The `grep` of `novawallet-api/src` returns nothing; mentions are in docs only. | Targeted grep of `src/main` and `src/test`. | CONFIRMED |
| F26 | Register and canonical mapping. R-01, R-02, R-03 are defined in register §5.1 (lines 689-699), and the definitions match the 1C log §7 and the matrix §2. NEW-FIN-01 and DB-29 are **not** in the canonical register; they appear only in Batch 0 report, the implementation plan, and the security baseline. | Register lines 689-699; grep of `novawallet-api/docs` and `docs/`. | CONFIRMED (register gap) |

### 1.3 Existing controls that must not regress

| # | Control | Owner test |
|---|---|---|
| P1 | Payout three-stage flow and `PENDING_RECONCILIATION` | `PayoutStageCTransactionBoundaryTest`, `PayoutIdempotencyTest`, `PayoutCompensatingCreditTest`, `PayoutReconciliationServiceTest` |
| P2 | Wallet freeze and unfreeze lock (`WalletService.java:74`, `:89`) | none directly; add in Batch 3 |
| P3 | Transfer ordering and post-lock status re-check (F8) | `TransactionServiceTransferLockOrderTest` (mock-level), `TransferRollbackIntegrationTest` |
| P4 | Email-verification guard (DEC-01) | `EmailVerificationPolicyTest` |

---

## 2. Exact Files to Change, and Why

Only paths under `novawallet-api/` are listed, plus the docs named by the prompt (see §8 for the path question).

### 2.1 Main code

| File | Change | Why (finding) | Decision needed |
|---|---|---|---|
| `admin/service/AdminService.java` | `toggleUserDeletedStatus` (`:76-116`): lock user row first (native `FOR UPDATE`), then lock wallet by user (new lock query, not `findByUserId`). Restore keeps `FROZEN` when the reason is a fraud or admin freeze; restores only the deactivation freeze. Deactivation sets a dedicated reason. Audit records the real prior and resulting state. Revocation (`:112-115`) unchanged. | F1, F2, F3, DEC-12 | D-2 (reason value), D-1 for pgtest tests |
| `user/repository/UserRepository.java` | Add native `SELECT * FROM users WHERE id = :id FOR UPDATE` (bypasses `@SQLRestriction`). | F3 | none |
| `wallet/repository/WalletRepository.java` | Add `existsByUserId`; add a list-returning `findAllByUserId` for detection (duplicates); add a lock query by user id that does not throw on duplicates and asserts single-row in code. | F4, F5, F10 | none |
| `wallet/entity/FreezeReason.java` | Add one value for deactivation freezes (name to confirm). Column is `VARCHAR(50)` (`V3:8`), so no migration. | F2, DEC-12 | D-2 |
| `wallet/entity/Wallet.java` | Javadoc only: correct `:16-19` claims (no DB-level one-wallet or non-negative guarantee). | F4, F11 | none |
| `wallet/service/WalletService.java` | Only if the lock query is exposed here; otherwise unchanged. | F1 | none |
| `kyc/service/AdminKycService.java` | `approveKyc` (`:105-154`): lock user row (`UserRepository` lock query) before the `PENDING` check; `existsByUserId` guard before wallet creation; a losing concurrent request maps to 409, not 500. Idempotent: if a wallet already exists for an approved user, do not create a second. | F5, NEW-FIN-01 | none |
| `exception/GlobalExceptionHandler.java` | Read `:158-170` first. Add explicit mapping for optimistic-lock and integrity conflicts to 409 only if the RED test proves the status is wrong. Do not map everything. | F5, F6 | none |
| `wallet/service/AccountNumberGenerator.java` | Remove reliance on the advisory check for correctness. Keep the check as a hint only. Move collision handling to the insert boundary using a savepoint (or equivalent). Make the digit source injectable for tests. Exhaustion throws a typed `ServiceUnavailableException` (503) instead of `IllegalStateException`. Stray `</p>` at `:12` removed. | F6, F7, R-03 | none |
| `kyc/service/AdminKycService.java` (wallet creation block) | Insert through the generator's savepoint-protected path; no other change. | F6, F7 | none |
| `transaction/service/TransactionService.java` | (a) `transfer` and `withdraw` accept the scoped idempotency key (DEC-05), stored on the debit row only, checked after the wallet lock (mirrors `PayoutService.java:233-257`). (b) Replay returns the stored result. (c) Optional fingerprint mismatch check if D-4 is approved. Lock and status logic (`:217-244`) unchanged. | F19, F23, DEC-05, NEW-FIN-04 | D-4 (required header, fingerprint) |
| `transaction/controller/TransactionController.java` | `transfer` (`:101-108`) and `withdraw` (`:84-92`): read the `Idempotency-Key` header and pass it down. Correct the Javadoc at `:37` to state what is actually enforced. | F19 | D-4 |
| `idempotency/filter/IdempotencyFilter.java` | Scope the stored key to include the endpoint (`:130`), so one raw key cannot replay another endpoint's response. Fingerprint check if D-4 is approved. | F20, NEW-FIN-03 | D-4 |
| `idempotency/service/IdempotencyService.java` | `scopedKey` gains an endpoint component; `tryAcquire` (`:68-86`) stores a request fingerprint if D-4 is approved. Existing `PROCESSING` rows are rejected by TTL, not migrated. | F20, F21 | D-4 |
| `transaction/repository/TransactionRepository.java` | `recalculateMissingBalances` (`:148-162`): parenthesise the `WHERE` so payout exclusion applies to the whole predicate. Javadoc (`:143-146`) stays true. Job remains contained. | F15, NEW-FIN-02 | none (small fix, owner may defer) |
| `fee/service/FeeEngineService.java` | `calculateFee` (`:43-67`): compute the percentage at full precision, add the flat fee, clamp, then `setScale(2, HALF_EVEN)` once. | F12, FIN-09 | none |
| `transaction/service/TransactionLimitService.java` | FIN-11 policy (`:105-127`). **Not implementable until D-3 is answered.** | F13, FIN-11 | **D-3 BLOCKING** |
| `notification/schedule/BalanceRecalculationJob.java` | **No code change.** Containment stays. Tests added (§5). | F16, R-02 | none |

### 2.2 Read-only (must not change unless a regression is found)

- `payment/service/PayoutService.java`, `payment/service/PayoutReconciliationService.java`, `payment/schedule/*`, all `Payout*Test` classes, `DisbursementWebhookServiceTest`.
- `security/EmailVerificationPolicy.java` (DEC-01).

### 2.3 Tests

**New test resources (requires D-1):**
- `novawallet-api/src/test/resources/application-pgtest.yml`: reads `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`; no secrets committed; Flyway on; `ddl-auto: validate`.

**New test classes** (`src/test/java/com/novawallet/novawallet_api/...`). Every PostgreSQL test is tagged `@Tag("postgres")` and run only under the `pgtest` profile.

| Class | Type | Proves |
|---|---|---|
| `wallet/ConcurrentTransferPostgresTest` | PostgreSQL, real threads | N parallel transfers from one wallet: no overdraft, total conserved, exactly the expected number succeed |
| `wallet/OppositeDirectionTransferPostgresTest` | PostgreSQL, real threads | A→B and B→A concurrently: no SQLState 40P01 deadlock, totals conserved |
| `wallet/WalletStatusRacePostgresTest` | PostgreSQL, real threads | Deactivation vs transfer: no debit with `createdAt` after the committed freeze |
| `admin/UserRestoreFreezePreservedTest` | H2 integration | Restore keeps `FROZEN` for a fraud freeze; restores only the deactivation freeze |
| `admin/AdminToggleLockOrderPostgresTest` | PostgreSQL, real threads | Concurrent toggle and transfer: no deadlock; the lock order matches the documented rule |
| `kyc/ConcurrentKycApprovalPostgresTest` | PostgreSQL, real threads | N concurrent approvals: exactly one APPROVED, exactly one wallet, all losers 400 or 409, none 500 |
| `wallet/AccountNumberConcurrencyPostgresTest` | PostgreSQL, real threads | N concurrent creations: zero duplicate account numbers; forced collision retried; exhaustion gives 503 |
| `wallet/AccountNumberCollisionTest` | H2 unit | Injected digit source: collision retried; exhaustion typed 503 |
| `fee/FeeRoundingTest` | H2 unit (pure) | Single rounding at the 10.00 / 0.05 / 0.01 edge; clamp and flat-only edges |
| `transaction/IdempotentTransferReplayPostgresTest` | PostgreSQL, real threads | N concurrent POSTs with one key: exactly one debit; identical body replayed |
| `transaction/IdempotencyScopeTest` | H2 integration | Same raw key, two users: both succeed; same key on another endpoint is not replayed |
| `transaction/IdempotencyFingerprintTest` | H2 integration | Only if D-4 approved: same key, different body gives 422 |
| `transaction/RecalcBackfillPrecedenceTest` | H2 integration (and PostgreSQL when available) | Payout row with NULL snapshot is not rewritten |
| `notification/schedule/BalanceRecalcContainmentTest` | Context test, parameterised over `prod`, `dev`, `test`, no profile | Bean absent in every profile |
| `transaction/UnverifiedLimitPolicyTest` | H2 unit | Only if D-3 approved |

**Existing tests that must stay green:** `ConcurrentTransferIntegrationTest`, `TransactionServiceTransferLockOrderTest`, `TransferRollbackIntegrationTest`, `BalanceRecalculationJobDisabledInProdTest`, `FeeEngineServiceTest`, `EmailVerificationPolicyTest`, all `Payout*` tests.

---

## 3. Data and Migration Changes

**Default for Batch 3: no migration.** Every fix above fits existing objects:

| Need | Existing object | Migration? |
|---|---|---|
| Freeze reason | `wallets.freeze_reason VARCHAR(50)` (`V3:8`); new Java enum value | No |
| Transfer and withdraw idempotency | `transactions.idempotency_key` with unique index (`V14:11`, `:18`) | No |
| Lock target | `wallets` and `users` rows with `@Version` | No |
| Duplicate-safe account numbers | `wallets.account_number` unique (`V3:15`) | No |

**Optional, only if D-4 approves a request fingerprint:** `V17__idempotency_request_fingerprint.sql` adds a nullable `request_fingerprint VARCHAR(64)` to `idempotency_keys`. This is additive and needs no pre-flight. It would take V17 from Batch 4, so Batch 4 would move to V18 onward. Nothing can be renumbered after it is applied anywhere, so the choice must be made before the first Batch 3 migration is run.

**Expand, migrate, contract:** Not applicable to Batch 3 code. New idempotency behaviour writes the key on new rows only; existing rows keep `NULL` (non-conflicting, per `V14:18`). Old `PROCESSING` rows expire by TTL.

**Pre-flight for Batch 4 (not run in Batch 3):** `SELECT user_id, COUNT(*) FROM wallets GROUP BY user_id HAVING COUNT(*) > 1` and `SELECT COUNT(*) FROM wallets WHERE balance < 0`. Batch 3 adds a read-only detection query and a report. If duplicates exist, stop and ask the owner (D-5). Do not merge or freeze automatically.

---

## 4. API Changes

| Endpoint (controller) | Change | Contract |
|---|---|---|
| `POST /api/v1/transfers` (`TransactionController.java:101-108`) | Reads `Idempotency-Key`. | Recommended under D-4: header required, missing gives 400 `IDEMPOTENCY_KEY_REQUIRED`. Replay of a completed key returns the original 201 body. Key in flight returns 409 `IDEMPOTENCY_IN_FLIGHT` (existing). Same key with different body gives 422 `IDEMPOTENCY_KEY_MISMATCH` (only if fingerprint approved). |
| `POST /api/v1/wallets/{walletId}/withdraw` (`:84-92`) | Same as transfer. | Same as transfer. |
| Admin user restore or deactivate (`AdminUserController`; route not re-read in this pass, confirm at S4) | Freeze behaviour per DEC-12. | Response body unchanged. Restore of a fraud-frozen wallet keeps `FROZEN` and writes an audit row. |
| Admin KYC approve (`AdminKycController`) | Concurrent losing request. | 400 when the status is already not `PENDING` (existing). 409 `CONFLICT` for a lost optimistic race. Never 500. |
| Wallet creation exhaustion | Typed failure. | 503 `SERVICE_UNAVAILABLE` (existing `ServiceUnavailableException` handler, `GlobalExceptionHandler.java:77-82`). |

Any change above requires an update to `reports/API_ENDPOINT_INVENTORY.md` and the OpenAPI annotations on the affected endpoints (AGENTS rule 7).

---

## 5. Risks and How Each Is Tested

| Risk | ID | Test (see §2.3) | Expected result |
|---|---|---|---|
| Admin restore clears a fraud freeze | R-01, DEC-12 | `UserRestoreFreezePreservedTest` | Stays `FROZEN`; reason preserved; audit row written |
| Admin toggle races a transfer | R-01 | `AdminToggleLockOrderPostgresTest`, `WalletStatusRacePostgresTest` | No debit after the committed freeze; no deadlock |
| Concurrent approvals create two wallets, or fail as 500 | NEW-FIN-01 | `ConcurrentKycApprovalPostgresTest` | Exactly one wallet; losers 400 or 409; zero 500 |
| Account-number collision surfaces as 500 | R-03 | `AccountNumberCollisionTest`, `AccountNumberConcurrencyPostgresTest` | Retried at the insert; exhaustion is 503; zero duplicates |
| Concurrent transfers overdraw | FIN-06 follow-on | `ConcurrentTransferPostgresTest` | Balance never negative; total conserved |
| Opposite-direction deadlock | FIN-06 | `OppositeDirectionTransferPostgresTest` | No 40P01; totals conserved |
| Double rounding of fees | FIN-09 | `FeeRoundingTest` | 10.00 / 0.05 / 0.01 gives 0.02 (to be confirmed by RED) |
| Unverified users exceed tier caps | FIN-11 | `UnverifiedLimitPolicyTest` (after D-3) | Reduced cap enforced |
| Duplicate financial request | DEC-05 | `IdempotentTransferReplayPostgresTest` | One debit; identical replayed body |
| Same key replays another endpoint | NEW-FIN-03 | `IdempotencyScopeTest` | Not replayed |
| Key reused with different body | NEW-FIN-03 | `IdempotencyFingerprintTest` (if D-4) | 422 |
| Backfill rewrites payout snapshot | NEW-FIN-02 | `RecalcBackfillPrecedenceTest` | Payout row unchanged |
| Recalc bean enabled in a non-prod profile | R-02 | `BalanceRecalcContainmentTest` | Absent in all four profiles |
| Stuck `PROCESSING` after crash | NEW-FIN-06 | Documented, not fixed in Batch 3 | Known: 409 until TTL; no double debit |
| Lock hold during PIN and velocity | NEW-FIN-07 | Documented; measured in PG test timing | Recorded; not fixed |

**Negative and authorisation tests** (AGENTS rule): every new endpoint behaviour needs owner, other-user, unauthenticated, and wrong-role cases. The admin toggle needs a non-admin attempt (403).

**Environment record:** PostgreSQL 16 via disposable container (`postgres:16`), exact version string recorded from `SELECT version()`. Java source level 17; runtime is JDK 21 (BLK-04). No Java 17 runtime claim.

---

## 6. Decisions

Status from `docs/DECISIONS.md` as read on 2026-11-14.

| ID | Status | Question | Recommendation | Blocks |
|---|---|---|---|---|
| **DEC-11** | **OPEN, BLOCKING for PostgreSQL proof** | Test infrastructure: `pgtest` profile or Testcontainers | **`pgtest`**: no new dependency; the profile must be created (F25). Owner confirms. | All `@Tag("postgres")` tests (S1) |
| **D-3 (new; proposed DEC-35)** | **BLOCKING for FIN-11 only** | Unverified (KYC-not-APPROVED) users: what limit replaces the current exemption? Options: (A) reduced daily-send cap for KYC-unapproved users, amount set by owner; (B) KYC gate before any wallet movement beyond velocity caps. | **A**, with the amount owner-set (not invented here). Links to DEC-15 (fees and limits, OPEN). Until the amount exists the code change is BLOCKED. | FIN-11 implementation only (S10) |
| **DEC-04** | OPEN | Confirm R-01/R-02/R-03 mapping | Confirm. Register §5.1 and 1C log §7 agree. Blocks sign-off, not code. | Batch 3 sign-off |
| **DEC-05** | DECIDED | Idempotency-key scope: user and key per operation | Implement scope `<userId>:<operation>:<rawKey>` on `transactions.idempotency_key`; filter scope gets endpoint too (F20). The exact encoding is an implementation detail under DEC-05; confirm at review. Not blocking. | — |
| **DEC-12** | DECIDED | Restore keeps freeze plus reason; explicit unfreeze required | Implement as written. Recommend a distinct deactivation reason (D-2) so restore reverses only its own freeze. Explicit unfreeze uses existing `WalletService.unfreezeWallet`. | — |
| **D-2 (new)** | Needs owner confirmation | Name and existence of a deactivation freeze reason in `FreezeReason` | Add one value (`USER_DEACTIVATED` or owner's choice). No migration (VARCHAR(50)). | S4 |
| **D-4 (new; proposed DEC-36)** | Needs owner | Transfer and withdraw: header required? Request fingerprint on replay? | Required: yes (AGENTS says money-moving POSTs "accept" the key; required gives real protection; pre-production so the contract change is acceptable per DEC-10 precedent). Fingerprint: yes. Fingerprint needs V17 (§3). | S7 |
| **D-5 (new)** | Only if pre-flight finds duplicates | Existing duplicate wallets: merge, freeze, or leave | Owner decides. Batch 3 does not repair financial data. | Batch 4 |
| **D-6 (new)** | Clarify | Location of `financial-invariants.md` | `novawallet-api/docs/architecture/financial-invariants.md` (scope rule, AGENTS rule 2). The prompt's `docs/architecture/` is ambiguous between the root `docs/` and `novawallet-api/docs/`. | S11 |

**Note on "unverified":** FIN-11 concerns KYC status (`TransactionLimitService.java:110`). It is a different concept from email verification (DEC-01), which already blocks money movement for unverified emails (`EmailVerificationPolicy`). The plan keeps the two apart.

---

## 7. Steps in Order

Each step ends with targeted tests. The RED tests come before any fix (BE-1C-3 rule 1).

- **S0 — Decisions.** Owner answers DEC-11, D-2, D-3 (amount or option), D-4. Until then only S1-S2 and S3 may proceed.
- **S1 — Baseline and infrastructure.** Read `KycExpiryJob.java` in full (F17) and `GlobalExceptionHandler.java:158-170` (F5). Run `./mvnw -B clean test` and record counts before any change. Create `application-pgtest.yml` (D-1). Start disposable `postgres:16`, record `SELECT version()`.
- **S2 — RED tests.** Write the failing tests in §2.3 for R-01, NEW-FIN-01, R-03, FIN-09, the concurrent transfer and opposite-direction cases, idempotency replay and scope, recalc precedence, and containment. Record which fail and why. Any test that passes before the fix means the finding is overstated: record it and do not implement that fix until the owner agrees.
- **S3 — FIN-09.** Change `FeeEngineService.calculateFee` to a single rounding. Run `FeeRoundingTest`, `FeeEngineServiceTest`. Green.
- **S4 — R-01 and DEC-12.** Add the native user-row lock and the wallet lock query (`UserRepository`, `WalletRepository`). Add the deactivation freeze reason (D-2). Rewrite `toggleUserDeletedStatus` with the documented order (user row, then wallet). Restore only reverses the deactivation freeze; a fraud freeze stays. Audit the real prior state. Green: `UserRestoreFreezePreservedTest`, `AdminToggleLockOrderPostgresTest`, `WalletStatusRacePostgresTest`. Existing revocation tests still green.
- **S5 — NEW-FIN-01.** `approveKyc`: lock the user row, check `PENDING`, guard with `existsByUserId`, create wallet only if absent. Map a lost race to 409 if S2 proves a 500 today. Green: `ConcurrentKycApprovalPostgresTest`.
- **S6 — R-03.** Make the digit source injectable. Move collision handling to a savepoint at the insert. Typed 503 on exhaustion. Remove the advisory check's correctness role. Green: `AccountNumberCollisionTest`, `AccountNumberConcurrencyPostgresTest`. Confirm the savepoint works under `JpaTransactionManager` by test before relying on it.
- **S7 — DEC-05 idempotency.** Scope the key by endpoint in `IdempotencyFilter` and `IdempotencyService`. Add the key to `transfer` and `withdraw` (debit row only). Required header and fingerprint only if D-4 approved (then V17). Green: `IdempotentTransferReplayPostgresTest`, `IdempotencyScopeTest`, `IdempotencyFingerprintTest`. Existing idempotency and payout idempotency tests still green.
- **S8 — Transfer concurrency proof.** Add `ConcurrentTransferPostgresTest` and `OppositeDirectionTransferPostgresTest` on PostgreSQL. Change no transfer lock logic unless a test shows a regression. Record the result against F8.
- **S9 — Recalc.** Parenthesise the WHERE in `TransactionRepository.recalculateMissingBalances`. Add `RecalcBackfillPrecedenceTest` and `BalanceRecalcContainmentTest`. Job stays off.
- **S10 — FIN-11.** **Only after D-3.** Implement the chosen policy in `TransactionLimitService`. Add `UnverifiedLimitPolicyTest`. Otherwise record as BLOCKED.
- **S11 — Documentation.** Create `financial-invariants.md` (path per D-6): balance ≥ 0, sum of holds ≤ balance (holds are not implemented; state so), one wallet per user (app-level until Batch 4), one financial effect per idempotency key per operation. Correct Javadoc at `Wallet.java:16-19` and `WalletRepository.java:33-35`. Tick Batch 3 stages in `TODO.md`.
- **S12 — Gate.** `git status --short`, `git diff --check`, `git diff --stat`, targeted and full test counts, PG16 run record, traceability matrix updated only with executable evidence, canonical register updated (`novawallet-api/docs/architecture/backend-risk-register.md`) including NEW-FIN-01 wording correction and the new proposed IDs, `API_ENDPOINT_INVENTORY.md` if §4 changed, then the batch report. Stop. Do not commit.

---

## 8. Notes, Conflicts, and Out-of-Scope Risks

- **Report naming conflict.** `_COMMON_BATCH_RULES.md` asks for `novawallet-api/docs/phase-1c/PHASE_1C_BATCH_3_REPORT.md`. `novawallet-api/AGENTS.md` asks for `reports/<PROMPT-ID>_REPORT.md`. AGENTS says the stricter rule wins and the conflict must be reported. Recommend writing the phase-1c path and noting the conflict.
- **Idempotency documentation drift.** `TransactionController.java:37` says all financial endpoints accept the key; the code does not enforce it. Fix the Javadoc in S7 regardless of D-4.
- **Out of Batch 3, recorded only:** `application.yml:5-6` `profiles.active: dev` (SEC-04, Batch 8); `TransactionLimitService` velocity and daily caps are check-then-act across wallets (NEW-FIN-05, depends on F4); `IdempotencyFilter` stuck `PROCESSING` after crash (NEW-FIN-06); PIN and velocity under row lock (NEW-FIN-07). Prompt: `LAUNCH` and Phase 2 ledger not touched.
- **Proposed new IDs** (not yet in any register; to be assigned in the canonical register at S12): NEW-FIN-02 (recalc precedence), NEW-FIN-03 (filter replay scope and body), NEW-FIN-04 (no service idempotency on transfer or withdraw), NEW-FIN-05 (cross-wallet caps), NEW-FIN-06 (stuck key after crash, low), NEW-FIN-07 (lock hold during PIN, low). NEW-FIN-01 wording is corrected per F5 and must not be repeated as "creates duplicate wallets" until a test proves it.
- **Do not touch:** `novawallet-app/`, `novawallet-admin/`, root `docs/` (read-only unless the prompt says update), Phase 1A/1B payout behaviour, V1-V16.
- **Not run in this planning pass:** Maven, Docker, PostgreSQL, git writes. Every "expected" result above must be confirmed by the RED tests at S2.
