# NovaWallet — Phase 1C Batch 1 Report

**Document:** `PHASE_1C_BATCH_1_REPORT.md`
**Phase:** 1C — Security and Database Hardening
**Batch:** 1 — Authentication and Session Hardening
**Branch:** `feature/phase-1c-security-database-hardening`
**Baseline HEAD:** `797571914b98d3b3206c5529e8eaf356ba79d1c8`
**Reported:** 2026-11-13
**Status:** COMPLETE — awaiting human review. No commit, push, merge, rebase or deployment performed.

---

## A. Batch 1 decision

**COMPLETE.**

All in-scope risks are implemented and verified:

- SEC-08 — JWT hardening.
- NEW-AUTH-01 — durable access-token revocation.
- SEC-01 — email-verification enforcement at money-movement boundaries.
- SEC-02 — refresh-token revocation on password change and admin deactivation.
- SEC-06 — authenticated logout endpoint.
- SEC-03 — notification/token exposure and redaction.

SEC-07 remains **OPEN and intentionally unchanged**, per the explicit scope boundary.

Closeout verification additionally confirmed and corrected:

- Logout is transactional across access-token and refresh-token revocation.
- Authentication failures return 401; authorization failures remain 403.
- Revocation-database unavailability returns 503.

---

## B. Decision record

| Decision | Resolution | Implementation |
|---|---|---|
| DEC-01 | Financial-only email-verification enforcement | `security/EmailVerificationPolicy`; applied in `TransactionService.withdraw`, `TransactionService.transfer`, `PayoutService.initiatePayout`; **not** applied in `JwtAuthFilter`. |
| DEC-02 | Database-backed access-token revocation | `V16__create_revoked_access_tokens.sql`; `RevokedAccessToken`; `RevokedAccessTokenRepository`; `AccessTokenRevocationService`; `JwtAuthFilter` check; `RevokedAccessTokenCleanupJob`; logout. |
| DEC-10 | Forced re-login accepted for `iss`/`aud` rollout | `JwtUtil` issues and requires `iss`/`aud`; no permissive fallback; configuration fail-fast. |
| BLK-03 | Canonical register is in-repo | `docs/architecture/backend-risk-register.md` updated; workspace-root copy untouched. |
| R-01/R-02/R-03 | Definitions recorded | Recorded in `docs/phase-1c/PHASE_1C_DECISION_LOG.md` §7; implementation deferred to Batch 3. |
| SEC-07 | Explicitly out of scope | No expiry column, migration or validation added. |

Full decision detail: `docs/phase-1c/PHASE_1C_DECISION_LOG.md`.

---

## C. JWT hardening (SEC-08)

Implemented in `security/JwtUtil.java`:

- Blank signing secret rejected at construction.
- Secret decoded as Base64; non-Base64 rejected.
- Minimum 256-bit key enforced via `Keys.hmacShaKeyFor` (WeakKeyException → IllegalStateException).
- No key material included in exception messages.
- Algorithm pinned to `HS256` on issuance (`.signWith(secretKey, Jwts.SIG.HS256)`) and verification (`.sig().clear().add(Jwts.SIG.HS256)` plus header algorithm check).
- Configurable `jwt.issuer` and `jwt.audience` added to issuance and required on verification.
- Unique cryptographically random `jti` (`UUID.randomUUID()`) on every token; required on parse.
- Existing claims preserved: `sub`, `email`, `role`, `iat`, `exp`.

Configuration:

- Base `application.yml`: `issuer: ${JWT_ISSUER}`, `audience: ${JWT_AUDIENCE}` (no defaults).
- `dev`: `novawallet-api` / `novawallet-client`.
- `prod`: `${JWT_ISSUER}` / `${JWT_AUDIENCE}` (fail-fast if unset).
- `test`: `novawallet-api-test` / `novawallet-client-test`.
- `.env.example` and `docker-compose.yml` updated.

Evidence:

