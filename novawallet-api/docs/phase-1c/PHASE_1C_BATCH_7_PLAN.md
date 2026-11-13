# Phase 1C Batch 7 Plan: Rate Limiting & Abuse Protection

Date: 2026-11-14 · Prompt: BE-1C-7 · Status: **PLAN ONLY — no source changed, no build run, no git writes**
Decision dependency: **DEC-09 is OPEN** (`docs/DECISIONS.md` §1). Recommendation recorded there: in-memory per operation, single instance, documented. This plan follows that recommendation for the counters, but it raises one further decision (D2 below) because the prompt also requires durable login/PIN lockouts.

## 0. Identifiers addressed
SEC-09 (in-memory, per-JVM, proxy-unaware limiters) — primary. Also covers the BE-1C-7 items for lookups, uploads and webhooks, and the enumeration requirement. Related: SEC-11 (Batch 8), DEC-13 (OTP provider, OPEN), DEC-15 (limit values, OPEN).

## 1. What I read (with paths)
- Governance: root `AGENTS.md`; `novawallet-api/AGENTS.md` (gates, "never delete tests", "no Redis/Kafka unless the prompt says so"); `docs/DECISIONS.md` (DEC-09, DEC-11, DEC-13, DEC-15, DEC-10); `prompts/templates/T5_plan_first.md`; `prompts/stage-01_backend-security/_COMMON_BATCH_RULES.md`; `BE-1C-7_rate_limiting.md`.
- Style model: `novawallet-api/docs/phase-1c/PHASE_1C_BATCH_2_PLAN.md`.
- Risk register: `novawallet-api/docs/architecture/backend-risk-register.md` §SEC-09 (line 649).
- Not re-read in this pass (read before implementing): `PHASE_1C_IMPLEMENTATION_PLAN.md` §7 and §13, `PHASE_1C_RISK_TRACEABILITY_MATRIX.md`, `TODO.md`, and `PHASE_1C_DECISION_LOG.md` beyond the DEC-09 grep.

Source evidence (verified by reading the files):

| # | Fact | Evidence |
|---|---|---|
| S1 | Global limiter: Caffeine, 100 req/60 s default, 10 req/60 s on auth prefixes, cache max 50,000, `expireAfterWrite(window)` | `security/RateLimitFilter.java:33-47`, `:56`; `application.yml:81-88` |
| S2 | Client key = authenticated principal name, else `request.getRemoteAddr()` | `RateLimitFilter.java:82-88` |
| S3 | Auth prefixes = `/api/v1/auth/`, `/password/`, `/pin/`, `/email/` | `RateLimitFilter.java:90-94` |
| S4 | 429 body is hand-built with `String.format` and the raw request URI; `Retry-After` is the constant window, not time remaining | `RateLimitFilter.java:67-75` |
| S5 | `RateLimitFilter` is `@Component` + `@Profile("!test")`; no reference in `SecurityConfig`; it is not in the security chain | `RateLimitFilter.java:23-24`; grep of `src/main` finds no other reference |
| S6 | Login limiter: keys `email|ip`, 5 attempts, 15 min lockout, two Caffeine caches (max 10,000 each) | `security/LoginRateLimiter.java:20-23`, `:32-40`, `:79-82` |
| S7 | Login lockout is checked before the credential check. The lock message hard-codes "15 minutes" while the config value exists | `auth/controller/AuthController.java:93-95` |
| S8 | Failures are recorded only on `UnauthorizedException`; success clears the counters | `AuthController.java:97-104` |
| S9 | Lockout state is process memory only (lost on restart) | `LoginRateLimiter.java:22-23`, `:37-40` |
| S10 | Login: user lookup occurs before the password hash check, so an unknown email skips bcrypt | `auth/service/AuthService.java:114` (lookup), `:117` (`passwordEncoder.matches`) |
| S11 | Same 401 text for unknown email and wrong password (good) | `AuthService.java:115`, `:118-119` |
| S12 | PIN lockout is durable: counters on `users` row, `MAX_PIN_ATTEMPTS=3`, `PIN_LOCK_MINUTES=15`. A locked PIN returns **400** with the lock timestamp | `AuthService.java:44-45`, `:189-199`; locked check in the same PIN path (lines 176-200) |
| S13 | Money velocity is per user only: 10 outgoing/hour and 50,000/hour | `transaction/service/TransactionLimitService.java:73-98`; `application.yml:113-119`; called from `TransactionService.java:118`, `:251` and `payment/service/PayoutService.java:293` |
| S14 | Registration returns 400 "email already exists" (enumeration) | `auth/controller/AuthController.java:57` (Swagger text) |
| S15 | Public routes: register, login, refresh, password forgot/reset, email verify, webhooks (`/api/v1/webhooks/lipila/**`) | `config/SecurityConfig.java:98-110` |
| S16 | Upload cap: 10 MB file, multipart 10 MB / 11 MB; no per-user frequency cap | `kyc/service/KycService.java:32`; `application.yml:19-24` |
| S17 | No public lookup route exists (user lookup / name check). Admin user endpoints only | route inventory: `admin/controller/AdminUserController.java:58-107` |
| S18 | No OTP route exists. `SmsService` exists but there is no OTP flow. DEC-13 OPEN | route inventory; `notification/service/SmsService.java` |
| S19 | No Caddy or proxy configuration exists in the repository. `infra/` contains only `AGENTS.md`. No `server.forward-headers-strategy` anywhere | `find`/grep over repo; `infra/` listing |
| S20 | Existing rate-limit tests: none. The `test` profile excludes `RateLimitFilter` | `RateLimitFilter.java:24` |
| S21 | Migration head is V16 (`V16__create_revoked_access_tokens.sql`). No V17 exists | `src/main/resources/db/migration` listing |
| S22 | Risk register: SEC-09 is "Design Weakness", not yet resolved | `backend-risk-register.md:54`, `:649-` |

