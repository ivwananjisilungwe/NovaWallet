# Phase 1C — Decision Log

**Document:** `PHASE_1C_DECISION_LOG.md`
**Phase:** Phase 1C — Security & Database Hardening
**Branch:** `feature/phase-1c-security-database-hardening`
**Baseline HEAD:** `797571914b98d3b3206c5529e8eaf356ba79d1c8`
**Recorded:** 2026-11-13
**Authority:** Decisions are made by the project owner. This log records what was decided, why, how it was implemented, and what remains accepted or deferred. It does not grant approval for anything not written here.

---

## 1. Purpose

Phase 1C's implementation plan (`PHASE_1C_IMPLEMENTATION_PLAN.md` §12) identifies decisions that an implementing engineer must not guess. This log is the authoritative record of the decisions taken before and during **Batch 1 (Authentication and Session Hardening)**.

Scope of this log:

| Kind | Covered here |
|---|---|
| Decisions resolved for Batch 1 | DEC-01, DEC-02, DEC-10 |
| Documentation blocker resolved for Batch 1 | BLK-03 |
| Identifiers clarified for Batch 1 traceability | R-01, R-02, R-03 |
| Explicit scope exclusion | SEC-07 |
| Decisions deliberately deferred | DEC-04, DEC-05 |
| Accepted residual limitations | §8 |

All other decisions in the plan (`DEC-03`, `DEC-06` … `DEC-09`, `DEC-11`, `DEC-12`) belong to later batches and are unchanged by this document.

---

## 2. Decision Summary

| ID | Subject | Resolution | Batch | Status |
|---|---|---|---|---|
| DEC-01 | Email-verification enforcement policy | **Financial-only enforcement.** Never enforce globally in the JWT filter. | 1 | **IMPLEMENTED** |
| DEC-02 | Access-token revocation after logout | **DB-backed revocation store.** No Redis, no in-memory deny-list. Fail closed with 503 if the store is unavailable. | 1 | **IMPLEMENTED** |
| DEC-10 | Forced re-login on `iss`/`aud` rollout | **Accept forced re-login.** Add and validate `iss`/`aud` with no permanently permissive fallback. | 1 | **IMPLEMENTED** |
| BLK-03 | Canonical risk-register location | **`novawallet-api/docs/architecture/backend-risk-register.md` is canonical.** | 1 | **RESOLVED** |
| R-01 | Admin wallet-status mutation | Defined as `AdminService.toggleUserDeletedStatus` unanchored wallet mutation. | 3 (deferred) | **DEFINED** |
| R-02 | Balance snapshot rewrite | Defined as `BalanceRecalculationJob` historical-snapshot rewrite (FIN-07). | 3 (deferred) | **DEFINED** |
| R-03 | Account-number generation TOCTOU | Defined as `AccountNumberGenerator` collision race. | 3 (deferred) | **DEFINED** |
| SEC-07 | Email-verification token expiry | **Explicitly out of scope for Batch 1.** No expiry added; semantics unchanged. | — | **DEFERRED** |
| DEC-04 | R-01/R-03 identifier confirmation | Owner confirmed the proposed meanings (2026-11-14). | 3 | **DECIDED** |
| DEC-05 | DB-29 duplicate-financial-effect design | Not designed in Batch 1. | 4 | **DEFERRED** |

---

## 3. DEC-01 — Email-Verification Enforcement Policy

### 3.1 Decision

Email verification is enforced **only at money-movement boundaries**, never globally:

- Enforcement point: `security/EmailVerificationPolicy.requireVerifiedForMoneyMovement(UUID)`.
- Applied at: `TransactionService.withdraw`, `TransactionService.transfer`, `PayoutService.initiatePayout`.
- **Not** applied in `JwtAuthFilter`, `SecurityConfig`, or any global authentication path.

An authenticated but unverified user must retain access to:

- login and token refresh;
- profile and balance reads;
- KYC document submission and status;
- notifications;
- password change and password reset.

### 3.2 Options considered

