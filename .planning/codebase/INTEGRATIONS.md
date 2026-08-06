# External Integrations

**Analysis Date:** 2026-08-06

The NovaWallet monorepo splits integrations cleanly: the backend (`novawallet-api/`) owns every external service call — database, email, SMS — while the mobile client (`novawallet-app/`) talks only to the backend REST API. The backend currently performs **no outbound HTTP calls of its own** (no `RestTemplate`/`WebClient`/Feign anywhere under `novawallet-api/src/main/java/`); SMS delivery is a stub awaiting the Africa's Talking SDK, and deposits/withdrawals are internal ledger operations, not payment-provider callouts.

## APIs & External Services

**Email delivery:**
- Service: SMTP via Spring `JavaMailSender` (`spring-boot-starter-mail`)
  - Implementation: `novawallet-api/src/main/java/com/novawallet/novawallet_api/notification/MailService.java`
  - Dev: no SMTP configured → emails logged to console; `docker-compose.yml` runs **Mailpit** (`axllent/mailpit:latest`, dev profile, SMTP port 1025 internal, UI on 8025) as the dev mail server
  - Prod: `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT` (default 587), `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `SPRING_MAIL_SMTP_STARTTLS` — SMTP-over-TLS; `application.yml` shows the SendGrid SMTP pattern (`smtp.sendgrid.net`, user `apikey`) as the intended prod provider
  - Sender address: `APP_MAIL_FROM` (default `noreply@novawallet.com`)
  - Uses: email verification, password reset, KYC-approval notices, generic notifications (`NotificationService`)

**SMS:**
- Africa's Talking (planned, currently stubbed)
  - Implementation: `novawallet-api/src/main/java/com/novawallet/novawallet_api/notification/service/SmsService.java`
  - Auth: `APP_SMS_API_KEY` env var (`app.sms.api-key`), username default `NovWallet`, sender default `NovWallet`
  - Current state: when API key is empty → messages logged to console (stub mode). When set, the code only logs "sent via Africa's Talking" — **the real SDK call is a TODO comment, not implemented**. 160-char truncation and failure handling exist
  - Used by: `NotificationService` for channel `SMS`

**HTTP/REST clients in the backend:** none outside Spring MVC itself. No payment gateway, no banking/ecocash-style APIs wired yet.

## Data Storage

**Databases:**
- PostgreSQL 16 (primary, production and dev)
  - Connection (dev): `jdbc:postgresql://localhost:5432/novawallet` (see `application-dev.yml`)
  - Connection (prod): env vars `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` (`application-prod.yml`); Hikari pool tuning via `DB_POOL_SIZE`, `DB_MIN_IDLE`
  - Compose service `db` — `postgres:16-alpine`, host port 5432, volume `novawallet-postgres-data`, healthcheck `pg_isready`
  - Client: Hibernate 6 via `spring-boot-starter-data-jpa` (object–relational, all repositories under `**/repository/`)
  - Migrations: Flyway 11, scripts `src/main/resources/db/migration/V1..V10` (`users`, `wallets`, `refresh_tokens`, `kyc`, `transactions`, `fee_configurations`, `audit_logs`, `idempotency_keys`, `notifications`)
- H2 (tests only) — `src/test/resources/application-test.yml`, `jdbc:h2:mem:testdb;MODE=PostgreSQL`, Flyway migrations run against it in integration tests

**File Storage:**
- Local filesystem only for KYC documents — `FileStorageService` (`novawallet-api/.../kyc/service/FileStorageService.java`)
  - Upload dir: `app.kyc.upload-dir` → `uploads/kyc` (dev) / `/app/uploads/kyc` (prod container, backed by named volume `novawallet-kyc-uploads` from `docker-compose.yml`)
  - Documents stored per-user under `<upload-dir>/<userId>/<documentId>_<sanitized-name>`; read path guarded with a `normalize().startsWith(uploadDir)` traversal check
  - No S3/object-storage/cloud storage service

**Caching:**
- In-process Caffeine (via `spring-boot-starter-cache`) — no Redis, no shared cache
  - Config: `novawallet-api/.../config/CacheConfig.java` (clearable `caffeine` cache manager)
  - Notes: cannot be shared across API instances (not distributed)

## Authentication & Identity

**Auth Provider:** fully custom (self-hosted), no third-party (no OAuth provider, no Okta/Auth0/Firebase)
  - Implementation: Spring Security stateless filter chain (`config/SecurityConfig.java`) + `security/JwtAuthFilter.java` + `security/JwtUtil.java` (HS256/384-HS512 via jjwt)
  - Passwords and wallet PINs: BCrypt via `BCryptPasswordEncoder` (`config/PasswordConfig.java`); PINs hashed, verified, not stored in clear
  - Tokens: signed JWT access token (default 15 min, `jwt.expiration`), opaque rotated refresh tokens persisted in `refresh_tokens` table (`token/service/TokenService.java`) with rotation invalidation on reuse
  - Rate limiting: built-in in-memory login attempt limiter + global `RateLimitFilter` (`security/LoginRateLimiter.java`, `security/RateLimitFilter.java`)
  - Admin seed account: `AdminDataInitializer` in `bootstrap/` — `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD` (prod requires the password and fails fast)

