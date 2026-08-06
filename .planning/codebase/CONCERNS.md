# Codebase Concerns

**Analysis Date:** 2026-08-06

Monorepo: `novawallet-api/` (Spring Boot 3.5.3 / Java 17 / PostgreSQL 16) + `novawallet-app/` (Flutter, in active development). Backend is MVP-complete with strong test discipline (28 test classes, 26 with `@Test`); the Flutter app is a compiling, analyzer-clean skeleton whose screens are newly wired but largely untested.

## Tech Debt

**Client-side idempotency keys are non-persistent timestamps — the dedupe protection is cosmetic:**
- Issue: The backend idempotency design (`IdempotencyFilter.java`, `IdempotencyService.java`) is sound, but the app generates a *new* key per submission: `novawallet-app/lib/features/wallet/screens/send_screen.dart:102-103` (`'send-$receiver-$amount-${DateTime.now().millisecondsSinceEpoch}'`), `withdraw_screen.dart:52`, `deposit_screen.dart:48-49`. A double-tap, app-level retry, or slow-network resubmit generates a fresh key every time, so the 24h replay window never fires. This is the money-critical path (transfer/withdraw/deposit).
- Impact: Duplicate debits are possible under double-submit; the entire backend idempotency investment is bypassed by the client.
- Fix approach: Generate the key once per logical intent and persist it (e.g. in `TokenStorage`/secure storage keyed by intent) until the request succeeds or is explicitly abandoned; reuse it across retries.

**Verification tokens stored plaintext in multiple sinks:**
- Issue: `AuthService.java:94` and `:198` write the email-verification / password-reset token verbatim into the `Notification` record body; `User.java:73,76` store `verificationToken`/`passwordResetToken` raw. Refresh tokens, by contrast, are SHA-256 hashed (`TokenService.java:85-89`).
- Impact: DB compromise leaks live account-takeover tokens; inconsistent with the hashed-refresh-token posture.
- Fix approach: Hash verification/reset tokens (same `hashToken` util) and look them up by hash; never embed tokens in notification bodies.