| Option | Description | Rejected because |
|---|---|---|
| **A — Global enforcement** | Reject every request from an unverified user in `JwtAuthFilter` / Spring Security. | Locks every new user out of onboarding immediately after registration. An unverified user could not submit KYC, read their profile, or even receive the verification notification. Self-defeating. |
| **B — Financial-only enforcement** | Enforce at each money-movement service boundary. | **Chosen.** Matches the intent of the `User` entity documentation and preserves the onboarding funnel while still blocking value transfer. |
| **C — Accept the risk** | No enforcement. | Leaves unverified identities able to move money; contradicts the existing guard and the Phase 1C objective. |

### 3.3 Correction to the Batch 0 baseline

The Batch 0 report states that email verification "gates nothing". **That statement is factually wrong and is corrected here.**

Verified against baseline commit `60179fb` and the current tree:

- `TransactionService.withdraw` already called a private `requireVerifiedEmail(userId)`.
- `TransactionService.transfer` already called the same private helper.
- `PayoutService.initiatePayout` already carried its own copy of the check.

The actual defect was therefore **duplication and missing negative-test coverage**, not absent gating. Three private copies meant a new money-movement entry point could silently omit the rule, and no test proved that an unverified user is rejected.

Batch 1 corrects this by consolidating all three copies into one shared policy component and deleting the private helpers. The user-facing message and HTTP status (`403 Forbidden`) are unchanged.

### 3.4 Rationale for consolidation

- One authoritative implementation means the rule cannot drift between services.
- A dedicated component is directly unit-testable without constructing a full service.
- The component is injectable, so a future money-movement service must depend on it explicitly.

### 3.5 Consequences and residual risk

- **Positive:** unverified users can complete onboarding; money movement is blocked uniformly; the negative-test gap is closed.
- **Residual:** enforcement is call-site based. A future endpoint that moves money but forgets to call the policy would bypass it. There is no compile-time or architectural rule forcing the call. Mitigation: the policy is the documented single entry point, all current money-movement paths call it, and Batch 1 adds negative tests for each path. A future ArchUnit rule is a reasonable follow-up but is not part of Batch 1.
- **Deposit:** `POST /api/v1/wallets/{walletId}/deposit` remains contained by the FIN-01 503 guard, so there is no user-principal credit path to gate. The collections webhook has no user principal and remains contained by FIN-02.
- **Disabled states:** the `users` table has no `locked`/`disabled` column and `UserDetails.isEnabled()` maps to `emailVerified`. DEC-01 deliberately keeps `isEnabled()` out of authentication decisions; account deactivation is handled by the soft-delete filter and by refresh-token revocation (SEC-02).

### 3.6 Verification

| Test | Proves |
|---|---|
| `EmailVerificationPolicyTest` | Unverified → `ForbiddenException`; verified → no exception; unknown user → `ResourceNotFoundException`. |
| Money-movement negative tests (withdraw, transfer, payout) | Unverified user receives 403 and no balance change, no transaction row, no provider call. |
| Onboarding/read-only tests | Unverified user can still read profile, read notifications, and access KYC. |

---

## 4. DEC-02 — Durable Access-Token Revocation

### 4.1 Decision

Access-token revocation is **database-backed**:

- New table `revoked_access_tokens` (migration `V16__create_revoked_access_tokens.sql`).
- The JWT carries a unique `jti`; the `jti` is the table's primary key.
- `JwtAuthFilter` checks the revocation store on **every authenticated request**, after signature/claim validation and before setting the `Authentication`.
- `POST /api/v1/auth/logout` revokes the presented access token and all of the user's refresh tokens.
- If the revocation store cannot be queried, the filter **fails closed with HTTP 503**.

This **supersedes the implementation plan's default recommendation** (`jti` + in-memory deny-list). The plan's recommendation was explicitly conditional on the single-instance limitation being acceptable; the project owner rejected that limitation.

### 4.2 Options considered