- `JwtUtilTest` — correct claims accepted; wrong issuer/audience rejected; missing claims rejected; `alg=none`, non-HS256, short/blank/non-Base64 secret rejected; `jti` uniqueness.
- Forced re-login (DEC-10) is accepted and documented; pre-deployment tokens lack `jti` and are rejected as malformed.

---

## D. Access-token revocation (NEW-AUTH-01 / DEC-02)

Database design (`V16__create_revoked_access_tokens.sql`):

- `jti UUID` primary key.
- `user_id UUID NOT NULL` FK → `users(id)`.
- `revoked_at TIMESTAMP NOT NULL`.
- `expires_at TIMESTAMP NOT NULL`.
- Indexes: `idx_revoked_access_tokens_expires_at`, `idx_revoked_access_tokens_user_id`.
- Only the token identifier is stored; the bearer token is never persisted.

Runtime behaviour:

- `JwtAuthFilter` parses the token once, checks `AccessTokenRevocationService.isRevoked(jti)` (indexed PK read), and only then sets `Authentication` and exposes `JwtClaims` as a request attribute.
- Revoked token → request remains unauthenticated → 401 on protected endpoints.
- Revocation-store failure → filter fails closed with HTTP 503 and the standard `ErrorResponse` body; downstream chain is not invoked.
- `AccessTokenRevocationService.revoke(...)` runs in a `REQUIRES_NEW` transaction, pre-checks existence, and treats a concurrent duplicate insert (`DataIntegrityViolationException`) as already revoked.
- `RevokedAccessTokenCleanupJob` removes expired rows in bounded batches (1000 rows, max 20 batches), ordered by `expires_at, jti`; gated by `app.scheduler.revoked-token-cleanup.enabled` with `matchIfMissing = true`.

Closeout atomicity correction:

- `AuthService.logout` is now `@Transactional`.
- Access-token and refresh-token revocation now use current-transaction variants (`revokeInCurrentTransaction`, `revokeAllUserTokensInCurrentTransaction`).
- If either revocation fails, the transaction rolls back and no revocation is committed. The presented access token remains valid, so the client can retry logout with the same token.
- `LogoutFailureAtomicityIntegrationTest` verifies rollback, recovery via refresh token, and successful retry.

Evidence:

- `AccessTokenRevocationServiceTest`, `JwtAuthFilterFailClosedTest`, `V16RevokedAccessTokensMigrationTest`, `RevokedAccessTokenCleanupJobTest`.
- PostgreSQL 16 fresh and upgrade migration verification (Section J).
- `LogoutFailureAtomicityIntegrationTest` (1 test, passing).

---

## E. Email-verification enforcement (SEC-01 / DEC-01)

- Single choke point: `security/EmailVerificationPolicy.requireVerifiedForMoneyMovement(UUID)`.
- Applied to: `TransactionService.withdraw`, `TransactionService.transfer`, `PayoutService.initiatePayout`.
- Not applied globally in `JwtAuthFilter`; unverified users retain login, profile, balance reads, KYC, notifications, password change/reset.
- Disabled deposit and collections containment preserved.
- Guard uses authoritative persisted user state (`UserRepository.findById`), not a client-supplied flag.

Evidence:

- `EmailVerificationPolicyTest`.
- Money-movement negative tests for withdraw, transfer and payout (unverified → 403, no balance change, no transaction row, no provider call).
- Onboarding/read-only tests proving an unverified user can still read profile, read notifications and access KYC.

Residual risk (accepted): enforcement is call-site based; a future money-movement endpoint that omits the call would bypass it. No compile-time rule currently enforces it.

---

## F. Refresh-token revocation (SEC-02)

- `UserService.changePassword` calls `TokenService.revokeAllUserTokens(userId)` after persisting the new password hash.
- `AdminService.toggleUserDeletedStatus` calls the same method **only when the resulting state is `deleted = true`**; reactivation does not revoke tokens.
- Existing password-reset revocation (`AuthService.resetPassword`) unchanged.
- Wallet freeze/unfreeze concurrency logic in `AdminService` untouched (Batch 3 owns R-01/DEC-12).

Evidence:

- Refresh-token revocation tests for password change and admin deactivation.
- Reactivation test asserting tokens are not revoked.
- Existing rotation/reuse suite still passes.

Accepted gap (documented): old access tokens remain valid until expiry (≤ 15 minutes) after password change/deactivation. The access-token revocation mechanism addresses only the **presented** token at logout; password change does not enumerate live access tokens. This is recorded in `PHASE_1C_DECISION_LOG.md` §8.1.

---

## G. Logout endpoint (SEC-06)

- Route: `POST /api/v1/auth/logout`.
- Authenticated endpoint; returns `204 No Content`.
- `userId`, `jti` and expiry are read from the validated `JwtClaims` request attribute; no client-supplied user ID is accepted.
- `AuthService.logout(userId, accessTokenId, accessTokenExpiry)` is transactional and revokes the access token and all refresh tokens together.
- OpenAPI annotations describe 204/401/503.
- Missing, invalid or revoked access tokens return 401.
- Duplicate revocation is idempotent at the store level; a second HTTP logout with the same (now revoked) access token returns 401 because the token is indistinguishable from a missing one.
- If revocation fails, logout returns 500 and no revocation is committed.

Evidence:

- `LogoutIntegrationTest` — 204 on success; same access token rejected afterwards; refresh tokens rejected; missing token → 401; duplicate logout → 401; user A cannot affect user B.
- `LogoutFailureAtomicityIntegrationTest` — failed refresh revocation rolls back access revocation; recovery via refresh token succeeds.

---

## H. Notification security (SEC-03)

- Verification and password-reset tokens are generated as raw values, persisted only as SHA-256 hashes (`TokenService.hashToken`), and compared by hash on presentation.
- `AuthService.verifyEmail` and `AuthService.resetPassword` hash the presented token and look up by hash; successful use clears the token (single use).
- Password-reset expiry preserved (1 hour).
- Notification bodies contain no raw token: registration records `"Please verify your email address."`; forgot-password records `"A password reset was requested for your account."`.
- `NotificationResponse.from` applies `SensitiveDataRedactor.redact(...)` to subject and message as defence in depth.
- Mail logging redacts the body in the no-SMTP path and redacts exception messages on send failure.
- Raw token exists only in the delivered email.

SEC-07 boundary:

- No `verificationTokenExpiresAt` column, migration or validation was added.
- `AuthService.verifyEmail` semantics unchanged.
- SEC-07 remains **OPEN** in the risk register.

Evidence:

- `SensitiveDataRedactorTest`, `NotificationResponseTest`, `MailServiceRedactionTest`.
- Registration/verification and forgot/reset integration tests rewritten to read the raw token from the captured email and assert the stored value is a 44-character SHA-256 Base64 hash.
- Captured logs asserted to contain no raw token.

---

## I. Authentication HTTP status contract

Implemented by `security/RestAuthenticationEntryPoint` and configured in `SecurityConfig`.

| Condition | HTTP status |
|---|---|
| Missing authentication | 401 |
| Expired access token | 401 |
| Invalid JWT signature | 401 |
| Invalid issuer/audience | 401 |
| Revoked access token | 401 |
| Authenticated user lacks permission | 403 |
| Revocation database unavailable | 503 |

Authorization failures remain 403. Revocation-store failures remain 503. Logout failure returns 500.

---

## J. Regression results

- Baseline: 243 tests, 0 failures, 0 errors, 0 skipped.
- Batch 1 (H2): **316 tests, 0 failures, 0 errors, 0 skipped** — BUILD SUCCESS.
- Full command: `JAVA_HOME=/home/ivwananji-silungwe/.jdks/jdk-21.0.12.1+1 ./mvnw -B clean test`.
- `git diff --check`: clean.
- No real provider request was made.
- Phase 1A transfer locking and non-negative conditional balance update unchanged.
- Phase 1B payout lifecycle, compensating-credit, webhook HMAC, timestamp validation, deduplication, reconciliation lease/backoff and parked-payout protection unchanged.

---