## 2. Audit findings (pre-change)

| # | Finding | Evidence | Action in Batch 7 |
|---|---|---|---|
| R1 | Both limiters are per-JVM and reset on restart. A restart clears attacker counters | S1, S6, S9 | Keep counters in-memory (DEC-09). Make lockout durable (D2) |
| R2 | `RateLimitFilter` is not in the explicit security chain, and the test profile excludes it. No test has ever run it. Its position relative to `FilterChainProxy` is Boot-default and unverified (inferred: Spring Security runs first, so anonymous 401s may bypass counting) | S5, S20 | Register explicitly with a defined order, and add a test that counts anonymous 401s |
| R3 | `remoteAddr` is the only IP source. There is no trusted-proxy handling. Behind a reverse proxy, every client shares the proxy's bucket. The proxy config is absent from the repo | S2, S19 | Add `ClientIpResolver` that honours `X-Forwarded-For` only from configured proxy CIDRs. Default: empty list, so `remoteAddr` is used |
| R4 | Limits are not per operation. Register, refresh, logout, verify and reset share one 10/min bucket per key. Money routes have only the global 100/min | S3 | Per-operation policy table (§5) |
| R5 | Login lockout is keyed by `email|IP`. An attacker who rotates IPs gets 5 tries per IP per account. There is no account-wide cap | S6 | Add an account-keyed counter (hashed email) alongside the IP-keyed one |
| R6 | Lockout message hard-codes "15 minutes" | S7 | Build the message from the config value |
| R7 | Login timing leaks existence: unknown email returns before bcrypt runs | S10 | Run a dummy `passwordEncoder.matches` on the unknown-email path |
| R8 | PIN lock returns 400 and reveals the lock timestamp and "PIN has not been set". Should be 429 with no timestamp | S12 | Decision D5. Recommendation: 429 with generic message and `Retry-After` |
| R9 | Registration reveals whether an email exists | S14 | Decision D6 (API contract change) |
| R10 | 429 body is string-built with raw URI, which is unescaped JSON. `Retry-After` is not time-remaining. `GlobalExceptionHandler.handleRateLimit` (`GlobalExceptionHandler.java:61-67`) sets no `Retry-After` header | S4, `buildResponse` (`:178-190`) | Single 429 writer: `ErrorResponse` plus `Retry-After` (computed) |
| R11 | Caffeine `maximumSize(50_000)` means that a flood of distinct keys evicts real counters and resets them | S1 | Size limit documented; alert on eviction; test eviction behaviour |
| R12 | Money velocity is per user only. There is no per-wallet cap | S13 | Add per-wallet count and amount policy. Values are DEC-15 (BLOCKED) |
| R13 | No public lookup route exists | S17 | Mark N/A. Strict policy is pre-defined for when one is added |
| R14 | No OTP route exists | S18 | Mark N/A. Policy defined. Blocked by DEC-13 |
| R15 | Webhook path has no dedicated cap. It falls under the global 100/min per source IP. Secret comparison is constant-time (`WebhookController.java:151-160`, `MessageDigest.isEqual`) | S15 | Keep, with a sane cap and an explicit provider-IP note |
| R16 | Upload path has a size cap but no frequency cap | S16 | Add per-user upload frequency cap |

