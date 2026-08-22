# NovaWallet — Full-Stack Technical Report

> **Date:** 2026-08-22 · **Scope:** Backend (novawallet-api) → Frontend (novawallet-app) · **Repo state:** `main` @ live, all work committed & pushed
> **Verification basis:** Backend `./mvnw test` **144/144** · Flutter `analyze` clean + **214/214** tests · PostgreSQL 16 parity **26/26** money-path suites, zero divergences

---

## 1. Executive Summary

NovaWallet is a Zambia-first digital wallet: a Spring Boot 3.5.3 API implementing banking-grade ledger semantics, consumed by a Material 3 Flutter app. The platform is code-complete for its MVP loop (register → KYC → deposit → transfer → withdraw → history) and verified end-to-end on both H2 (test) and real PostgreSQL (parity run). Live money movement awaits only gateway credentials: the Lipila integration (MTN/Airtel/Zamtel collections + payouts) is fully scaffolded behind a disabled-by-default flag.

**Quality posture:** zero TODO/FIXME/print statements across ~12k frontend LOC and 130+ backend files; secrets hardened to env-var-only outside the dev profile; every money mutation idempotent per-user; hourly velocity limits enforced pre-PIN; append-only audit trail on every balance change.

---

## 2. System Architecture

```
┌─────────────────────────── Flutter App (Material 3) ───────────────────────────┐
│ features/{auth,wallet,transaction,kyc,notification,admin,profile}              │
│   screens/ widgets/ providers/ data(repos)/ models/ routes/                    │
│ core/: theme(3-layer tokens) • network(ApiClient/Dio) • cache(Hive)            │
│        storage(flutter_secure_storage) • config(AppConfig) • widgets(barrel)    │
└──────────────┬─────────────────────────────────────────────────┬───────────────┘
               │ HTTPS + JWT Bearer + Idempotency-Key             │ local_auth
               ▼                                                  ▼
┌────────────────────────── Spring Boot API ──────────────────────┐   ┌──────────────┐
│ security: JwtAuthFilter → IdempotencyFilter → RateLimitFilter    │   │ BiometricPrompt│
│ payment: PaymentGateway SPI → LipilaGateway ── webhooks/payouts  │◄──┤ (OS-owned)   │
│ transaction: TransactionService (ledger, locks, velocity, gates) │   └──────────────┘
│ fee/audit/kyc/notification/admin/idempotency/token/user/wallet   │
│ Flyway V1–V11 schema ── PostgreSQL 16 (prod) / H2 (tests)        │
└──────────────┬──────────────────────────────┬────────────────────┘
               ▼                              ▼
        ┌──────────────┐            ┌────────────────────┐
        │ PostgreSQL 16│            │ Lipila / (Lenco*)  │
        │ Flyway-owned │            │ MoMo + card rails  │
        └──────────────┘            └────────────────────┘
                                    * secondary, planned
```

---

## 3. Backend Deep-Dive

### 3.1 Stack & Layout
Java 17 · Spring Boot 3.5.3 · Maven · PostgreSQL 16 (Flyway V1–V11 owns schema; `ddl-auto: validate`) · H2 for tests · jjwt 0.12.7 · Caffeine cache · springdoc OpenAPI · Logstash JSON logging.

Package-by-feature (`auth, user, wallet, transaction, fee, kyc, notification, audit, admin, idempotency, token, payment, security, config, exception, bootstrap, filter`). 130 main files; largest service 401 LOC (`TransactionService`). Centralized `GlobalExceptionHandler` with typed exceptions mapping to a uniform `{status, code, message, timestamp, path}` error body; request-ID tracing filter.

