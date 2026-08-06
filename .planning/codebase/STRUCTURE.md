# Codebase Structure

**Analysis Date:** 2026-08-06

## Directory Layout

```
NovaWallet/                          # Monorepo root (git)
├── novawallet-api/                  # Spring Boot 3.5.3 REST backend (Java 17, Maven)
│   ├── docker-compose.yml           # api :8080 · PostgreSQL 16 :5432 · Mailpit :8025/:1025
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/novawallet/novawallet_api/
│       │   └── resources/
│       │       ├── application.yml          # dev profile default + JWT/env wiring
│       │       ├── application-dev.yml      # PostgreSQL local, ddl-auto: validate
│       │       ├── application-prod.yml
│       │       ├── application-test.yml     # H2 in PostgreSQL mode
│       │       ├── logback-spring.xml       # Logstash encoder, MDC requestId
│       │       └── db/migration/            # Flyway V1..V10
│       └── test/                            # H2-backed integration tests
├── novawallet-app/                  # Flutter app (Dart, module novawallet_app)
│   ├── pubspec.yaml
│   ├── lib/
│   │   ├── main.dart
│   │   ├── app/
│   │   ├── core/
│   │   └── features/
│   └── test/widget_test.dart
├── .planning/                       # GSD planning artifacts (prompts/, stitch-*, codebase/)
├── .claude/                         # Claude Code config + rules (ecc java/dart style rules)
├── stitch_duplicate_of_novawallet/  # Stitch MCP design exports (HTML + PNG per screen)
├── uploads/                         # Runtime KYC upload artifacts (gitignored — PII)
├── .codegraph/                      # Codegraph index (gitignored, regenerable)
└── SRS.md · SPECIFICATION.md · USER-FLOW.md · DIFFERENTIATOR-FEATURES.md
    · SCAN-REPORT.md · novawallet-project-plan.md · HANDOFF.md
```

## Backend Directory Purposes — `novawallet-api/src/main/java/com/novawallet/novawallet_api/`

Package-by-feature: each feature owns its `controller/`, `service/`, `repository/`, `entity/`, `dto/`, and `enums/` layers.

**[feature] packages** (`admin`, `auth`, `fee`, `kyc`, `notification`, `token`, `transaction`, `user`, `wallet`):
- Purpose: one package per business domain; a feature is self-contained except for cross-cutting infra.
- Contains: `*Controller.java` (endpoints, no logic), `*Service.java` (business rules), `*Repository.java` (Spring Data JPA), `*entity/*.java` (JPA models, Lombok `@Builder`), `dto/` (request/response records), `enums/` (status/type constants), `schedule/*Job.java` (scheduled cleanup).
- Key examples:
  - `wallet/` — `wallet/service/WalletService.java`, `wallet/service/AccountNumberGenerator.java`, `wallet/repository/WalletRepository.java` (pessimistic-lock + atomic guarded balance update), `wallet/entity/Wallet.java`, `wallet/controller/WalletController.java`
  - `transaction/` — `transaction/service/TransactionService.java`, `transaction/service/TransactionHistoryService.java`, `transaction/service/TransactionLimitService.java`, `transaction/service/TransactionSpecifications.java`, `transaction/service/TransactionReferenceGenerator.java`, `transaction/controller/TransactionController.java`, `transaction/schedule/PendingTransactionCleanupJob.java`
  - `auth/` — `auth/service/AuthService.java` (register/login/verify/reset), `auth/controller/AuthController.java`
  - `token/` — `token/service/TokenService.java`, `token/repository/RefreshTokenRepository.java`, `token/entity/RefreshToken.java`, `token/schedule/RefreshTokenCleanupJob.java`
  - `kyc/` — `kyc/service/KycService.java`, `kyc/service/AdminKycService.java`, `kyc/service/FileStorageService.java`, `kyc/config/KycConfig.java`, `kyc/controller/KycController.java`, `kyc/schedule/KycExpiryJob.java`
  - `fee/` — `fee/service/FeeEngineService.java`, `fee/entity/FeeConfiguration.java`, `fee/controller/FeeController.java`
  - `notification/` — `notification/service/NotificationService.java`, `notification/service/MailService.java`, `notification/service/SmsService.java`, `notification/schedule/NotificationRetryJob.java`, `notification/schedule/BalanceRecalculationJob.java`
  - `admin/` — admin KYC approval/freeze/user-listing services+controller (admin endpoints under `/api/v1/admin/*`)

