# Project TODO — NovaWallet

## Current Objective
**Phase 1C Batch 1 — Authentication and Session Hardening.**
Resolve SEC-01, SEC-02, SEC-03, SEC-06, SEC-08 and NEW-AUTH-01 on branch
`feature/phase-1c-security-database-hardening` (starting HEAD `7975719`).
No commit, push, merge, rebase or deployment. Stop for human review.

## Project Context
- Repository (backend): `novawallet-api/` — Spring Boot 3.5.3 / Java 17 (release) / PostgreSQL / Flyway.
- Workspace root: `/home/ivwananji-silungwe/DEVELOPMENT/PROJECTS/NovaWallet` (not a git repo; backend subdir is).
- Backend branch: `feature/phase-1c-security-database-hardening`; HEAD `797571914b98d3b3206c5529e8eaf356ba79d1c8`.
- Baseline tests: **243** (H2, PostgreSQL mode). PostgreSQL 16.15 available via disposable Docker container.
- JDK 21 at `/home/ivwananji-silungwe/.jdks/jdk-21.0.12.1+1` — system `java` is JRE-only.
- Batch 0 reports are versioned into `novawallet-api/docs/phase-1c/` in this batch.

## Preparation Tasks
- [x] Verify repository state: branch, HEAD, clean tree, no Phase 1C commit.
- [x] Read Batch 0 reports (batch report, baseline, traceability matrix, implementation plan).
- [x] Read all in-scope source: JwtUtil, JwtAuthFilter, CustomUserDetailsService, AuthService, AuthController,
      TokenService, RefreshToken(+Repository), UserService, AdminService, NotificationService, Notification,
      NotificationResponse, MailService, SecurityConfig, application*.yml.
- [x] Map every money-movement entry point and every `updateBalance` call site.
- [x] Version the four Batch 0 reports into `novawallet-api/docs/phase-1c/`.
- [x] Create `docs/phase-1c/PHASE_1C_DECISION_LOG.md` (DEC-01, DEC-02, DEC-10, BLK-03, R-01/R-02/R-03, SEC-07 boundary).
- [x] Correct canonical risk register (`docs/architecture/backend-risk-register.md`): SEC-01 verified findings,
      restore `(R-02)` label on FIN-07, record R-01/R-03 meanings, add NEW-AUTH-01.
- [x] Update implementation plan: Batch 1 migration is `V16`; Batch 4 must use `V17`+.

## Stage A — JWT Hardening (SEC-08)
- [x] `JwtUtil`: startup key validation (blank / Base64 / >= 256-bit) with no key material in messages.
- [x] `JwtUtil`: pin HS256 on issuance and verification.
- [x] `JwtUtil`: add and validate configurable `iss`/`aud`; fail startup when blank.
- [x] `JwtUtil`: unique cryptographically random `jti` on every issued token.
- [x] Preserve `sub`, `email`, `role`, `iat`, `exp` claims.
- [x] Config: `jwt.issuer` / `jwt.audience` in base/dev/prod/test profiles + `.env.example` + docker-compose.
- [x] Document forced re-login (DEC-10).

## Stage B — Durable Access-Token Revocation (NEW-AUTH-01 / DEC-02)
- [x] `V16__create_revoked_access_tokens.sql` (jti PK, user FK, revoked_at, expires_at, indexes).
- [x] `RevokedAccessToken` entity + repository (`existsById`, bounded expired reads/deletes).
- [x] `AccessTokenRevocationService` (idempotent revoke, fail-closed lookup).
- [x] `JwtAuthFilter`: parse once, check jti revocation before setting Authentication, 503 on store failure.
- [x] `RevokedAccessTokenCleanupJob` (bounded, idempotent, concurrency-safe).
- [x] Document the single indexed lookup per authenticated request.

## Stage C — Account-State Enforcement (SEC-01 / DEC-01)
- [x] **Verified correction:** `requireVerifiedEmail` already exists at TransactionService (withdraw, transfer)
      and PayoutService (initiatePayout) since baseline `60179fb`. Batch 0's "gates nothing" claim is wrong.
- [x] Consolidate into one `EmailVerificationPolicy` choke point (single authoritative guard).
- [x] Apply to every money-movement path; prove no alternate entry point bypasses it.
- [x] Leave profile, KYC, notification and read-only operations available to unverified users.
- [x] Preserve disabled deposit and collections containment.