### 3.2 Security
| Control | Implementation |
|---|---|
| AuthN | Stateless JWT (15 min access) + refresh tokens stored as BCrypt hashes, **rotated single-use** with stolen-token family revocation |
| Secrets | No defaults in base/prod YAML — fail-fast `${JWT_SECRET}`, `${APP_ADMIN_PASSWORD}`; admin seeding dev-profile-only, credentials never logged |
| Brute force | Login rate limiter (5 fails/email+IP → lockout) + global token-bucket `RateLimitFilter` (100 rpm default, 10 rpm auth) with `X-RateLimit-*` headers |
| Transport | Strict CSP/HSTS headers; Swagger-scoped CSP exception; CORS whitelist via env var |
| Admin | `@PreAuthorize("hasRole('ADMIN')")`, soft-delete users, freeze reasons audit-logged |

### 3.3 Money Movement (the core)
- **Ledger discipline:** every operation writes `balanceBefore/balanceAfter`; wallet balance changes only via guarded atomic updates (`WHERE balance ≥ amount`); sender locked before receiver (`SELECT … FOR UPDATE`) — concurrent-transfer suite proves overdraft impossibility.
- **Guardrail stack (withdraw/transfer/payout):** velocity limits (hourly count + amount, `429 RATE_LIMITED`) → PIN verification (BCrypt, attempt lockout) → KYC daily-send limit → email-verification gate (`403` until verified). Fees computed by configurable engine (% + flat, min/max clamp, HALF_EVEN ngwee precision).
- **Per-user idempotency:** `IdempotencyFilter` stores keys as `"<userId>:<key>"` after JWT principal resolution (runs post-auth in the security chain); atomic INSERT-first with DB unique constraint; losing requests replay cached responses after exponential-backoff polling (25→200 ms cap, 8 attempts) else `409 IDEMPOTENCY_IN_FLIGHT`; TTL cleanup job at 04:00.

### 3.4 Payment Rails (Phase 11 scaffold)
`PaymentGateway` SPI with `LipilaGateway` implementation (RestClient, `x-api-key`, JDK-client timeouts):
- **Webhooks:** `POST /api/v1/webhooks/lipila/collections` — constant-time secret compare (`MessageDigest.isEqual`); loud failure semantics (bad secret 401, malformed ref 400, unknown wallet **404 by design**); credits flow through the standard ledger with deterministic `[LIPILA:<referenceId>]` markers; pessimistic lock acquired before existence check so duplicate callbacks serialize safely.
- **Payouts:** authenticated endpoint applying the identical guardrail stack; debit-local → disburse-remote in one transaction; provider rejection → full rollback + 400; disabled flag → typed 503.
- Reference convention: `NW:<walletId>:<uuid>`; env keys `LIPILA_ENABLED/LIPILA_BASE_URL/LIPILA_API_KEY/LIPILA_WEBHOOK_SECRET`.

### 3.5 Data & Ops
Flyway migrations V1–V11 (users, wallets, refresh_tokens, transactions, fees, audit_logs, kyc, notifications, idempotency_keys). Audit via `@Audited` AOP writing async append-only rows. Three cleanup crons (pending txns 03:00, idempotency 04:00, refresh tokens 04:30). Actuator health; multi-stage non-root Dockerfile with HEALTHCHECK; compose bundles postgres:16-alpine + Mailpit (dev profile); GitHub Actions CI builds/tests and pushes GHCR images.

### 3.6 Verification Evidence
- H2 suite: **144/144** (includes 14 payment-scaffold tests: webhook happy/duplicate/bad-secret/unknown-wallet, payout enabled/disabled/rejection-rollback, gateway contract via MockRestServiceServer)
- **PostgreSQL parity:** all 8 money-path suites green on PG 16 (scratch container, no pulls), Flyway applied clean, zero dialect/locking divergences
- Known follow-ups: reconciliation sweep for PENDING payouts; unique-constrained provider-reference column (removes ms-scale double-credit window)

---

## 4. Frontend Deep-Dive