**Client-side token storage:**
- `flutter_secure_storage` 11.0.0 — Keychain on iOS, EncryptedSharedPreferences on Android (`novawallet-app/lib/core/storage/token_storage.dart`)
- Refresh flow: `lib/core/network/api_client.dart` `refresh()` — single-flight, posts to `/v1/auth/refresh` with `Bearer <refreshToken>`; on failure clears the local session

## Monitoring & Observability

**Error Tracking:** none (no Sentry/Honeybadger/BugSnag anywhere)
**Logs:**
- Backend: `logback-spring.xml` with request-ID tracing (`%X{requestId}`, populated by `filter/RequestTracingFilter.java`) — pretty console in dev, structured JSON (LogstashEncoder, port 8.0) in prod for aggregation to ELK/DataDog
- Frontend: no production logging framework; console only

**Metrics/Health:**
- Spring Boot Actuator: `health` + `info` exposed in dev (`application.yml`); `health`, `info`, `metrics`, `prometheus` exposed in prod (`application-prod.yml`)
- Docker healthchecks hit `/api/actuator/health` (Dockerfile + compose)
- Note: the `/api` prefix is mounted at the controller layer (`@RequestMapping("/api/v1/...")`) and Swagger (`springdoc.*` paths)

## CI/CD & Deployment

**Hosting:**
- Container image only — built/pushed to **GitHub Container Registry** (`ghcr.io`, `REGISTRY: ghcr.io` in `novawallet-api/.github/workflows/ci.yml`) on push to `main`/`master`
- No cloud deploy target (Vercel/Heroku/AWS) configured; no Kubernetes/Heti manifest

**CI Pipeline:**
- GitHub Actions — `novawallet-api/.github/workflows/ci.yml` (two jobs)
  1. `test` — JDK 17 (Temurin, Maven cache), `mvn test -B`, surefire reports uploaded on failure
  2. `build` — depends on the `test` job (via `needs: test`), only on branch push to `main`/`master`; logs in to GHCR and pushes the image (short-sha + branch + `latest` tags)
- CI for the Flutter app: **none** (no `.github` under `novawallet-app/`)

## Environment Configuration

**Required env vars (backend prod / compose):**
- `DB_PASSWORD` (`:?` required in `docker-compose.yml` — used both by `postgres` container and API)
- `JWT_SECRET` — base64 256+ bit signing secret (`openssl rand -base64 64`)
- `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD` (admin seed; `APP_ADMIN_PASSWORD` fail-fast required)
- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` (prod profile)
- Optional: `APP_SMS_API_KEY` (Africa's Talking; empty = stub), `APP_CORS_ORIGINS` (defaults `http://localhost:3000,http://localhost:5173`), `APP_MAIL_FROM`, `SPRING_MAIL_*` (SMTP), `JWT_EXPIRATION`, `DB_POOL_SIZE`, `DB_MIN_IDLE`, `TZ` (default `Africa/Lusaka`)

**Frontend env:**
- `--dart-define=API_BASE_URL=...` — compile-time override of the backend URL (`lib/core/config/app_config.dart`); defaults resolve to `http://localhost:8080/api` (desktop/web) or `http://10.0.2.2:8080/api` (Android emulator)

**Secrets location:**
- Env vars at runtime for the backend (`application-prod.yml` + `.env.example`); local dev overrides hardcoded in `application-dev.yml`; secret obtainable from the container runtime — no secret manager/Vault wired up
- Mobile: tokens live in platform secure storage only; no API keys compiled into the app

## Webhooks & Callbacks

**Incoming (webhooks to us):**
- None defined. Only inbound REST surface is the app API (`/api/v1/*`) plus the email verification link handling — `VerificationController` at `/api/v1/email/verify?token=...`

**Outgoing (we subscribe/deliver):**
- None — no outbound webhooks, no payment/IPN endpoints; async outbound is limited to SMTP (email) via `MailService`, SMS (stub) via `SmsService`, and in-process scheduling (`@EnableScheduling` jobs: `NotificationRetryJob`, `KycExpiryJob`, `BalanceRecalculationJob`, `RefreshTokenCleanupJob`, `IdempotencyCleanupJob`)

## Known Gaps

- **Africa's Talking SDK not actually integrated** — only the env hook and placeholder (`SmsService.java` lines ~65-68); production SMS will silently "succeed" while logging
- **No object storage** for KYC files → `uploads/kyc` is node-local; breaks across multiple API instances/containers unless mounted or replaced with S3/S3-compatible later
- **No payment processor** — deposit/withdraw paths are internal ledger writes (`TransactionController.deposit/withdraw`), not rails tobanks/MoMo/cards; `DepositRequest`/`WithdrawRequest` are placeholders awaiting real money flows
- **No caching/incident system** — Caffeine is single-instance; metrics exist but no Prometheus instance, Alertmanager, or error-tracking service is wired in the compose/dev setup
- **App CI/CD absent** — no Flutter build/test/deploy workflow in `.github/`

---

*Integration audit: 2026-08-06*