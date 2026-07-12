# NovaWallet — Project Plan

## Overview

| **Project** | NovaWallet |
|-------------|-----------|
| **Purpose** | Digital wallet backend API (RESTful, no frontend in MVP) |
| **Stack** | Java 17, Spring Boot 4.1, Maven, PostgreSQL, Flyway, JWT (jjwt), SpringDoc OpenAPI |
| **Target** | University students & small businesses in Zambia |
| **Status** | Phases 0–5 (Foundation → Idempotency) ✅ complete. Phase 5.5 (Fintech Hardening) ✅ complete. Phase 6 (Notifications, Scheduling & Caching) ✅ complete. Phase 7–8 in progress. Phase 9 (Docker & CI/CD) ✅ complete. Phase 10–11 pending. See Day-by-Day schedule below for details |

---

## Phase 0 — Project Foundation
**Goal**: Build out the dependency graph, base configuration, project packaging, and code standards.

| Step | Task | Details |
|------|------|---------|
| 0.1 | **Expand `pom.xml`** | Add missing dependencies: `spring-boot-starter-security`, `spring-boot-starter-actuator`, `spring-boot-starter-cache`, `spring-boot-starter-mail`, `jjwt-api/impl/jackson` (0.12.x), `springdoc-openapi-starter-webmvc-ui` (2.x), `h2` (test scope), `caffeine` (cache), `lombok` (already present), `spring-boot-starter-web` (use webmvc) |
| 0.2 | **Multi-profile config** | `application.yml` base + `application-dev.yml` (H2, auto-DDL) + `application-prod.yml` (PostgreSQL, flyway validate) |
| 0.3 | **Base package structure** | `com.novawallet` root with sub-packages: `config`, `entity`, `dto`, `repository`, `service`, `controller`, `security`, `exception`, `audit`, `scheduler` |
| 0.4 | **Global exception handler** | `@RestControllerAdvice` — uniform error response: `{status, code, message, timestamp, path}` |
| 0.5 | **Standard API response wrapper** | `ApiResponse<T>` envelope: `{success, data, message, timestamp}` for all successful responses |
| 0.6 | **Flyway baseline migration** | `V1__init_schema.sql` — create initial tables |
| 0.7 | **Security headers filter** | HSTS, X-Content-Type-Options, X-Frame-Options, CSP (basic) via `SecurityFilterChain` |
| 0.8 | **Health check verification** | `/actuator/health` and `/actuator/info` working |
| 0.9 | **Swagger/OpenAPI config** | `/swagger-ui.html` + `api-docs` endpoint with security scheme defined |
| 0.10 | **MDC request tracing** | Add `MDC` filter to inject `X-Request-ID` (or `trace-id`) into every log line |

**Deliverable**: Application boots, serves Swagger UI, actuator health, and returns consistent JSON error format.

---

## Phase 1 — User & Authentication Core
**Goal**: Register, login, JWT issuance, email verification, transaction PIN setup, rate limiting, password management. Wallet is created after KYC approval (Phase 1.5).

| Step | Domain | Task | Details |
|------|--------|------|---------|
| 1.1 | Entity | **User entity** | `id (UUID)`, `firstName`, `lastName`, `email` (unique), `phone` (unique), `passwordHash`, `pinHash`, `pinAttempts`, `pinLockedUntil`, `role` (USER/ADMIN), `emailVerified` (boolean), `verificationToken`, `passwordResetToken`, `passwordResetExpiresAt`, `deleted` (boolean), `deletedAt`, `createdAt`, `updatedAt`, `@Version` |
| 1.2 | Entity | **Role enum** | `USER`, `ADMIN` |
| 1.3 | Entity | **Wallet entity** | `id (UUID)`, `userId`, `accountNumber` ("NW" + 10 digits), `balance` (BigDecimal scale=2), `currency` ("ZMW"), `status` (ACTIVE/FROZEN), `freezeReason` enum (SUSPICIOUS_ACTIVITY/ADMIN_ACTION/USER_REQUEST/null), `@Version`, `createdAt`, `updatedAt` |
| 1.4 | Entity | **RefreshToken entity** | `id (UUID)`, `userId`, `tokenHash`, `expiresAt`, `createdAt`, `revoked` (boolean) |
| 1.5 | Migration | **Flyway V1..V3** | `V1__create_users_table.sql`, `V2__create_wallets_table.sql`, `V3__create_refresh_tokens_table.sql` |
| 1.6 | Security | **JWT utility** | Generate + validate access tokens (jjwt), configurable expiry (default 15 min), secret from `application.yml` |
| 1.7 | Security | **Spring Security config** | `SecurityFilterChain` — permit `/api/v1/auth/**`, `/swagger-ui/**`, `/api-docs/**`, `/actuator/**`; all else authenticated; stateless session; `BCryptPasswordEncoder` bean |
| 1.8 | Security | **JWT auth filter** | `OncePerRequestFilter` — extract JWT from `Authorization: Bearer <token>`, validate, set `SecurityContextHolder` |
| 1.9 | DTO | **Auth DTOs** | `RegisterRequest`, `LoginRequest`, `AuthResponse` (accessToken, refreshToken, expiresIn), `ApiResponse<T>` |
| 1.10 | Service | **AuthService** | `register()` — validate uniqueness, hash password, create User, issue tokens, send verification email (async); `login()` — verify credentials, issue tokens |
| 1.11 | Service | **Wallet generation** | `AccountNumberGenerator` — atomic sequence-based "NW" + 10-digit zero-padded number, retry on collision. Used when wallet is created (post-KYC, not on registration) |
| 1.12 | Controller | **AuthController** | `POST /v1/auth/register`, `POST /v1/auth/login`, `POST /v1/auth/refresh`. Integrates `LoginRateLimiter` for brute-force protection |
| 1.13 | Controller | **PinController** | `POST /v1/pin` (separate from auth) — authenticated, accepts 4-6 digit PIN, BCrypt hashed, stored on User |
| 1.14 | Enhancement | **PIN rate limiting** | Track `pinAttempts`, `pinLockedUntil` — lock for 15 min after 3 failed attempts |
| 1.15 | Controller | **VerificationController** | `POST /v1/email/verify?token=` — marks `emailVerified=true`, clears token |
| 1.16 | Controller | **PasswordController** | `POST /v1/password/forgot` → email link (async); `POST /v1/password/reset` with token + new password, revokes all sessions |
| 1.17 | Enhancement | **Strong password validation** | `@Pattern` regex: min 8 chars, must contain uppercase + lowercase + digit + special character |
| 1.18 | Service | **MailService** | Async (`@Async`) email sending. Sends via `JavaMailSender` when SMTP is configured, logs to console in dev |
| 1.19 | Enhancement | **Login rate limiter** | `LoginRateLimiter` — 5 failed attempts per email+IP → 15-min lockout. Resets on successful login |
| 1.20 | Filter | **RateLimitFilter** | Token-bucket algorithm (Caffeine). 100 req/min default, 10 req/min for auth endpoints. `X-RateLimit-*` headers, 429 JSON response. Auto-registered as servlet filter (before Spring Security) |
| 1.21 | Enhancement | **Logout** | Revokes all refresh tokens for the user |
| 1.22 | Test | **AuthService unit tests** | Mock repository, verify password hashing, token issuance, validation errors, email verification, password reset |
| 1.23 | Test | **AuthController integration tests** | Full request/response cycle with H2 |

