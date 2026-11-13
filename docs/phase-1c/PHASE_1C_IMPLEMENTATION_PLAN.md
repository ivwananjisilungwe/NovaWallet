# NovaWallet — Phase 1C Implementation Plan

> **Location:** version-controlled copy — `novawallet-api/docs/phase-1c/`. Do not edit findings in this copy; supersede with a later batch report.`

**Document:** PHASE_1C_IMPLEMENTATION_PLAN.md
**Phase:** 1C — Security and Database Hardening (Batch 0 deliverable)
**Branch:** `feature/phase-1c-security-database-hardening`
**Starting HEAD:** `797571914b98d3b3206c5529e8eaf356ba79d1c8`
**Date:** 2026-11-12
**Author:** Forge (primary engineering orchestrator)
**Status:** Batch 0 complete — **awaiting human approval. No Batch 1 work has begun.**

Companion documents:
- `docs/phase-1c/PHASE_1C_SECURITY_BASELINE.md` — architecture, threat model, verified weaknesses
- `docs/phase-1c/PHASE_1C_RISK_TRACEABILITY_MATRIX.md` — verbatim register entries, re-verification log, status

---

## 0. Execution Rules For This Plan

1. **One batch at a time.** Every batch ends with a report and a hard stop for human review.
2. **No commits, pushes, merges, or deploys** without explicit authorization.
3. **No historical migration is rewritten.** All schema change is additive (`V16+`).
4. **No new major dependency.** In particular: no Redis, no Kafka, no RabbitMQ, no microservices, no Spring Boot upgrade.
5. **Java 17 source compatibility preserved.** Local runtime is JDK 21; **no Java 17 runtime exists on this machine**, so no Java 17 runtime claim will be made.
6. **H2 is not accepted as proof** of PostgreSQL locking, constraint, or isolation behaviour. Batches 3, 4, and 9 require a disposable PostgreSQL 16 instance.
7. **Financial source of truth unchanged.** Wallet balance remains `wallets.balance`; the double-entry ledger is Phase 2.
8. **Phase 1A/1B safeguards are regression-locked.** Any batch that breaks one is a failed batch regardless of what else it fixed.

---

## 1. Current Architecture

Modular monolith. Spring Boot 3.5.3, Java 17 source level, Maven, PostgreSQL 16, Flyway, Spring Data JPA, Spring Security, stateless JWT, Lipila provider abstraction.

17 packages under `com.novawallet.novawallet_api`: `admin`, `audit`, `auth`, `bootstrap`, `common`, `config`, `exception`, `fee`, `filter`, `idempotency`, `kyc`, `notification`, `payment`, `security`, `token`, `transaction`, `user`, `wallet`.

13 entities / tables. Migrations `V1`–`V15`, of which **`V12` is a Java-based migration** at `src/main/java/db/migration/V12__CreateTransactionReferenceSequence.java` (not SQL, not in `db/migration`).

Full detail: `PHASE_1C_SECURITY_BASELINE.md` §2.

### 1.1 Verified schema facts that constrain this plan

| Fact | Evidence |
|---|---|
| `wallets.balance` is `DECIMAL(15,2) NOT NULL DEFAULT 0.00` | `V3__create_wallets_table.sql` |
| **No `CHECK (balance >= 0)`** — non-negativity enforced only by the conditional `UPDATE` in `WalletRepository.updateBalance` | `V3`; `WalletRepository.java` |
| `wallets.account_number` uniqueness **is** DB-enforced (`idx_wallets_account_number` UNIQUE) | `V3` |
| **`wallets.user_id` has NO unique constraint**, but the code assumes one wallet per user (`Optional<Wallet> findByUserId`) | `V3`; `WalletRepository.java:23` |
| `transactions.reference` uniqueness **is** DB-enforced (`uq_transactions_reference`) | `V6` |
| `transactions.status` widened to `VARCHAR(30)` by `V13`; **no CHECK** on allowed values | `V13` |
| `idempotency_keys.idempotency_key` is `VARCHAR(255) NOT NULL` with `uq_idempotency_key` UNIQUE | `V9` |
| `transactions.idempotency_key` is nullable with a **UNIQUE** index from `V14` — multiple NULLs intended | `V14` |
| All FKs omit `ON DELETE`, so PostgreSQL default `RESTRICT` applies | `V3`, `V6` |
| Money is `DECIMAL`, **not** integer ngwee | `V3`, `V6` |

---

## 2. Security Baseline (Summary)

243 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS on JDK 21.0.12.1 Temurin / Maven 3.9.16, `./mvnw -B clean test`, 1:28. Log: `/tmp/opencode/phase1c-baseline-test.log`.

Existing controls that work and must not regress: BCrypt password + PIN hashing; SHA-256-hashed refresh tokens with rotation; revocation on password reset and logout; uniform login errors; anti-enumeration on forgot-password; login and PIN lockout; **enforced** in-memory rate limiting (100 req/min global, 10 auth, 429); stateless session; security headers; `@EnableMethodSecurity`; admin role gates; disbursement webhook HMAC + timestamp window + durable dedup + amount match + provider re-query outside transaction + quarantine; collections credit path contained by a 503 guard; KYC `loadFile` traversal guard; uploads not statically served; no committed secrets; non-root Docker; safe exception handler; fail-closed schedulers.

Full detail: `PHASE_1C_SECURITY_BASELINE.md` §3.

---

## 3. Threat Model (Summary)

STRIDE model T1–T24 in `PHASE_1C_SECURITY_BASELINE.md` §4. The four scenarios that drive sequencing:

| Priority | Scenario | Why it ranks here |
|---|---|---|
| 1 | **Concurrency or replay creates/destroys money** (T9, T10, T22, T24, plus newly found T25) | Irreversible financial harm; hardest to detect after the fact |
| 2 | **Credential exposure via product surfaces** (T15) and **unrevoked sessions** (T4) | Direct account takeover |
| 3 | **Financial invariants rest on application code, not the DB** (T9, T10, T24) | A single code path bypass silently corrupts balances |
| 4 | **Collections credit path without HMAC** (T11) | Contained today; catastrophic if uncontained without the provider contract |

---

## 4. Existing Protections — Regression-Locked

The following are **not** to be modified except where this plan explicitly says so. Each has an owning test that must stay green.

| # | Protection | Owner test |
|---|---|---|
| P1 | Three-stage payout: debit → provider call outside tx → finalise | `PayoutService*Test`, `DisbursementWebhookServiceTest` |
| P2 | Compensating credit on definite payout failure (never double-credits) | `PayoutCompensatingCreditTest` |
| P3 | `PENDING_RECONCILIATION` replay guard | payout tests |
| P4 | Disbursement HMAC + 300 s window + 120 s future bound | `DisbursementWebhookServiceTest` |
| P5 | Durable `(provider, event_id)` dedup | `DisbursementWebhookServiceTest` |
| P6 | Collections 503 containment guard | (no test today — **Batch 5 must add one**) |
| P7 | `BalanceRecalculationJob` containment (`matchIfMissing=false`) | `BalanceRecalculationJobDisabledInProdTest` |
| P8 | Transfer pessimistic locking + conditional non-negative `UPDATE` | transfer concurrency tests |
| P9 | Idempotency filter + `uq_idempotency_key` | idempotency tests |
| P10 | Refresh-token rotation and revocation | `RefreshTokenRotationIntegrationTest` |
| P11 | KYC path-traversal guard | (no direct test — Batch 6 adds one) |
| P12 | Fail-closed schedulers in prod | `BalanceRecalculationJobDisabledInProdTest` |

---

## 5. Identified Weaknesses (Consolidated)

Register IDs plus **new findings discovered during Batch 0** (marked `NEW` — these are not register IDs and must be added to the register in the owning batch).

| ID | Weakness | Sev | Batch |
|---|---|---|---|
| SEC-01 | `JwtAuthFilter` never checks `isEnabled()` (= `emailVerified`). Soft-delete already mitigated by `@SQLRestriction`; no locked/disabled column exists | M | 1 |
| SEC-02 | Password change and admin deactivation do not revoke refresh tokens | H | 1 |
| SEC-03 | Raw verification/reset tokens in notification bodies, exposed via `/api/v1/notifications`, logged by `MailService` | H | 1 (+8) |
| SEC-04 | `spring.profiles.active: dev` hardcoded default | H | 8 |
| SEC-05 | Collections webhook lacks HMAC/timestamp/dedup/amount verification; contained by 503 | H | 5 |
| SEC-06 | No logout endpoint | M | 1 |
| SEC-08 | No key-length/entropy validation, no `iss`/`aud`, algorithm not explicitly pinned | M | 1 |
| SEC-09 | In-memory rate limiting, `getRemoteAddr()`, no proxy awareness | M | 7 |
| SEC-10 | No `@Digits`/`@DecimalMax` on monetary DTOs | M | 2 |
| SEC-11 | Public `/actuator/info`, Swagger in all profiles, `allowedHeaders("*")` + credentials | M | 8 |
| SEC-12 | Unvalidated `X-Request-ID` → MDC; client MIME trusted without magic bytes | L/M | 8 / 6 |
| FIN-09 | Fee percentage rounded to 2dp before flat-fee addition and clamping | M | 3 |
| FIN-11 | Unverified users exempt from daily-send and wallet limits | L | 3 |
| R-01 | `AdminService.toggleUserDeletedStatus` mutates wallet status with no lock and no status precondition | H | 3 |
| R-02 | `BalanceRecalculationJob` rewrites historical snapshots — contained, not corrected | M | 3 |
| R-03 | `AccountNumberGenerator` TOCTOU; collision surfaces as unhandled 500 | H | 3 |
| **NEW-FIN-01** | **KYC approval TOCTOU creates duplicate wallets**: `approveKyc` checks `PENDING`, saves user, then creates a wallet with no uniqueness guard; `wallets.user_id` is not unique; `Optional<Wallet> findByUserId` then throws `IncorrectResultSizeDataAccessException`, making the user's funds unreachable | **H** | 3 (+4) |
| **NEW-DB-01** | No `CHECK (balance >= 0)` | H | 4 |
| **NEW-DB-02** | No `CHECK` on `wallets.status` / `transactions.status` enum values | L | 4 |
| **NEW-DB-03** | `audit_logs` has no DB-level immutability | M | 4 |
| **NEW-DB-04** | Missing FK/query indexes for reconciliation and pending-payout scans | L | 4 |
| **NEW-AUTH-01** | No `jti` claim → access-token replay undetectable; access token valid up to 15 min after logout | M | 1 |
| **NEW-OPS-01** | Collections containment guard has no regression test | M | 5 |

### 5.1 Severity recalibrations applied (evidence in the matrix)

| Register/audit claim | Actual | Reason |
|---|---|---|
| SEC-01 "soft-deleted user keeps access" | **Mitigated** | `@SQLRestriction("deleted = false")` + filter catch → no authentication |
| SEC-01 severity High | **Medium** | Narrowed to an email-verification policy gap |
| Admin "CRITICAL IDOR" ×4 | **Not a vulnerability** | `@EnableMethodSecurity` active; class-level `hasRole('ADMIN')` |
| KYC "CRITICAL RCE" | **Medium** | Uploads never served or executed |
| "Rate limiting not enforced" | **False** | `@Component` + `OncePerRequestFilter` → auto-registered, returns 429 |
| "LipilaGateway lacks timeouts" | **False** | 5 s connect / 10 s read configured |
| `V14` unique index "defect" | **Partly false** | Index exists; multiple NULLs are required |

---

## 6. Source-Defined Risk Inventory

16 in-scope identifiers, all accounted for:

- **Security (11):** SEC-01, SEC-02, SEC-03, SEC-04, SEC-05, SEC-06, SEC-08, SEC-09, SEC-10, SEC-11, SEC-12
- **Financial (2):** FIN-09, FIN-11
- **Existing backend (3):** R-01, R-02, R-03

`SEC-07` exists in the register and is **deliberately out of Phase 1C scope** (email-verification token expiry). It must not be "fixed" incidentally.

Out-of-scope boundary (do not touch): FIN-01, FIN-02, FIN-08 (ledger), FIN-10, ARCH-01/02/03, DB-01.

Verbatim entries and statuses: `PHASE_1C_RISK_TRACEABILITY_MATRIX.md` §4.

---

## 7. Proposed Remediation — Batch by Batch

Each batch lists the **exact files likely to change**. A file may appear in two batches only where noted; where that happens, the owning batch is stated explicitly to prevent conflicting concurrent edits.

---

### Batch 1 — Authentication and Session Hardening

**Addresses:** SEC-01, SEC-02, SEC-03, SEC-06, SEC-08, NEW-AUTH-01
**Blocked by:** DEC-01 (email-verification policy), DEC-02 (access-token revocation approach)

**Changes:**

1. **SEC-08 — JWT hardening** (`JwtUtil`):
   - `@PostConstruct` validation: decoded key ≥ 256 bits, non-blank, fail startup otherwise.
   - Pin the algorithm explicitly on both generate and parse; reject `alg=none` and algorithm substitution.
   - Add `iss` and `aud` claims on issue and validate on parse, sourced from config.
   - Add `jti` (UUID) per token (supports NEW-AUTH-01).
2. **SEC-01 — meaningful account-state enforcement** (`JwtAuthFilter`, `CustomUserDetailsService`):
   - Implement the policy chosen in DEC-01. If **Option B**, enforce at the money-movement service boundary, not globally.
   - Do **not** blindly add `isEnabled()` globally — that locks out every unverified user.
   - Keep the existing catch-and-continue behaviour for token failures (no stack traces, no 500).
3. **SEC-02 — revoke on credential/status change**:
   - `UserService.changePassword` → `tokenService.revokeAllUserTokens(userId)`.
   - `AdminService.toggleUserDeletedStatus` → revoke on deactivation (coordinate with Batch 3, which owns this file).
4. **SEC-06 — logout endpoint**: `POST /api/v1/auth/logout`, authenticated, calls `AuthService.logout(userId)`, returns 204. Add to OpenAPI.
5. **SEC-03 — token exposure**:
   - Remove raw verification/reset tokens from `Notification` bodies and from `NotificationResponse`.
   - Hash tokens at rest; enforce single-use and expiry.
   - Redact tokens in `MailService` logs (SMTP-unconfigured path).
   - **Backward-compatibility consequence:** email links must still work. If tokens are removed from notification bodies, the delivery channel must carry the link (email) while the API response does not. This is a deliberate contract change to document in the batch report.
6. **NEW-AUTH-01 — access-token replay**: implement DEC-02. Default recommendation: `jti` + in-memory deny-list with a documented single-instance limitation, **or** accept the 15-minute window as an explicit accepted risk. No Redis.

**Files likely to change:**

| File | Change |
|---|---|
| `src/main/java/com/novawallet/novawallet_api/security/JwtUtil.java` | key validation, `iss`/`aud`/`jti`, algorithm pinning |
| `src/main/java/com/novawallet/novawallet_api/security/JwtAuthFilter.java` | account-state enforcement per DEC-01; optional `jti` deny-list check |
| `src/main/java/com/novawallet/novawallet_api/security/CustomUserDetailsService.java` | correct `enabled`/`accountNonLocked` mapping |
| `src/main/java/com/novawallet/novawallet_api/auth/controller/AuthController.java` | add logout |
| `src/main/java/com/novawallet/novawallet_api/auth/service/AuthService.java` | token lifecycle, hashing, single-use |
| `src/main/java/com/novawallet/novawallet_api/user/service/UserService.java` | revoke on password change |
| `src/main/java/com/novawallet/novawallet_api/admin/service/AdminService.java` | revoke on deactivation **(shared with Batch 3 — Batch 3 owns the lock change)** |
| `src/main/java/com/novawallet/novawallet_api/token/service/TokenService.java` | hashed lookup, reuse handling |
| `src/main/java/com/novawallet/novawallet_api/token/repository/RefreshTokenRepository.java` | only if a new query is needed |
| `src/main/java/com/novawallet/novawallet_api/notification/MailService.java` | redact tokens from logs |
| `src/main/java/com/novawallet/novawallet_api/notification/service/NotificationService.java` | no tokens in bodies |
| `src/main/java/com/novawallet/novawallet_api/notification/dto/NotificationResponse.java` | no token field |
| `src/main/resources/application.yml` | `jwt.issuer`, `jwt.audience` |
| `src/main/resources/application-prod.yml` | `jwt.issuer`, `jwt.audience`, no defaults for secrets |
| `src/main/resources/application-test.yml` | test issuer/audience |

**New tests:** valid login; invalid credentials; expired JWT; invalid signature; malformed JWT; `alg=none`; wrong `iss`; wrong `aud`; short key fails startup; revoked refresh token; refresh reuse; logout revokes refresh tokens; logout does not invalidate a *different* user's tokens; cross-user token manipulation; PIN failures; rate-limited login; unverified-user behaviour per DEC-01; soft-deleted user with a valid JWT → 401.

**Exit criteria:** all Batch 1 tests green; full suite green; no token or PIN in any captured log; 1B payout tests unchanged.

---

### Batch 2 — Authorization and Ownership Hardening

**Addresses:** SEC-10 (input bounds), ownership/IDOR verification, role-separation assessment
**Blocked by:** DEC-03 (admin role model)

**Changes:**

1. **Ownership matrix enforcement.** For every user-scoped endpoint, resolve the resource through the authenticated principal — never trust a client-supplied `userId`/`walletId` for authorization.
2. **SEC-10 — monetary input bounds.** Add `@Digits(integer = 13, fraction = 2)` and `@DecimalMax` to amount fields; add max length to narration/description.
3. **Mass-assignment review.** Confirm no request DTO binds `id`, `userId`, `role`, `balance`, or `status`. Add explicit tests.
4. **Role separation.** Assess only. Implementing SUPPORT / KYC_REVIEWER / FINANCE / SUPER_ADMIN requires a role-model change, a migration, and admin seeding — that is DEC-03 and must not be done speculatively.

**Files likely to change:**

| File | Change |
|---|---|
| `src/main/java/com/novawallet/novawallet_api/wallet/controller/WalletController.java` | ownership resolution |
| `src/main/java/com/novawallet/novawallet_api/transaction/controller/TransactionController.java` | ownership resolution |
| `src/main/java/com/novawallet/novawallet_api/transaction/service/TransactionHistoryService.java` | owner-scoped queries |
| `src/main/java/com/novawallet/novawallet_api/payment/controller/PayoutController.java` | wallet ownership |
| `src/main/java/com/novawallet/novawallet_api/kyc/controller/KycController.java` | ownership |
| `src/main/java/com/novawallet/novawallet_api/notification/controller/NotificationController.java` | ownership |
| `src/main/java/com/novawallet/novawallet_api/user/controller/UserController.java` | self-only |
| `src/main/java/com/novawallet/novawallet_api/transaction/dto/DepositRequest.java` | `@Digits`/`@DecimalMax` |
| `src/main/java/com/novawallet/novawallet_api/transaction/dto/WithdrawRequest.java` | `@Digits`/`@DecimalMax` |
| `src/main/java/com/novawallet/novawallet_api/transaction/dto/TransferRequest.java` | `@Digits`/`@DecimalMax`, narration bound |
| `src/main/java/com/novawallet/novawallet_api/payment/dto/PayoutRequest.java` | `@Digits`/`@DecimalMax` |
| `src/main/java/com/novawallet/novawallet_api/fee/dto/FeeEstimateRequest.java` | amount bounds |
| `src/main/java/com/novawallet/novawallet_api/admin/dto/CreateFeeRequest.java` | fee bounds |
| `src/main/java/com/novawallet/novawallet_api/admin/dto/FreezeWalletRequest.java` | reason bound |

**New tests (negative, per resource):** owner access; another customer's access; unauthenticated; wrong role; missing resource; invalid identifier; tampered identifier. Expected: 403/404, never 200.

**Exit criteria:** full negative matrix green; no endpoint returns another user's data; oversized/over-precise amounts → 400.

---

### Batch 3 — Wallet, Account and Financial Concurrency (financially critical)

**Addresses:** R-01, R-02, R-03, FIN-09, FIN-11, **NEW-FIN-01**
**Blocked by:** DEC-04 (R-01/R-03 identifier confirmation — defects are confirmed regardless), DEC-05 (DB-29 uniqueness design)

**Changes:**

1. **R-03 — account-number generation.**
   - Keep `wallets.account_number` UNIQUE as authoritative.
   - Move collision handling to the insert boundary: catch `DataIntegrityViolationException`, retry within a bounded loop, and fail with a clean 409/503 — never an unhandled 500.
   - Preserve `MAX_ATTEMPTS = 10`; fail safe on exhaustion; never rewrite an existing number.
2. **NEW-FIN-01 — duplicate wallet creation.**
   - In `AdminKycService.approveKyc`, take a pessimistic lock on the user row (or use a conditional status update) so two concurrent approvals cannot both pass the `PENDING` guard.
   - Add an `existsByUserId` guard **and** rely on the Batch 4 `UNIQUE (user_id)` constraint.
   - Handle `DataIntegrityViolationException` idempotently: if a wallet already exists, do not create a second.
3. **R-01 — administrative wallet status changes.**
   - `AdminService.toggleUserDeletedStatus`: replace `findByUserId` + `save` with `findByIdWithLock` on the wallet.
   - Do not overwrite a wallet status without checking its current state (restoring a user must not silently clear a fraud freeze — record the prior reason).
   - Document lock order: **user row → wallet row**; transfers lock **wallets ordered by UUID**. Admin user operations therefore never hold two wallet locks, and transfers never hold a user lock. This must be asserted in the batch report.
4. **Transfer concurrency proof.** Verify sender/receiver lock acquisition is ordered deterministically by wallet id; verify sender `ACTIVE` status is re-checked **after** the lock; verify no partial mutation on failure.
5. **R-02 — `BalanceRecalculationJob`.** Keep containment. Do **not** re-enable. Add a test proving the bean is absent for every shipped profile. Document that the calculation is unsafe because it writes the current balance into historical snapshots.
6. **FIN-09 — fee rounding.** Round once: compute percentage at full precision, add flat fee, clamp, then `setScale(2, HALF_EVEN)` exactly once.
7. **FIN-11 — unverified limits.** Implement the approved policy: either a reduced unverified default or a KYC gate before tier limits. Do not silently remove the velocity caps (they apply to all users today).

**Files likely to change:**

| File | Change |
|---|---|
| `src/main/java/com/novawallet/novawallet_api/wallet/service/AccountNumberGenerator.java` | bounded retry at insert boundary |
| `src/main/java/com/novawallet/novawallet_api/wallet/service/WalletService.java` | creation path, status precondition |
| `src/main/java/com/novawallet/novawallet_api/wallet/repository/WalletRepository.java` | lock/exists queries |
| `src/main/java/com/novawallet/novawallet_api/kyc/service/AdminKycService.java` | approve TOCTOU fix, idempotent wallet creation |
| `src/main/java/com/novawallet/novawallet_api/admin/service/AdminService.java` | R-01 wallet lock; status precondition |
| `src/main/java/com/novawallet/novawallet_api/transaction/service/TransactionService.java` | lock order, post-lock status re-check |
| `src/main/java/com/novawallet/novawallet_api/transaction/repository/TransactionRepository.java` | recalc query scope |
| `src/main/java/com/novawallet/novawallet_api/notification/schedule/BalanceRecalculationJob.java` | containment only |
| `src/main/java/com/novawallet/novawallet_api/fee/service/FeeEngineService.java` | single rounding |
| `src/main/java/com/novawallet/novawallet_api/transaction/service/TransactionLimitService.java` | FIN-11 policy |
| `src/main/java/com/novawallet/novawallet_api/payment/service/PayoutService.java` | **read-only verification** — no change unless a regression is found |
| `src/main/java/com/novawallet/novawallet_api/payment/service/PayoutReconciliationService.java` | **read-only verification** |

**New tests (PostgreSQL 16 required):** concurrent transfers; sender-status race; admin deactivation vs transfer; concurrent KYC approval → exactly one wallet; concurrent account creation → zero duplicates; collision → retry not 500; exhaustion → clean failure; duplicate financial request; recalc job absent in every profile; fee single-rounding edge cases; unverified-limit policy; deadlock/lock-order assertion.

**Exit criteria:** zero duplicate account numbers persisted under concurrency; exactly one wallet per user under concurrent approval; no negative balance; 1B payout tests unchanged; all concurrency tests executed on PostgreSQL 16 and recorded as such.

---

### Batch 4 — Database Integrity and Migration Hardening

**Addresses:** NEW-DB-01..04, DB-29 (design), duplicate-wallet constraint
**Blocked by:** DEC-05 (composite uniqueness design), DEC-06 (audit immutability approach)

**Changes (all additive; `V17+` — Phase 1C Batch 1 has already claimed `V16` for `revoked_access_tokens`):**

1. `CHECK (balance >= 0)` on `wallets` — **pre-flight required**: query for existing negative rows; if any exist, stop and report rather than silently fixing financial data.
2. `UNIQUE (user_id)` on `wallets` — **pre-flight required**: query for duplicate `user_id` rows; if any exist, stop and report.
3. `CHECK` constraints on `wallets.status` and `transactions.status` allowed values — must match the Java enums exactly.
4. Indexes justified by real queries: reconciliation scans (`next_status_check_at`, `external_status`), pending-payout scans, audit-log search. Add only where an actual query pattern exists.
5. **DB-29 — duplicate financial effects.** Do **not** add a naive `UNIQUE (sender, receiver, type, amount, reference)`: it would reject legitimate identical transfers. The defensible constraints are:
   - `transactions.reference` UNIQUE (already exists),
   - `transactions.idempotency_key` UNIQUE (already exists),
   - `(provider, event_id)` UNIQUE (already exists).
   If a stronger invariant is wanted, it needs a designed idempotency scope (DEC-05), not a guess.
6. `audit_logs` immutability: revoke `UPDATE`/`DELETE` from the application role, or add a trigger. Must not break Flyway or the audit writer (DEC-06).
7. **Never rewrite `V1`–`V16`.** `V12` is Java-based and must be accounted for in the migration ordering check. `V16` is owned by Batch 1 (`V16__create_revoked_access_tokens.sql`); Batch 4 starts at `V17`.

**Files likely to change:**

| File | Change |
|---|---|
| `src/main/resources/db/migration/V17__*.sql` | new constraints/indexes (name TBD in batch) |
| `src/main/resources/db/migration/V18__*.sql` | only if audit immutability needs a separate step |
| `src/main/java/com/novawallet/novawallet_api/wallet/entity/Wallet.java` | only if mapping needs to reflect a constraint |
| `src/main/java/com/novawallet/novawallet_api/transaction/entity/Transaction.java` | only if mapping needs alignment |
| `src/main/java/com/novawallet/novawallet_api/audit/entity/AuditLog.java` | only if immutability requires it |
| `src/main/java/com/novawallet/novawallet_api/payment/entity/ProviderWebhookEvent.java` | verify unique mapping matches DDL |

**New tests:** fresh PG16 migration; historical upgrade from the Phase 1B schema; Flyway `validate`; constraint rejection tests; duplicate-data rejection; index presence assertions; migration idempotence.

**Exit criteria:** fresh and upgrade paths both pass on PostgreSQL 16; Flyway validation recorded; no destructive change; historical data preserved.

---

### Batch 5 — Webhook and External Integration Security

**Addresses:** SEC-05, NEW-OPS-01
**Blocked by:** DEC-07 (Lipila collections signing contract — external)

**Changes:**

1. **Disbursement path:** preserve exactly. Add regression tests for every control in P1–P5.
2. **Collections path:** the 503 containment guard **stays**. Add an explicit test that no credit can occur.
   - HMAC cannot be implemented without the provider contract. Do not invent a signature format, header name, canonical string, or tolerance.
   - Record as **BLOCKED** with the precise documentation required.
3. **Provider client:** timeouts already exist (5 s connect / 10 s read) — verify and record, do not "fix".
4. **Webhook hardening that is contract-independent:** constant-time comparison (already present), durable dedup (already present), no secret/signature/payload logging, provider status re-query outside the transaction (already present).

**Files likely to change:**

| File | Change |
|---|---|
| `src/main/java/com/novawallet/novawallet_api/payment/service/WebhookService.java` | collections guard hardening + tests; no invented HMAC |
| `src/main/java/com/novawallet/novawallet_api/payment/controller/WebhookController.java` | response-code correctness |
| `src/main/java/com/novawallet/novawallet_api/payment/config/PaymentConfig.java` | collections secret placeholder only if the contract is confirmed |
| `src/main/java/com/novawallet/novawallet_api/payment/entity/ProviderWebhookEvent.java` | only if dedup needs a field |
| `src/main/java/com/novawallet/novawallet_api/payment/repository/ProviderWebhookEventRepository.java` | dedup query if needed |
| `src/main/resources/application.yml`, `application-prod.yml` | collections secret property **only if** confirmed |

**New tests:** valid signature; invalid; missing; expired timestamp; replay; duplicate event; wrong amount; wrong reference; wrong provider; malformed payload; provider status disagreement; concurrent duplicate callbacks; **collections credit impossible**.

**Exit criteria:** disbursement controls verified by test; collections remains contained and provably cannot credit; blocker documented.

---

### Batch 6 — KYC, PII and Document Security

**Addresses:** SEC-12 (upload half), KYC audit trail, document ownership
**Blocked by:** DEC-08 (retention policy — legal)

**Changes:**

1. **Magic-byte validation** for JPEG, PNG, WebP, PDF; reject extension/MIME/content mismatch.
2. **Defence-in-depth on `storeFile`**: apply the same `normalize().startsWith(uploadDir)` guard already present in `loadFile`.
3. **KYC audit events** via `AuditService` for upload, submit, approve, reject — actor, timestamp, before/after.
4. **Ownership tests** for document download (customer own-only; admin role-gated).
5. **PII minimisation**: review `UserSummaryResponse` fields; do not add new PII to responses.
6. **Retention**: do not implement deletion without legal confirmation. Record the question.
7. **Logging**: stop logging rejection reasons at INFO if they may contain PII.

**Files likely to change:**

| File | Change |
|---|---|
| `src/main/java/com/novawallet/novawallet_api/kyc/service/KycService.java` | magic bytes, audit events |
| `src/main/java/com/novawallet/novawallet_api/kyc/service/AdminKycService.java` | audit events, log redaction |
| `src/main/java/com/novawallet/novawallet_api/kyc/service/FileStorageService.java` | store-path guard |
| `src/main/java/com/novawallet/novawallet_api/kyc/config/KycConfig.java` | size/type limits if needed |
| `src/main/java/com/novawallet/novawallet_api/kyc/controller/KycController.java` | ownership |
| `src/main/java/com/novawallet/novawallet_api/admin/controller/AdminKycController.java` | role/ownership |
| `src/main/java/com/novawallet/novawallet_api/kyc/dto/KycDocumentResponse.java` | no internal paths |
| `src/main/java/com/novawallet/novawallet_api/admin/dto/UserSummaryResponse.java` | PII minimisation |
| `src/main/java/com/novawallet/novawallet_api/audit/service/AuditService.java` | KYC event helpers if needed |

**New tests:** upload valid types; spoofed MIME; executable bytes; oversized; traversal filename; owner download; cross-user download; admin download; unauthorized role; audit event written; no PII in logs.

**Exit criteria:** spoofed content rejected; document access owner/admin only; audit events durable; no internal path leaked.

---

### Batch 7 — Rate Limiting and Abuse Protection

**Addresses:** SEC-09
**Blocked by:** none (DEC-09 documents the in-memory limitation)

**Changes:**

1. Configure `server.forward-headers-strategy=framework` and document the trusted-proxy assumption.
2. Apply per-operation limits to login, OTP (if present), PIN verify, password reset, KYC submit, transfers, withdrawals, payouts, beneficiary creation, webhooks, admin operations.
3. Keep the existing `RateLimitFilter`; do not replace it.
4. **Do not add Redis.** Document that limits are per-instance and that a multi-instance deployment needs a shared store.
5. Ensure 429 responses do not leak whether an account exists.

**Files likely to change:**

| File | Change |
|---|---|
| `src/main/java/com/novawallet/novawallet_api/security/RateLimitFilter.java` | per-route policy |
| `src/main/java/com/novawallet/novawallet_api/security/LoginRateLimiter.java` | proxy-aware keying |
| `src/main/java/com/novawallet/novawallet_api/config/SecurityConfig.java` | filter ordering if needed |
| `src/main/resources/application.yml`, `application-prod.yml` | limits, forward-headers |

**New tests:** within limit; exceeded; window reset; concurrent requests; multiple endpoints; auth failures; legitimate recovery after throttling; `X-Forwarded-For` behaviour.

**Exit criteria:** limits enforced per operation; concurrency safe; single-instance limitation documented; no Redis.

---

### Batch 8 — Audit, Logging and Error Handling

**Addresses:** SEC-04, SEC-11, SEC-12 (request-id half), log hygiene
**Blocked by:** DEC-10 (issuer/audience rollout — if deferred from Batch 1)

**Changes:**

1. **SEC-04** — remove `spring.profiles.active: dev`; require an explicit profile; fail fast when absent.
2. **SEC-11** — restrict `/actuator/info` and Swagger to non-prod; tighten CORS headers; never wildcard headers with credentials.
3. **SEC-12** — validate `X-Request-ID` against `[A-Za-z0-9-]{1,64}`, else generate a UUID.
4. **Log hygiene** — no passwords, PINs, tokens, HMAC secrets, full credentials, document contents, raw idempotency keys, or unnecessary PII.
5. **Error handling** — customer-facing errors never expose stack traces, SQL, or internal paths.
6. **Audit durability** — verify audit writes are attributable and not silently mutable.

**Files likely to change:**

| File | Change |
|---|---|
| `src/main/resources/application.yml` | remove dev default |
| `src/main/resources/application-prod.yml` | actuator/swagger restriction |
| `src/main/java/com/novawallet/novawallet_api/config/SecurityConfig.java` | CORS headers, permitAll tightening |
| `src/main/java/com/novawallet/novawallet_api/filter/RequestTracingFilter.java` | request-id validation |
| `src/main/java/com/novawallet/novawallet_api/exception/GlobalExceptionHandler.java` | safe responses |
| `src/main/java/com/novawallet/novawallet_api/audit/aspect/AuditedAspect.java` | no sensitive fields |
| `src/main/java/com/novawallet/novawallet_api/audit/service/AuditService.java` | durability |
| `src/main/resources/logback-spring.xml` | redaction/levels |

**New tests:** captured logs contain no secret/token/PIN; CRLF request-id rejected; prod profile exposes no Swagger/info; unknown origin CORS rejected; error response contains no internals.

**Exit criteria:** no sensitive value in captured output; prod config hardened; startup fails without an explicit profile.

---

### Batch 9 — Final Security Regression and Hardening

**Changes:** only fixes discovered by regression. No new scope.

**Activities:**
1. Full `mvn clean test` on H2 (baseline equivalence ≥ 243 tests).
2. Full PostgreSQL 16 run for concurrency, constraints, migrations.
3. Flyway `validate` on fresh and upgraded schemas.
4. Complete the 16-document report set, including `PHASE_1C_FINAL_VERIFICATION_LOG.md`.
5. Produce `PHASE_1C_FINAL_SECURITY_REVIEW.md`, `PHASE_1C_FINAL_ACCEPTANCE_REPORT.md`, `PHASE_1C_FINAL_REMAINING_RISKS.md`, `PHASE_1C_FINAL_FILE_CHANGE_INVENTORY.md`.

**Exit criteria:** all 20 acceptance criteria from the master prompt satisfied or explicitly recorded as BLOCKED/ACCEPTED; every modified file accounted for.

---

## 8. Test Strategy

### 8.1 Two-tier execution

| Tier | Database | Purpose | Command |
|---|---|---|---|
| **T1 — unit/integration** | H2 (`MODE=PostgreSQL`) | Existing suite, fast feedback | `./mvnw -B clean test` |
| **T2 — PostgreSQL 16** | Disposable container | Locking, constraints, isolation, migrations | `./mvnw -B test -Dspring.profiles.active=pgtest` with `SPRING_DATASOURCE_URL` pointed at the container |

**PostgreSQL 16 is available and verified**: Docker 29.9.3 running, `postgres:16` image cached, probe returned **PostgreSQL 16.15**. Container is disposable and removed after use. **The user's local 5432 instance is never used** — it requires credentials I do not have and it is not the project database.

**No new dependency required.** T2 uses a `pgtest` Spring profile that reads `SPRING_DATASOURCE_URL`/`SPRING_DATASOURCE_USERNAME`/`SPRING_DATASOURCE_PASSWORD` from the environment. Testcontainers is **not** added (it would be a new dependency — see DEC-11 if the team prefers it).

### 8.2 Tagging

Concurrency, constraint, and migration tests are tagged so T1 stays fast:

```java
@Tag("postgres")
```

T1 excludes the tag; T2 includes it. No test is deleted or weakened.

### 8.3 Mandatory test classes to add

| Batch | Test | Proves |
|---|---|---|
| 1 | `JwtHardeningTest` | alg pinning, iss/aud, key length, jti |
| 1 | `LogoutRevocationTest` | refresh tokens revoked; cross-user isolation |
| 1 | `AccountStatePolicyTest` | DEC-01 policy enforced exactly |
| 1 | `TokenExposureTest` | no raw token in notification body/response/log |
| 2 | `AuthorizationNegativeMatrixTest` | owner / other user / anon / wrong role per resource |
| 2 | `MonetaryBoundsTest` | `@Digits`, `@DecimalMax` → 400 |
| 3 | `ConcurrentTransferPostgresTest` | no lost update, no negative balance, deterministic lock order |
| 3 | `WalletStatusRacePostgresTest` | admin deactivation vs transfer |
| 3 | `ConcurrentKycApprovalPostgresTest` | exactly one wallet per user |
| 3 | `AccountNumberConcurrencyPostgresTest` | zero duplicates persisted; collision retried |
| 3 | `BalanceRecalcContainmentTest` | bean absent in every profile |
| 3 | `FeeRoundingTest` | single rounding |
| 3 | `UnverifiedLimitPolicyTest` | FIN-11 policy |
| 4 | `MigrationFreshPostgresTest` | V1→V17 on empty PG16 |
| 4 | `MigrationUpgradePostgresTest` | Phase 1B schema → V17 |
| 4 | `ConstraintIntegrityPostgresTest` | CHECK/UNIQUE reject invalid rows |
| 5 | `WebhookNegativeMatrixTest` | signature/timestamp/replay/amount/reference |
| 5 | `CollectionsContainmentTest` | no credit possible |
| 6 | `KycUploadValidationTest` | magic bytes, spoofed MIME, traversal |
| 6 | `KycAuthorizationTest` | owner/admin only |
| 7 | `RateLimitConcurrencyTest` | limits under concurrency, recovery |
| 8 | `SensitiveLoggingTest` | no secrets in captured logs |
| 8 | `RequestIdValidationTest` | CRLF rejected |

### 8.4 Environment record (required in every batch report)

Java version, Maven version, PostgreSQL version (when used), exact command, exit code, tests run, failures, errors, skipped, BUILD result, log path.

**Standing caveat:** no Java 17 runtime is installed; Java 17 compatibility is claimed only at the source/`--release 17` level.

---

## 9. Database Migration Implications

### 9.1 Rules

1. `V1`–`V15` are immutable history. **Never rewrite.**
2. `V12` is Java-based (`src/main/java/db/migration/V12__CreateTransactionReferenceSequence.java`) and participates in ordering — Batch 4 must confirm the sequence it creates still exists after a fresh migration.
3. All new migrations are additive and must be safe on a populated table.
4. Any migration that could fail on existing data must **pre-check and fail loudly** rather than silently repair financial data.
5. **Migration ownership:** Batch 1 owns `V16` (`V16__create_revoked_access_tokens.sql`). Later batches must use `V17` or higher. A migration that has been applied anywhere is immutable even if the branch is unmerged.

### 9.2 Planned migrations (names finalized per batch)

**Batch 1 has already consumed `V16`** (`V16__create_revoked_access_tokens.sql`). Any migration added after Batch 1 starts at `V17`. If Batch 1's `V16` is still uncommitted when Batch 4 begins, it must still be treated as taken — never renumber a migration that has been applied to any environment.

| Migration | Content | Pre-flight |
|---|---|---|
| `V16__create_revoked_access_tokens.sql` (**Batch 1, implemented**) | Durable access-token revocation: `jti` PK, `user_id` FK, `revoked_at`, `expires_at`, indexes | None (new table) |
| `V17__wallet_and_transaction_integrity_constraints.sql` | `CHECK (balance >= 0)`; `UNIQUE (user_id)` on `wallets`; `CHECK` on `wallets.status`; `CHECK` on `transactions.status` | Query negative balances; query duplicate `wallets.user_id`; confirm enum values |
| `V18__query_indexes.sql` | Reconciliation / pending-payout / audit-search indexes | `EXPLAIN` the real queries first; add only justified indexes |
| `V19__audit_immutability.sql` (conditional) | Revoke `UPDATE`/`DELETE` on `audit_logs` from the app role, or trigger | Confirm Flyway/owner permissions; confirm audit writer only inserts |

### 9.3 Explicitly rejected migrations

- **No** composite `UNIQUE (sender, receiver, type, amount, reference)` — would reject legitimate identical transfers. DB-29 needs a designed idempotency scope (DEC-05).
- **No** ledger tables. The double-entry ledger is Phase 2.
- **No** destructive column drops, type narrowing, or `NOT NULL` backfills without verified data.

### 9.4 Verification matrix

| Check | Fresh PG16 | Upgraded PG16 |
|---|---|---|
| All migrations apply | required | required |
| Flyway `validate` | required | required |
| `V12` sequence exists | required | required |
| Constraints reject bad rows | required | required |
| Indexes present | required | required |
| No data loss | — | required |

---

## 10. Implementation Order

The order is driven by **risk, not by register numbering**.

```
Batch 1  Authentication & session        ← highest account-takeover risk
Batch 2  Authorization & ownership       ← cross-user exposure
Batch 3  Wallet & financial concurrency  ← money corruption (financially critical)
Batch 4  Database integrity              ← makes Batch 3 invariants durable
Batch 5  Webhook security                ← contained today; provider-blocked
Batch 6  KYC / PII / documents           ← privacy + audit
Batch 7  Rate limiting                   ← abuse resistance
Batch 8  Audit / logging / errors        ← hygiene + config hardening
Batch 9  Final regression                ← proof
```

**Rationale for Batch 1 before Batch 3:** an attacker who can take over a session does not need to exploit concurrency. Authentication is the cheapest path to financial loss.

**Rationale for Batch 3 before Batch 4:** the application-level fix lands first so the constraint in Batch 4 cannot be bypassed by an unfixed code path. Batch 4 then makes the invariant durable.

**Hard dependency:** Batch 4's `UNIQUE (user_id)` requires Batch 3's idempotent creation path to be in place, otherwise concurrent approval will surface as a 500 instead of a controlled retry.

---

## 11. Dependencies and Blockers

| ID | Dependency / Blocker | Affects | Status |
|---|---|---|---|
| BLK-01 | Lipila collections signing contract not evidenced in the repo | Batch 5 | **BLOCKED** — needs provider docs: header name, algorithm, encoding, canonical string, timestamp header, tolerance, event-id field |
| BLK-02 | `R-01`/`R-03` identifiers undefined in the canonical register | Batches 3, 9 | Awaiting human confirmation (defects themselves are confirmed) |
| BLK-03 | Two divergent risk-register copies (`DOC-SOT-01`) | All batches (documentation) | Awaiting human decision on canonical path |
| BLK-04 | No Java 17 runtime installed | Reporting only | Standing limitation; never claim Java 17 runtime verification |
| BLK-05 | Local PostgreSQL on 5432 is not the project DB and is not accessible | Batches 3/4/9 | Resolved via disposable PG16 container (16.15 verified) |
| BLK-06 | Testcontainers not in `pom.xml` | Test infrastructure | Resolved without new dependency via `pgtest` profile; see DEC-11 |

---

## 12. Risks Requiring Human Decisions

**No batch may proceed past a decision it depends on.** Each is a product, legal, or architecture choice that I will not guess.

| ID | Decision | Blocks | Recommended option |
|---|---|---|---|
| **DEC-01** | **Email-verification enforcement policy.** Today `isEnabled()` = `emailVerified` and is never checked. Enforcing it globally locks out every unverified user. | Batch 1 | **Option B — financial-only enforcement.** Matches the entity Javadoc; preserves onboarding; gates money movement. |
| **DEC-02** | **Access-token revocation after logout.** Options: `jti` + in-memory deny-list (single-instance), accept the 15-minute window as an accepted risk, or a DB-backed revocation table. Redis is not permitted. | Batch 1 | `jti` + in-memory deny-list, with the single-instance limitation documented; keep the 15-minute TTL. |
| **DEC-03** | **Admin role model.** Keep single `ADMIN`, or introduce SUPPORT / KYC_REVIEWER / FINANCE / SUPER_ADMIN (needs migration + seeding). | Batch 2 | Defer role separation unless operationally required; document the single-role risk. |
| **DEC-04** | **R-01/R-03 identifier confirmation.** Proposed meanings in the matrix §2. | Batch 3 sign-off | Confirm the proposed mapping so the matrix can close the identifiers. |
| **DEC-05** | **DB-29 duplicate-financial-effect design.** A naive composite unique would reject legitimate identical transfers. | Batch 4 | Define an idempotency scope; do not add a composite unique on amount/reference. |
| **DEC-06** | **Audit immutability mechanism.** DB permissions vs trigger vs application-only. | Batch 4 | DB-level revoke of `UPDATE`/`DELETE`, after confirming Flyway/owner permissions. |
| **DEC-07** | **Collections provider contract.** External documentation required. | Batch 5 | Keep the 503 guard; record BLOCKED; do not invent a signature scheme. |
| **DEC-08** | **KYC/PII retention period.** Requires qualified Zambian counsel or a regulated partner. | Batch 6 | Do not implement deletion; record the question. |
| **DEC-09** | **Rate limiting scope and deployment assumption.** In-memory only; multi-instance needs a shared store (not Redis in this phase). | Batch 7 | Per-operation in-memory limits; document single-instance. |
| **DEC-10** | **Forced re-login on `iss`/`aud` rollout.** Adding and validating these invalidates all existing tokens. | Batch 1 | Accept forced re-login (pre-production) or make validation opt-in per environment. |
| **DEC-11** | **Test infrastructure.** `pgtest` profile (no new dependency) vs adding Testcontainers (new test dependency). | Batches 3/4/9 | `pgtest` profile — no dependency change. |
| **DEC-12** | **Restoring a soft-deleted user currently unfreezes the wallet unconditionally**, which can clear a fraud freeze. | Batch 3 | Preserve the freeze reason and require an explicit unfreeze action. |

---

## 13. Batch Gate Protocol

At the end of **every** batch, before stopping:

1. `git status --short` — record modified, untracked, staged.
2. `git diff --check` — no whitespace errors.
3. `git diff --stat` and `git diff --name-status` — confirm only intended files.
4. Targeted tests — record command, exit code, counts, log path.
5. Full suite — record the same.
6. Update `PHASE_1C_RISK_TRACEABILITY_MATRIX.md` statuses **only** with executable evidence.
7. Update the risk register **in the canonical copy only** (pending DEC on BLK-03).
8. Write `docs/phase-1c/PHASE_1C_BATCH_N_REPORT.md`.
9. **Stop.** Do not begin the next batch.

A risk moves to `CLOSED` only when both the correction and its verification evidence exist. A risk with a correction but no test stays `IN PROGRESS`.

---

## 14. Definition of Done for Phase 1C

All 20 acceptance criteria from the master prompt, plus:

- Every in-scope identifier has a status backed by evidence.
- Every new finding discovered in Batch 0 (`NEW-*`) is registered with an ID.
- Fresh and upgraded PostgreSQL 16 migrations both verified, with Flyway validation recorded.
- Collections is either verified against a real contract or explicitly BLOCKED.
- No sensitive value appears in captured logs.
- Phase 1A and Phase 1B safeguards demonstrably intact.
- Every modified file is listed in `PHASE_1C_FINAL_FILE_CHANGE_INVENTORY.md`.
- Final decision is one of ACCEPT / CONDITIONAL ACCEPT / REJECT — with no claim of certification, regulatory approval, live financial readiness, or production readiness.

---

## 15. Change Log

| Date | Change |
|---|---|
| 2026-11-12 | Initial Batch 0 implementation plan. 9 batches, per-batch file manifests, PG16 test strategy, 12 human decisions, 6 blockers. |