## Stage D — Refresh-Token Revocation (SEC-02)
- [x] `UserService.changePassword` revokes all active refresh tokens.
- [x] `AdminService.toggleUserDeletedStatus` revokes refresh tokens on deactivation only.
- [x] Preserve password-reset revocation and refresh-token rotation.
- [x] Do not touch wallet freeze/unfreeze concurrency logic (Batch 3 owns R-01/DEC-12).

## Stage E — Logout Endpoint (SEC-06)
- [x] `POST /api/v1/auth/logout`, authenticated, 204 No Content.
- [x] Revoke current access token (jti) + existing refresh-token logout behaviour.
- [x] Never accept a client-supplied user id.
- [x] OpenAPI annotations.

## Stage F — Notification and Token Exposure (SEC-03)
- [x] Remove raw verification/reset tokens from persisted notification bodies and API responses.
- [x] Hash verification/reset tokens at rest (SHA-256), compare hashes on presentation, keep single use.
- [x] Preserve password-reset expiry (1 hour). **Do not add email-verification expiry (SEC-07 stays OPEN).**
- [x] Redact tokens from every mail logging path including the SMTP-unconfigured path.
- [x] Keep verification and reset emails deliverable with the raw token.

## Testing
- [x] Test compilation verified (`AuthServiceTest`, `UserServiceTest`, `TransactionServiceTransferLockOrderTest`).
- [x] Token-hash tests updated (`EmailVerificationIntegrationTest`, `PasswordResetIntegrationTest`, `AuthServiceTest`).
- [x] New tests: JWT iss/aud/HS256/jti hardening; revoked-token rejection; logout endpoint; 503 fail-closed.
- [x] New tests: email-verification negative cases for withdraw/transfer/payout; unrestricted onboarding/read-only.
- [x] New tests: refresh-token revocation on password change and admin deactivation.
- [x] New tests: redaction of tokens from mail logs and notification API responses.
- [x] Full suite: `./mvnw -B clean test` — 316 tests, 0 failures, 0 errors, 0 skipped (baseline 243).
- [x] PostgreSQL 16: fresh V1–V16 and upgrade V15→V16 verified; targeted closeout suite 20 tests, 0 failures, 0 errors, 0 skipped.
- [x] `git diff --check` clean; secret scan of full diff completed.

## Delivery (authorized by project owner)
- [ ] Commit Batch 1 on `feature/phase-1c-security-database-hardening` after all verification passes.
- [ ] Push branch to `https://github.com/ivwananjisilungwe/NovaWallet` (owner explicitly authorized commit + push).
- [ ] Do **not** push until tests, reviews, PostgreSQL 16 verification, and the Batch 1 report are complete.

## Review
- [x] Independent code review completed (no blockers; documentation/idempotency notes applied).
- [x] Independent security review completed (no blockers; SEC-07 remains intentionally open).
- [x] Verify no Phase 1A / Phase 1B regression; no real provider request.

## Documentation
- [x] `docs/phase-1c/PHASE_1C_BATCH_1_REPORT.md` with required sections A–M.

## Completed (carried forward)
- [x] Phase 1A financial guardrails (commit `aac8737`).
- [x] Phase 1B Batch 2 / Batch 3 implementation and final acceptance gate (commit `7975719`).
- [x] Phase 1C Batch 0 — read-only baseline, risk matrix, implementation plan (no source changes).
- [x] PostgreSQL 16 migration verification V1–V15.

## Blocked / Non-blocking (carried forward)
- FLY-01: Flyway CLI checksum validation unavailable (non-blocking, pre-production).
- BLK-01: Lipila collections signing contract not evidenced in repository — Batch 5 (SEC-05) remains BLOCKED.
- BLK-04: no Java 17 runtime installed; Java 17 compatibility claimed at source/`--release 17` level only.
- SEC-07: email-verification token expiry — **out of Phase 1C scope, must stay OPEN.**
- Old access tokens remain valid until expiry (<= 15 min) after password change / admin deactivation —
      documented residual gap in the Batch 1 report.
- No commit/push/merge/deploy without explicit human authorization.