**Deliverable**: Register + login with JWT working. PIN set. Email verification + password reset flows functional. Rate limiting (global + login lockout). Password strength validation.

---

## Phase 2 — Transaction Core
**Goal**: Deposit, withdraw, transfer with atomicity, PIN validation, ledger-style records, and human-friendly transaction references.

| Step | Domain | Task | Details |
|------|--------|------|---------|
| 2.1 | Entity | **Transaction entity** | `id (UUID)`, `reference` ("NWTX" + yyyyMMdd + 8-digit sequence), `type` (DEPOSIT/WITHDRAWAL/TRANSFER_DEBIT/TRANSFER_CREDIT/FEE), `amount` (BigDecimal), `balanceBefore`, `balanceAfter`, `status` (PENDING/SUCCESSFUL/FAILED), `description`, `senderWalletId`, `receiverWalletId`, `relatedTransactionId` (for linking debit/credit pairs), `createdAt`, `updatedAt`, `@Version` |
| 2.2 | Migration | **Flyway V4** | `V4__create_transactions_table.sql` |
| 2.3 | DTO | **Transaction DTOs** | `DepositRequest`, `WithdrawRequest`, `TransferRequest`, `TransactionResponse`, `TransactionReference` |
| 2.4 | Service | **Deposit** | `deposit(walletId, amount, description)` — find wallet (active), validate amount > 0, increment balance via `@Transactional`, create DEPOSIT record (SUCCESSFUL), record balanceBefore/balanceAfter |
| 2.5 | Service | **Withdraw** | `withdraw(walletId, amount, pin, description)` — verify PIN, validate balance >= amount, decrement balance, create WITHDRAWAL record |
| 2.6 | Service | **Transfer** | `transfer(senderWalletId, receiverWalletId, amount, pin, description)` — verify PIN, validate not self-transfer, check balance >= amount, validate receiver exists and active, debit sender, credit receiver, create TRANSFER_DEBIT + TRANSFER_CREDIT pair linked by `relatedTransactionId` |
| 2.7 | Service | **Reference generator** | `TransactionReferenceGenerator` — atomic sequence: `NWTX{yyyyMMdd}{00000001}` format, safe under concurrent access |
| 2.8 | Service | **Balance integrity** | Wallet balance = sum of all balance changes from transaction records (ledger-first mental model). Updates use `UPDATE wallets SET balance = balance + ? WHERE id = ? AND balance + ? >= 0` (optimistic lock fallback) |
| 2.9 | Controller | **TransactionController** | `POST /api/v1/wallets/{walletId}/deposit`, `POST /api/v1/wallets/{walletId}/withdraw`, `POST /api/v1/transfers` |
| 2.10 | Validation | **Input validation** | `@Positive` amount, `@NotBlank` descriptions, `@Size(min=4, max=6)` PIN, `@NotNull` wallet IDs |
| 2.11 | Audit | **Balance change audit** | Every deposit/withdraw/transfer writes async audit log entry (entityType=WALLET, action=DEPOSIT/WITHDRAWAL/TRANSFER_DEBIT/TRANSFER_CREDIT, oldBalance, newBalance, performedBy=userId, IP) |
| 2.12 | Test | **TransactionService unit tests** | Mock wallet repo, verify balance changes, PIN validation, constraint enforcement |
| 2.13 | Test | **TransactionController integration tests** | Full deposit/withdraw/transfer cycles with H2, verify DB state after each |

**Deliverable**: Money moves through the system atomically. Deposits, withdrawals, and transfers create proper ledger trail. Transaction references are human-friendly.

---

## Phase 3 — Transaction History & Querying
**Goal**: List transactions with filtering, pagination, and sorting. Users see their own history only.

| Step | Domain | Task | Details |
|------|--------|------|---------|
| 3.1 | Repository | **TransactionRepository** | Spring Data JPA with `Specification` for dynamic filtering |
| 3.2 | Service | **TransactionHistoryService** | `getTransactionHistory(walletId, pageable, filters)` — type filter, date range, status filter, paginated, sorted by createdAt desc |
| 3.3 | DTO | **Paged response** | `PagedResponse<T>` — `{content, page, size, totalElements, totalPages, last}` |
| 3.4 | Controller | **History endpoint** | `GET /api/v1/wallets/{walletId}/transactions` — query params: `type`, `status`, `from`, `to`, `page`, `size`, `sort` |
| 3.5 | Security | **Ownership check** | Users can only see their own wallet's transactions (AOP or manual check) |
| 3.6 | Enhancement | **Single transaction lookup** | `GET /api/v1/transactions/{reference}` — public-ish, user must own at least one of the involved wallets |
| 3.7 | Enhancement | **Wallet balance endpoint** | `GET /api/v1/wallets/{walletId}/balance` — returns current balance with currency |
| 3.8 | Test | **History service tests** | Pagination, filtering, ownership edge cases |