| Option | Description | Rejected because |
|---|---|---|
| **A — In-memory deny-list** | `ConcurrentHashMap<jti, expiry>` per JVM. | Lost on restart; not shared across instances. A token revoked on instance A is still accepted by instance B. Fails the durability requirement. |
| **B — Accept the 15-minute window** | No server-side revocation; rely on short TTL. | Does not satisfy SEC-06's requirement that logout terminate the session. A stolen access token remains usable for up to 15 minutes after logout. |
| **C — Redis / external cache** | Shared TTL store. | Explicitly not permitted in this phase. |
| **D — DB-backed table** | Persist revoked `jti` rows until natural expiry. | **Chosen.** Durable across restarts, shared by all instances, no new infrastructure, and indexable. |

### 4.3 Schema

`V16__create_revoked_access_tokens.sql`:

| Column | Type | Notes |
|---|---|---|
| `jti` | `UUID` | Primary key. Makes the per-request lookup an indexed point read and makes duplicate writes idempotent. |
| `user_id` | `UUID` | `NOT NULL`, FK → `users(id)`. Supports operational forensics. |
| `revoked_at` | `TIMESTAMP` | UTC. When the revocation was recorded. |
| `expires_at` | `TIMESTAMP` | UTC. Mirrors the token's `exp` claim; the row is purgeable once this passes. |

Indexes:

- `idx_revoked_access_tokens_expires_at` on `expires_at` — supports the bounded cleanup job.
- `idx_revoked_access_tokens_user_id` on `user_id` — supports per-user forensics.

Migration numbering: **Batch 1 owns `V16`.** Batch 4 must start at `V17` or later. Migrations `V1`–`V15` are untouched.

### 4.4 Deliberate design choices

| Choice | Reason |
|---|---|
| Store only `jti`, never the bearer token | A database or backup leak must not yield usable credentials. |
| `jti` as primary key | Point lookup; the primary key also rejects duplicate inserts, giving idempotency for free. |
| `revoke(...)` in `REQUIRES_NEW` | Logout succeeds even when called inside an outer transaction that later rolls back; a concurrent duplicate insert cannot poison the caller's transaction. |
| Idempotent revoke | Pre-check `existsById`; a concurrent duplicate is caught as `DataIntegrityViolationException` outside the transaction and treated as "already revoked". The store-level write is idempotent; a second HTTP logout with the same (now revoked) access token returns 403 (unauthenticated), not 204 or 500, and never corrupts state. |
| Fail closed (503) | A revocation-store outage must never convert into "revoked tokens are accepted". 503 is truthful: authentication is temporarily unavailable. |
| Bounded cleanup job | Deletes only expired rows, in bounded batches, ordered by `expires_at, jti`; safe to run on every instance. Gated by `app.scheduler.revoked-token-cleanup.enabled` with `matchIfMissing = true`. |
| LocalDateTime (UTC) | Matches the existing entity convention and `ddl-auto: validate` against the `TIMESTAMP` columns. |

### 4.5 Logout ordering

`AuthService.logout` is transactional. It revokes the presented access token and all refresh tokens in the **same transaction**.

- If either revocation fails, the transaction rolls back and no revocation is committed. The presented access token remains valid, so the client can retry logout with the same token.
- If refresh revocation were committed independently while access revocation failed, the client would lose its recovery path; the transactional design prevents that.
- If access revocation were committed independently while refresh revocation failed, the presented access token would be dead and recovery would require a refresh token; the transactional design prevents that partial state.

Logout returns `204 No Content` only after both revocation writes commit. It accepts no client-supplied user identifier: `userId`, `jti` and expiry are read from the validated `JwtClaims` request attribute set by `JwtAuthFilter`.

### 4.6 Consequences and residual risk