**Email links are broken (relative URL + wrong HTTP method):**
- Issue: `MailService.java:34,56` emit `"/api/v1/email/verify?token=" + token` — a path with no scheme/host (unusable in an email client) — and the endpoint only accepts POST (`VerificationController.java:43` `@PostMapping("/verify")` with `@RequestParam`). Clicking the link issues a GET → 405. Same for the password-reset link vs `PasswordController`.
- Impact: Users cannot verify email or reset passwords via the emailed links; the flows work only in-app (`auth_repository.dart:76-78` correctly POSTs with a query param).
- Fix approach: Add a configurable `app.mail.base-url` (e.g. the Flutter app's deep link or an API gateway URL) and either expose GET handlers or point links at a frontend route that POSTs the token.

**Dev database password committed to git:**
- Issue: `novawallet-api/src/main/resources/application-dev.yml:12` contains a plaintext numeric PostgreSQL password (`password: 8407`). The file is git-tracked, so the credential is in history.
- Impact: Anyone with repo access can log into a dev DB; the same habit would leak prod creds.
- Fix approach: Replace with `${DB_PASSWORD:?}` fail-fast in the dev profile too (the base `application.yml` defaults `spring.profiles.active: dev`, so dev config is the default boot path — see Fragile Areas).

**SMS channel is a stub and never invoked:**
- Issue: `SmsService.java` only logs to console; the actual Africa's Talking client is commented out (`SmsService.java:66`) — even with `APP_SMS_API_KEY` set nothing is sent. Additionally every `sendBoth(...)` call site passes `null` for phone (`TransactionService.java:128-135, 206-213, 345-363`), so the SMS channel is never triggered.
- Impact: OTP/transaction SMS does not exist; a wallet that markets mobile money has no SMS delivery.
- Fix approach: Implement the HTTP call, populate phone numbers at call sites, and keep the log fallback only when no API key is configured.

**Notifications are write-only and the app screen is static:**
- Issue: Backend has `NotificationRepository`/`NotificationService.recordSent` but no read endpoint (no notification controller exists); `novawallet-app/lib/features/extras/screens/notifications_screen.dart` is a hardcoded sample list ("Static sample list for now").
- Impact: In-app notification feature is non-functional; tokens stored in notification bodies (see above) have no legitimate consumer.
- Fix approach: Add `GET /api/v1/notifications` (paged, auth-scoped) and wire the screen.

**No external payment/mobile-money integration — "deposits" are ledger-only:**
- Issue: `TransactionService.deposit/withdraw` (`TransactionService.java:87-138, 141-216`) atomically update balances with no provider call (Flutterwave / MTN/Airtel Money), and there are no callback endpoints or `PENDING`→`CONFIRMED` transaction states (acknowledged in `HANDOFF_2026-08-06.md`: "Add reconciliation job + pending->confirmed transaction states for mobile money callbacks").
- Impact: The product cannot move real money; a deposit "succeeds" without any money arriving.
- Fix approach: Phase the provider integration with a `TransactionStatus.PENDING` lifecycle, webhook endpoints (idempotency-filtered — the filter already covers public endpoints), and a reconciliation job.

**KYC document storage is local, unencrypted PII:**
- Issue: `FileStorageService.java` writes national IDs, selfies, and proof-of-address to `uploads/kyc/{userId}/` on the app's local disk (gitignored, but not encrypted, not in object storage, no AV/malware scan, no retention policy). `KycService.uploadDocument` saves the entity first and stores the file after (`KycService.java:73-77`); if storage fails mid-way the transaction rolls back but the written file can orphan on disk.
- Impact: Regulatory (PII at rest), and orphans accumulate.
- Fix approach: Object storage with server-side encryption; store file only after entity persist succeeds (or accept-and-sweep orphans); add a cleanup job.

**Unverified users bypass all KYC tier limits:**
- Issue: `TransactionLimitService.enforceDailySendLimit` returns early when `kycStatus != APPROVED || tier < 1` ("Unverified users (tier 0, not approved) are not subject to limits during MVP", `TransactionLimitService.java:44-49`).
- Impact: A no-KYC account can withdraw/transfer its full balance repeatedly; AML/compliance risk for a real-money wallet.
- Fix approach: Enforce a low default cap for unverified users rather than none.

**KYC upload size check contradicts the servlet default:**
- Issue: `KycService.MAX_FILE_SIZE` is 10MB (`KycService.java:32`) and the OpenAPI docs advertise "max 10MB" (`KycController.java:62`), but no `spring.servlet.multipart.*` is configured anywhere in `src/main/resources/`. Spring Boot's default `max-file-size` is 1MB, so files 1–10MB are rejected by the multipart resolver before `KycService` runs and surface as a generic 500 (no `MaxUploadSizeExceededException` handler in `GlobalExceptionHandler.java`).
- Impact: Larger (realistic) KYC documents fail with an opaque server error.
- Fix approach: Set `spring.servlet.multipart.max-file-size: 10MB` (+ `max-request-size`), and add a `MaxUploadSizeExceededException` handler returning 400 with a clear message.

**Idempotency records are not user-scoped:**
- Issue: `IdempotencyFilter.java:99-112` — the user extraction block is effectively dead code (the `instanceof` branch does nothing; `userId` stays null), so cached responses (`serveCachedResponse`/`replay`) are served to any caller presenting the same key + path.
- Impact: If keys are ever predictable, a second user could receive the first user's cached transaction response (transaction reference disclosure).
- Fix approach: Resolve the authenticated user id in the filter (it runs after `JwtAuthFilter` in the chain) and scope `tryAcquire`/`getCachedResponse` by `userId`.

**`AuthService.refreshAccessToken` error handling relies on message equality:**
- Issue: `AuthService.java:231` compares `e.getMessage().equals("Refresh token has been revoked")` to decide family revocation.
- Impact: Brittle — a message reword changes behavior; also if the revoked-token lookup fails the exception propagates before cleanup completes.
- Fix approach: Add a typed exception (e.g. `RevokedTokenException`) instead of string matching.

**GlobalExceptionHandler returns raw `exception.getMessage()` to clients:**
- Issue: `GlobalExceptionHandler.java:33,41,49,57,65,127` echo `exception.getMessage()` for NOT_FOUND/DUPLICATE/UNAUTHORIZED/BAD_REQUEST/RATE_LIMITED/missing-header codes.
- Impact: Internal identifiers or framework wording can leak (e.g. resource ids embedded in messages).
- Fix approach: Return safe generic messages; log detail server-side.

## Known Bugs

**`ApiClient` 401-retry uses a bare `Dio()` instance:**
- Issue: `novawallet-app/lib/core/network/api_client.dart:288` — `Dio().fetch<dynamic>(options)` constructs a fresh client with no baseUrl-driven defaults and, critically, no interceptors, so the retried response is not normalized into `ApiException` and timeout/option defaults are lost.
- Fix: Retry through the same `_dio` (guard with the existing `auth_retried` flag) so interceptors and options apply.

**Single-flight refresh reports success when the in-flight refresh failed:**
- Issue: `api_client.dart:171-174` — a second caller awaiting an in-flight refresh returns `true` unconditionally, even when the in-flight refresh failed (cleared session on 401, or network error). It then retries the original request with a dead/absent token.
- Fix: `await _refreshInFlight` must return the in-flight future's *result*, not `true`.

**Email verification link method mismatch (see Tech Debt) — clicking the emailed link yields HTTP 405.**

**`RateLimitFilter` is proxy-blind:**
- Issue: `RateLimitFilter.java:82-88` keys on `request.getRemoteAddr()`; no `server.forward-headers-strategy` is configured in any profile, so behind a reverse proxy/load balancer every client shares the proxy's IP.
- Impact: One client exhausting the shared bucket (100 req/min default, 10 for auth) 429s *all* users; conversely per-user isolation never works in production.
- Fix: Set `server.forward-headers-strategy: framework` in prod, honor `X-Forwarded-For`, and prefer the authenticated `userId` key when present.

## Security Considerations

**Committed dev DB credential:** `novawallet-api/src/main/resources/application-dev.yml:12` (see Tech Debt). Highest-priority hygiene fix; rotate the dev password and scrub history or move to env-driven.

**Default-profile footgun:** `application.yml:6-7` sets `spring.profiles.active: dev`, and the base file carries real fallbacks for every secret: JWT dev secret (`application.yml:35`, known base64 value), admin password `Admin@123` (`application.yml:45`), DB `localhost:5432/novawallet`. The Dockerfile forces prod (`Dockerfile` `--spring.profiles.active=prod`) and `application-prod.yml` fail-fasts on `${JWT_SECRET}`/`${APP_ADMIN_PASSWORD}`/`${DATABASE_PASSWORD}` — but any bare `java -jar` run (or mis-set profile) silently boots with the dev secret and default admin password. Recommend `profiles.active: ${SPRING_PROFILES_ACTIVE:prod}`… at minimum remove the JWT dev-secret and admin-password defaults from the *base* config so a non-prod boot fails loudly.

**Login/brute-force controls are in-memory and per-instance:**
- Issue: `LoginRateLimiter.java` (Caffeine, email+IP) and `RateLimitFilter.java` (Caffeine) are per-JVM. With N app instances, effective attempts scale N× (5 logins / 15 min per instance).
- Fix: Shared store (Redis) or DB-backed counters for production; document single-instance assumption otherwise.

**Email bodies (incl. tokens) logged in cleartext when SMTP is unconfigured:** `MailService.java:107-111` logs full body at INFO — the default in dev. Any log aggregation leak exposes verification/reset tokens. Redact tokens from log output.

**Registration endpoint is an email/phone oracle:** `AuthService.register` (`AuthService.java:71-76`) returns distinct `DuplicateResourceException` ("Email already registered") — user enumeration. Acceptable MVP tradeoff but flag for rate-limiting/account-takeover-aware design; the auth endpoints are limited to 10 req/min/IP only.

**App defaults to cleartext HTTP:** `novawallet-app/lib/core/config/app_config.dart:20-24` defaults to `http://10.0.2.2:8080/api` / `http://localhost:8080/api`; `AndroidManifest.xml` declares no `networkSecurityConfig`/`usesCleartextTraffic`. If a release build ships without `--dart-define=API_BASE_URL=https://…`, credentials and PINs travel in cleartext (on Android ≥ 9 the calls would fail rather than leak, but iOS/web would not). Enforce HTTPS in release builds (fail-fast when `API_BASE_URL` is absent or non-https) and add `network_security_config.xml`.

**KYC upload MIME check trusts client Content-Type:** `KycService.validateFile` (`KycService.java:33-35, 177`) accepts based on `file.getContentType()` — spoofable; no magic-byte sniffing, no malware scan. Acceptable MVP, but flag before regulated launch.

**PIN/withdraw flow:** PIN is BCrypt-hashed with 3-attempt/15-min lockout (`AuthService.java:41-42, 159-163`) — good. Note `verifyPin` is invoked before balance checks in `TransactionService.withdraw` (`TransactionService.java:144`), so a wrong PIN consumes an attempt even when the balance is insufficient — minor UX/attacker-noise issue.

## Performance Bottlenecks

**Per-request DB user lookup on every authenticated call:** `JwtAuthFilter.java` → `userDetailsService.loadUserById(userId)` hits the DB for each request. `CustomUserDetailsService` is not cached.
- Fix: Cache `UserDetails` in Caffeine keyed by userId with short TTL, or issue JWT claims sufficient for authorization and load the user lazily only where the payload is needed.

**`@CacheEvict(allEntries = true)` on every transfer:** `TransactionService.java:218` evicts the entire `walletBalances` cache on each transfer → thundering-herd repopulation under load.
- Fix: Evict only the two affected wallet keys (sender + receiver) — the deposit/withdraw methods already do keyed eviction.

**Idempotency replay busy-waits in the request thread:** `IdempotencyFilter.java:161-175` polls the DB with `Thread.sleep(100ms)` × 10 (1s) per duplicate, blocking a servlet thread. Low volume today; under a retry storm this compounds.
- Fix: Reduce poll attempts / back off exponentially, or return 409 immediately and let the client back off.

## Fragile Areas

**`novawallet-app/lib/core/network/api_client.dart` (330 LOC, the app's most complex file):** refresh single-flight + retry loop + envelope unwrapping + idempotency headers, with **zero tests**. The retry/refresh interplay (see Known Bugs) is the highest-risk logic in the app. Add unit tests with a mocked `Dio` adapter before touching it.

**Filter ordering assumptions:** `RateLimitFilter`, `IdempotencyFilter`, and `JwtAuthFilter` ordering is implicit (component filters vs security chain). `IdempotencyFilter.java:100-112` already depends on `getUserPrincipal()` being populated; `RateLimitFilter.java:83-87` has the same latent assumption. If filter registration order changes (e.g., switching to `FilterRegistrationBean` or a servlet container update), user-scoped rate limiting silently degrades. Make ordering explicit via `FilterRegistrationBean` + `@Order`.

**`TransactionService.transfer` (388 LOC, `TransactionService.java:219-366`):** money movement with lock ordering, dual atomic updates, 4 transaction rows, 2 audit entries, 2 notification fan-outs. Recently split from a 443-LOC god class — re-split candidates remain: transfer orchestration vs notification fan-out.

**H2-based integration tests do not exercise PostgreSQL semantics:** `application-test.yml` runs Flyway migrations against `jdbc:h2:mem:testdb;MODE=PostgreSQL`; CI's Postgres service is commented out (`novawallet-api/.github/workflows/ci.yml`). Pessimistic `SELECT … FOR UPDATE` (`WalletRepository.findByIdWithLock`), deadlock behavior, and advisory locking are untested against the production engine.
- Safe modification: Never assume H2 validated a locking/DDL behavior; run migrations + concurrency tests against real Postgres 16 before release.

**Config fallbacks across profiles:** three YAML layers (`application.yml` → `application-dev.yml` → `application-prod.yml`) with overlapping keys (rate limits, KYC tiers, CORS origins are duplicated in base + prod). KYC tier values are declared twice (`application.yml:54-77` and `application-prod.yml`); drift between them silently changes limits per profile. Single-source the config or add a profile-consistency test.

## Scaling Limits

**In-memory rate limiting:** `RateLimitFilter` (50k entries) + `LoginRateLimiter` (10k entries) are per-instance; multi-node deployments multiply brute-force allowances and break global limits. Move to Redis before horizontal scaling.

**Local-disk KYC uploads:** `uploads/kyc` on the container filesystem — not shared across instances, lost on redeploy (unless volume-mounted; compose mounts are local), unbounded growth (no retention job). Move to object storage with lifecycle rules.

**Caffeine caches** (`walletBalances`, rate-limit counters) are single-node by design; no distributed cache.

## Dependencies at Risk

**No CVE scanning:** CI (`ci.yml`) runs tests + image build only — no `dependency-check`/Dependabot/Trivy. Versions are current (Spring Boot 3.5.3, JJWT 0.12.7, springdoc 2.8.9), but regressions would go unnoticed. Add OWASP dependency-check to CI and Dependabot to the repo.

**CI never executed against a remote:** repo has no remote yet; `ci.yml` is unverified (`HANDOFF_2026-08-06.md`: "CI workflow exists but not yet run (no remote)"). First push should validate.

**Flutter dependency set is small and current** (`dio ^5.11.0`, `riverpod ^2.6.1`, `go_router ^17.4.0`, `flutter_secure_storage ^11.0.0`) — no concerns; `pubspec.lock` is committed.

## Missing Critical Features

- **Mobile money / payment provider integration** (Flutterwave per SRS) — no callbacks, no reconciliation, no `PENDING`/`CONFIRMED` transaction states (`HANDOFF_2026-08-06.md` pending list).
- **SMS delivery** — stubbed (`SmsService.java`).
- **Readable notifications API** — write-only backend; static app screen (`notifications_screen.dart`).
- **Flutter app test suite** — see below.
- **Remote git + activated CI** — blocks Dependabot, deployment verification.
- **Audit log search/export** — `AuditLogRepository` + `AdminService` exist; no evidence of admin-facing audit retrieval UX (admin screens in-app are minimal).

## Test Coverage Gaps

**Flutter app — effectively zero tests (critical):** the entire `novawallet-app/test/` tree is one smoke test (`test/widget_test.dart`, checks a theme color). Untested: `ApiClient` refresh/retry/idempotency logic (`api_client.dart`), `TokenStorage`, `AuthNotifier` session restore/logout (`auth_provider.dart`), all repositories, all screens, router redirect gates (`router.dart:41-77` — auth/PIN gating logic is pure logic with no tests).
- Priority: High — this is the money-movement client.

**Backend:** strong integration coverage (concurrency, idempotency, rate-limit, refresh-rotation per `SCAN-REPORT.md`), but:
- No tests against real PostgreSQL (H2 only — see Fragile Areas).
- `IdempotencyFilter` user-scoping branch and `RateLimitFilter` user-key branch are effectively dead code paths — untested and misleading.
- `MailService`/`SmsService` stub branches (no-SMTP logging) untested.
- No test asserting the email-verify GET-vs-POST contract (the current bug shipped).

---

*Concerns audit: 2026-08-06*