**Deliverable**: Full read-side of the transaction system with pagination and filtering.

---

## Phase 4 — Fee Engine & Audit Logging
**Goal**: Configurable fee calculation per transaction type. Audit log for all balance changes.

| Step | Domain | Task | Details |
|------|--------|------|---------|
| 4.1 | Entity | **FeeConfiguration entity** | `id`, `transactionType` (TRANSFER/WITHDRAWAL/DEPOSIT), `percentageFee` (BigDecimal), `flatFee` (BigDecimal), `minFee`, `maxFee`, `active` (boolean), `createdAt`, `updatedAt` |
| 4.2 | Migration | **Flyway V5, V6** | `V5__create_fee_configurations_table.sql`, `V6__create_audit_logs_table.sql` |
| 4.3 | Entity | **AuditLog entity** | `id (UUID)`, `entityType` (string), `entityId` (UUID), `action` (string), `oldValue` (text), `newValue` (text), `performedBy` (UUID), `ipAddress` (string), `createdAt` (timestamp) — **append-only**, no update/delete operations |
| 4.4 | Service | **FeeEngineService** | `calculateFee(transactionType, amount)` — loads active config, computes: `totalFee = (amount * percentageFee / 100) + flatFee`, clamped by min/max, rounded to 2 decimals HALF_EVEN |
| 4.5 | Service | **Fee deduction** | Modify transfer flow to deduct fee from sender's wallet and create FEE transaction record linked to transfer |
| 4.6 | Controller | **Fee estimation** | `GET /api/v1/fees/estimate?type=TRANSFER&amount=100.00` — read-only, no auth required (or lightweight auth) |
| 4.7 | Service | **AuditService** | `recordAction(entityType, entityId, action, oldValue, newValue, performedBy, ipAddress)` — `@Async` (non-blocking), returns `CompletableFuture<Void>` |
| 4.8 | AOP | **Audit annotation** | `@Audited(action="DEPOSIT")` — optional AOP aspect that auto-captures method params for balance changes |
| 4.9 | Controller | **Admin fee config** | `GET/PUT /api/v1/admin/fees/{id}` — ADMIN-only CRUD for fee configurations |
| 4.10 | Test | **FeeEngine tests** | Percentage + flat, rounding, min/max clamping, inactive config fallback |
| 4.11 | Test | **AuditService tests** | Verify async behavior, data capture |

**Deliverable**: Fees calculated and deducted on transfers. Audit log recording every balance change asynchronously.

---

## Phase 5 — Idempotency ✅ COMPLETE
**Goal**: Duplicate charge prevention for financial operations.

> **Note**: Rate limiting (5.7–5.9), refresh token rotation (5.1–5.2), and logout (5.3) were **accelerated and completed in Phase 1**. Idempotency was completed in Phase 5.

| Step | Domain | Task | Details |
|------|--------|------|---------|
| 5.4 | Entity | **IdempotencyKey entity** | `id`, `key` (unique), `responseBody` (text/JSON), `responseStatusCode` (int), `status` (PROCESSING/COMPLETED), `expiresAt` (createdAt + TTL), `@Version` optimistic locking |
| 5.5 | Migration | **Flyway V9** | `V9__create_idempotency_keys_table.sql` — includes UNIQUE constraint on `idempotency_key`, index on `expires_at`, status column, version column |
| 5.6 | Filter | **IdempotencyFilter** | `OncePerRequestFilter` — extract `Idempotency-Key` header for mutating POST endpoints (skip GET/HEAD/OPTIONS/TRACE); atomic INSERT-first with `saveAndFlush()` → DB unique constraint prevents concurrent duplicates; poll-and-replay for losing requests (10×100ms) with 409 Conflict on timeout; non-2xx error recovery deletes PROCESSING record for retry |
| 5.7 | Service | **IdempotencyService** | `tryAcquire(key, userId, endpoint, method)` — atomic INSERT with `saveAndFlush()`, catches `DataIntegrityViolationException` for concurrent losers; `complete(key, statusCode, body)` — updates PROCESSING → COMPLETED with cached response; `getCachedResponse(key)` — returns cached body/status; `deleteProcessing(key)` — removes failed/crashed PROCESSING records |
| 5.8 | Schedule | **IdempotencyCleanupJob** | `@Scheduled(cron = "0 0 4 * * ?")` — deletes expired records where `expires_at < NOW()` |
| 5.9 | Config | **application.yml** | `app.idempotency.ttl-hours: 24` |
| 5.10 | Test | **Idempotency integration tests** | Deposit with Idempotency-Key → byte-for-byte identical cached response; different keys produce different transactions; no header → normal processing; GET ignores key; withdrawal with key; transfer with key |
| 5.11 | Framework | **@EnableScheduling** | Added to `NovawalletApiApplication` to enable the cleanup cron job |

**Deliverable**: Idempotent financial operations with atomic INSERT-first race-condition safety, poll-and-replay caching, error recovery, and automated TTL cleanup.

---

## Phase 5.5 — Fintech Hardening ✅ COMPLETE
**Goal**: Production-hardening fixes identified during fintech-readiness audit. Strengthen concurrency, enforce KYC limits, unify error responses, add CORS, paginate admin endpoints, and audit admin actions.