- **Cost:** one indexed primary-key read per authenticated request. This is the deliberate price of durable revocation. It is not cached, because caching would reintroduce the staleness that DEC-02 rejects.
- **Cross-session asymmetry:** logout revokes **all refresh tokens** for the user (existing `TokenService.revokeAllUserTokens` semantics) but only the **presented access token**. Other devices' access tokens remain valid for up to their remaining 15-minute lifetime. See §8.1.
- **Availability coupling:** an outage of the `revoked_access_tokens` table makes authenticated endpoints return 503. This is intended fail-closed behaviour, not a defect. It must be reflected in operational runbooks.
- **Pre-deployment tokens:** tokens issued before this deployment carry no `jti` and are rejected by `JwtUtil.parseToken` as malformed. Combined with DEC-10, all pre-deployment sessions require re-login. There is therefore no population of legacy, unrevokable access tokens after deployment.
- **Table growth:** bounded by the cleanup job. Rows live at most one access-token lifetime (15 minutes) beyond revocation.

### 4.7 Verification

| Test | Proves |
|---|---|
| `AccessTokenRevocationServiceTest` | Insert, idempotent duplicate, concurrent duplicate, unknown-jti lookup, expired-row handling. |
| `LogoutIntegrationTest` | Logout returns 204; the same access token is rejected afterwards; refresh tokens are rejected; missing or revoked access tokens return 401. |
| `JwtAuthFilterFailClosedTest` | Revocation-store failure returns 503 with the standard error body; no `Authentication` is set; the downstream chain is not invoked. |
| `V16RevokedAccessTokensMigrationTest` | Migration applies; PK, FK and indexes exist; UTC round-trip is correct. |
| PostgreSQL 16 verification | Fresh `V1`–`V16` and upgrade `V15`→`V16` on a disposable instance. |

---

## 5. DEC-10 — Issuer/Audience Validation and Forced Re-Login

### 5.1 Decision

- `JwtUtil` **issues** `iss` and `aud` on every access token.
- `JwtUtil` **requires** both on verification (`requireIssuer`, `requireAudience`).
- There is **no permissive fallback**: no configuration switch that disables the checks and no "accept if absent" branch.
- Adding and enforcing these claims invalidates every token issued before deployment. **Forced re-login is accepted.** This is pre-production software; session continuity across a security hardening release is not a requirement.

### 5.2 Rationale

Without `iss`, a token minted by any other service sharing the same signing key authenticates here. Without `aud`, a token minted for a different audience is accepted. Both are token-confusion paths. Optional validation would be permanently left off in practice, so it is not offered.

### 5.3 Configuration

| Profile | `jwt.issuer` | `jwt.audience` |
|---|---|---|
| base `application.yml` | `${JWT_ISSUER}` — no default | `${JWT_AUDIENCE}` — no default |
| `dev` | `novawallet-api` | `novawallet-client` |
| `prod` | `${JWT_ISSUER}` / `${JWT_AUDIENCE}` — must be supplied | same |
| `test` | `novawallet-api-test` | `novawallet-client-test` |

The base configuration intentionally has **no default**. A production deployment that omits either variable fails at startup rather than silently issuing unvalidated tokens. `.env.example` and `docker-compose.yml` were updated accordingly.

### 5.4 Consequences

- Every existing session is terminated at deployment. Clients must handle a 401 on refresh by sending the user to login. This is expected, not a regression.
- Password-reset and email-verification tokens are **not** JWTs and are unaffected by `iss`/`aud`.
- Key rotation is still manual and undocumented; `JwtUtil` validates strength at startup but does not support overlapping keys. Recorded as a known limitation, not a Batch 1 defect.

### 5.5 Verification

| Test | Proves |
|---|---|
| `JwtUtilTest` | Correct `iss`/`aud` accepted; wrong `iss` rejected; wrong `aud` rejected; missing `iss`/`aud` rejected; `alg=none` rejected; non-HS256 rejected; short/blank/non-Base64 secret fails construction. |
| `JwtAuthFilter` tests | Token with wrong issuer/audience leaves the request unauthenticated → 401. |

---

## 6. BLK-03 — Canonical Risk Register

### 6.1 Decision

**`novawallet-api/docs/architecture/backend-risk-register.md` is the single canonical risk register.**

The copy under the workspace-root `reports/` directory is a historical Phase 0 artefact. It is **not** updated, and no new `novawallet-api/reports/` directory is created.