## K. PostgreSQL evidence

Disposable PostgreSQL 16.15 instance (`postgres:16`, host port 5433, database created for verification only; not the original database).

Closeout targeted verification:

- Command: `./mvnw -B test -Dtest='V16RevokedAccessTokensMigrationTest,AccessTokenRevocationServiceTest,LogoutIntegrationTest,LogoutFailureAtomicityIntegrationTest,SessionRevocationIntegrationTest'` with datasource overrides to `localhost:5433`.
- Result: **20 tests, 0 failures, 0 errors, 0 skipped** — BUILD SUCCESS.
- Flyway applied `V1`–`V16` successfully on real PostgreSQL 16.
- `revoked_access_tokens` table present with primary key, foreign key and both secondary indexes.
- Disposable container stopped and removed after verification.

---

## L. Complete Git inventory

No staging, commit, push, merge, rebase or history rewrite was performed.

Tracked modified files (29):

1. `.env.example`
2. `docker-compose.yml`
3. `docs/architecture/backend-risk-register.md`
4. `src/main/java/com/novawallet/novawallet_api/admin/service/AdminService.java`
5. `src/main/java/com/novawallet/novawallet_api/auth/controller/AuthController.java`
6. `src/main/java/com/novawallet/novawallet_api/auth/service/AuthService.java`
7. `src/main/java/com/novawallet/novawallet_api/config/SecurityConfig.java`
8. `src/main/java/com/novawallet/novawallet_api/notification/MailService.java`
9. `src/main/java/com/novawallet/novawallet_api/notification/dto/NotificationResponse.java`
10. `src/main/java/com/novawallet/novawallet_api/payment/service/PayoutService.java`
11. `src/main/java/com/novawallet/novawallet_api/security/JwtAuthFilter.java`
12. `src/main/java/com/novawallet/novawallet_api/security/JwtUtil.java`
13. `src/main/java/com/novawallet/novawallet_api/token/service/TokenService.java`
14. `src/main/java/com/novawallet/novawallet_api/transaction/service/TransactionService.java`
15. `src/main/java/com/novawallet/novawallet_api/user/service/UserService.java`
16. `src/main/resources/application-dev.yml`
17. `src/main/resources/application-prod.yml`
18. `src/main/resources/application.yml`
19. `src/test/java/com/novawallet/novawallet_api/auth/controller/AdminSecurityIntegrationTest.java`
20. `src/test/java/com/novawallet/novawallet_api/auth/controller/EmailVerificationIntegrationTest.java`
21. `src/test/java/com/novawallet/novawallet_api/auth/controller/FullAuthLifecycleIntegrationTest.java`
22. `src/test/java/com/novawallet/novawallet_api/auth/controller/PasswordResetIntegrationTest.java`
23. `src/test/java/com/novawallet/novawallet_api/auth/controller/PinValidationIntegrationTest.java`
24. `src/test/java/com/novawallet/novawallet_api/auth/service/AuthServiceTest.java`
25. `src/test/java/com/novawallet/novawallet_api/endpoint/PayoutEndpointIntegrationTest.java`
26. `src/test/java/com/novawallet/novawallet_api/notification/schedule/BalanceRecalculationJobDisabledInProdTest.java`
27. `src/test/java/com/novawallet/novawallet_api/transaction/service/TransactionServiceTransferLockOrderTest.java`
28. `src/test/java/com/novawallet/novawallet_api/user/service/UserServiceTest.java`
29. `src/test/resources/application-test.yml`

Staged files (0): none.

Untracked files (27):

