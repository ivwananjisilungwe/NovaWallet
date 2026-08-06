<!-- refreshed: 2026-08-06 -->
# Architecture

**Analysis Date:** 2026-08-06

## System Overview

NovaWallet is a monorepo containing two applications: a Spring Boot REST API (`novawallet-api/`) and a Flutter mobile app (`novawallet-app/`). The backend is the single source of truth for money movement, KYC, fees, and audit; the frontend is a thin Riverpod/GoRouter client that talks exclusively through the API envelope.

```text
┌─────────────────────────────────────────────────────────────────────┐
│                        Flutter App (novawallet-app/)                │
│  screens ──▶ feature providers ──▶ feature repositories             │
│  (lib/features/*/screens) (providers/*)  (data/*_repository.dart)   │
│                              │                                       │
│                              ▼                                       │
│              ApiClient (lib/core/network/api_client.dart)            │
│              Dio interceptors: Bearer · refresh · idempotency        │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ HTTP/JSON  /api/v1/*
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                  Spring Boot API (novawallet-api/)                  │
│  Filter chain: RequestTracing → JwtAuth → RateLimit → Idempotency   │
│  Controllers ──▶ Services ──▶ JPA Repositories (per-feature pkgs)   │
│  Cross-cutting: GlobalExceptionHandler · AuditedAspect · @Async     │
│                 NotificationService · Caffeine cache · @Scheduled   │
└──────────────────────────────┬──────────────────────────────────────┘
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│  PostgreSQL 16 (docker-compose: db :5432) · Flyway V1..V10          │
│  uploads/ (KYC documents) · Mailpit (dev SMTP :1025 / UI :8025)     │
└─────────────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| API entry point | Boots Spring context, enables scheduling | `novawallet-api/src/main/java/com/novawallet/novawallet_api/NovawalletApiApplication.java` |
| Security chain | Stateless JWT auth, CORS, disables CSRF/sessions | `novawallet-api/src/main/java/com/novawallet/novawallet_api/config/SecurityConfig.java` |
| JWT filter | Parses `Authorization: Bearer`, sets `SecurityContext` | `novawallet-api/src/main/java/com/novawallet/novawallet_api/security/JwtAuthFilter.java` |
| Idempotency filter | Enforces `Idempotency-Key` on mutating requests, replays cached responses | `novawallet-api/src/main/java/com/novawallet/novawallet_api/idempotency/filter/IdempotencyFilter.java` |
| Request tracing | Generates `requestId`, exposes via MDC + `X-Request-Id` header | `novawallet-api/src/main/java/com/novawallet/novawallet_api/filter/RequestTracingFilter.java` |
| Error mapping | `@RestControllerAdvice` → `ErrorResponse` with proper HTTP codes | `novawallet-api/src/main/java/com/novawallet/novawallet_api/exception/GlobalExceptionHandler.java` |
| API envelope | Uniform `{success, data, message, timestamp}` response record | `novawallet-api/src/main/java/com/novawallet/novawallet_api/common/dto/ApiResponse.java` |
| Audit | AOP aspect records `@Audited` method calls via `AuditService` | `novawallet-api/src/main/java/com/novawallet/novawallet_api/audit/aspect/AuditedAspect.java` |
| Auth | Register/login/verify/forgot+reset password, BCrypt, verification tokens | `novawallet-api/src/main/java/com/novawallet/novawallet_api/auth/service/AuthService.java` |
| Tokens | JWT access (15 min) + hashed rotating refresh tokens with family revocation | `novawallet-api/src/main/java/com/novawallet/novawallet_api/token/service/TokenService.java`, `.../token/service/RefreshTokenService.java` |
| Wallet | Balance, deposit/withdraw/transfer, freeze, PIN verify with lockout | `novawallet-api/src/main/java/com/novawallet/novawallet_api/wallet/service/WalletService.java` |
| Transactions | Orchestrates money moves: lock → PIN → fee → limit → atomic debit/credit | `novawallet-api/src/main/java/com/novawallet/novawallet_api/transaction/service/TransactionService.java` |
| Fee engine | Computes fee from `FeeConfiguration` | `novawallet-api/src/main/java/com/novawallet/novawallet_api/fee/service/FeeEngineService.java` |
| Daily limits | Tracks/clears per-wallet daily send limit | `novawallet-api/src/main/java/com/novawallet/novawallet_api/transaction/service/TransactionLimitService.java` |
| KYC | Submit/status; admin approval auto-creates wallet | `novawallet-api/src/main/java/com/novawallet/novawallet_api/kyc/service/KycService.java`, `.../kyc/service/AdminKycService.java` |
| Notifications | `@Async` email/SMS with attempts tracking + retry job | `novawallet-api/src/main/java/com/novawallet/novawallet_api/notification/service/NotificationService.java` |
| Scheduled jobs | Retry notifications, expire stale KYC, clean pending txns/idempotency/refresh tokens | `novawallet-api/src/main/java/com/novawallet/novawallet_api/**/schedule/*.java` |
| Frontend shell | `MaterialApp.router`, theme, GoRouter with redirect gates | `novawallet-app/lib/app/app.dart`, `novawallet-app/lib/app/router.dart` |
| Frontend tabs | 4-tab `StatefulShellRoute`: Home/Send/Cards/Profile | `novawallet-app/lib/app/main_shell.dart` |
| API client | Dio base, Bearer attach, single-flight refresh, auto idempotency key, envelope unwrap | `novawallet-app/lib/core/network/api_client.dart` |
| Auth session | `AuthNotifier` (ChangeNotifier) drives router gates + PIN state | `novawallet-app/lib/features/auth/providers/auth_provider.dart` |
| Wallet state | `AsyncNotifierProvider<WalletNotifier, Wallet>` for balance/dashboard | `novawallet-app/lib/features/wallet/providers/wallet_provider.dart` |

## Pattern Overview

**Overall:** Two independently deployable applications sharing an API contract. Backend = classic layered (Controller → Service → Repository) **package-by-feature** Spring Boot app with heavy cross-cutting infrastructure (JWT, idempotency, audit AOP, async notifications, scheduled jobs). Frontend = **feature-first** Riverpod + GoRouter app where each feature owns its data layer, models, providers, screens, and route group.

**Key Characteristics:**
- Uniform `ApiResponse<T>` envelope on every endpoint; errors always `ErrorResponse` via `GlobalExceptionHandler`
- **Idempotency-first money movement**: every mutating call carries `Idempotency-Key`; backend dedupes/replays (DB-backed via `idempotency_keys` table)
- Money math in `BigDecimal` persisted as numeric; serialized as strings in JSON — no floats anywhere
- Pessimistic row locks (`findByIdWithLock` PESSIMISTIC_WRITE) + atomic guarded `UPDATE ... WHERE balance + ? >= 0` for concurrency-safe debits
- Audit via declarative `@Audited` annotation + AOP aspect
- Async side effects (email/SMS) with DB-attempts tracking and scheduled retry (max 3 attempts)
- Token rotation: hashed refresh tokens in DB, family revoked on reuse detection
- KYC tier gating: wallet cap + daily send limit enforced in services
- Single-instance friendly: Caffeine cache, in-memory rate limiters (see Constraints)

## Layers

### Backend — `novawallet-api/src/main/java/com/novawallet/novawallet_api/`

- **Web/Controller layer:** one controller per feature in `<feature>/controller/` (e.g. `auth/controller/AuthController.java`, `wallet/controller/WalletController.java`, `transaction/controller/TransactionController.java`, `admin/controller/AdminController.java`). Owns endpoint mapping under `/api/v1`, `@Valid` DTO binding, and never contains business logic. Depends on: services.
- **Service layer:** `<feature>/service/` — holds all business rules: `AuthService`, `WalletService`, `TransactionService`, `FeeEngineService`, `TransactionLimitService`, `AdminKycService`, `NotificationService`, `TokenService`/`RefreshTokenService`. Annotated `@Service`, transactional where money is involved. Depends on: repositories, other services (e.g. `TransactionService` → `FeeEngineService`, `TransactionLimitService`, `NotificationService`).
- **Repository layer:** Spring Data JPA interfaces in `<feature>/repository/` (e.g. `wallet/repository/WalletRepository.java` with `findByIdWithLock` PESSIMISTIC_WRITE and atomic `updateInventory` queries). Depends on: entities.
- **Entity/domain layer:** JPA entities in `<feature>/entity/` (User, Wallet, Transaction, FeeConfiguration, RefreshToken, KycSubmission, AuditLog, Notification, IdempotencyKey, WalletDailyLimit...), Lombok `@Builder` construction, enums in `<feature>/enums/` (`TransactionType`, `TransactionStatus`, `KycStatus`, `WalletStatus`, `UserStatus`...).
- **Cross-cutting packages:** `config/` (SecurityConfig, CacheConfig, AsyncConfig, OpenApiConfig, AppDataInitializer, WebMvcConfig), `filter/` (RequestTracing, RateLimit), `exception/`, `common/dto/`, `audit/`, `idempotency/`, `security/` (JwtService, LoginRateLimiter).
- **Persistence layer:** Flyway migrations `novawallet-api/src/main/resources/db/migration/V1__init_schema.sql` … `V10__create_notifications_tables.sql`; `ddl-auto: validate`; PostgreSQL (H2 in PostgreSQL mode for tests).

### Frontend — `novawallet-app/lib/`

- **Presentation:** `lib/features/<feature>/screens/*.dart` — `ConsumerWidget` screens, no business logic, read state via `ref.watch(...Provider)` and call provider methods. Shared UI in `lib/core/widgets/widgets.dart` (barrel: `PillButton`, `BalanceCard`, `TransactionTile`, `StatusChip`, `AmountInput`, `PinPad`, `ErrorStateView`, `EmptyStateView`, `LoadingView`, `SectionHeader`, `InitialsAvatar`, `OrDivider`, `GoogleSignInButton`).
- **State:** Riverpod providers in `lib/features/<feature>/providers/*.dart` (`AuthNotifier` ChangeNotifier, `WalletNotifier` AsyncNotifier) plus global wiring in `lib/core/providers.dart` (`tokenStorageProvider`, `dioProvider`, `apiClientProvider` — the latter is overridden in `lib/main.dart` with the real `ApiClient`).
- **Data:** `lib/features/<feature>/data/*_repository.dart` — thin classes taking `ApiClient`, exposing typed methods (`AuthRepository`, `WalletRepository`). Models in `lib/features/<feature>/models/*.dart` with `fromJson` mirroring backend DTOs.
- **Routing:** `lib/app/router.dart` (`GoRouterForApp` with redirect gates) → root-level `authRoutes` (`lib/features/auth/routes.dart`) + feature route groups (`lib/features/{transaction,kyc,admin,extras}/routes.dart`) aggregated by `lib/app/main_shell.dart` `buildFeatureRoutes()` into a `StatefulShellRoute.indexedStack` (4 tabs: Home `/wallet`, Send `/send`, Cards `/cards`, Profile `/profile`).
- **Networking:** `lib/core/network/api_client.dart` — Dio with interceptors: attach Bearer token; single-flight refresh on 401 (retry once, clear session on failure); auto-generate `Idempotency-Key` (UUID) on mutating requests; unwrap `ApiResponse<T>`; normalize failures into `ApiException`. Base URL from `lib/core/config/app_config.dart` (dart-define; Android emulator → `http://10.0.2.2:8080/api`, else `http://localhost:8080/api`).
- **Secure storage:** `lib/core/storage/token_storage.dart` — FlutterSecureStorage keys: `auth_access_token`, `auth_refresh_token`, `auth_user_json`, `auth_pin_set`.

## Data Flow

### Primary Request Path (authenticated feature call)

1. Screen gesture → provider method, e.g. `WalletNotifier.transfer(...)` (`novawallet-app/lib/features/wallet/providers/wallet_provider.dart`)
2. Provider → `WalletRepository.transfer(...)` with an idempotency key (`novawallet-app/lib/features/wallet/data/wallet_repository.dart`)
3. `ApiClient.post('/v1/transfers', ...)` — Dio interceptors attach `Authorization: Bearer`, inject `Idempotency-Key` (`novawallet-app/lib/core/network/api_client.dart`)
4. Backend filter chain: `RequestTracingFilter` → `JwtAuthFilter` (validates JWT, sets context) → `RateLimitFilter` → `IdempotencyFilter` (acquires/locks the key) (`novawallet-api/.../filter/*.java`, `.../security/JwtAuthFilter.java`, `.../idempotency/filter/IdempotencyFilter.java`)
5. `TransactionController.transfer` → `TransactionService.transfer`: lock sender+receiver wallets (`findByIdWithLock`), verify PIN + lockout, check frozen status, compute fee (`FeeEngineService`), enforce daily limit (`TransactionLimitService`), atomic guarded debit/credit + records, wrap in `@Audited` aspect → `AuditService` (`novawallet-api/.../transaction/service/TransactionService.java`)
6. `NotificationService.send` fires `@Async` email/SMS with attempts tracking (retried by `NotificationRetryJob`)
7. Response returns as `ApiResponse<TransactionResponse>` → interceptor unwraps envelope → typed model → provider updates → `ConsumerWidget` rebuilds

### Authentication Flow

1. `POST /api/v1/auth/register` → `AuthService.register`: hash password (BCrypt), create user + email verification token, async email (`novawallet-api/.../auth/service/AuthService.java`)
2. `POST /api/v1/auth/login` → `LoginRateLimiter` (5 attempts, 15-min lockout) → verify credentials → `TokenService` issues JWT access token (15 min, `app.jwt.access-token-expiration-ms`) + hashed refresh token stored in DB with rotation (`novawallet-api/.../token/service/`)
3. `POST /api/v1/auth/refresh` → `RefreshTokenService.rotate` — issues new pair, revokes family on reuse detection
4. Frontend stores tokens via `TokenStorage`, `AuthNotifier.restoreSession()` rehydrates on launch; router gates: initializing → `/splash`, unauthenticated → `/login`, PIN not set → `/pin/set` (`novawallet-app/lib/app/router.dart`)

### Scheduled Job Pattern

`@EnableScheduling` on `NovawalletApiApplication`; jobs live in `<feature>/schedule/*Job.java` (NotificationRetryJob, KycExpiryJob, PendingTransactionCleanupJob, IdempotencyCleanupJob, RefreshTokenCleanupJob, BalanceRecalculationJob). Pattern: query due records → process → update state; e.g. retry notifications with `attempts < 3`.

**State Management:**
- Backend: stateless (JWT); all mutable state in PostgreSQL; Caffeine cache `walletBalances` (10-min write expiry) for balance reads; in-memory `LoginRateLimiter` + global `RateLimitFilter` (per-instance).
- Frontend: Riverpod providers as the single state source; `AuthNotifier` (ChangeNotifier) drives navigation via `refreshListenable`; `WalletNotifier` (AsyncNotifier) holds `AsyncValue<Wallet>`; no setState-external mutations outside providers.

## Key Abstractions

**`ApiResponse<T>`** (backend) / `ApiException` (frontend):
- Purpose: uniform success envelope and normalized error model across the wire.
- Backend: `novawallet-api/src/main/java/com/novawallet/novawallet_api/common/dto/ApiResponse.java` (record: `success, data, message, timestamp`) + `ErrorResponse` in `exception/`.
- Frontend: `novawallet-app/lib/core/network/api_client.dart` unwraps envelope; `novawallet-app/lib/core/network/api_exception.dart` carries status/message/errors.

**Feature Repository (frontend):**
- Purpose: every feature exposes its HTTP surface as a typed class taking `ApiClient`.
- Examples: `novawallet-app/lib/features/auth/data/auth_repository.dart`, `novawallet-app/lib/features/wallet/data/wallet_repository.dart`.
- Pattern: `class XRepository { XRepository(this._api); final ApiClient _api; ... }` — const-constructible, injected via Riverpod.

**Route Group (frontend):**
- Purpose: feature-owned navigation; root vs shell routes are composed in `main_shell.dart`.
- Examples: `novawallet-app/lib/features/auth/routes.dart` (root-level: `/splash`, `/onboarding`, `/login`, `/register`, `/forgot-password`, `/reset-password`, `/verify-email`, `/pin/set`, `/change-password`), `novawallet-app/lib/features/transaction/routes.dart` (`/transactions`, `/transactions/:reference`, `/fees/estimate`), `kyc/routes.dart`, `admin/routes.dart`, `extras/routes.dart`.

**Idempotency Service (backend):**
- Purpose: DB-backed dedupe of mutating operations; the key primitive that makes money moves retry-safe.
- Files: `novawallet-api/.../idempotency/service/IdempotencyService.java`, `.../idempotency/filter/IdempotencyFilter.java` (uses `ContentCachingResponseWrapper` to replay the first response).

## Entry Points

**Backend API:**
- Location: `novawallet-api/src/main/java/com/novawallet/novawallet_api/NovawalletApiApplication.java` (`main`, `@SpringBootApplication`, `@EnableScheduling`)
- Triggers: HTTP on `:8080` under context path `/api` (see `novawallet-api/src/main/resources/application.yml`)
- Also: OpenAPI/Swagger UI via springdoc (`/api/swagger-ui.html`, config in `config/OpenApiConfig.java`); scheduled jobs via `@Scheduled`
- Deployment: `novawallet-api/docker-compose.yml` (api, postgres:16, mailpit) + `novawallet-api/Dockerfile`

**Frontend App:**
- Location: `novawallet-app/lib/main.dart` — builds `ProviderContainer` (overrides `tokenStorageProvider`, `apiClientProvider`), runs `UncontrolledProviderScope` → `NovawalletApp`
- Triggers: app launch → initial route `/splash` → redirect gates in `lib/app/router.dart` → `MainShell` (4 tabs)

## Architectural Constraints

- **Threading:** Spring MVC thread-per-request; a single `@Async` executor (AsyncConfig) for notifications; Flutter single-threaded event loop (all IO async).
- **Global state:** Caffeine `walletBalances` cache (`config/CacheConfig.java`) — balances may be 10 min stale on read paths; `LoginRateLimiter` and `RateLimitFilter` are in-memory → **per-instance only**; scaling to N instances requires a shared rate-limit store (Redis) and the DB-backed lock/idempotency already covers money safety.
- **Money invariants:** debits only via guarded atomic `UPDATE ... WHERE balance + :amount >= 0` (plus `version` bump for optimistic-lock safety net) — never read-modify-write outside a `PESSIMISTIC_WRITE` lock.
- **Idempotency:** mutating endpoints (POST/PUT/PATCH/DELETE) pass through `IdempotencyFilter`; clients must send `Idempotency-Key`. The Flutter `ApiClient` does this automatically.
- **API contract:** everything under `/api/v1`; success → `ApiResponse<T>`, failure → `ErrorResponse`; CORS restricted to `app.cors.origins`; stateless sessions (no JSESSIONID).
- **Circular imports:** service-layer graph is intentionally acyclic; `TransactionService` depends on fee/limit/notification services, never the reverse. Keep it that way when adding features.

## Anti-Patterns

### Duplicate balance sources on the dashboard

**What happens:** The user payload embeds a wallet summary (`UserResponse`/`User.fromJson`), while the wallet feature separately fetches full wallet state via `GET /v1/wallets/me` (`novawallet-app/lib/features/wallet/data/wallet_repository.dart`). Dashboard balance can disagree with the wallet provider's fresher value.
**Why it's wrong:** Two sources of truth for the same monetary value invites stale-balance display bugs.
**Do this instead:** Treat `/v1/wallets/me` (via `WalletNotifier`) as the single balance source; drop wallet fields from user responses or keep them only as a cache hint, never rendered directly.

### In-memory rate limiting

**What happens:** Login rate limiting lives in a JVM map (`security/LoginRateLimiter`, `filter/RateLimitFilter`).
**Why it's wrong:** Silent no-op when the API is scaled horizontally — lockout can be bypassed by hitting another instance.
**Do this instead:** When multi-instance deployment is planned, back rate limits with Redis (or DB) and document the single-instance assumption explicitly (see Constraints).

### Throwing provider as composition-root footgun

**What happens:** `apiClientProvider` in `novawallet-app/lib/core/providers.dart` throws by default and is only usable after `lib/main.dart` overrides it.
**Why it's wrong:** Any widget/unit test resolving the provider before `main()` wiring hits a runtime `UnimplementedError` instead of a compile error.
**Do this instead:** Keep the override pattern (it enforces explicit wiring) but construct a default `ApiClient` with a failing-but-lazy base URL in tests, or expose a `@visibleForTesting` factory.

## Error Handling

**Strategy:** Backend — centralized `@RestControllerAdvice`; frontend — normalized `ApiException` + declarative screen states.

**Patterns:**
- Backend: `exception/GlobalExceptionHandler.java` maps domain exceptions (e.g. insufficient funds, wallet frozen, PIN lockout, invalid refresh token) to `ErrorResponse` with appropriate HTTP statuses; validation errors from `@Valid` DTOs are also centralized there.
- Frontend: `ApiClient` interceptors normalize Dio errors into `ApiException` (status, server message, field errors); screens render `ErrorStateView` with retry (`novawallet-app/lib/core/widgets/widgets.dart`); providers expose `AsyncValue.error` for AsyncNotifier-driven states.

## Cross-Cutting Concerns

**Logging:** `novawallet-api/src/main/resources/logback-spring.xml` (Logstash encoder); `RequestTracingFilter` seeds `requestId` into MDC (visible in log pattern `[%X{requestId}]`) and returns it via `X-Request-Id` header — include it in every log line for request correlation.
**Validation:** Jakarta Bean Validation on controller DTOs (`@Valid`, `@NotBlank`, `@Email`, `@Size`...), plus service-level domain checks (PIN, balances, limits, KYC tier).
**Authentication:** Stateless JWT (15-min access) + rotating hashed refresh tokens; PIN required for money operations with failed-attempt lockout; email verification + password reset via tokenized emails.
**Audit:** Declarative `@Audited` annotation + `audit/aspect/AuditedAspect.java` → `AuditService` persists to `audit_logs` (V8), admin UI lists via `/api/v1/admin/audit-logs`.

---

*Architecture analysis: 2026-08-06*