| Step | Domain | Task | Details |
|------|--------|------|---------|
| 5.5.1 | Repository | **Pessimistic locking** | `WalletRepository.findByIdWithLock(id)` — `@Lock(PESSIMISTIC_WRITE)` + `SELECT ... FOR UPDATE`. Used in `deposit()`, `withdraw()`, `transfer()`. Sender locked before receiver (consistent order prevents deadlocks) |
| 5.5.2 | Service | **KYC tier enforcement** | `TransactionService.enforceDailySendLimit()` — checks `dailySendLimit` from `KycConfig` before withdrawals/transfers using `TransactionRepository.sumDailyOutgoing()`. `deposit()` enforces `walletLimit` from KYC tier. Only enforced for APPROVED KYC users (tier ≥ 1) |
| 5.5.3 | Filter | **Unified error response** | `RateLimitFilter` changed from `{success,message,code}` to match `ErrorResponse` format: `{status,code,message,timestamp,path}` |
| 5.5.4 | Config | **CORS configuration** | `CorsConfigurationSource` bean in `SecurityConfig`. Allowed origins: localhost:3000, 5173, 8100, capacitor://localhost. Exposes `X-RateLimit-*` headers |
| 5.5.5 | Controller | **Admin pagination** | `AdminService.getAllUsers(Pageable)` returns `Page<UserSummaryResponse>`. `AdminController.getAllUsers(?page=0&size=20)` with sorting by `createdAt DESC`. Response wrapped in `PagedResponse` |
| 5.5.6 | Service | **Admin audit logging** | `WalletService.freezeWallet/unfreezeWallet(adminId)` — records audit via `auditService.recordAction()`. `AdminService.toggleUserDeletedStatus(adminId)` — records audit. Controller passes `@AuthenticationPrincipal adminId` |
| 5.5.7 | Test | **Regression verification** | Updated KYC admin test JSON paths for pagination. Fixed KYC limit enforcement scope (approved users only). Added `Sort.by("createdAt").descending()` for test order stability. All 105 tests passing |

**Deliverable**: Pessimistic write locks on wallet operations, KYC tier limits enforced, unified API error format, CORS configured, admin pagination, admin actions audit-logged.

---

## Phase 6 — Notifications, Scheduling & Caching
**Goal**: Async email/SMS alerts, additional scheduled cleanup jobs, and cache for hot data.

> **Note**: Idempotency key cleanup (6.8) was accelerated and completed in Phase 5.

| Step | Domain | Task | Details |
|------|--------|------|---------|
| 6.1 | Entity | **Notification entity** | `id (UUID)`, `userId`, `type` (EMAIL/SMS), `channel`, `recipient`, `subject`, `body`, `status` (PENDING/SENT/FAILED), `retryCount`, `maxRetries` (3), `createdAt`, `sentAt` | ✅ |
| 6.2 | Entity | **NotificationAttempt entity** | `id`, `notificationId`, `attemptNumber`, `status`, `errorMessage`, `attemptedAt` | ✅ |
| 6.3 | Migration | **Flyway V10** | `V10__create_notifications_tables.sql` — unified: notifications + notification_attempts tables with indexes on user_id, notification_id, status, created_at | ✅ |
| 6.4 | Service | **NotificationService** | `sendEmail(userId, email, type, subject, message)` — `@Async`, creates Notification record, calls `MailService.sendRaw()`, updates status on completion/failure. `sendSms()`, `sendBoth()`, `recordSent()` for MailService-compatible audit, `retryFailed()` for up to 3 retries | ✅ |
| 6.5 | Service | **SmsService** | `sendSms(phone, message)` — Africa's Talking API integration stub. Logs to console when no API key configured. Truncates to 160 chars. Returns `true` in dev mode | ✅ |
| 6.6 | Integration | **Transaction notifications** | `TransactionService.deposit()` → fires `TRANSACTION_DEPOSIT` notification. `withdraw()` → `TRANSACTION_WITHDRAWAL`. `transfer()` → `TRANSACTION_TRANSFER_SENT` (sender) + `TRANSACTION_TRANSFER_RECEIVED` (receiver) | ✅ |
| 6.7 | Scheduler | **Pending transaction cleanup** | `PendingTransactionCleanupJob` — `@Scheduled(cron = "0 0 3 * * *")` — marks PENDING transactions older than 24h as FAILED (idempotent) | ✅ |
| 6.8 | Scheduler | **Idempotency key cleanup** | Completed in Phase 5 — `IdempotencyCleanupJob` at 4am cron | ✅ |
| 6.9 | Scheduler | **Expired refresh token cleanup** | `RefreshTokenCleanupJob` — `@Scheduled(cron = "0 30 4 * * *")` — deletes expired refresh tokens via `deleteByExpiresAtBefore()` | ✅ |
| 6.10 | Caching | **Cache configuration** | `CacheConfig` — Caffeine: max 100 entries, 10 min write expiry, `recordStats()`, `@EnableCaching` | ✅ |
| 6.11 | Caching | **Cache eviction** | `@CacheEvict("walletBalances")` on `deposit(walletId)`, `withdraw(walletId)`, `transfer(allEntries = true)` | ✅ |
| 6.12 | Config | **Email config** | `spring.mail.*` in `application.yml` — SMTP host/port/credentials configured for Mailtrap in dev | ✅ |
| 6.13 | Test | **Notification retry tests** | `NotificationServiceIntegrationTest` — 7 tests covering: sendEmail success, sendEmail failure, recordSent, retry qualifying, retry exhaustion, sendSms, sendBoth | ✅ |
| 6.14 | Test | **Full regression** | 112/112 tests passing — 99 existing + 7 notification + 6 idempotency | ✅ |

**Deliverable**: Email/SMS alerts on transactions (deposit, withdraw, transfer sender + receiver). 3 daily cleanup jobs (pending transactions 3am, idempotency keys 4am, refresh tokens 4:30am). Caffeine cache with balance eviction on transaction. `@Async` notification delivery with up to 3 retries. 7 notification integration tests + 112 total tests passing.

---

## Phase 7 — Admin Endpoints & Production Hardening
**Goal**: Admin dashboard API, soft delete, soft freeze with reasons, and security hardening.

