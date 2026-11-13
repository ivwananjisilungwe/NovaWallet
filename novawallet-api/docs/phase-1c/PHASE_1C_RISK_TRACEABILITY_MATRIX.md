# NovaWallet — Phase 1C Risk Traceability Matrix

> **Location:** version-controlled copy — `novawallet-api/docs/phase-1c/`. Do not edit findings in this copy; supersede with a later batch report.`

**Document:** PHASE_1C_RISK_TRACEABILITY_MATRIX.md
**Phase:** 1C — Security and Database Hardening
**Branch:** `feature/phase-1c-security-database-hardening`
**Starting HEAD:** `797571914b98d3b3206c5529e8eaf356ba79d1c8` (Phase 1B commit)
**Date:** 2026-11-12
**Author:** Forge (primary engineering orchestrator), integrating delegated read-only audits and independent re-verification.
**Status of this document:** Batch 0 deliverable — awaiting human approval.

---

## 0. Evidence Standard Used In This Matrix

A risk is only marked **CLOSED** when both a correction and executable verification evidence exist.
A risk is marked **MITIGATED** only when a control is present **and I verified it in the current source or by executing a test**.
A risk is marked **OPEN** when I confirmed the defect is present in the current code at `7975719`.
A risk is marked **BLOCKED** when remediation requires information that does not exist in the repository.

Delegated-agent severity ratings were **not** accepted as-is. Every rating below is my own, re-calibrated against the actual code. Where I overrode a subagent, the override is recorded in §5.

---

## 1. Source-of-Truth Conflict — MUST BE RESOLVED BY HUMAN

**Finding ID:** DOC-SOT-01
**Severity:** High (process/traceability, not runtime)

There are **two divergent copies** of the canonical risk register:

| Path | Versioned? | Size | MD5 | Last modified |
|---|---|---|---|---|
| `NovaWallet/docs/architecture/backend-risk-register.md` (workspace root) | **No** — workspace root is not a git repository | 87,591 B | `70b06e2c…` | 2026-11-11 00:49 |
| `NovaWallet/novawallet-api/docs/architecture/backend-risk-register.md` (in-repo) | **Yes** — updated in Phase 1B commit `7975719` | 89,784 B | `34a5c3a7…` | 2026-11-12 12:40 |

The files **differ**. The in-repo copy carries the Phase 1B status updates (e.g. `FIN-05` → `MITIGATED (Phase 1B)`). Critically, the Phase 1B edit **removed the `(R-02)` traceability label** from the `FIN-07` containment note:

- Root copy line 187: `- **Containment (R-02):** Contained via @ConditionalOnProperty(...)`
- In-repo copy line 187: `- **Containment (BalanceRecalculationJob containment — not one of the ten P1B-CLOSE closure findings):** Contained via ...`

**Consequence:** the Phase 1C prompt designates `docs/architecture/backend-risk-register.md` as source of truth, but that path resolves differently depending on working directory. Phase 1B **degraded** traceability by dropping the R-02 label.

**Decision required from human:**
1. Confirm the **in-repo** `novawallet-api/docs/architecture/backend-risk-register.md` is canonical (recommended — it is versioned and current).
2. Approve restoring the `(R-02)` label in the in-repo copy during Batch 3, so the identifier survives.
3. Approve retiring or re-syncing the unversioned root copy to prevent future divergence.

No file has been modified to resolve this yet.

---

## 2. R-Series Identifier Integrity — MUST BE RESOLVED BY HUMAN

**Finding ID:** RISK-ID-01
**Severity:** High (traceability)

The Phase 1C prompt mandates `R-01`, `R-02`, `R-03` and states: *"Retrieve the exact description, severity, affected files, root cause and remediation requirements of each identifier from the repository's canonical risk register. Do not invent their meanings."*

**Factual finding: `R-01` and `R-03` are NOT defined in either copy of the canonical risk register.** `R-02` appears only as a label on the `FIN-07` containment note in the stale root copy.

A **second, conflicting** R-numbering scheme exists: `reports/PHASE_1B_BATCH_2_REVIEW_CONSOLIDATION.md` §"Consolidated remediation backlog" defines `R1`…`R12` (no hyphen, no zero-padding) as Phase 1B Batch 3 work items. Those meanings are unrelated:

| Phase 1B backlog ID | Phase 1B meaning |
|---|---|
| R1 | Sanitize provider text before `log.error` |
| R2 | Remove `idempotencyKey` from `PAYOUT_INITIATED` audit |
| R3 | Close S5 direct-service race beyond unique index |
| R4 | S7 fault-injection test (superseded by R9) |
| R9 | Narrow `catch (Throwable)` → `catch (Exception)` |
| R10 | Exclude payout rows from `recalculateMissingBalances()` |

If the Phase 1C prompt's `R-01`/`R-02`/`R-03` were read as that scheme, Batch 3 would be scoped to log sanitization and audit fields — which contradicts the prompt's own Batch 3 subject matter (wallet concurrency, admin status changes, account-number generation, `BalanceRecalculationJob`).

**Proposed mapping, derived from context and corroborating Phase 1B text — REQUIRES HUMAN CONFIRMATION:**

| Prompt ID | Proposed meaning | Evidence for the mapping | Confidence |
|---|---|---|---|
| **R-01** | Wallet/administrative status-change concurrency: pessimistic locking exists for payout wallets, but there is no dedicated, lock-ordered administrative wallet status-change flow | `PHASE_1B_BATCH_2_REVIEW_CONSOLIDATION.md:50` — "R-01 (pessimistic lock exists for payout wallets; no dedicated locked admin status-change flow)"; prompt §12.2 is devoted to exactly this | Medium |
| **R-02** | `BalanceRecalculationJob` overwrites historical snapshots with the current balance | Root register line 187 explicitly labels the containment note `(R-02)` on the `FIN-07` entry; prompt acceptance criterion 4 — "R-02 has verified safe containment or correction" — matches a contained, not corrected, defect | **High** |
| **R-03** | Account-number uniqueness must be DB-enforced with bounded retry | `PHASE_1B_BATCH_2_REVIEW_CONSOLIDATION.md:49` — "R-03 unique account numbers + bounded retry (MAX_ATTEMPTS=10)"; prompt §12.3 is devoted to exactly this | High |

**Until confirmed, R-01/R-02/R-03 are tracked below under these proposed meanings and are marked `OPEN — IDENTITY UNCONFIRMED`.** I will not mark them CLOSED on an inferred definition.

### 2.1 HUMAN-DECISION-1 — Email-verification enforcement policy (blocks Batch 1 design)

**Finding ID:** DEC-01
**Severity:** High (product behaviour + security policy)

The code conflates two different concepts:

| `UserDetails` flag | Mapped to | Checked by `JwtAuthFilter`? |
|---|---|---|
| `enabled` | `user.isEmailVerified()` | **No** |
| `accountNonLocked` | `!user.isDeleted()` | **No** (redundant — `@SQLRestriction` already hides deleted users) |
| `accountNonExpired` | `true` (constant) | No |
| `credentialsNonExpired` | `true` (constant) | No |

`User` has **no** `locked`, `disabled`, or `suspended` column. Soft-deleted users are already excluded from `findById` by `@SQLRestriction("deleted = false")`, so they cannot authenticate at all.

Therefore the only observable consequence of adding the "obvious" SEC-01 fix (`if (!userDetails.isEnabled()) reject`) is:

> **Every user who has not verified their email is immediately locked out of every authenticated endpoint.**

That is a product-breaking behaviour change, not a pure security fix. The entity Javadoc claims "Email verification is required before financial operations" — but nothing enforces that today, and enforcing it globally is a different policy from enforcing it for money movement.

**Options for human decision:**

| Option | Behaviour | Risk |
|---|---|---|
| **A. Global enforcement** | Unverified users get 401 on all authenticated endpoints; only `/auth/*`, `/password/*`, `/email/verify` remain reachable | Strongest, but breaks any existing unverified user and the current mobile flow |
| **B. Financial-only enforcement** (recommended) | Unverified users may read profile/KYC/notifications but cannot transfer, withdraw, or pay out | Matches the Javadoc's stated intent; preserves onboarding UX; needs a guard at the money-movement service boundary |
| **C. No change; remap `enabled`** | `enabled` becomes a real disable flag (`!deleted`), and email verification is enforced elsewhere later | Smallest change; leaves SEC-01's policy gap open and must be recorded as ACCEPTED RISK |
| **D. Introduce a real `locked` column** | Adds schema + admin lock/unlock flow | Out of Phase 1C scope unless approved; expands Batch 4 |

**No Batch 1 implementation may proceed until this is chosen.** I will not guess a product policy.

---

## 3. In-Scope Risk Traceability (Required Table)

Severity key: C = Critical, H = High, M = Medium, L = Low.
Status key: OPEN / IN PROGRESS / MITIGATED / CLOSED / ACCEPTED RISK / BLOCKED.

| Risk ID | Original description (abbreviated — verbatim in §4) | Sev | Affected component | Planned action | Test evidence | Status |
|---|---|---|---|---|---|---|
| SEC-01 | `JwtAuthFilter` never consults `UserDetails.isEnabled()`/`isAccountNonLocked()`. **Re-verified:** soft-delete is already mitigated by `@SQLRestriction("deleted = false")`; there is no locked/disabled column; `isEnabled()` maps to `emailVerified` and is unchecked, so email verification gates nothing | M | `security/JwtAuthFilter.java:45-64`, `security/CustomUserDetailsService.java:31-34,44-47`, `user/entity/User.java:26` | Decide the email-verification policy (see §2 HUMAN-DECISION-1); add only meaningful account-state checks; never break existing verified users | New: unverified user behaviour asserted per approved policy; deleted user with valid JWT → 401 | **OPEN (narrowed)** — soft-delete portion already mitigated; email-verification enforcement absent |
| SEC-02 | Password change and admin deactivation do not revoke active refresh tokens | H | `user/service/UserService.java`, `admin/service/AdminService.java` | Call `tokenService.revokeAllUserTokens()` in both flows | New: token issued pre-change rejected post-change; same for deactivation | **OPEN** (partially confirmed — reset revokes, admin deactivation does not) |
| SEC-03 | Plaintext email-verification and password-reset tokens embedded in `Notification` bodies, exposed via `/api/v1/notifications`, logged by `MailService` | H | `auth/service/AuthService.java:94,222`, `notification/MailService.java` | Remove tokens from notification bodies and responses; hash tokens at rest; redact logs; single-use + expiry | New: notification body/response contains no token; captured logs contain no token | **OPEN** (confirmed in code) |
| SEC-04 | `spring.profiles.active: dev` hardcoded as default in base `application.yml` | H | `src/main/resources/application.yml:5-6` | Remove default profile; fail fast or default to prod; dev opt-in only | New: context startup without explicit profile does not activate dev; admin seeder absent | **OPEN** (confirmed at line 6) |
| SEC-05 | Webhook auth was static shared-secret header only; no payload HMAC, timestamp window, replay nonce, durable dedup | H | `payment/controller/WebhookController.java`, `payment/service/WebhookService.java` | Disbursement: done in 1B — preserve. Collections: implement same control set, or keep contained and record BLOCKED on provider contract | Existing 1B tests must stay green; new collections negative tests | **MITIGATED (disbursement, 1B)** / **BLOCKED (collections — provider signing contract not evidenced in repo)** |
| SEC-06 | No logout endpoint exposed despite `AuthService.logout()` existing | M | `auth/controller/AuthController.java` | Expose `POST /api/v1/auth/logout`, JWT-authenticated, call `logout(userId)` | New: logout revokes refresh tokens; token replay after logout rejected | **OPEN** (confirmed — no mapping exists) |
| SEC-08 | `JwtUtil` enforces no minimum key length/entropy at startup and validates no `iss`/`aud` | M | `security/JwtUtil.java` | Startup validation ≥256-bit key; add and validate `iss`/`aud`; pin algorithm on parse | New: short key fails startup; wrong `iss`/`aud` rejected; `alg=none` rejected | **OPEN** (confirmed — no validation present) |
| SEC-09 | `RateLimitFilter` / `LoginRateLimiter` are in-memory per-JVM and use `getRemoteAddr()` without proxy awareness | M | `security/RateLimitFilter.java:86`, `security/LoginRateLimiter.java:80` | Configure `server.forward-headers-strategy`; document single-instance assumption; do **not** add Redis without approval | New: `X-Forwarded-For` behaviour test; per-instance limit documented | **OPEN** (confirmed — Caffeine + `getRemoteAddr`, no forward-headers config) |
| SEC-10 | Monetary DTOs validate `@Positive` but lack `@Digits` / `@DecimalMax` upper bounds | M | `transaction/dto/{Deposit,Withdraw,Transfer}Request.java`, `payment/dto/PayoutRequest.java` | Add `@Digits(integer=13, fraction=2)` and `@DecimalMax`; bound narration length | New: oversized and over-precise amounts → 400 | **IN PROGRESS** — amount bounds MITIGATED (`@Digits(13,2)` + `@DecimalMax` on Deposit/Withdraw/Transfer/Payout DTOs; `InputBoundsIntegrationTest#transfer_amountAboveMaximum_isBadRequest` and related, 361/0 full run). Narration length bound **not** evidenced — still OPEN. |
| SEC-11 | `management.info.env.enabled: true`, unauthenticated `/actuator/info`, public Swagger in all profiles, CORS wildcard headers with credentials | M | `application.yml`, `config/SecurityConfig.java`, `application-prod.yml` | Disable env info in prod; restrict Swagger to non-prod; explicit origin+header allowlist | New: prod profile does not expose Swagger/info; CORS preflight rejects unknown origin | **OPEN** (partially verified — Swagger permitAll and `allowedHeaders("*")` confirmed) |
| SEC-12 | `RequestTracingFilter` copies unvalidated `X-Request-ID` into MDC (log injection); `KycService` trusts client MIME without magic-byte inspection | L | `filter/RequestTracingFilter.java:33`, `kyc/service/KycService.java:181-186` | Allowlist `X-Request-ID` `[A-Za-z0-9-]{1,64}` else UUID; magic-byte validation on uploads | New: CRLF-injected header is rejected/sanitised; spoofed MIME rejected | **OPEN** (both confirmed in code) |
| FIN-09 | Intermediate rounding to scale 2 in percentage fee calculation | M | `fee/service/FeeEngineService.java:51-53,68` | Round once, after flat-fee addition and clamping | New: sub-cent fee-config edge cases | **OPEN** (confirmed — `divide(...,2,HALF_EVEN)` before `+ flatFee` and before clamping; final `setScale(2)` is a no-op on 2dp values) |
| FIN-11 | Daily send limit per-wallet vs velocity per-user; unverified users bypass tier limits | L | `transaction/service/TransactionLimitService.java:117-120` | Gate tier limits on verification or apply a reduced unverified default | New: unverified user cannot exceed reduced limit | **OPEN** (confirmed — early `return` for non-APPROVED/tier<1; velocity caps still apply to all users) |
| R-01 | *Proposed:* administrative wallet status change can race money movement — **confirmed at `AdminService.toggleUserDeletedStatus`** | H | `admin/service/AdminService.java:72-102` | Take `findByIdWithLock` on the wallet before status mutation; assert expected prior status; keep audit | New: concurrent user-deactivation vs transfer → no invalid intermediate state | **OPEN** (defect confirmed; identifier still unconfirmed) |
| R-02 | `BalanceRecalculationJob` rewrites historical snapshots to current balance | M | `notification/schedule/BalanceRecalculationJob.java:16-20`, `TransactionRepository.recalculateMissingBalances()` | Preserve containment; do not re-enable; prove job logic unsafe or fix by replay | `BalanceRecalculationJobDisabledInProdTest` (exists, must re-run) | **MITIGATED (containment)** — must be re-verified in Batch 3 |
| R-03 | Account-number TOCTOU: `existsByAccountNumber()` then later INSERT; collision surfaces as unhandled 500 | H | `wallet/service/AccountNumberGenerator.java:31-40` | DB unique index authoritative; catch `DataIntegrityViolationException` and retry within a bounded loop; fail safe on exhaustion | New: concurrent wallet creation → zero duplicates persisted; collision → retry, not 500 | **OPEN** (defect confirmed; identifier still unconfirmed) |


### 3.1 BE-1C-2 authorization evidence (no register ID in §3)

These rows record executable evidence for authorization controls. No register ID exists for them in §3, so none is assigned here.

| Control | Affected component | Planned action | Test evidence | Status |
|---|---|---|---|---|
| IDOR / ownership on walletId, reference, payout and KYC document routes | `TransactionHistoryService`, `TransactionService.findActiveWalletWithLock`, `PayoutService.findOwnedActiveWalletWithLock`, `AdminKycService` (document must belong to path user) | Prove ownership with executable tests; no code change expected | `AuthorizationMatrixIntegrationTest` (`intruder_cannotRead`, `intruder_cannotMoveMoney`, `referenceOwnership`); `PayoutEnabledIntegrationTest#payout_fromAnotherUsersWallet_isForbidden_andGatewayNotCalled`; `AuthorizationGapIntegrationTest#adminDocumentDownload_crossUserDocument_isForbidden`; 361/0 full run (`AuthorizationGapIntegrationTest` run separately, 5/0) | **MITIGATED** (verified by test; was not a defect per §5) |
| Route-level authentication (no anonymous access to non-public routes) | `config/SecurityConfig.java` `anyRequest().authenticated()` | Enumerate every handler mapping and assert 401 for anonymous | `RouteSecurityEnumerationTest#everyNonPublicRouteRequiresAuthentication`; 361/0 full run | **MITIGATED** (verified by test) |
| Admin URL-level protection (defence in depth) | `config/SecurityConfig.java` new `/api/v1/admin/**` → `hasRole("ADMIN")` rule, on top of class-level `@PreAuthorize` | Add URL-level rule; prove customers get 403 and anonymous gets 401 on every admin route | `AuthorizationMatrixIntegrationTest#customerGets403_anonymousGets401` (parameterised); `AuthorizationMatrixIntegrationTest#adminAllowed`; 361/0 full run | **MITIGATED** (verified by test; single `ADMIN` role per DEC-03, split roles deferred to BE-1D-07) |

Batch report: `novawallet-api/docs/phase-1c/PHASE_1C_BATCH_2_REPORT.md`. Full-suite evidence: `./mvnw -B clean test` exit 0, Tests run 361, Failures 0, Errors 0, Skipped 0 (JDK 21 runtime; Java 17 runtime not verified, BLK-04).

---

## 4. Verbatim Register Entries

Quoted from `novawallet-api/docs/architecture/backend-risk-register.md` (in-repo, canonical candidate). Titles, severities and statuses are the register's own words.

### SEC-01 — High — Confirmed Vulnerability
> `JwtAuthFilter` authenticates requests from valid JWTs without checking `userDetails.isEnabled()`, `isAccountNonLocked()`, or soft-delete status
>
> Root cause: On a valid JWT, the filter loads `UserDetails` and sets the authentication in the security context (lines 45-61) without ever calling `userDetails.isEnabled()`, `isAccountNonLocked()`, `isAccountNonExpired()`, or `isCredentialsNonExpired()`.
>
> Remediation: After `loadUserById`, check `isEnabled()` and `isAccountNonLocked()`; if false, do not set authentication. Ensure `CustomUserDetailsService` maps soft-delete/locked flags onto `UserDetails`.

### SEC-02 — High — Confirmed Vulnerability
> Password change and admin user deactivation do not revoke active refresh tokens
>
> Root cause: Neither `UserService.changePassword(...)` nor `AdminService.deactivateUser(...)` calls `refreshTokenRepository.revokeAllByUserId(...)`.
>
> Remediation: Call `refreshTokenRepository.revokeAllByUserId(userId)` in `UserService.changePassword` and `AdminService.deactivateUser`. Add a logout endpoint (SEC-06) and revoke on password reset too.

### SEC-03 — High — Confirmed Vulnerability
> Plaintext email-verification & password-reset tokens are embedded in Notification messages and exposed via `/api/v1/notifications`, and logged in full by `MailService` when SMTP is unconfigured
>
> Remediation: Never persist tokens in notification bodies. Redact tokens from all logs. Hash tokens at rest and enforce single-use + expiry. Remove token from `NotificationResponse`.

### SEC-04 — High — Confirmed Misconfiguration
> `spring.profiles.active: dev` is hardcoded as the default in base `application.yml`, risking accidental activation of `AdminDataInitializer`, SQL debug logging, and dev settings if `SPRING_PROFILES_ACTIVE` is omitted
>
> Remediation: Remove the hardcoded `active: dev`; default to no profile (fail-fast) or `prod`. Add a startup guard that refuses to boot with dev profile when a prod marker is set.

### SEC-05 — High — MITIGATED (disbursement path); collections contained
> Webhook authentication relies solely on static `X-Webhook-Secret` header comparison without payload HMAC signing, timestamp tolerance window, or replay nonce table; deduplication checks `existsByDescription` string matching
>
> Phase 1B note (P1B-CLOSE-09): The `/lipila/collections` endpoint lacks HMAC/timestamp/dedup/amount-verification. It is contained (no unverified credits possible today), but must be upgraded with the shared verification plumbing when verified collection initiation is implemented.

### SEC-06 — Medium — Confirmed Weakness
> No logout endpoint exposed despite `AuthService.logout()` existing
>
> Remediation: Expose `POST /api/v1/auth/logout` (JWT-authenticated) calling `AuthService.logout(userId)`. Document token clearing on client. Add to OpenAPI.

### SEC-08 — Medium — Confirmed Weakness
> `JwtUtil` does not enforce minimum key length/entropy at startup or validate `iss`/`aud` claims
>
> Remediation: Add a `@PostConstruct` that fails startup if the key is shorter than 256 bits / low entropy. Add `iss` and `aud` claims on issue and validate them on parse.

### SEC-09 — Medium — Design Weakness
> `RateLimitFilter` & `LoginRateLimiter` are in-memory per-JVM, proxy-header unaware
>
> Root cause: Counters are held in in-memory Caffeine caches; keys derive from `request.getRemoteAddr()`. No `server.forward-headers-strategy` is configured anywhere.
>
> Remediation: Configure `server.forward-headers-strategy=framework` and trust only the proxy. Either move counters to a shared store (Redis) or document single-instance deployment.

### SEC-10 — Medium — Confirmed Weakness
> Monetary request DTOs validate `@Positive` but lack `@Digits(integer=13, fraction=2)` / `@DecimalMax` upper bounds
>
> Remediation: Add `@Digits(integer = 13, fraction = 2)` and `@DecimalMax` to each amount field. Add `@Size`/max on description/narration strings.

### SEC-11 — Medium — Confirmed Misconfiguration
> `management.info.env.enabled: true` with unauthenticated `/actuator/info`, public Swagger + CORS wildcard headers with credentials
>
> Remediation: Disable `management.info.env.enabled` in prod. Restrict Swagger to non-prod profiles. Tighten CORS to an explicit origin list and specific headers; never combine wildcard headers with credentials.

### SEC-12 — Low — Confirmed Weakness
> `RequestTracingFilter` accepts unvalidated `X-Request-ID` header into MDC logs (log injection); `KycService` trusts client-supplied MIME header without magic-byte inspection
>
> Remediation: Validate/sanitise `X-Request-ID` (allowlist `[A-Za-z0-9-]{1,64}`, else generate a UUID). Inspect magic bytes for KYC uploads. Serve uploaded files with `Content-Disposition: attachment`.

### FIN-09 — Medium — Design Weakness
> Intermediate rounding to scale 2 in percentage fee calculation
>
> Remediation: Ensure all intermediate fee computations retain full `MathContext` precision and only round to 2dp at the final presentation/DB-write stage. Add unit tests asserting single-rounding correctness at edge cases.

### FIN-11 — Low — Design Weakness
> Daily send limit per-wallet vs velocity per-user; unverified users bypass tier limits
>
> Remediation: Add a verification gate before applying KYC-tier daily/velocity limits. For unverified users, apply a reduced default limit or enforce KYC completion first.

### R-02 (root-copy label on FIN-07) — Medium — MITIGATED
> Containment (R-02): Contained via `@ConditionalOnProperty(name = "app.scheduler.balance-recalc.enabled", havingValue = "true", matchIfMissing = false)` on `BalanceRecalculationJob`. In production `app.scheduler.balance-recalc.enabled: false` so the job bean is never registered. Containment is verified by `BalanceRecalculationJobDisabledInProdTest`.

---

## 5. Independent Re-Verification Log (Forge)

Each row records what I personally confirmed in the source at `7975719`, not what a subagent reported.

| Check | Command / file | Result |
|---|---|---|
| SEC-01 account-state checks | `grep isEnabled\|isAccountNonLocked\|isAccountNonExpired\|isCredentialsNonExpired` over `security/*.java` | **No matches** → SEC-01 confirmed. `JwtAuthFilter` sets authentication immediately after `loadUserById` |
| SEC-04 profile default | `grep -n "profiles:\|active:" application.yml` | `5: profiles:` / `6: active: dev` → confirmed |
| SEC-06 logout mapping | `grep -rn "logout" auth/controller/` | Only a Javadoc mention at `AuthController.java:25`; **no request mapping** → confirmed |
| SEC-08 key/issuer/audience | `grep -nE "issuer\|audience\|key.length\|256\|@PostConstruct" JwtUtil.java` | **No matches** → confirmed |
| SEC-09 rate limiter | `grep getRemoteAddr\|Caffeine` in `security/`; `grep forward-headers-strategy *.yml` | `RateLimitFilter.java:86` and `LoginRateLimiter.java:80` use `getRemoteAddr()`; Caffeine at `:44` and `:32`; **no** forward-headers config → confirmed |
| SEC-10 amount bounds | `grep -B2 -A2 "BigDecimal amount"` over DTOs | Only `@Positive` / `@NotNull`; **no `@Digits`, no `@DecimalMax`** → confirmed |
| SEC-12 request id | `RequestTracingFilter.java` body | `String requestId = request.getHeader("X-Request-ID");` then `MDC.put(...)` with **no validation** → confirmed |
| SEC-02 revocation sites | `grep -rn "revokeAll"` | Calls at `AuthService.java:243,259,279` only. **No call from `AdminService`** → partially confirmed |
| SEC-03 token exposure | `grep "Token: "` | `AuthService.java:94` (verification) and `:222` (reset) embed the raw token in the notification body → confirmed |
| Method security enabled | `grep EnableMethodSecurity` | `SecurityConfig.java:31 @EnableMethodSecurity` → **`@PreAuthorize` is enforced** (refutes a subagent claim of admin IDOR) |
| Admin endpoint protection | `grep @PreAuthorize` on `Admin*Controller` | Class-level `hasRole('ADMIN')` on `AdminUserController:35`, `AdminKycController:36`, `AdminWalletController:44` → **no IDOR**; single-role model only |
| KYC path traversal | `FileStorageService.loadFile()` | `path.normalize().startsWith(uploadDir)` guard **present** → refutes subagent "CRITICAL path traversal"; `storeFile()` lacks the guard but `sanitizeFileName` strips `/` and `\` and the name is UUID-prefixed → **not exploitable** |
| Uploads statically served? | `grep addResourceHandler\|/uploads` | **No resource handler**; only `upload-dir: /app/uploads/kyc` in prod config → refutes "CRITICAL RCE"; content-type spoofing is **Medium**, not Critical |
| R-02 label present? | `grep R-0[0-9]` in both register copies | In-repo copy: **no matches**. Root copy line 187: `Containment (R-02)` → label lost in Phase 1B edit |

### Severity overrides applied

| Item | Subagent rating | My rating | Reason |
|---|---|---|---|
| Admin cross-user access (4 findings) | CRITICAL/HIGH | **Not a vulnerability** | `@EnableMethodSecurity` active; class-level `hasRole('ADMIN')`; admin acting across users is the endpoint's purpose |
| KYC path traversal (KYC-05) | HIGH | **Low (defence-in-depth)** | `loadFile` guard present; filename sanitisation removes separators; UUID prefix |
| KYC MIME/magic-byte (KYC-03/04/12) | CRITICAL | **Medium** | Files are never served or executed; risk materialises only if a document pipeline or static handler is added later. Register already classifies as **Low (SEC-12)** |
| PII in admin user listing (KYC-08) | HIGH | **Low** | Intended admin function; should be minimised but is not an access-control defect |
| Access token valid after logout (AUTH-07) | HIGH | **Medium** | 15-minute bounded window is standard for stateless JWT; Phase 1C requirement 8 ("revoked credentials cannot be reused") still forces a decision |
| `WalletService.freeze/unfreezeWallet` "unlocked" | financial audit | **False** | Both call `walletRepository.findByIdWithLock(...)` — PESSIMISTIC_WRITE at `WalletService.java:74,89` |
| `BalanceRecalculationJob` "ACTIVE in dev/test" | financial audit | **False** | `@ConditionalOnProperty(matchIfMissing = false)`; property is set **only** in `application-prod.yml:145-146` as `false` → bean never registers in any profile |
| Baseline suite size | 175 tests (financial audit) | **243 tests** | My own run of `./mvnw -B clean test`: 243/0/0/0, BUILD SUCCESS. The subagent's count is not reproducible and is not used |

### 5.1 Financial and concurrency findings (verified by Forge)

| ID | Finding | Sev | Evidence | Status |
|---|---|---|---|---|
| FIN-A | `AdminService.toggleUserDeletedStatus` mutates wallet status with **no lock and no status precondition** | H | `AdminService.java:84-88` (restore → ACTIVE) and `:96-100` (deactivate → FROZEN); `walletRepository.findByUserId` then `save` | OPEN — the concrete R-01 defect |
| FIN-B | `AdminWalletController` freeze/unfreeze **is** correctly locked | — | `WalletService.java:74` and `:89` → `findByIdWithLock` | Verified control — must not regress |
| FIN-C | Account-number TOCTOU; collision surfaces as unhandled `DataIntegrityViolationException` | H | `AccountNumberGenerator.java:31-40` — `existsByAccountNumber()` is advisory; the INSERT occurs later in `WalletService` | OPEN — R-03 |
| FIN-D | `BalanceRecalculationJob` contained in **all** profiles | M | `@ConditionalOnProperty(..., matchIfMissing = false)`; only `application-prod.yml:145-146` sets it, to `false` | MITIGATED — containment must be re-proven; job logic unsafe |
| FIN-E | Phase 1B payout safeguards intact (three-stage split, lock release after Stage A, `PENDING_RECONCILIATION` replay guard, compensating credit) | — | Financial audit; 1B tests green in my 243-test run | Verified control — must not regress |
| FIN-F | `FeeEngineService.calculateFee` rounds the percentage component to 2dp before adding the flat fee and before min/max clamping | M | `FeeEngineService.java:51-53` (`divide(...,2,HALF_EVEN)`), `:56-65` clamping, `:68` final `setScale(2)` | OPEN — FIN-09 confirmed; sub-cent impact per transaction |
| FIN-G | Unverified users are exempt from daily-send and wallet limits | L | `TransactionLimitService.java:117-120` — early `return` when `kycStatus != APPROVED || tier < 1`; Javadoc states this is intentional for MVP | OPEN — FIN-11 confirmed; hourly velocity caps still apply to all users |

**Balance-mutation paths to cover in Batch 3** (12-path catalogue from the financial audit; each must be re-read and confirmed during Batch 3, not assumed):

withdrawal · transfer (sender debit + receiver credit) · payout Stage A debit · payout Stage B/C finalisation · payout compensating credit · collection credit (currently unreachable) · webhook finalisation · `BalanceRecalculationJob` (contained) · admin user-deactivation wallet freeze · admin user-restore wallet unfreeze · admin wallet freeze/unfreeze (locked) · deposit (no-op today)

---

## 6. Out-of-Scope Risks (Visible for Boundary Control)

These are present in the register but **not** in Phase 1C scope. They must not be "fixed" incidentally.

| ID | Title (abbreviated) | Sev | Register status |
|---|---|---|---|
| SEC-07 | Email verification token has no expiration timestamp | M | Confirmed Weakness — **present in register, out of Phase 1C scope** |
| FIN-01 | Direct unverified wallet deposit endpoint | C | TEMPORARILY DISABLED |
| FIN-02 | Webhook collection callback trusts unverified amount | C | TEMPORARILY DISABLED |
| FIN-03 | Payout debit + provider call in one transaction | H | MITIGATED (1B) |
| FIN-04 | Provider timeout rolls back debit despite possible disbursement | H | MITIGATED (1B) |
| FIN-05 | Cleanup job marks PENDING payouts FAILED without refund | H | MITIGATED (1B) |
| FIN-06 | Transfer pessimistic locks in caller-supplied order | H | VERIFIED (1B) |
| FIN-07 | `BalanceRecalculationJob` overwrites historical snapshots | M | MITIGATED (contained) — **this is the R-02 subject** |
| FIN-08 | Single-entry ledger + mutable balance | M | Design Weakness — **Phase 2, explicitly out of scope** |
| FIN-10 | Missing payout polling/webhook/refund workflows | M | Design Weakness — partly delivered 1B |
| FIN-12 | In-memory `AtomicLong` reference counter | H | MITIGATED (1A) |
| ARCH-01 | Cross-domain repository injection | H | Architectural (Phase 4) |
| ARCH-02 | Identity/KYC coupled in `users` | H | Architectural (Phase 4) |
| ARCH-03 | No domain events / outbox | M | Architectural (Phase 4) |
| DB-01 | Missing CHECK constraints, FK indexes, leftover test table | M | Database — **in Phase 1C Batch 4 scope via DB work, but tracked as DB-01 not R-xx** |

**Note on FIN-08:** the register itself places the double-entry ledger outside Phase 1C. Phase 1C must therefore harden constraints and concurrency **without** changing the balance source of truth.

---

## 7. Blocked Items Requiring External Input

| ID | Blocker | What is needed | Owner |
|---|---|---|---|
| SEC-05 (collections) | Lipila collections callback signing contract is not evidenced anywhere in the repository | Written provider documentation or partner confirmation specifying: signature header name, algorithm, encoding, canonical string, timestamp header, tolerance, and event-id field | Lipila / business owner |
| R-01, R-03 | Identifiers undefined in canonical register | Human confirmation of the proposed meanings in §2 | Product/tech lead |
| DOC-SOT-01 | Two divergent register copies | Human decision on canonical path | Tech lead |

*(FIN-09 and FIN-11 were resolved during Batch 0 by direct source reading; see §5.1 FIN-F/FIN-G. Neither is a blocker.)*

---

## 8. Status Summary

| Status | Count | IDs |
|---|---|---|
| OPEN (confirmed in code) | 13 | SEC-01, SEC-02, SEC-03, SEC-04, SEC-06, SEC-08, SEC-09, SEC-11, SEC-12, FIN-09, FIN-11, R-01, R-03 |
| IN PROGRESS (SEC-10: amount bounds mitigated; narration length open) | 1 | SEC-10 |
| MITIGATED (verified, must not regress) | 1 | R-02 (containment, all profiles) |
| MITIGATED (disbursement) / BLOCKED (collections) | 1 | SEC-05 |
| CLOSED | 0 | — |
| **Total in-scope IDs** | **16** | all accounted for (§3.1 authorization rows are not register IDs and are excluded from this count) |

**No risk is marked CLOSED.** Phase 1C has not yet implemented a single correction.

**Identifier caveat:** R-01 and R-03 remain unconfirmed as identifiers even though the underlying defects are now confirmed in code. Batch 3 will fix the defects; the matrix will not claim the identifier was satisfied until the human confirms the mapping in §2.

---

## 9. Change Log

| Date | Change |
|---|---|
| 2026-11-12 | Initial Batch 0 matrix. 18 in-scope IDs extracted, 16 independently re-verified, 2 severity overrides groups applied, 2 identifier-integrity findings raised (DOC-SOT-01, RISK-ID-01). |
| 2026-11-14 | BE-1C-2 (Batch 2). SEC-10 moved OPEN → IN PROGRESS: amount `@Digits`/`@DecimalMax` verified and tested; narration length still open. Added §3.1 evidence rows for IDOR/ownership, route authentication and admin URL-level protection. Evidence: full suite 361/0/0/0 (JDK 21); `AuthorizationGapIntegrationTest` 5/0 run separately. No risk marked CLOSED. |
| 2026-11-12 | Financial/concurrency audit integrated. R-01 defect confirmed at `AdminService.toggleUserDeletedStatus`; R-03 TOCTOU confirmed; FIN-09 and FIN-11 confirmed by direct source read. Three subagent claims rejected after re-verification (wallet freeze/unfreeze is locked; recalc not active in dev; 175-test count not reproducible). |