## 3. Files to change

New (`novawallet-api/src/main/java/com/novawallet/novawallet_api/security/`):
- `RateLimiter.java`: interface `tryAcquire(LimitKey key, Policy policy)` returning `Decision(allowed, remaining, retryAfterSeconds)`. Callers never see Caffeine.
- `InMemoryRateLimiter.java`: Caffeine implementation with an injectable `Ticker` for tests.
- `RateLimitPolicy.java` and `RateLimitProperties.java` (`@ConfigurationProperties("app.rate-limit")`): per-operation limits, with safe defaults.
- `ClientIpResolver.java`: trusts `X-Forwarded-For` only when `remoteAddr` is inside `app.rate-limit.trusted-proxies` (CIDR list). Takes the right-most untrusted hop.
- `LoginLockoutStore.java` (interface), with `InMemoryLoginLockoutStore` (current behaviour) and `JpaLoginLockoutStore` (only if D2 = DB). The DB variant needs `LoginLockout` entity and `LoginLockoutRepository`.

Modified:
- `RateLimitFilter.java`: delegate to `RateLimiter`. Keys: `op:ip`, `op:user`. Writes the 429 via the shared writer.
- `LoginRateLimiter.java`: delegate to `LoginLockoutStore`. Keys hashed (SHA-256 of normalised email, plus IP). No raw email in logs.
- `auth/controller/AuthController.java:93-105`: message built from config; account-keyed lock check.
- `auth/service/AuthService.java`: dummy hash on unknown email in `login` (lines 114-117 region); PIN lock path → 429 (decision D5).
- `exception/GlobalExceptionHandler.java:61-67`: `Retry-After` header for `RateLimitException`.
- `config/SecurityConfig.java`: explicit filter registration/order for `RateLimitFilter` and `ClientIpResolver`.
- `src/main/resources/application.yml`, `application-prod.yml`, `application-dev.yml`: per-operation limits and `trusted-proxies` (empty by default).
- `novawallet-api/SECURITY.md` §6 (single-instance limitation, proxy configuration, unlock procedure). Currently §6.1 describes only the global filter.
- `reports/API_ENDPOINT_INVENTORY.md`: only if an endpoint's response changes (PIN lock, register, 429 headers).

Not changed here, reported only:
- Caddy/reverse-proxy configuration: **BLOCKED** (no proxy config in repo, S19). Trusted-proxy values need the real Caddy address.

## 4. Data / migration changes

Only if D2 = DB (recommended for item 5). Expand only, no change to `users`:
- `V17__create_login_lockouts.sql` (name to confirm at implementation; V17 is unclaimed, S21, but check that BE-1C-6 has not claimed it):
  `login_lockouts(key_hash CHAR(64) PRIMARY KEY, scope VARCHAR(16) NOT NULL, failed_count INT NOT NULL, locked_until TIMESTAMP NULL, updated_at TIMESTAMP NOT NULL)`, index on `locked_until`.
