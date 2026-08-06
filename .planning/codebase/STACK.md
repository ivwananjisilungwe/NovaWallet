# Technology Stack

**Analysis Date:** 2026-08-06

Monorepo with two independent applications: a Spring Boot REST backend (`novawallet-api/`) and a Flutter mobile/desktop/web client (`novawallet-app/`). Root project docs live in `SPECIFICATION.md`, `SRS.md`, and `USER-FLOW.md`; the backend also carries a full OWASP-style security spec in `novawallet-api/SECURITY.md`.

## Languages

**Primary:**
- Java 17 — entire backend under `novawallet-api/src/main/java/` (package root `com.novawallet.novawallet_api`)
- Dart 3 (`sdk: ^3.12.2`), compiled by Flutter stable channel — entire frontend under `novawallet-app/lib/`

**Secondary:**
- SQL (Flyway migration scripts) — `novawallet-api/src/main/resources/db/migration/V1__init_schema.sql` through `V10__create_notifications_tables.sql`
- Bash for Maven wrapper (`novawallet-api/mvnw`) and Docker shell steps

## Runtime

**Backend:**
- JVM 17 — Spring Boot 3.5.3 (`novawallet-api/pom.xml`, parent `spring-boot-starter-parent`)
- Package Manager: Maven 3.9.16, wrapper script `novawallet-api/mvnw` (`.mvn/wrapper/maven-wrapper.properties`)
- Lockfile-equivalent: `novawallet-api/pom.xml` declares all direct deps (Maven has transitive resolution; no lockfile)

**Frontend:**
- Flutter stable channel (revision `ad70ec4617166f1c38e5d2bfd388af71fda14f06`, `.metadata`) — Dart SDK `^3.12.2`
- Package Manager: pub — lockfile `novawallet-app/pubspec.lock` (in sync with `pubspec.yaml`)
- Target platforms declared in `.metadata`: Android, iOS, Linux, Web (platform folders `android/`, `ios/`, `linux/`, `web/` all present)

**Containerization:**
- Docker multi-stage build — `novawallet-api/Dockerfile` (builder `maven:3.9-eclipse-temurin-17` → runtime `eclipse-temurin:17-jre-alpine`, non-root user, healthcheck on `/api/actuator/health`)
- Compose stack — `novawallet-api/docker-compose.yml`: `api` (port 8080), `db` (PostgreSQL 16, port 5432), `mailpit` (dev profile only, ports 8025 web / 1025 SMTP)

## Frameworks

**Backend:**
- Spring Boot 3.5.3 — web (REST), data-jpa (Hibernate 6), security, validation, actuator, cache, AOP, mail starters (`novawallet-api/pom.xml`)
- springdoc-openapi 2.8.9 — Swagger UI/OpenAPI at `/api/swagger-ui.html` and `/api/api-docs` (`application.yml`)
- Flyway 11 — schema migrations, `locations: classpath:db/migration`
- jjwt 0.12.7 (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) — self-issued JWT auth
- Lombok — boilerplate reduction (annotation-processor configured in `pom.xml`)
- Jackson — REST serialization, dates as ISO-8601 (`spring.jackson.serialization.write-dates-as-timestamps: false`)

**Frontend:**
- Flutter (Material 3) — `lib/app/app.dart`, `lib/core/theme/app_theme.dart`, `lib/core/theme/nova_colors.dart`
- flutter_riverpod 2.6.1 + riverpod 2.6.1 — state management (`lib/core/providers.dart`, `lib/features/*/providers/`)
- go_router 17.4.0 — declarative navigation with redirect/auth gates (`lib/app/router.dart`)
- dio 5.11.0 — HTTP client with interceptors (`lib/core/network/api_client.dart`)
- flutter_secure_storage 11.0.0 — platform-secure token storage (`lib/core/storage/token_storage.dart`)

**Testing:**
- Backend: `spring-boot-starter-test` (JUnit 5, Mockito, AssertJ), `spring-security-test`, H2 in-memory DB for integration tests (`pom.xml`; config at `src/test/resources/application-test.yml`). ~112+ tests across `src/test/java/`
- Frontend: `flutter_test` + `flutter_lints` 6.0.0; single smoke test `test/widget_test.dart`

**Build/Dev (frontend):**
- `flutter_lints` via `analysis_options.yaml` (`include: package:flutter_lints/flutter.yaml`)
- `apiBaseUrl` injectable at build time via `--dart-define=API_BASE_URL` (`lib/core/config/app_config.dart`)

## Key Dependencies