### 6.2 Rationale

- The register lives next to the code it describes and is versioned with it.
- Two writable copies caused the `DOC-SOT-01` divergence recorded in Batch 0.
- The workspace root is not a Git repository; a document there has no history.

### 6.3 Consequences

- Batch 1 corrects the in-repo register only.
- Later batches update the same file.
- The workspace-root copies remain untouched as historical evidence.
- The register now carries a header note stating its canonical status and update convention.

---

## 7. Identifier Definitions — R-01, R-02, R-03

Batch 0 used `R-01`/`R-02`/`R-03` in the traceability matrix but the canonical register never defined them. They are defined here so traceability is unambiguous. **All three are Batch 3 work** unless stated otherwise.

| ID | Definition | Location | Severity | Batch | Cross-reference |
|---|---|---|---|---|---|
| **R-01** | `AdminService.toggleUserDeletedStatus` mutates wallet status with no pessimistic lock and no status precondition. Deactivating a user can leave the wallet in an inconsistent state; restoring a user unconditionally unfreezes the wallet and can clear a fraud freeze. | `admin/service/AdminService.java` | High | 3 | Related to DEC-12 |
| **R-02** | `BalanceRecalculationJob` rewrites missing historical `balance_before`/`balance_after` snapshots with the wallet's **current** balance, destroying historical accuracy. Currently **contained** (bean absent in `prod`), not corrected. | `notification/schedule/BalanceRecalculationJob.java` | Medium | 3 | Same defect as **FIN-07** |
| **R-03** | `AccountNumberGenerator` has a time-of-check/time-of-use race: uniqueness is checked before insert, and a collision surfaces as an unhandled 500 rather than a retry or a domain error. | `account/service/AccountNumberGenerator.java` | High | 3 | Related to DB uniqueness work (DEC-05) |

Notes:

- `R-02` and `FIN-07` are the **same** risk. The register's FIN-07 entry previously carried a `(R-02)` label; Batch 1 restores it and records the equivalence.
- Batch 3 owns the corrections. Batch 1 does not modify `AdminService`'s wallet freeze/unfreeze logic, add locks, or add status preconditions. Batch 1's only `AdminService` change is refresh-token revocation on deactivation (SEC-02).
- Formal confirmation of these mappings is **DEC-04**, which remains open. The defects themselves are confirmed regardless of the identifier mapping.

---

## 8. Accepted Residual Limitations

### 8.1 Cross-session access-token window

Logout revokes the presented access token immediately and all refresh tokens immediately. Access tokens held by **other** active sessions of the same user are not enumerated and revoked; they remain valid for at most their remaining lifetime (≤ 15 minutes).

**Why accepted:** enumerating a user's live access tokens would require persisting every issued token, which contradicts the stateless-JWT design and DEC-02's "store only revoked identifiers" rule. The refresh-token revocation prevents those sessions from extending beyond the current access-token lifetime.

**Operational implication:** a "log out all devices" feature would need either per-session token families or a user-level revocation cut-off. Neither is in Batch 1.

### 8.2 Revocation-store availability

If `revoked_access_tokens` is unavailable, authenticated requests return 503. This is deliberate (fail closed). Availability of authentication now depends on the primary database, which it already did for user lookup.

### 8.3 Per-request revocation read

Every authenticated request performs one indexed primary-key read. No cache is used, because a cache would reintroduce the staleness DEC-02 rejects. If this becomes a measured bottleneck, the correct next step is a short-TTL negative cache with an explicit revocation-propagation budget — a new decision, not a Batch 1 change.

### 8.4 Pre-existing verification/reset tokens invalidated

Verification and password-reset tokens are now stored as SHA-256 hashes (SEC-03). Tokens generated before deployment were stored in plaintext and cannot be matched against a hash, so they stop working.

**Why accepted:** the alternative is a plaintext-compatible fallback that keeps the vulnerability alive. This is consistent with DEC-10's forced re-login: the deployment is pre-production and all sessions are reset anyway. Affected users re-request a verification email or a password reset.