| Step | Domain | Task | Details |
|------|--------|------|---------|
| 7.1 | Controller | **Admin user management** | `GET /api/v1/admin/users` (paginated, searchable), `GET /api/v1/admin/users/{id}` (detail with wallet), `GET /api/v1/admin/wallets` (all wallets with status filter) |
| 7.2 | Controller | **Admin wallet actions** | `PATCH /api/v1/admin/wallets/{id}/freeze` (with reason: SUSPICIOUS_ACTIVITY/ADMIN_ACTION/USER_REQUEST), `PATCH /api/v1/admin/wallets/{id}/unfreeze` |
| 7.3 | Controller | **Admin transaction search** | `GET /api/v1/admin/transactions` — search by reference, wallet, user, date range, type, status |
| 7.4 | Controller | **Admin audit log view** | `GET /api/v1/admin/audit-logs` — paginated, filterable by entityType, action, performedBy, date range |
| 7.5 | Controller | **Admin fee management** | Full CRUD on `FeeConfiguration`: `GET/POST/PUT/DELETE /api/v1/admin/fees` |
| 7.6 | Security | **Role-based access** | `@PreAuthorize("hasRole('ADMIN')")` on all admin endpoints |
| 7.7 | Enhancement | **Soft delete user** | `PATCH /api/v1/admin/users/{id}/deactivate` — sets `deleted=true`, `deletedAt=now`, freezes wallet. `GET` filters out deleted by default |
| 7.8 | Enhancement | **Security headers audit** | Verify HSTS, CSP, XSS protection, content-type options. Add `Strict-Transport-Security` in production profile |
| 7.9 | Enhancement | **Input sanitization** | Strip/escape HTML from string inputs to prevent log injection and stored XSS (defense in depth) |
| 7.10 | Test | **Admin endpoint security tests** | Verify USER cannot access ADMIN endpoints (403) |

**Deliverable**: Complete admin API. Soft delete, freeze reasons, role enforcement. Security hardening complete.

---

## Phase 8 — Testing & Quality Assurance
**Goal**: Comprehensive test coverage, not just happy paths.

| Step | Domain | Task | Details |
|------|--------|------|---------|
| 8.1 | Unit | **Service layer tests** | All services: `AuthService`, `TransactionService`, `WalletService`, `FeeEngineService`, `AuditService`, `NotificationService` |
| 8.2 | Unit | **Security tests** | `JwtUtil`, `JwtAuthFilter`, `RateLimitingFilter`, `IdempotencyFilter` |
| 8.3 | Unit | **Fee engine edge cases** | Zero amount, max/min clamping, rounding scenarios, inactive config fallback, concurrent calculation |
| 8.4 | Integration | **Full transaction flow** | H2-based: register → deposit → transfer → withdraw → verify all balances, transaction records, audit logs |
| 8.5 | Integration | **Concurrent transfer test** | Spawn threads attempting simultaneous transfers from same wallet, verify no negative balance and correct final balance |
| 8.6 | Integration | **Idempotency end-to-end** | Send same `Idempotency-Key` twice, verify second call returns cached response, only one transaction recorded |
| 8.7 | Integration | **Rate limiting end-to-end** | Exceed rate limit, verify 429 with correct headers |
| 8.8 | Integration | **Refresh token rotation** | Full cycle: login → refresh → old token rejected after rotation → reuse detection |
| 8.9 | Integration | **Scheduler idempotency** | Run pending cleanup twice, verify same FAILED count |
| 8.10 | Integration | **Admin authorization** | USER token gets 403 on admin endpoints, ADMIN token succeeds |

**Deliverable**: >85% code coverage on core business logic. All critical paths have integration tests.

---

## Phase 9 — Docker, CI/CD & Production Readiness ✅ COMPLETE
**Goal**: Containerize the application, add CI pipeline, and harden production configuration.

| Step | Domain | Task | Details |
|------|--------|------|---------|
| 9.1 | Infra | **Dockerfile** | Multi-stage build: Maven compile → JRE 17 runtime (alpine). Expose port 8080. Healthcheck via `/actuator/health`. Non-root user (`novawallet`). KYC upload directory volume | ✅ |
| 9.2 | Infra | **docker-compose.yml** | `api` service + `postgres:16-alpine` (with healthcheck) + `mailpit` (dev email via `--profile dev`). Persistent volumes for PostgreSQL data and KYC uploads. All secrets via environment variables | ✅ |
| 9.3 | Config | **application-prod.yml** | Env-var-based: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `SPRING_MAIL_*`, `APP_SMS_API_KEY`, `APP_CORS_ORIGINS`. HikariCP pool config (`max 10`, `min 2`). Metrics + Prometheus endpoints. Flyway `validate` mode | ✅ |
| 9.4 | CI | **GitHub Actions workflow** | `.github/workflows/ci.yml` — two-stage pipeline: (1) test — JDK 17, `mvn test` (112+ tests), upload results on failure; (2) build — Docker build + push to `ghcr.io` on push to main/master. GHA cache layers for rebuild speed | ✅ |
| 9.5 | Logging | **logback-spring.xml** | JSON structured logging in prod (via LogstashEncoder), colored console in dev with `%X{requestId}`, minimal output in test. Rolling file appender (30-day retention, 1GB cap) | ✅ |
| 9.6 | Auth | **CORS env-var support** | `SecurityConfig.corsConfigurationSource()` reads `app.cors.origins` from config (set via `APP_CORS_ORIGINS` env var), falls back to hardcoded dev origins | ✅ |
| 9.7 | Infra | **.dockerignore** | Excludes `target/`, `.git/`, `.omo/`, `.opencode/`, `docs/`, IDE files, env files from Docker build context | ✅ |
| 9.8 | Docs | **README.md** | (Updated earlier) Project overview, setup instructions, environment variables, API docs link, Docker instructions | ✅ |
| 9.9 | Docs | **API curl examples** | Document key flows: register → login → deposit → transfer → withdraw → refresh |

**Deliverable**: `docker-compose up` runs the full stack. CI pipeline builds and tests.

---

## Phase 10 — Flutter Frontend (Post-MVP)
**Goal**: Cross-platform mobile app for NovaWallet.

| Step | Task | Details |
|------|------|---------|
| 10.1 | Scaffold | Flutter project with `go_router`, `riverpod`/`bloc`, `dio` for HTTP |
| 10.2 | Auth screens | Register, login, PIN setup, forgot password, email verification |
| 10.3 | Dashboard | Balance display, quick actions (deposit/withdraw/send), recent transactions |
| 10.4 | Send money flow | Select contact/enter account number, enter amount, PIN confirmation, success screen |
| 10.5 | Transaction history | Paginated list with pull-to-refresh, type/date filters |
| 10.6 | Profile | View profile, change PIN, transaction settings |
| 10.7 | Push notifications | Firebase Cloud Messaging integration for transaction alerts |