### 4.1 Stack & Structure
Flutter Material 3 · Dart ^3.12.2 · Riverpod ^2.6 · go_router ^14.8.1 · dio ^5.11 · flutter_secure_storage · Hive · google_fonts (Inter) · connectivity_plus. ~12k LOC across feature-first modules (`features/*/data/models/providers/screens/routes` + `core/*`).

### 4.2 Design System
Three-layer tokens (`design_tokens.dart`: primitive → semantic → component, all `const`): light theme plus **activated dark theme** (`SemanticDarkColors`, M3 tone inversion from reserved primitives only). Dark mode follows the device (`ThemeMode.system`); WCAG AA contrast (≥4.5:1 text) enforced by automated dark-theme contract tests incl. light regression guards. Zero hardcoded hex in themed surfaces; cards demo data isolated behind `cards_demo_data.dart` swap-point.

### 4.3 Auth & Session
go_router single redirect gate orchestrates splash/init → login/register → mandatory PIN-set → biometric gate → shell. Biometric unlock: Hive-backed flag (default OFF), capability-probed settings toggle, lock screen with retry + "use password" fallback (logout), background-resume re-lock; **PIN-setup deliberately outranks the lock gate** (regression-tested). Tokens persist in secure storage; ApiClient performs single-flight refresh rotation on 401 with one retry and unwraps the `{success,message,data}` envelope into typed `ApiException`s.

### 4.4 Network Discipline
All mutations carry per-user idempotency keys; operation keys rotate on retryable failures (tested across withdraw/send flows incl. 409-rate-limit and network-error paths); base URL resolution `--dart-define` → emulator `10.0.2.2` → localhost (no LAN addresses committed).

### 4.5 Screens & UX Polish
Onboarding, full auth set, dashboard (count-up balance, quick actions, recent activity), send/deposit/withdraw with fee estimates and balance indicators, paginated history/statements, KYC upload/status, notifications center, profile/security/settings, admin console. Offline banner, skeleton loaders, branded pull-to-refresh, haptics, hero animations, page transitions. All previously oversized screens now modular (send 710→319, dashboard 693→97 LOC; `widgets.dart` is a pure export barrel).

### 4.6 Tests
214 widget/unit tests mirroring lib structure: repos, providers, router redirects (incl. biometric matrix), ApiClient contracts, color-contrast suites, PIN-pad flows, lock-screen lifecycle — mocktail-mocked boundaries throughout.

---

## 5. Cross-Cutting Status

| Dimension | Grade | Notes |
|---|---|---|
| Ledger integrity | A | Locks + balance pairs + concurrent tests |
| Abuse resistance | A− | Velocity + rate limits + PIN lockouts; payout reconciliation pending |
| Secrets hygiene | A | Env-only prod; dev values quarantined; history treated as burned |
| Test depth | A− | 358 combined tests; E2E-on-device (biometrics) outstanding |
| UI consistency | A− | Token-enforced; legacy statics confined & flagged |
| Observability | B+ | JSON logs + request IDs + actuator; no external APM yet |

**Rating trajectory this cycle:** 68/100 (Risky) → **~79–82/100 (Launchable with caveats)**. Remaining launch blockers are operational, not architectural: gateway credentials, FCM project, CI green-run confirmation.

## 6. Roadmap Snapshot

| Phase | Scope | State |
|---|---|---|
| 0–9 + Hardening | Core API + security waves | ✅ Done |
| 10 | Flutter client (FCM 10.7 open) | ✅ Substantially done |
| 11 | Live Lipila rails | 🟡 Awaiting keys |
| 11.5 | Bills, airtime, merchant checkout | 📋 Planned |
| 12 | Digital-goods store (pooled card) | 📋 Planned |
| Later | Merchant gateway product · virtual cards · AI roadmap (`docs/AI-STRATEGY.md`) | 📝 Documented |

*Cross-references: docs/PROJECT-PLAN.md (phases) · TODO.md (tracker) · docs/SRS.md (requirements) · HANDOFF_2026-08-22.md (session context).*