1. `docs/phase-1c/PHASE_1C_BATCH_0_REPORT.md`
2. `docs/phase-1c/PHASE_1C_BATCH_1_REPORT.md`
3. `docs/phase-1c/PHASE_1C_DECISION_LOG.md`
4. `docs/phase-1c/PHASE_1C_IMPLEMENTATION_PLAN.md`
5. `docs/phase-1c/PHASE_1C_RISK_TRACEABILITY_MATRIX.md`
6. `docs/phase-1c/PHASE_1C_SECURITY_BASELINE.md`
7. `src/main/java/com/novawallet/novawallet_api/security/EmailVerificationPolicy.java`
8. `src/main/java/com/novawallet/novawallet_api/security/JwtClaims.java`
9. `src/main/java/com/novawallet/novawallet_api/security/RestAuthenticationEntryPoint.java`
10. `src/main/java/com/novawallet/novawallet_api/security/SensitiveDataRedactor.java`
11. `src/main/java/com/novawallet/novawallet_api/token/entity/RevokedAccessToken.java`
12. `src/main/java/com/novawallet/novawallet_api/token/repository/RevokedAccessTokenRepository.java`
13. `src/main/java/com/novawallet/novawallet_api/token/schedule/RevokedAccessTokenCleanupJob.java`
14. `src/main/java/com/novawallet/novawallet_api/token/service/AccessTokenRevocationService.java`
15. `src/main/resources/db/migration/V16__create_revoked_access_tokens.sql`
16. `src/test/java/com/novawallet/novawallet_api/auth/controller/LogoutFailureAtomicityIntegrationTest.java`
17. `src/test/java/com/novawallet/novawallet_api/auth/controller/LogoutIntegrationTest.java`
18. `src/test/java/com/novawallet/novawallet_api/endpoint/SessionRevocationIntegrationTest.java`
19. `src/test/java/com/novawallet/novawallet_api/notification/MailServiceRedactionTest.java`
20. `src/test/java/com/novawallet/novawallet_api/notification/dto/NotificationResponseTest.java`
21. `src/test/java/com/novawallet/novawallet_api/security/EmailVerificationPolicyTest.java`
22. `src/test/java/com/novawallet/novawallet_api/security/JwtAuthFilterFailClosedTest.java`
23. `src/test/java/com/novawallet/novawallet_api/security/JwtUtilTest.java`
24. `src/test/java/com/novawallet/novawallet_api/security/SensitiveDataRedactorTest.java`
25. `src/test/java/com/novawallet/novawallet_api/token/migration/V16RevokedAccessTokensMigrationTest.java`
26. `src/test/java/com/novawallet/novawallet_api/token/schedule/RevokedAccessTokenCleanupJobTest.java`
27. `src/test/java/com/novawallet/novawallet_api/token/service/AccessTokenRevocationServiceTest.java`

Total changed paths: 56. `git diff --check`: clean.

---

## M. Remaining risks

- SEC-07 (email-verification token expiry) — OPEN, intentionally out of scope.
- DEC-04 (R-01/R-03 identifier confirmation) — deferred to Batch 3.
- DEC-05 (DB-29 duplicate-financial-effect design) — deferred to Batch 4.
- DEC-03, DEC-06, DEC-07, DEC-08, DEC-09, DEC-11, DEC-12 — unchanged, assigned to later batches.
- SEC-04, SEC-09, SEC-10, SEC-11, SEC-12 — out of Batch 1 scope.
- FIN-09, FIN-11 — Batch 3.
- R-01, R-02, R-03 — Batch 3.
- Database constraints — Batch 4.
- Collections signing contract (SEC-05/BLK-01) — Batch 5.
- Accepted residual limitations per `PHASE_1C_DECISION_LOG.md` §8:
  - Cross-session access-token window (≤ 15 minutes) after password change/deactivation.
  - Per-request indexed revocation read (no cache).
  - Call-site-based email-verification enforcement (no compile-time rule).
  - Redactor may mask legitimate long identifiers in notification text (cosmetic).
  - Mail copy still says "expires in 24 hours" while SEC-07 remains open (pre-existing).

---

## N. Recommendation

Batch 1 is **ready for human review and acceptance**. All in-scope tests pass, PostgreSQL 16 fresh and upgrade migrations are verified, closeout atomicity and HTTP-status corrections are implemented and tested, and independent reviews found no blockers. Batch 2 should **not** begin until a human reviewer approves this batch and authorizes the next scope.

No commit, push, merge, rebase or deployment was performed.