**`config/`** (cross-cutting Spring wiring):
- `SecurityConfig.java` — stateless JWT chain (`JwtAuthFilter`, `RateLimitFilter`, `IdempotencyFilter` order), CORS from `app.cors.origins`
- `CacheConfig.java` — Caffeine `walletBalances` cache
- `AsyncConfig.java` — notification executor for `@Async`
- `OpenApiConfig.java` — springdoc/Swagger
- `PasswordConfig.java`, `JacksonConfig.java`, `JpaAuditingConfig.java`

**`security/`** — JWT + rate-limit primitives: `JwtAuthFilter.java`, `JwtUtil.java`, `CustomUserDetailsService.java`, `LoginRateLimiter.java`, `RateLimitFilter.java`

**`filter/`** — `RequestTracingFilter.java` (requestId → MDC + `X-Request-Id` header)

**`idempotency/`** — `filter/IdempotencyFilter.java`, `service/IdempotencyService.java`, `entity/IdempotencyKey.java`, `repository/IdempotencyKeyRepository.java`, `schedule/IdempotencyCleanupJob.java`

**`audit/`** — `audit/aspect/AuditedAspect.java`, `audit/service/AuditService.java`, `@Audited` annotation (AOP audit logging)

**`exception/`** — `GlobalExceptionHandler.java` (`@RestControllerAdvice`) + `ErrorResponse.java` + domain exceptions (`BadRequestException`, `UnauthorizedException`, `ForbiddenException`, `ResourceNotFoundException`, `DuplicateResourceException`, `RateLimitException`)

**`common/dto/`** — `ApiResponse.java`, `PagedResponse.java` (shared response envelope)

**`bootstrap/`** — `AdminDataInitializer.java` (seeds admin + fee config from env on startup)

**Root:** `NovawalletApiApplication.java` (`main`, `@EnableScheduling`, `@SpringBootApplication`)

## Frontend Directory Purposes — `novawallet-app/lib/`

- **`main.dart`** — composition root: builds `ProviderContainer` overriding `tokenStorageProvider` + `apiClientProvider`, runs `UncontrolledProviderScope` → `NovawalletApp`
- **`app/`** — glue: `app.dart` (MaterialApp.router + wiring), `router.dart` (root `GoRouter`, splash/login/onboarding/PIN-set redirect gates — this is the ONLY place GoRouter is created), `main_shell.dart` (4-tab `StatefulShellRoute`)
- **`core/`** — shared infrastructure that has NO feature imports:
  - `config/app_config.dart` — `API_BASE_URL` (runtime dart-define, platform-aware defaults)
  - `network/api_client.dart` + `network/api_exception.dart` — Dio client singleton, interceptors, envelope unwrap
  - `storage/token_storage.dart` — FlutterSecureStorage wrapper
  - `providers.dart` — global Riverpod providers (`tokenStorageProvider`, `dioProvider`, `apiClientProvider`)
  - `theme/` — `nova_colors.dart`, `app_theme.dart`
  - `utils/formatters.dart` — currency/date formatters (`formatZmw`, `formatRelativeDate`...)
  - `widgets/` — shared UI: `widgets.dart` barrel aggregating `pill_button.dart`, `pin_pad.dart` and card/tile/avatar/state views