**Deliverable**: Flutter app connecting to the NovaWallet API.

---

## Phase 11 — Payment Provider Integration (Post-MVP)
**Goal**: Live mobile money and card payments via Flutterwave/Paystack.

| Step | Task | Details |
|------|------|---------|
| 11.1 | Flutterwave integration | Deposit via mobile money (Airtel/MTN Zambia), webhook handling for payment confirmation |
| 11.2 | Paystack integration | Alternative payment gateway, card payments |
| 11.3 | Withdraw to mobile money | Payout API integration for cash-out to mobile money |
| 11.4 | Webhook security | Signature verification, idempotent webhook processing, idempotency keys from provider |
| 11.5 | Transaction status sync | Polling/webhook-based status updates, reconciliation |

**Deliverable**: Real money in/out via mobile money.

---

## Entity Relationship Summary

```
User (1) ──── (1) Wallet
User (1) ──── (N) RefreshToken
Wallet (1) ──── (N) Transaction (senderWallet)
Wallet (1) ──── (N) Transaction (receiverWallet)
User (1) ──── (N) Notification
Transaction (N) ──── (1) Transaction (relatedTransactionId) — self-referencing for paired records
FeeConfiguration — standalone, no FK
AuditLog — standalone, entityType + entityId as polymorphic reference
IdempotencyKey — standalone
NotificationAttempt (N) ──── (1) Notification
```

## Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Wallet balance | DB-stored + ledger-verifiable | Balance is stored with atomic `UPDATE` using `WHERE balance + ? >= 0` guard; periodic reconciliation possible |
| Money precision | BigDecimal scale=2, HALF_EVEN | Banking standard, ngwee (ZMW cent) precision |
| Concurrency | `@Lock(PESSIMISTIC_WRITE)` + `SELECT FOR UPDATE` | Prevents phantom reads and lost updates on concurrent transfers; sender locked before receiver to prevent deadlocks |
| Wallet locking | Repository-level `findByIdWithLock()` | Wallet locked before any balance change in the same `@Transactional` method — prevents two requests from seeing the same balance |
| KYC limit enforcement | Service-level check before transactions | `enforceDailySendLimit()` sums today's outgoing transactions and rejects if `dailySendLimit` exceeded; only applies to APPROVED KYC users |
| Audit writes | `@Async` + `REQUIRES_NEW` | Non-blocking and independent — won't roll back with main transaction |
| Admin audit | Explicit service method calls | Admin operations (freeze, toggle-delete) pass `adminId` to `AuditService.recordAction()` for full accountability |
| Token storage | BCrypt hash in DB (refresh tokens) | Server-side storage prevents MitM token theft persistence |
| Rate limiting | Token-bucket filter (Caffeine) | Constant memory, burst-tolerant, O(1) per check. Error format unified with `ErrorResponse` standard |
| CORS | Whitelist-based `CorsConfigurationSource` | Allows dev origins (localhost:3000, 5173, 8100, capacitor://localhost); no wildcard in production |
| Admin pagination | Spring Data `Pageable` + `PagedResponse` | Prevents OOM on large user sets; sorted by `createdAt DESC` |
| Migration strategy | Flyway, sequential versioned SQL | Explicit schema control, no auto-DDL in production |
| Idempotency | Atomic INSERT-first with `saveAndFlush()` | DB unique constraint prevents concurrent duplicates; poll-and-replay with 409 on timeout; non-2xx error recovery |

## Recommended Implementation Order

```
Phase 0 ─► Phase 1 ─► Phase 2 ─► Phase 3 ─► Phase 4
                                                    │
                                                    ▼
                                          Phase 5 (Idempotency)
                                                    │
                                                    ▼
                                        Phase 5.5 (Hardening)
                                                    │
                                          ┌─────────┼─────────┐
                                          ▼         ▼         ▼
                                    Phase 6   Phase 7   Phase 8
                                    (Notify)  (Admin)   (Tests)
                                          │         │         │
                                          └─────────┼─────────┘
                                                    │
                                                    ▼
                                              Phase 9 (Docker/CI)
                                                    │
                                              ┌─────┴─────┐
                                              ▼           ▼
                                         Phase 10     Phase 11
                                         (Flutter)    (Payments)
```

Phases 0–5.5 are strictly sequential (each builds on the previous). Phase 6–8 can partially overlap (notifications, admin hardening, and tests can be developed in parallel). Phase 9 is independent and can start early. Phases 10–11 are post-MVP.

---

## Day-by-Day Schedule

**Core philosophy**: Every day has the same 🟡 (medium, ~6h) intensity. No 🔴 burnout days, no 🟢 wasted days. Phases are split into consistent daily chunks so you always know what's for the day.

### Week 1 — Foundation & Authentication

| Day | Phase | Focus | Tasks | Intensity |
|-----|-------|-------|-------|-----------|
| **1** | **0** | Project Foundation | Expand `pom.xml` with all deps; set up multi-profile `application.yml` (dev/prod); create base package structure (config/entity/dto/repository/service/controller/security/exception/audit/scheduler); implement global `@RestControllerAdvice` exception handler; create `ApiResponse<T>` standard response wrapper | 🟡 |
| **2** | **0** | Foundation Wrap + Flyway Init | Flyway `V1__init_schema.sql` baseline migration; security headers filter (HSTS, X-Content-Type-Options, X-Frame-Options, CSP); verify `/actuator/health` + `/actuator/info`; Swagger/OpenAPI config with security scheme; MDC request tracing filter with X-Request-ID; verify app boots on port 8080 with `/api` context-path | 🟡 |
| **3** | **1** | User & Wallet Entities + Migrations | Create `User` entity (UUID, names, email/phone unique, passwordHash, role, emailVerified, soft-delete fields, @Version); `Role` enum (USER/ADMIN); `Wallet` entity (UUID, accountNumber "NW"+10digits, balance BigDecimal, currency "ZMW", status ACTIVE/FROZEN, freezeReason, @Version); `RefreshToken` entity; Flyway `V1__create_users_table.sql`, `V2__create_wallets_table.sql`, `V3__create_refresh_tokens_table.sql` | 🟡 |
| **4** | **1** | Security Foundation | JWT utility (jjwt 0.12.x) — generate access tokens, validate, configurable expiry (15 min default); `SecurityFilterChain` — permit `/api/v1/auth/**`, `/swagger-ui/**`, `/api-docs/**`, `/actuator/**`, all else authenticated, stateless session; `BCryptPasswordEncoder` bean; `JwtAuthFilter` (OncePerRequestFilter) — extract `Authorization: Bearer <token>`, validate, set SecurityContextHolder | 🟡 |
| **5** | **1** | Auth Service | `RegisterRequest`/`LoginRequest`/`AuthResponse` DTOs; `AccountNumberGenerator` — atomic sequence "NW" + 10-digit zero-padded, collision retry; `AuthService.register()` — validate uniqueness, hash password, create User + Wallet atomically, issue JWT + refresh token; `AuthService.login()` — verify credentials, issue tokens; `AuthController` (`POST /api/v1/auth/register`, `POST /api/v1/auth/login`); verify end-to-end with curl | 🟡 |
| **6** | **1** | PIN + Email + Password Reset | `POST /api/v1/auth/pin` — accept 4-6 digit PIN, BCrypt hash, store on User; PIN rate limiting — track `pinAttempts`, `pinLockedUntil`, lock 15 min after 3 failures; email verification — generate token on register, `GET /api/v1/auth/verify?token=...` marks `emailVerified=true`; password reset — `POST /api/v1/auth/forgot-password` → email link, `POST /api/v1/auth/reset-password` with token + new password | 🟡 |

### Week 2 — Core Transactions

| Day | Phase | Focus | Tasks | Intensity |
|-----|-------|-------|-------|-----------|
| **7** | **2** | Transaction Entity + Setup | `Transaction` entity (UUID, reference "NWTX"+yyyyMMdd+8digit sequence, type DEPOSIT/WITHDRAWAL/TRANSFER_DEBIT/TRANSFER_CREDIT/FEE, amount, balanceBefore, balanceAfter, status PENDING/SUCCESSFUL/FAILED, senderWalletId, receiverWalletId, relatedTransactionId, @Version); Flyway `V4__create_transactions_table.sql`; `TransactionReferenceGenerator` (NWTX{yyyyMMdd}{00000001}); `DepositRequest`/`WithdrawRequest`/`TransactionResponse`/`TransactionReference` DTOs; `TransactionController` scaffold | 🟡 |
| **8** | **2** | Deposit + Withdraw Services | `deposit(walletId, amount, description)` — find wallet (active), validate amount > 0, increment balance atomically via `UPDATE wallets SET balance = balance + ? WHERE id = ?`, create DEPOSIT record with balanceBefore/balanceAfter; `withdraw(walletId, amount, pin, description)` — verify PIN, validate balance >= amount, decrement balance, create WITHDRAWAL record | 🟡 |
| **9** | **2** | Transfer + Atomicity | `transfer(senderWalletId, receiverWalletId, amount, pin, description)` — verify PIN, prevent self-transfer, check balance >= amount, validate receiver exists and active; `@Transactional` — debit sender, credit receiver atomically; create TRANSFER_DEBIT + TRANSFER_CREDIT pair linked by `relatedTransactionId`; input validation (`@Positive`, `@NotBlank`, `@Size` for PIN); unit tests for deposit/withdraw/transfer | 🟡 |
| **10** | **3** | Transaction History & Querying | `TransactionRepository` with `JpaSpecificationExecutor` for dynamic filtering; `TransactionHistoryService` — paginated history with type/date/status filters; `PagedResponse<T>` DTO; `GET /api/v1/wallets/{walletId}/transactions` with query params (`type`, `status`, `from`, `to`, `page`, `size`, `sort`); ownership check (users see only own wallet); `GET /api/v1/transactions/{reference}` single lookup; `GET /api/v1/wallets/{walletId}/balance` endpoint; tests for pagination/filtering/ownership | 🟡 |
| **11** | **4** | Fee Engine | `FeeConfiguration` entity (id, transactionType TRANSFER/WITHDRAWAL/DEPOSIT, percentageFee BigDecimal, flatFee BigDecimal, minFee, maxFee, active boolean); Flyway `V5__create_fee_configurations_table.sql`; `FeeEngineService.calculateFee(type, amount)` — percentage + flat, clamp by min/max, HALF_EVEN rounding; `GET /api/v1/fees/estimate?type=TRANSFER&amount=100` public endpoint; admin `GET/PUT /api/v1/admin/fees/{id}` for fee config CRUD | 🟡 |
| **12** | **4** | Audit Logging + Fee Integration | `AuditLog` entity (UUID, entityType, entityId, action, oldValue, newValue, performedBy UUID, ipAddress, createdAt) — append-only; Flyway `V6__create_audit_logs_table.sql`; `AuditService.recordAction(...)` with `@Async` + `CompletableFuture<Void>`; integrate fee deduction into transfer flow — deduct from sender, create FEE record linked to transfer; `@Audited` annotation for AOP auto-capture; FeeEngine + AuditService tests | 🟡 |

### Week 3 — Security Hardening, Idempotency & Notifications

| Day | Phase | Focus | Tasks | Intensity |
|-----|-------|-------|-------|-----------|
| **13** | **5** | Refresh Token Rotation | Implement rotation — hash incoming token, match DB, check expiry + not revoked, issue new access + new refresh, revoke old; stolen token detection (reuse of already revoked → revoke ALL user tokens); `POST /api/v1/auth/refresh` endpoint; `POST /api/v1/auth/logout` (revoke one); `POST /api/v1/auth/logout-all` (revoke all); unit tests: happy path, expired, stolen reuse, all-revoked | 🟡 |
| **14** | **5** | **I**dempotency | `IdempotencyKey` entity + `IdempotencyStatus` enum; `IdempotencyKeyRepository` with custom `@Modifying` queries; `IdempotencyService` — atomic INSERT-first via `saveAndFlush()`, catch `DataIntegrityViolationException` for concurrent losers; `IdempotencyFilter` (OncePerRequestFilter) — extract `Idempotency-Key` header, ContentCachingResponseWrapper, poll-and-replay (10×100ms), 409 on timeout, error recovery for non-2xx; wire in SecurityConfig; Flyway `V9__create_idempotency_keys_table.sql`; `@EnableScheduling` on Application; `IdempotencyCleanupJob` (4am cron); `app.idempotency.ttl-hours: 24` config; 6 integration tests | 🟡 |
| **15** | **5.5** | **Fintech Hardening** | Pessimistic locking (`@Lock PESSIMISTIC_WRITE` + `FOR UPDATE`) on wallet balance operations; KYC tier `dailySendLimit` + `walletLimit` enforcement in TransactionService (approved KYC only); unify `RateLimitFilter` error response format with standard `ErrorResponse`; CORS configuration bean (localhost:3000, 5173, 8100); admin pagination via `Pageable` on user list; audit logging for admin freeze/unfreeze/toggle-delete with `adminId` param; regression test fixes; 105/105 tests passing | 🟡 |
| **16** | **6** | Notifications Setup | `Notification` entity (UUID, userId, type EMAIL/SMS, channel, recipient, subject, body, status PENDING/SENT/FAILED, retryCount, maxRetries 3, createdAt, sentAt); `NotificationAttempt` entity (id, notificationId, attemptNumber, success, errorMessage, attemptedAt); Flyway V10 migration (unified tables); `NotificationService` — sendEmail/sendSms/sendBoth with `@Async` + `MailService.sendRaw()`, create records, update status, 3 retry logic via `retryFailed()`; `SmsService` — Africa's Talking stub (logs when no API key); `recordSent()` for MailService-compatible audit; TransactionService wired: deposit/withdraw/transfer → email + SMS; AuthService + AdminKycService wired → `recordSent()` for verification/KYC emails | ✅ |
| **17** | **6** | Scheduling + Caching | `PendingTransactionCleanupJob` (`@Scheduled 3am`) — marks PENDING transactions older than 24h as FAILED; `IdempotencyCleanupJob` (`@Scheduled 4am` — done in Phase 5); `RefreshTokenCleanupJob` (`@Scheduled 4:30am`) — deletes expired refresh tokens; `CacheConfig` — Caffeine max 100, 10 min write expiry, `recordStats()`, `@EnableCaching`; `@CacheEvict("walletBalances")` on deposit/withdraw/transfer; `spring.mail.*` config for Mailtrap; `spring.cache.*` config; 7 notification integration tests; 112/112 total tests passing | ✅ |
| **18** | **7** | Admin Endpoints | `GET /api/v1/admin/users` (paginated — done in Phase 5.5); `GET /api/v1/admin/users/{id}` (detail with wallet); `GET /api/v1/admin/wallets` (status filter); `PATCH /api/v1/admin/wallets/{id}/freeze`/`unfreeze` (done in Phase 5.5 with audit); `GET /api/v1/admin/transactions` (search by reference, wallet, user, date, type, status); `GET /api/v1/admin/audit-logs` (filterable); full fee CRUD: `POST/PUT/DELETE /api/v1/admin/fees` | 🟡 |

### Week 4 — Testing, Docker & CI/CD

| Day | Phase | Focus | Tasks | Intensity |
|-----|-------|-------|-------|-----------|
| **19** | **8** | Integration Tests — Edge Cases | Concurrent transfer test (spawn threads from same wallet, verify no negative balance, correct final balance); idempotency E2E (same Idempotency-Key twice → cached response, only one transaction); rate limiting E2E (exceed limit → 429 with correct headers); refresh token rotation full cycle (login → refresh → old rejected → reuse detection) | 🟡 |
| **20** | **8** | Integration Tests + Coverage | Scheduler idempotency (run cleanup twice, same FAILED count); admin authorization (USER token → 403, ADMIN token → 200); fee engine edge cases (zero amount, max/min clamping, rounding, inactive config); wallet balance integrity (verify DB-stored balance matches computed from transactions); coverage report — identify gaps, write missing tests; final end-to-end smoke test (register → login → deposit → transfer → withdraw → verify all records) | 🟡 |
| **21** | **9** | Docker + Production Config | Multi-stage `Dockerfile` (Maven compile → JRE 17 runtime, healthcheck via `/actuator/health`); `docker-compose.yml` (API service + PostgreSQL 16 + optional Mailpit for dev email); `application-prod.yml` (env-var-based PostgreSQL, Flyway validate, secure CORS origins); `logback-spring.xml` (JSON format in prod, rolling file, password/PIN masking); CORS whitelist config (`APP_CORS_ORIGINS` env var); `pom.xml` (+logstash-logback-encoder) | ✅ |
| **22** | **9** | CI/CD + Documentation | GitHub Actions workflow (`.github/workflows/ci.yml`) — compile, unit tests, integration tests (H2), package JAR, Docker build + push to GHCR; `.dockerignore`; project plan updated; 112/112 tests passing | ✅ |

---

### Post-MVP Schedule

| Phase | Timeline | What |
|-------|----------|------|
| **10** — Flutter Frontend | 3–4 weeks | Cross-platform mobile app: auth screens, dashboard, send money, transaction history, profile, push notifications |
| **11** — Flutterwave/Paystack | 2–3 weeks | Live mobile money deposits, webhook handling, payout API, reconciliation, card payments |

### Alternative Schedules

| Mode | Total Days | Per-Phase Pace | Trade-offs |
|------|-----------|----------------|------------|
| **Full (this plan)** | **22 days** | ~2 days per phase | Every day 🟡 balanced. No burnout, no wasted days. Full test coverage, admin, Docker, CI/CD included |
| **Aggressive MVP** | **12 days** | ~1 day per phase | Cut: email verification, password reset, rate limiting, admin endpoints, soft delete, notification retry. Keep core financial operations |
| **Part-time (evenings)** | **7–8 weeks** | Same 22 days spread | ~3 days/week pace, good if you're employed or studying |
