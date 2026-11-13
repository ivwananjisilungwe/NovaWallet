# NovaWallet — Phase 1C Security Baseline

> **Location:** version-controlled copy — `novawallet-api/docs/phase-1c/`. Do not edit findings in this copy; supersede with a later batch report.`

**Document:** PHASE_1C_SECURITY_BASELINE.md
**Phase:** 1C — Security and Database Hardening (Batch 0 deliverable)
**Branch:** `feature/phase-1c-security-database-hardening`
**HEAD:** `797571914b98d3b3206c5529e8eaf356ba79d1c8`
**Date:** 2026-11-12
**Author:** Forge (primary engineering orchestrator)
**Method:** Read-only assessment. 8 parallel specialist audits, then independent re-verification of every load-bearing claim by Forge. No source file was modified. No provider was called. No production database was touched.

> **No formal certification is claimed.** This is an internal engineering baseline, not a security audit opinion, regulatory approval, or production-readiness statement.

---

## 1. Verified Entry State

| Item | Value |
|---|---|
| Phase 1B commit | `797571914b98d3b3206c5529e8eaf356ba79d1c8` — 43 files, +5,561 / −173 |
| Phase 1A commit (parent) | `aac8737` — "chore: complete phase 1a financial guardrails" |
| Baseline commit | `60179fb` — "chore: establish NovaWallet API baseline" |
| Working tree at Batch 0 start | **clean** (0 modified, 0 untracked, 0 staged) |
| Phase 1B reports | 5/5 present at workspace `reports/` |
| Baseline test suite | **243 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS** |
| Test environment | JDK 21.0.12.1 Temurin, Maven 3.9.16, `./mvnw -B clean test`, 1:28 |
| Baseline log | `/tmp/opencode/phase1c-baseline-test.log` |

**Environment caveat (material):** the system-wide `java` is **JRE-only** (no `javac`). A naive `mvn test` fails with `release version 17 not supported`. The build requires `JAVA_HOME=/home/ivwananji-silungwe/.jdks/jdk-21.0.12.1+1`. This must be documented for CI and for every later batch.

**Java 17 claim:** the project compiles with `--release 17` (`<java.version>17</java.version>`), and I verified `javac --release 17` succeeds on JDK 21. **No Java 17 runtime is installed**, so no Java 17 *runtime* verification is claimed.

---

## 2. Current Architecture

Modular monolith, Spring Boot 3.5.3, Java 17 source level, Maven, PostgreSQL 16, Flyway, Spring Data JPA, Spring Security, stateless JWT.

### 2.1 Packages (`com.novawallet.novawallet_api`)

| Package | Responsibility |
|---|---|
| `admin` | Admin-only controllers (users, wallets, KYC, audit search) |
| `audit` | `@Audited` aspect, `AuditLog` entity/repository/service |
| `auth` | Register, login, refresh, PIN, password reset, email verification |
| `bootstrap` | `AdminDataInitializer` — seeds first admin |
| `config` | `SecurityConfig`, `PasswordConfig`, `AsyncConfig`, `CacheConfig`, `JacksonConfig`, `JpaAuditingConfig`, `OpenApiConfig` |
| `exception` | `GlobalExceptionHandler` + typed exceptions |
| `fee` | Fee engine + admin fee configuration |
| `filter` | `RequestTracingFilter` |
| `idempotency` | `IdempotencyKey`, filter, cleanup job, service |
| `kyc` | KYC documents, upload/storage, admin review |
| `notification` | Mail/SMS, notifications, scheduled jobs |
| `payment` | Lipila gateway, webhook controller, payout service, reconciliation scheduler |
| `security` | `JwtUtil`, `JwtAuthFilter`, `CustomUserDetailsService`, `RateLimitFilter`, `LoginRateLimiter` |
| `token` | Refresh tokens, rotation, cleanup |
| `transaction` | Deposit/withdraw/transfer, pessimistic locking, limits |
| `user` | Profile, password change |
| `wallet` | Wallet entity, locked repository, freeze/unfreeze |

### 2.2 HTTP surface (57+ endpoints)

Public (`permitAll`): `/api/v1/auth/{register,login,refresh}`, `/api/v1/password/{forgot,reset}`, `/api/v1/email/verify`, `/api/v1/webhooks/lipila/**`, `/api/swagger-ui/**`, `/api/api-docs/**`, `/actuator/{health,info}`.
Everything else: `authenticated()`.
Admin: class-level `@PreAuthorize("hasRole('ADMIN')")` on `AdminUserController`, `AdminKycController`, `AdminWalletController`; method-level on `FeeController` admin methods.

**Method security is active:** `@EnableMethodSecurity` at `SecurityConfig.java:31`. Verified — this is load-bearing, because without it every admin `@PreAuthorize` would be inert.

### 2.3 Persistence

13 entities / tables: `users`, `wallets`, `refresh_tokens`, `kyc_documents`, `transactions`, `fee_configurations`, `audit_logs`, `idempotency_keys`, `notifications`, `notification_attempts`, `provider_webhook_events`, plus enum tables.

Migrations `V1`–`V15`. **`V12` is a Java-based Flyway migration** (`V12__CreateTransactionReferenceSequence`) that creates a DB sequence — it is not present in `db/migration` as SQL and must be accounted for separately in Batch 4.

### 2.4 Concurrency primitives in use

- `WalletRepository.findByIdWithLock` — `@Lock(PESSIMISTIC_WRITE)`.
- `WalletRepository.updateBalance` — native conditional `UPDATE ... WHERE id = :id AND balance + :amount >= 0`, version-bumped.
- `Wallet.version` — JPA optimistic `@Version`.
- `PayoutService` / `PayoutReconciliationService` — atomic claim + lease (Phase 1B).
- `ProviderWebhookEvent` — unique `(provider, event_id)` durable dedup.

### 2.5 Trust boundaries

```
[ Mobile / Web client ]  --JWT bearer-->  [ Spring Security filter chain ]
                                                   |
                                          [ Controllers @PreAuthorize ]
                                                   |
                                          [ Services @Transactional ]
                                                   |
                                          [ PostgreSQL 16 ]
        ^                                          |
        |                                          |
[ Lipila provider ] --webhook (HMAC/secret)--------+
```

Boundaries that cross trust: (1) client → API, (2) provider → webhook endpoint, (3) API → provider HTTP, (4) app → filesystem (KYC uploads), (5) app → SMTP.

---

## 3. Security Baseline — What Already Works

These are verified controls that Phase 1C **must not regress**.

| Control | Evidence | Verified by |
|---|---|---|
| BCrypt password + PIN hashing | `PasswordConfig.java:14` | Auth audit + code |
| Refresh tokens stored as SHA-256 hash, never plaintext | `TokenService.java:85-93` | Auth audit |
| Refresh-token rotation (old revoked on refresh) | `TokenService.java:266-271` | Auth audit |
| Revocation on password **reset** | `AuthService.java:243` | Forge grep |
| Revocation on logout (service layer) | `AuthService.java:279` | Forge grep |
| Uniform login error message (no enumeration) | `AuthService.java:108-109` | Auth audit |
| Forgot-password returns 200 for unknown email | `AuthService.java:212-225` | Auth audit |
| Login lockout: 5 failures / email+IP / 15 min | `LoginRateLimiter.java` | Auth audit |
| PIN lockout: 3 failures / 15 min | `AuthService.java:163-191` | Auth audit |
| Global rate limiting **is enforced** (100 req/min, 10 auth) | `RateLimitFilter.java:23-79` — `@Component`, `@Profile("!test")`, `OncePerRequestFilter`, returns 429 | **Forge** (refutes a subagent claim) |
| Stateless session, CSRF disabled appropriately | `SecurityConfig.java:50-53` | Both |
| Security headers: HSTS, X-Frame-Options, X-XSS-Protection, CSP | `SecurityConfig.java:55-90` | Ops audit |
| Method security enabled | `SecurityConfig.java:31` | **Forge** |
| Admin endpoints role-gated | `Admin*Controller:35/36/44` | **Forge** |
| Disbursement webhook: HMAC-SHA256, constant-time compare, 300 s window + 120 s future bound, durable `(provider,event_id)` dedup, amount match, provider re-query outside transaction, quarantine | `WebhookService.java:286-362,455-484` | Ops audit + 1B tests |
| Collections webhook cannot credit (503 guard) | `WebhookService.java:159-175` | Ops audit |
| KYC `loadFile` path-traversal guard | `FileStorageService.loadFile()` — `normalize().startsWith(uploadDir)` | **Forge** |
| Uploads are **not** statically served | no `addResourceHandler` anywhere | **Forge** |
| No secrets committed; all env-sourced | `application*.yml` + secret scan of the Phase 1B commit | **Forge** |
| Docker runs non-root, no secrets in image, healthcheck | `Dockerfile:36-68` | Ops audit |
| Global exception handler hides stack traces | `GlobalExceptionHandler.java:148-166` | Ops audit |
| Scheduler jobs fail-closed in prod | `application-prod.yml:142-151` | Ops audit + `BalanceRecalculationJobDisabledInProdTest` |
| Account number uniqueness **DB-enforced** | unique index from `V3` | DB audit |
| Phase 1A/1B financial safeguards | 243 green tests | **Forge baseline run** |

---

## 4. Threat Model (STRIDE)

Scope: the deployed API, its database, its provider integration, and its file storage. Adversaries considered: unauthenticated internet attacker, authenticated customer (horizontal), compromised/low-privilege admin (vertical), malicious provider callback, and a stolen-token holder.

| # | Threat | STRIDE | Entry point | Precondition | Impact | Existing control | Gap |
|---|---|---|---|---|---|---|---|
| T1 | Forged JWT accepted | Spoofing | `Authorization` header | Signing secret guessed/leaked | Full account takeover | HS256 + signature verify | No key-strength/entropy check, no `iss`/`aud`, algorithm not explicitly pinned (SEC-08) |
| T2 | Unverified-email user retains full authenticated access | Spoofing / Elevation | Any authenticated endpoint | Attacker registers without verifying email | Full API access including money movement | Token expiry; soft-delete blocked by `@SQLRestriction` | `isEnabled()` (= `emailVerified`) never checked; no locked/disabled field exists (SEC-01, narrowed — see DEC-01) |
| T3 | Access token replayed after logout | Spoofing / Repudiation | Any authenticated endpoint | Token captured | Access up to 15 min post-logout | Refresh revoked | No `jti`, no revocation list (AUTH-07/AUTH-10; requirement 8 of Batch 1) |
| T4 | Refresh token replayed after password change or admin deactivation | Spoofing | `/auth/refresh` | Token captured earlier | Persistent access | Reset revokes; logout revokes | Password change + admin deactivation do not revoke (SEC-02) |
| T5 | Account enumeration | Information Disclosure | `/auth/login`, `/password/forgot` | — | User-base mapping | Uniform errors; 200 on unknown email | Mitigated |
| T6 | Credential stuffing | Spoofing / DoS | `/auth/login` | — | Account takeover / lockout DoS | 5-attempt lockout, rate limit | Lockout can be weaponised against a known email (accepted, monitor) |
| T7 | IDOR on wallet / transaction / KYC document | Elevation / Info Disclosure | Resource IDs in path | Authenticated customer | Read/modify another customer's money or PII | Owner-scoped queries; admin role gate | **Needs per-endpoint negative tests** (Batch 2) |
| T8 | Privilege escalation to ADMIN | Elevation | Role-bearing fields | Mass assignment | Full platform compromise | Role not client-bindable (to be confirmed) | Single-role model: no SUPPORT/KYC_REVIEWER/FINANCE separation |
| T9 | Balance corruption via concurrent transfers | Tampering | `/transfers`, `/withdrawals` | Two concurrent requests | Money created/destroyed | Pessimistic lock + conditional UPDATE + `@Version` | Lock ordering and sender-status re-check under lock need proof (Batch 3) |
| T10 | Duplicate financial effect via retry | Tampering | Payout / transfer / webhook | Retry or replay | Double debit/credit | Idempotency key + unique `(provider,event_id)` | No composite unique constraint on transaction identity (DB-29) |
| T11 | Forged provider webhook credits a wallet | Spoofing / Tampering | `/webhooks/lipila/collections` | Endpoint reachable | Unearned credit | Collections returns 503 (contained) | Collections has **no HMAC**; BLOCKED on provider contract (SEC-05) |
| T12 | Replay of a valid disbursement webhook | Tampering | `/webhooks/lipila/disbursements` | Capture within window | Duplicate settlement | Timestamp window + durable dedup + terminal-state guard | Verified in 1B |
| T13 | Log injection via `X-Request-ID` | Tampering | Any request | Attacker sets header | Forged/CRLF log lines, SIEM poisoning | none | **No validation** (SEC-12) |
| T14 | Malicious KYC upload (content spoofing) | Tampering | `/kyc/documents/upload` | Authenticated | Stored arbitrary bytes; future RCE if served/processed | 10 MB cap, MIME allowlist, UUID filenames, path guard | No magic-byte check (SEC-12) |
| T15 | Token leakage through notifications | Information Disclosure | `/notifications` | Authenticated user (own) or DB reader | Account takeover via reset/verify token | none | Raw tokens in notification bodies + logs (SEC-03) |
| T16 | Dev profile accidentally active in production | Elevation / Info Disclosure | Deployment | Ops omission | Admin seeded, SQL logging, dev config | none | `active: dev` hardcoded (SEC-04) |
| T17 | Rate-limit bypass via proxy | DoS | Any endpoint | Reverse proxy deployed | Global throttle or bypass | In-memory limiter | Uses `getRemoteAddr()`; no forward-headers config (SEC-09) |
| T18 | Oversized/high-precision amounts | Tampering | Money DTOs | Authenticated | Precision/limit abuse | `@Positive`, service limits | No `@Digits`/`@DecimalMax` (SEC-10) |
| T19 | Information disclosure via Actuator/Swagger | Information Disclosure | `/actuator/*`, `/swagger-ui` | Network reachability | Version/config fingerprinting | health `when-authorized` | `info` public, Swagger public in all profiles (SEC-11) |
| T20 | Fee rounding drift | Tampering (financial) | Fee calculation | Repeated transactions | Small value leakage per txn | `HALF_EVEN` at 2dp | Intermediate rounding not proven single-step (FIN-09) |
| T21 | Unverified user bypasses tier limits | Elevation (financial) | Transfers/payouts | Unverified account | Higher limits than policy | KYC-tier limits | Verification gate not proven (FIN-11) |
| T22 | Admin wallet freeze races a transfer | Tampering | `/admin/wallets/{id}/freeze` | Concurrent transfer | Money moves on a frozen wallet or freeze lost | `@Transactional`, wallet lock on transfer | Admin path locking not proven (R-01) |
| T23 | Historical snapshot corruption | Tampering (audit) | `BalanceRecalculationJob` | Job enabled | Ledger history rewritten | Containment (`enabled: false`, `matchIfMissing=false`) | Must remain contained; job logic unsafe (R-02) |
| T24 | Account-number collision | Tampering | Wallet creation | Concurrent creation | Duplicate account numbers | DB unique index + bounded retry | Concurrency proof + exhaustion behaviour needed (R-03) |

### 4.1 Highest-risk scenarios (financial impact first)

1. **T9/T10/T22** — concurrency or replay creates or destroys money. Highest consequence, and the area where Phase 1A/1B safeguards are most intricate.
2. **T2/T4** — deactivated or password-changed users retain access. Direct account-takeover path.
3. **T11** — a collections credit path without HMAC. Currently contained; must stay contained.
4. **T15** — reset/verify tokens readable via the notifications API.

---

## 5. Identified Weaknesses (Verified)

All confirmed by Forge against source at `7975719`. Full detail and remediation in `PHASE_1C_RISK_TRACEABILITY_MATRIX.md`.

### 5.1 Authentication and session

| ID | Weakness | Sev | Evidence |
|---|---|---|---|
| SEC-01 | JWT filter never consults `UserDetails` account-state flags. **Re-verified:** soft-delete already blocked by `@SQLRestriction`; no locked/disabled column exists; `isEnabled()` maps to `emailVerified` and is unchecked → email verification gates nothing | M | `JwtAuthFilter.java:45-64`; `CustomUserDetailsService.java:31-34,44-47`; `User.java:26` — **see DEC-01, requires a human policy decision** |
| SEC-02 | Password change and admin deactivation do not revoke refresh tokens | H | `revokeAllUserTokens` only at `AuthService.java:243,259,279`; absent from `AdminService` |
| SEC-03 | Raw verification/reset tokens in notification bodies and mail logs | H | `AuthService.java:94,222` |
| SEC-06 | No logout endpoint | M | no mapping in `AuthController` |
| SEC-08 | No key-length/entropy validation, no `iss`/`aud`, algorithm not pinned | M | `JwtUtil.java` — no matches for issuer/audience/key-length |
| AUTH-07 | Access token remains valid after logout (bounded 15 min) | M | `JwtAuthFilter` has no revocation check |
| AUTH-10 | No `jti` claim → replay undetectable | M | `JwtUtil.generateToken` claims: sub, email, role, iat, exp |

### 5.2 Configuration and operational

| ID | Weakness | Sev | Evidence |
|---|---|---|---|
| SEC-04 | `spring.profiles.active: dev` hardcoded default | H | `application.yml:5-6` |
| SEC-09 | Rate limiter in-memory, `getRemoteAddr()`, no proxy awareness | M | `RateLimitFilter.java:86`, `LoginRateLimiter.java:80` |
| SEC-11 | Public `/actuator/info`, Swagger in all profiles, `allowedHeaders("*")` + credentials | M | `SecurityConfig.java:104-113,141-142` |
| SEC-12 | Unvalidated `X-Request-ID` → MDC (log injection) | L | `RequestTracingFilter.java:33` |

### 5.3 Input validation

| ID | Weakness | Sev | Evidence |
|---|---|---|---|
| SEC-10 | No `@Digits`/`@DecimalMax` on monetary DTOs | M | only `@Positive`/`@NotNull` in `Deposit/Withdraw/Transfer/PayoutRequest` |

### 5.4 Webhooks

| ID | Weakness | Sev | Evidence |
|---|---|---|---|
| SEC-05 | Collections webhook has no HMAC/timestamp/dedup/amount check | H (contained) | `WebhookService.processCollectionCallback` — no `verifySignature` call; 503 guard prevents credit |
| — | Provider signing contract for collections is **not evidenced in the repo** | BLOCKED | no documentation, no code |

### 5.5 KYC and PII

| ID | Weakness | Sev | Evidence |
|---|---|---|---|
| KYC-AUDIT | No audit event for KYC submit/approve/reject | H | `AuditService` exists but is not called from KYC services (needs Batch 6 confirmation) |
| SEC-12b | Client-supplied MIME only; no magic-byte inspection | M | `KycService.validateFile` lines 181-186 |
| KYC-LOG | Admin rejection reason logged (may contain PII) | L | `AdminKycService` log statement |
| KYC-RET | No retention/deletion mechanism; `deleteFile` never called | L | `FileStorageService.deleteFile` has no callers |

### 5.6 Database

| ID | Weakness | Sev | Evidence |
|---|---|---|---|
| DB-15 | **No `CHECK (balance >= 0)`** — negative balance prevented only in application code | H | schema audit; `wallets.balance` is `DECIMAL` NOT NULL DEFAULT 0.00 |
| DB-29 | No composite unique constraint preventing duplicate financial effects | H | relies on idempotency key + reference only |
| DB-18 | `V14` unique index on `idempotency_key` fails if historical duplicates exist | M | additive migration risk on populated DB |
| DB-19 | PostgreSQL unique index permits multiple NULLs on `idempotency_key` | M | NULL-multiplicity semantics |
| DB-20 | `audit_logs` has no DB-level immutability (update/delete permitted) | M | application-policy only |
| DB-14 | All FKs rely on default `RESTRICT`; behaviour undocumented | L | no explicit `ON DELETE` |
| DB-24/25 | Missing composite indexes for reconciliation and pending-payout queries | L | query-pattern analysis |
| DB-22 | `V13` widens `transactions.status` with no CHECK on allowed values | L | metadata-only change |
| — | **Monetary representation is `DECIMAL(15,2)`, not integer ngwee** | INFO | contradicts an assumption in the Phase 1C brief; mobile maps ngwee ↔ decimal |

### 5.7 Financial

| ID | Weakness | Sev | Evidence |
|---|---|---|---|
| FIN-09 | Percentage fee component rounded to 2dp before flat-fee addition and before min/max clamping | M | `FeeEngineService.java:51-53,56-68` — double rounding; sub-cent impact per transaction |
| FIN-11 | Unverified users exempt from daily-send and wallet limits | L | `TransactionLimitService.java:117-120` — early return for `kycStatus != APPROVED \|\| tier < 1`; hourly velocity caps still apply |
| R-01 | `AdminService.toggleUserDeletedStatus` mutates wallet status with no lock and no status precondition | H | `AdminService.java:84-88` (→ ACTIVE) and `:96-100` (→ FROZEN) via `findByUserId` + `save` |
| R-02 | `BalanceRecalculationJob` rewrites historical snapshots | M | contained in **all** profiles — `matchIfMissing=false`, only prod sets it (`false`) |
| R-03 | Account-number TOCTOU; collision surfaces as unhandled 500 | H | `AccountNumberGenerator.java:31-40` — advisory `existsByAccountNumber`, INSERT later in `WalletService` |

**Verified financial controls (must not regress):** `WalletService.freezeWallet`/`unfreezeWallet` take `findByIdWithLock` (`WalletService.java:74,89`); transfers use pessimistic locking with the conditional `balance + :amount >= 0` update; Phase 1B three-stage payout, `PENDING_RECONCILIATION` replay guard, and compensating credit are intact and green in the 243-test run.

---

## 6. Corrections Applied to Delegated Findings

Subagent reports contained material false positives. I did not carry them into scope. Recorded for auditability:

| Claim | Source | My finding |
|---|---|---|
| "4 CRITICAL admin cross-user authorization bypasses / IDOR" | authorization audit | **False positive.** `@EnableMethodSecurity` active; class-level `hasRole('ADMIN')` on all three admin controllers. Admin acting on any user is intended behaviour. Real gap is only the **single-role model**. |
| "KYC path traversal — HIGH" | KYC audit | **Not exploitable.** `loadFile` has `normalize().startsWith(uploadDir)`; `sanitizeFileName` strips `/` and `\`; filenames are UUID-prefixed. Defence-in-depth only. |
| "KYC upload → CRITICAL RCE" | KYC audit | **Overstated.** No static resource handler; uploads are never served or executed. Register already rates this Low (SEC-12). Re-rated **Medium**. |
| "PII in admin user listing — HIGH" | KYC audit | **Not an access-control defect.** Intended admin function; minimise, don't treat as HIGH. |
| "Rate limiting configured but not enforced" | ops audit | **False.** `RateLimitFilter` is `@Component`, `@Profile("!test")`, extends `OncePerRequestFilter`, returns 429 — auto-registered by Spring Boot. |
| "Access token valid after logout — HIGH" | auth audit | **Re-rated Medium.** Bounded 15-minute window is standard; still requires a decision under Batch 1 requirement 8. |
| `SEC-07` missing from Phase 1C list | prompt | **Resolved.** SEC-07 exists in the register and is deliberately out of scope. |
| "`JwtAuthFilter` ignores soft-delete / disabled / locked account state" | risk register (SEC-01) | **Narrowed.** Soft-delete is already enforced by `@SQLRestriction("deleted = false")`; no locked/disabled column exists. The real gap is `isEnabled()` = `emailVerified`, never checked → email verification gates nothing. Re-rated **Medium**; needs product policy (DEC-01). |
| "`LipilaGateway` lacks HTTP timeouts" | implied by ops scope | **False.** `CONNECT_TIMEOUT = 5s`, `READ_TIMEOUT = 10s` (`LipilaGateway.java:43-47,77-83`). |
| "`V14` unique index is a defect" | DB audit DB-18/19 | **Partly false.** `V14` already creates `idx_transactions_idempotency_key` UNIQUE; multiple NULLs are **required** for non-payout rows, so NULL multiplicity is intended. |

---

## 7. Read-Only Batch Integrity

Two subagents wrote files despite a read-only mandate:

| Artifact | Original path | Action |
|---|---|---|
| `phase1c-risk-extraction.md` | workspace root | Relocated to `/tmp/opencode/phase1c-batch0-artifacts/risk-extraction.md` |
| `schema-audit-report.md` | **inside `novawallet-api/`** | Relocated to `/tmp/opencode/phase1c-batch0-artifacts/db-schema-audit.md` |

Both were moved, not deleted. Repository verified clean afterwards: `git status --short` empty at `7975719`. No source, test, migration, or configuration file was changed during Batch 0.

---

## 8. Conclusion

The Phase 1B baseline is **sound and reproducible**: 243/243 tests green on a clean checkout, with the Phase 1A and 1B financial safeguards demonstrably in place.

The security posture is **not yet acceptable for a financial platform**, for four reasons that are specific and fixable:

1. **Session state is not authoritative** — account status (`enabled`, locked, deleted) is ignored on every request, and refresh tokens survive password changes and deactivation (SEC-01, SEC-02).
2. **Credentials leak through product surfaces** — reset and verification tokens are embedded in notification bodies readable through the API (SEC-03).
3. **Financial invariants rest on application code, not the database** — there is no `CHECK (balance >= 0)` and no composite uniqueness guaranteeing one financial effect (DB-15, DB-29).
4. **Deployment can silently run in dev mode** — `spring.profiles.active: dev` is the hardcoded default (SEC-04).

None of these require an architectural change, a new dependency, a new datastore, or a change to the balance source of truth. All are addressable inside Phase 1C's stated constraints.

**Proceed to `PHASE_1C_IMPLEMENTATION_PLAN.md` for remediation sequencing, per-batch file manifests, and the decisions that require human approval.**

---

## 9. Change Log

| Date | Change |
|---|---|
| 2026-11-12 | Initial Batch 0 security baseline. 8 specialist audits integrated; 7 claims overridden after independent verification; 2 read-only violations remediated. |