- **`features/<feature>/`** — one folder per domain (`auth`, `wallet`, `transaction`, `kyc`, `profile`, `admin`, `extras`). Standard sub-layout:
  - `data/<feature>_repository.dart` — HTTP calls via ApiClient (e.g. `wallet/data/wallet_repository.dart`, `auth/data/auth_repository.dart`, `transaction/data/transaction_repository.dart`, `kyc/data/kyc_repository.dart`, `profile/data/user_repository.dart`, `admin/data/admin_repository.dart`)
  - `models/*.dart` — backend DTO mirror + `fromJson` (e.g. `wallet/models/wallet.dart`, `transaction/models/transaction.dart`, `transaction/models/fee_estimate.dart`, `kyc/models/kyc_status.dart`)
  - `providers/<feature>_provider.dart` — Riverpod state (e.g. `auth/providers/auth_provider.dart`, `wallet/providers/wallet_provider.dart`, `transaction/providers/transaction_provider.dart`, `profile/providers/profile_provider.dart`, `kyc/providers/kyc_provider.dart`)
  - `routes.dart` — `List<RouteBase>` for nested routes (gem in `transaction/routes.dart`, `kyc/routes.dart`, `admin/routes.dart`, `extras/routes.dart`; register new child routes in the correct feature's files)
  - `screens/*.dart` — pages per feature (e.g. `wallet/screens/{dashboard,deposit,withdraw,send,cards}_screen.dart`, `transaction/screens/{transaction_history,transaction_detail,fee_estimate,success,insufficient_balance}_screen.dart`, `auth/screens/...`)
- **`test/widget_test.dart`** — single Flutter smoke test (party assertion on theme `0xFF3525CD`).

## Key File Locations

**Entry points:**
- Backend: `novawallet-api/src/main/java/com/novawallet/novawallet_api/NovawalletApiApplication.java`
- Frontend: `novawallet-app/lib/main.dart`
- Routes (all gating): `novawallet-app/lib/app/router.dart`, `novawallet-app/lib/app/main_shell.dart`

**Configuration:**
- `novawallet-api/src/main/resources/application.yml` (JWT expiry/secret, env var bindings, `/api` context path)
- `novawallet-api/src/main/resources/application-dev.yml` (PostgreSQL)
- `novawallet-api/src/main/resources/application-prod.yml`
- `novawallet-api/src/main/resources/application-test.yml` (H2)
- `novawallet-api/docker-compose.yml` (local PostgreSQL + Mailpit)

**Data & persistence:**
- Flyway: `novawallet-api/src/main/resources/db/migration/` (V1..V10)
- Returns: `wallet/repository/WalletRepository.java`, `transaction/repository/TransactionRepository.java`, `fee/repository/FeeConfigurationRepository.java`, `notification/repository/NotificationRepository.java`, `token/repository/RefreshTokenRepository.java`, `kyc/repository/KycDocumentRepository.java`, `idempotency/repository/IdempotencyKeyRepository.java`

**Frontend core logic:**
- `novawallet-app/lib/core/network/api_client.dart` (Dio, interceptors, error normalization)
- `novawallet-app/lib/core/providers.dart` (Riverpod composition root)
- `novawallet-app/lib/features/*/providers/*_provider.dart` (state)

## Naming Conventions

**Files:**
- Backend: `Controller`, `Service`, `Repository`, `Job` suffixes (e.g. `TransactionService.java`, `NotificationRetryJob.java`); DTOs as `*Request`/`*Response` (`TransferRequest.java`, `WalletResponse.java`); migrations `V{n}__snake_case.sql`
- Frontend: feature-qualified `snake_case.dart` with `_screen`, `_provider`, `_repository`, `_model` suffixes (e.g. `transaction_detail_screen.dart`, `wallet_provider.dart`, `wallet_repository.dart`)
- Enums: `snake_case` in `enums/` packages (e.g. `TransactionStatus.java`, `KycStatus.java`)

**Directories:**
- Backend: singular feature package names: `wallet/`, `transaction/`, `kyc/`, `auth/`, `user/`, `fee/`, `token/`, `notification/`, `audit/`, `idempotency/`, `config/`, `security/`, `filter/`, `exception/`, `common/`, `bootstrap/` — all lowercase
- Frontend: `lib/<layer>/` (`app`, `core`, `features`) → `features/<feature>/<layer>/` (`data`, `models`, `providers`, `screens`) — all lowercase, always plural layer folders

**Route paths (`novawallet-app`):**
- Consumer paths lowercase-and-slashed: `auth/routes.dart` has `/splash`, `/onboarding`, `/login`, `/register`, `/forgot-password`, `/reset-password?token=...`, `/verify-email?token=...`, `/pin/set`, `/change-password`
- Shell routes: `/wallet` (Home), `/send`, `/cards`, `/profile`
- Admin/extra routes: `/admin/...`, `/transactions`, `/transactions/:reference`

**API endpoint conventions (backend):**
- `/api/v1/<resource>[/<id>]/<action>` — e.g. `/api/v1/wallets/{id}/deposit`, `/api/v1/admin/kyc/pending`, `/api/v1/kyc/status`
- Response envelope: `ApiResponse<T>` with `{success, data, message, timestamp}`

## Where to Add New Code

**New backend feature:**
- Create a new package under `com.novawallet.novawallet_api/` (mirror an existing feature: `controller/`, `service/`, `repository/`, `entity/`, `dto/`, plus `enums/` if needed) — follow the `fee/` or `kyc/` template
- Add endpoints under `/api/v1/...` and register the subdomain-agnostic security matcher in `config/SecurityConfig.java` (e.g. permit `/api/v1/auth/**`, `/api/v1/admin/**` for ADMIN)
- For DB state: bump the current Flyway migration number (script `V{n+1}__your_migration.sql` in `db/migration/`) and run it locally before writing repo code; keep `ddl-auto: validate`
- Add H2-based integration tests under `src/test/java/...` and mirror any env vars in `application-test.yml`

**New frontend feature:**
- Add `lib/features/<feature>/` with the same 4 subfolders: `data/`, `models/`, `providers/`, `screens/`, plus `routes.dart`
- Create the feature's Riverpod provider (choose `ChangeNotifier` for sessions/global state, `AsyncNotifier` for streamed resources like wallet/transactions per existing patterns)
- Register `routes.dart` in the router: if inside the authenticated 4-tab shell → add to `buildFeatureRoutes()` in `lib/app/main_shell.dart`; if a guest/root flow page → add to the root `GoRoute` list in `lib/app/router.dart`
- Reuse `lib/core/` building blocks: `ApiClient` (never raw `Dio`), `widgets.dart` themes, `formatters.dart`; never import `core/` from a feature for business rule logic — feature data belongs in that feature's `data/`

**Shared / cross-cutting changes (e.g. new audit event, new notification channel, new idempotency edge):**
- Audit: add `@Audited` on the service method; channel: extend `notification/)` `NotificationService`/`MailService`/`SmsService`; idempotency: the `IdempotencyFilter` + `ApiClient` append-height key propagation stays fixed — backend validation stays in `IdempotencyService`; keep global vs per-instance constraints in the `config/` docs.

## Special Directories

- **`.planning/`** — generated GSD workflow artifacts (only planning state, not source); committed.
- **`.claude/`** — Claude Code project rules (`ecc/java/`, `ecc/dart/` coding-style) that encode the conventions above.
- **`.codegraph/`** — local codegraph search index; **regenerable**, gitignored.
- **`uploads/`** — runtime KYC upload staging (PII; never commit; gitignored per `.gitignore`).
- **`stitch_duplicate_of_novawallet/`** — Stitch MCP screen-export artifacts (HTML+PNG per UI screen); not part of the runtime app build.
- **`novawallet-app/`** — currently **not tracked in git** (top-level `git ls-files` shows no `novawallet-app` entries — the Flutter app is untracked as of 2026-08-06); `.gitignore` covers `target/`, `uploads/`, `session-*.md`, `.env*`, so the app tree itself is intended to be committed once added.

---

*Structure analysis: 2026-08-06*