### 8.5 Email-verification link text vs behaviour

`MailService.sendVerificationEmail` still tells the user "This link expires in 24 hours", but email-verification tokens have no expiry (SEC-07). This is a **pre-existing** inconsistency and is **not** changed in Batch 1. It is recorded here and in the risk register so it is not mistaken for a Batch 1 regression. Correcting the text or adding expiry belongs to the SEC-07 decision.

---

## 9. SEC-07 — Explicit Scope Exclusion

### 9.1 Decision

**Email-verification token expiry is out of scope for Phase 1C Batch 1.** Batch 1:

- does **not** add a `verificationTokenExpiresAt` column;
- does **not** add a migration for it;
- does **not** change the semantics of `AuthService.verifyEmail`;
- does **not** change the verification email's expiry wording.

### 9.2 Why it is called out explicitly

SEC-03's correction (hash tokens at rest) and SEC-07 (add expiry) touch the same field and the same flows. It would be easy for a reviewer to assume Batch 1 silently resolved SEC-07. It did not. SEC-07 remains **OPEN** in the risk register with this boundary recorded.

### 9.3 Interaction with SEC-03

Hashing tokens at rest removes the plaintext-leak path but does **not** bound the token's lifetime. A hashed token that leaks is still usable forever. SEC-03 and SEC-07 are therefore independent: Batch 1 resolves the storage/disclosure defect, and SEC-07 remains a real, unmitigated weakness.

### 9.4 Recommendation for the SEC-07 owner

Add `verificationTokenExpiresAt`, set it at registration (a 24-hour window matches the existing email copy), reject expired tokens with `410 Gone` or `400 Bad Request`, and add a negative test. This needs a migration after `V16` (i.e. `V17` or later if Batch 4 has not already claimed it).

---

## 10. Deferred Decisions

### 10.1 DEC-04 — R-01/R-03 identifier confirmation

The definitions in §7 are the working mapping. Formal sign-off is still required before Batch 3 closes `R-01` and `R-03`. Batch 1 records the definitions so the traceability matrix is readable; it does not claim the identifiers are confirmed.

### 10.2 DEC-05 — DB-29 duplicate-financial-effect design

Not addressed in Batch 1. No composite unique constraint on amount/reference was added, and none should be added without a designed idempotency scope. Batch 4 owns this.

### 10.3 Other decisions

`DEC-03`, `DEC-06`, `DEC-07`, `DEC-08`, `DEC-09`, `DEC-11` and `DEC-12` are unchanged and remain with their assigned batches.

---

## 11. Implementation Traceability

| Decision | Primary code | Migration | Configuration |
|---|---|---|---|
| DEC-01 | `security/EmailVerificationPolicy.java`; `TransactionService`; `PayoutService` | — | — |
| DEC-02 | `security/JwtClaims.java`; `security/JwtUtil.java`; `security/JwtAuthFilter.java`; `token/entity/RevokedAccessToken.java`; `token/repository/RevokedAccessTokenRepository.java`; `token/service/AccessTokenRevocationService.java`; `token/schedule/RevokedAccessTokenCleanupJob.java`; `AuthService.logout`; `AuthController.logout` | `V16__create_revoked_access_tokens.sql` | `app.scheduler.revoked-token-cleanup.enabled` |
| DEC-10 | `security/JwtUtil.java` | — | `jwt.issuer`, `jwt.audience` in base/dev/prod/test, `.env.example`, `docker-compose.yml` |
| SEC-03 (context) | `token/service/TokenService.java`; `auth/service/AuthService.java`; `notification/MailService.java`; `notification/dto/NotificationResponse.java`; `security/SensitiveDataRedactor.java` | — | — |

---

## 12. Change Log

| Date | Change | Author |
|---|---|---|
| 2026-11-13 | Initial decision log for Phase 1C Batch 1: DEC-01, DEC-02, DEC-10, BLK-03, R-01/R-02/R-03 definitions, SEC-07 boundary, deferred decisions, accepted limitations. | Forge |

---

*End of decision log.*