- Migrate: none. Existing in-memory state is lost on deploy (accepted, documented as forced reset of counters, no user-visible data).
- Contract: a later batch may remove the in-memory store once the DB store has run in production.
- Rollback: drop the table; the in-memory store is still present.
- Verification: fresh V1→V17 and V16→V17 upgrade on PostgreSQL 16, `flyway validate`, per `novawallet-api/AGENTS.md`.

## 5. API changes

| Route group | Limit (default, configurable) | Key | Response |
|---|---|---|---|
| `POST /auth/login` | 5 failed per `hash(email)|ip` per 15 min; 10 failed per `hash(email)` per 15 min (account-wide, new) | email + IP, email | 429 `RATE_LIMITED`, `Retry-After` |
| `POST /auth/register` | 5/min per IP | IP | 429 |
| `POST /auth/refresh`, `/auth/logout` | 10/min per user (existing global auth bucket) | user | 429 |
| `POST /password/forgot`, `/email/verify` | 3/min per IP, 5/hour per hashed email | IP, email | 429 |
| `POST /password/reset` | 5/15 min per IP | IP | 429 |
| `POST /pin`, `/pin/change` (PIN attempts) | 3 wrong per user per 15 min (existing DB lock, kept). Plus 20/min per IP | user, IP | 429 (D5); no timestamp |
| Money: withdraw, transfers, payouts | Existing per-user velocity (10/h, 50,000/h) kept. New: per-wallet count and amount (values BLOCKED, DEC-15) | user, wallet | 429 `RATE_LIMITED` |
| Lookups | N/A today (no route, S17). Policy reserved: strict (5/min per IP, identical response for hit and miss) | IP | 429 |
| Uploads (`/kyc/documents/upload`) | 10/hour per user; existing 10 MB cap | user | 429 |
| Webhooks (`/webhooks/lipila/**`) | 300/min per source IP (sane cap, not auth). Secret check unchanged | IP | 429 |
| Global (all other routes) | 100/min per user or IP (unchanged) | user or IP | 429 |

Common 429 contract: `ErrorResponse` with `status=429`, `code=RATE_LIMITED`, `message` generic ("Too many requests. Please try again later."), `path`; header `Retry-After` in seconds (real remaining time); the existing `X-RateLimit-*` headers stay.

Enumeration rule (BE-1C-7 §4): for the same route, responses for existing and non-existing accounts must match in status, code and message, and be in the same timing class. Register (D6) is the known exception until D6 is decided.

## 6. Risks and how each is tested

| Risk | Test (class, type) |
|---|---|
| Limit reached → 429 with `Retry-After` | `RateLimitFilterTest` (unit, `MockMvc`) |
| Window reset | `InMemoryRateLimiterTest` with fake `Ticker` |
| Per-key isolation (user A vs user B, IP vs IP) | `InMemoryRateLimiterTest` |
| Spoofed `X-Forwarded-For` ignored from untrusted peer | `ClientIpResolverTest` (unit) |
| `X-Forwarded-For` honoured only from trusted CIDR | `ClientIpResolverTest` |
| Anonymous 401s counted (R2 ordering) | `RateLimitFilterOrderingIT` (new `@SpringBootTest` profile, not `test`, because `RateLimitFilter` is excluded there) |
| Lockout survives restart (D2 = DB) | `LoginLockoutRepositoryPgTest` (`pgtest` profile, BLK-06 / DEC-11). Writes lock, rebuilds context, asserts still locked |
| Account-wide cap across IPs | `AuthControllerLoginLockoutIT` |
| Enumeration: unknown vs wrong email, same status and body, same timing class | `LoginEnumerationTimingTest` (measures median over N requests, asserts within a documented ratio; flaky-risk noted) |
| Eviction under key flood | `InMemoryRateLimiterTest` (size bound, counter reset documented) |
| 429 body is valid JSON for hostile URIs | `RateLimitWriterTest` |
| Money per-user limit still enforced (regression) | existing `TransactionService` velocity tests; re-run |
| Per-wallet cap | `TransactionLimitServiceTest` (after DEC-15) |

