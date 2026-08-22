# NovaWallet

A mobile-first digital wallet for university students and small businesses in Zambia — ZMW-denominated accounts, PIN-protected transfers, KYC tiers, and an admin console, built as a hardened Spring Boot API with a Flutter app on top.

## Architecture

```
┌───────────────────────────────┐         ┌─────────────────────────────────────┐
│   novawallet-app (Flutter)    │  HTTPS  │      novawallet-api (Spring Boot)   │
│                               │ ──────► │                                     │
│  Material 3 · Riverpod        │  JSON   │  REST /api/v1 · Swagger UI          │
│  go_router · dio              │ ◄────── │  JWT access + rotating refresh      │
│  Hive cache · biometric lock  │         │  Flyway V1–V11 schema ownership     │
│  Dark mode · offline banner   │         │  Rate limiting · Idempotency keys   │
└───────────────────────────────┘         │  Velocity + KYC limits · Fee engine │
                                          │  Audit AOP · Notifications · Admin  │
                                          └─────────────────┬───────────────────┘
                                                            │
                                          ┌─────────────────┼───────────────────┐
                                          ▼                 ▼                   ▼
                                    PostgreSQL 16     Mailpit (dev)       Actuator health
                                    (prod datastore)  email capture       Swagger UI
```

## Repository Layout

```
NovaWallet/                          # monorepo (no root manifest)
├── novawallet-api/                  # Backend — Java 17, Spring Boot 3.5.3, Maven
│   ├── src/main/java/com/novawallet/
│   ├── src/main/resources/db/migration/   # Flyway V1–V11
│   ├── Dockerfile                   # multi-stage, non-root
│   ├── docker-compose.yml           # postgres:16 (+ mailpit under dev profile)
│   └── .github/workflows/ci.yml     # GitHub Actions: test → docker build/push
├── novawallet-app/                  # Frontend — Flutter (Dart ^3.12.2), Material 3
│   ├── lib/core/theme/              # three-layer design tokens (primitive→semantic→component)
│   ├── lib/features/                # auth, wallet, kyc, admin, … screens
│   └── test/                        # unit + widget tests (214)
└── docs/                            # SRS, project plan, UI plans
```

## Feature Highlights

### Backend (`novawallet-api`)
- **Auth** — JWT access tokens with persisted refresh-token rotation (jjwt 0.12.7), email verification, password reset, transaction PIN with lockout.
- **Money safety** — pessimistic-lock ledger transactions; DB-backed idempotency keys scoped per user (`userId:key` composite); hourly velocity limits on withdrawals/transfers (default 10 tx/h and 50,000 ZMW/h → `429 RATE_LIMITED`).
- **Limits & fees** — KYC-tier daily-send/wallet limits; configurable fee engine with full admin CRUD.
- **Rate limiting** — token-bucket filter with a global bucket plus a stricter auth bucket and standard `X-RateLimit-*` headers.
- **Audit & observability** — append-only audit log via `@Audited` AOP; Actuator health; Swagger/springdoc UI.
- **Notifications** — async email/SMS (SMS is a stub) with persisted retry records.
- **Admin API** — user management, wallet freeze/unfreeze, transaction search, audit-log viewer, fee configuration, KYC approve/reject.
- **Hardening** — no secret defaults outside the dev profile; dev-only admin seeding whose password is never logged; verified email required before money-out endpoints.
- **Shipping** — multi-stage non-root Dockerfile, docker-compose (Postgres + Mailpit dev profile), GitHub Actions CI.

### Frontend (`novawallet-app`)
- **Material 3** with a three-layer token system (primitive → semantic → component) and active dark mode (`ThemeMode.system`) backed by contrast contract tests.
- **Security UX** — optional biometric unlock (`local_auth`, Hive-backed flag) with lock screen, retry counter, and password fallback; PIN-set flow outranks the lock; PIN pad flows throughout money movement.
- **Resilience** — secure token storage with single-flight refresh, idempotency-key headers on mutations, offline detection banner, Hive cache boxes, skeleton loaders.
- **Polish** — page transitions, haptics, pull-to-refresh, count-up balance animation.
- **Screens** — onboarding; auth (login/register/forgot/reset/verify); dashboard; send/deposit/withdraw/history/statements; cards; KYC; notifications; profile/security/settings; admin console.
- **Configuration** — API base URL via `--dart-define=API_BASE_URL`; defaults to `localhost` on desktop or `10.0.2.2` on the Android emulator.

## Quickstart

### Backend

```bash
cd novawallet-api

# Required outside the dev profile — the app fails fast without them:
export JWT_SECRET="$(openssl rand -base64 48)"
export APP_ADMIN_PASSWORD="choose-a-strong-password"   # dev-only admin seed credential

docker compose up -d          # postgres:16 (add --profile dev for Mailpit)
./mvnw spring-boot:run        # http://localhost:8080/api
```

Swagger UI: <http://localhost:8080/swagger-ui.html> · Health: <http://localhost:8080/actuator/health>

### Frontend

```bash
cd novawallet-app
flutter pub get
flutter run                   # Android emulator reaches the API via 10.0.2.2 automatically

# Physical device — point at your machine's LAN IP:
flutter run --dart-define=API_BASE_URL=http://<your-lan-ip>:8080/api
```

## Testing

```bash
cd novawallet-api && ./mvnw test        # 130/130 passing
cd novawallet-app && flutter analyze    # clean
cd novawallet-app && flutter test       # 214/214 passing
```

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.5.3, Maven, Spring Security, jjwt 0.12.7 |
| Database | PostgreSQL 16 (prod), H2 (tests), Flyway V1–V11 migrations |
| Caching & limits | Caffeine (token-bucket rate limiting, balance cache) |
| Docs & ops | springdoc OpenAPI (Swagger UI), Spring Boot Actuator, Docker, GitHub Actions CI |
| Frontend | Flutter Material 3, Dart ^3.12.2, Riverpod ^2.6, go_router ^14.8.1, dio ^5.11 |
| Local storage | Hive (cache boxes, biometric flag), secure token storage with single-flight refresh |
| Biometrics | local_auth ^2.3.0 |

## Roadmap

See [`docs/PROJECT-PLAN.md`](docs/PROJECT-PLAN.md) for the full phased plan: Phases 0–9 (backend) ✅ complete, Phase 10 (Flutter app) ✅ substantially complete, Phase 11 (payment-gateway integration — Zambia mobile-money rails) ⏳ pending.

## License

License not yet chosen — all rights reserved until a LICENSE file is added.