**Backend — critical:**
- `spring-boot-starter-web` — REST API layer; controllers under `/api/v1/*`
- `spring-boot-starter-data-jpa` + `postgresql` (runtime) — persistence on PostgreSQL 16 / H2 in tests
- `spring-boot-starter-security` + `jjwt` — JWT auth, BCrypt, refresh-token rotation (`config/SecurityConfig.java`, `security/JwtUtil.java`, `security/JwtAuthFilter.java`)
- `spring-boot-starter-mail` — transactional emails via `JavaMailSender` (`notification/MailService.java`)
- `spring-boot-starter-cache` + `caffeine` — in-process caching (`config/CacheConfig.java`)
- `flyway-core` + `flyway-database-postgresql` — managed schema
- `logstash-logback-encoder` 8.0 — JSON structured logging in prod (`logback-spring.xml`)
- `spring-boot-starter-actuator` — health/info (dev), health/info/metrics/prometheus (prod) endpoints

**Frontend:**
- `dio` 5.11.0 — networking + interceptors (auth refresh single-flight, error mapping, `Idempotency-Key` header on mutating calls)
- `flutter_riverpod` / `riverpod` 2.6.1 — providers per feature (`lib/features/*/providers/`)
- `go_router` 17.4.0 — routing with auth/PIN/onboarding redirect gates in `lib/app/router.dart`
- `flutter_secure_storage` 11.0.0 — Keychain/EncryptedSharedPreferences for tokens (`lib/core/storage/token_storage.dart`)
- `google_fonts` 8.2.1 — font loading (`lib/core/theme/`)
- `intl` 0.20.3 — formatting (`lib/core/utils/formatters.dart`)
- `cupertino_icons` 1.0.9 — icon assets

**Backend build/config files:**
- `novawallet-api/pom.xml` — dependency manifest
- `novawallet-api/mvnw` — wrapper
- `novawallet-api/.mvn/` — wrapper config
- `novawallet-api/docker-compose.yml`, `novawallet-api/Dockerfile`, `novawallet-api/.dockerignore`
- `novawallet-api/.github/workflows/ci.yml` — CI/CD

**Frontend build/config files:**
- `novawallet-app/pubspec.yaml`, `novawallet-app/pubspec.lock`
- `novawallet-app/analysis_options.yaml`
- `novawallet-app/README.md`
- `.metadata` — Flutter tool metadata (not a build config)

## Configuration

**Backend:**
| File | Purpose | Key settings |
|------|---------|--------------|
| `application.yml` || Default (dev) profile — sets base config, default JWT secret (BASE64-encoded dev secret), rate limits, KYC tiers, `app.kyc.upload-dir: uploads/kyc` |
| `application-dev.yml` | Local dev — local PostgreSQL at `jdbc:postgresql://localhost:5432/novawallet`, Hibernate `ddl-auto: validate`, Flyway enabled, SQL debug logging. NOTE: local dev credentials are hardcoded here (not env-driven) |
| `application-prod.yml` | Production — all secrets from env vars (`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `APP_ADMIN_PASSWORD`, `SPRING_MAIL_*`), Hikari pool tuning, actuator + prometheus, JSON logging |
| `.env.example` | Required env template (DB_PASSWORD, JWT_SECRET, APP_ADMIN_PASSWORD, optional APP_SMS_API_KEY / APP_CORS_ORIGINS) |
| `.env` | Optional — noted in `.env.example`; used together with `docker compose up -d` |
| `logback-spring.xml` | Logback profiles: console with `%X{requestId}` request tracing (dev), Logstash JSON appender (prod) |

JWT defaults: `jwt.secret=${JWT_SECRET:...dev-fallback}`, `jwt.expiration=${JWT_EXPIRATION:900000}` (15 min access tokens). Refresh tokens rotated server-side and tracked in `refresh_tokens` table — see `token/service/TokenService.java`.

**Frontend:**
- **`API_BASE_URL`** via `--dart-define=API_BASE_URL=...` (highest priority), else platform defaults — `http://10.0.2.2:8080/api` on Android emulator, `http://localhost:8080/api` everywhere else (`lib/core/config/app_config.dart`). Backend serves REST under `/api/v1/*` (controllers map the `/api` prefix via `@RequestMapping`)
- `lib/core/network/api_client.dart` — timeouts (connect 15s, receive 30s, send 30s), `Accept: application/json`
- `lib/core/storage/token_storage.dart` — access token, refresh token, user JSON, PIN-set flag in secure storage

## Platform Requirements

**Development:**
- JDK 17 (backend), Maven 3.9.9+
- PostgreSQL 16 (via `docker compose up -d` or local install)
- Flutter stable with Dart SDK ≥ 3.12.2
- Node/npm not required — no JS tooling in this repo

**Production:**
- Container image `ghcr.io/<repo>/novawallet-api` (pushed by CI on main)
- `docker compose` with `--profile dev` for Mailpit during email development
- Runtime JRE 17 (alpine), non-root user, healthcheck via `/api/actuator/health`

## Package Manager Summary

| App | Manager | Lockfile | Repo |
|-----|----------|----------|------|
| Backend | Maven (`.mvn/wrapper`) | None (pom.xml authoritative) | `novawallet-api/` |
| Frontend | pub (Dart) | `pubspec.lock` | `novawallet-app/` |

---

*Stack analysis: 2026-08-06*