Risks not mitigated here: a shared store across instances (DEC-09 accepted limitation); a distributed attack on one account from many IPs stays limited only by the account-wide cap (R5).

## 7. Decisions needed

| ID | Question | Options | Recommendation | Status |
|---|---|---|---|---|
| DEC-09 | Rate-limit store | in-memory per operation · shared store | in-memory, single instance, documented in `SECURITY.md` | OPEN (in `docs/DECISIONS.md`) |
| D2 (proposed, not in DECISIONS.md) | Are login and PIN lockout counters durable? BE-1C-7 §5 says yes; the in-memory login limiter (S9) says no | DB table (V17) · in-memory accepted and documented | DB table for lockouts (durable, hashed keys). Counters for general limits stay in memory under DEC-09 | Needs owner |
| D3 (proposed) | Trusted proxy CIDR list | env var · config file | env var, default empty | Blocked on Caddy address (S19) |
| D4 (proposed) | Per-account login cap across IPs | add (10/15 min per hashed email) · IP only | add | Needs owner |
| D5 (proposed) | PIN lock response code and content | 400 + timestamp (current) · 429 generic + `Retry-After` | 429 generic | Needs owner (changes API) |
| D6 (proposed) | Registration enumeration | keep 400 "email exists" · generic accepted response plus email notice | generic response (API contract change) | Needs owner |
| DEC-15 | Money limit values per wallet | — | placeholder config; owner sets values | OPEN, blocks per-wallet numbers |
| DEC-13 | OTP provider | owner shortlist | defer; no OTP route yet | OPEN, blocks OTP policy |
| DEC-11 | Postgres test infra | `pgtest` · Testcontainers | `pgtest` | OPEN, needed for D2 test |

## 8. Steps in order (each verifiable)

1. Owner decides DEC-09 (confirm), D2, D3, D4, D5, D6. Stop if any is unanswered for the chosen scope.
2. Add `RateLimiter`, `InMemoryRateLimiter`, `RateLimitProperties` with defaults equal to current behaviour. Test: `InMemoryRateLimiterTest` green.
3. Add `ClientIpResolver` with empty trusted list (behaviour unchanged). Test: `ClientIpResolverTest` (spoofed header ignored).
4. Move `RateLimitFilter` onto `RateLimiter`, explicit order, shared 429 writer. Tests: `RateLimitFilterTest`, `RateLimitFilterOrderingIT`, `RateLimitWriterTest`.
5. Login: account-keyed cap (D4), hashed keys, message from config, dummy hash for unknown email. Tests: `AuthControllerLoginLockoutIT`, `LoginEnumerationTimingTest`. Confirm no raw email in logs (grep the test output).
6. If D2 = DB: V17 migration, `JpaLoginLockoutStore`, `LoginLockoutRepositoryPgTest`. Verify PostgreSQL 16 fresh and upgrade.
7. PIN lock per D5. Test: wrong PIN ×3 → 429, no timestamp in body, counter durable across context restart.
8. Per-operation policy table (§5) wired; upload frequency cap; webhook cap. Tests: per-route 429 tests.
9. Per-wallet money cap if DEC-15 answered. Otherwise leave a documented placeholder.
10. Update `SECURITY.md` §6 and `reports/API_ENDPOINT_INVENTORY.md` where responses changed.
11. Gates per `novawallet-api/AGENTS.md`: `./mvnw -B clean test` (report counts against baseline 316), `git diff --check`, secret scan of diff, PG16 migration check if V17.

## 9. Out of scope / risks noted
- Caddy/reverse-proxy config is absent from the repo. Trusted-proxy values are **BLOCKED** until infra supplies them (`infra/AGENTS.md` owner).
- Response-body and logging hygiene belong to BE-1C-8 (`PHASE_1C_BATCH_8_PLAN.md`), which needs this batch first.
- OTP: no route, DEC-13 open.
- Inferred, to confirm by test: whether `RateLimitFilter` runs after the Spring Security chain (R2).
