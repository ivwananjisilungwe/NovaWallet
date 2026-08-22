# NovaWallet — Project Plan

## Overview

| **Project** | NovaWallet |
|-------------|-----------|
| **Purpose** | Digital wallet for university students and small businesses in Zambia — Spring Boot REST API + Flutter mobile app |
| **Stack** | Java 17, Spring Boot 3.5.3, Maven, PostgreSQL 16 / H2 (tests), Flyway V1–V11, JWT (jjwt 0.12.7), SpringDoc OpenAPI; Flutter (Dart ^3.12.2), Riverpod ^2.6, go_router ^14.8.1, dio ^5.11 |
| **Target** | University students & small businesses in Zambia |
| **Status** | Phases 0–9 ✅ complete. Post-MVP hardening ✅ complete. Phase 10 (Flutter) ✅ substantially complete — only FCM push (10.7) outstanding. Phase 11 (payment gateway) ⏳ pending. |

---

## Status Snapshot (verified against current codebase)

| Area | State |
|------|-------|
| Backend tests | **130/130 passing** (`./mvnw test`, H2 profile) |
| Frontend tests | **214/214 passing** (`flutter test`); `flutter analyze` clean |
| Schema ownership | Flyway migrations V1–V11 (PostgreSQL in prod, H2 in tests) |
| Auth | JWT access tokens + persisted refresh-token rotation with reuse detection |
| Money safety | Pessimistic locking, ledger transactions, per-user idempotency keys, hourly velocity limits, KYC tier limits |
| CI/CD | GitHub Actions (`.github/workflows/ci.yml` under `novawallet-api/`): test → Docker build/push to GHCR |
| Containers | Multi-stage non-root Dockerfile; docker-compose with postgres:16 (+ Mailpit under dev profile) |

---

## Phase Status Summary

| Phase | Focus | Status |
|-------|-------|--------|
| **0** | Project Foundation | ✅ Complete |
| **1** | User & Auth Core | ✅ Complete |
| **2** | Transaction Core | ✅ Complete |
| **3** | Transaction History | ✅ Complete |
| **4** | Fee Engine & Audit | ✅ Complete |
| **5** | Idempotency | ✅ Complete |
| **5.5** | Fintech Hardening | ✅ Complete |
| **6** | Notifications & Scheduling | ✅ Complete |
| **7** | Admin Endpoints | ✅ Complete |
| **8** | Testing & QA | ✅ Complete |
| **9** | Docker & CI/CD | ✅ Complete |
| **—** | Post-MVP Hardening (per-user idempotency, velocity limits, secrets, email gate) | ✅ Complete |
| **10** | Flutter Frontend | ✅ Substantially complete (10.7 FCM ⏳) |
| **11** | Payment Provider Integration | ⏳ Pending |

---

## Post-MVP Hardening ✅ COMPLETE

Production-hardening additions made after the original Phases 0–9 were finished.

### H1 — Per-user idempotency scoping
- Idempotency keys are persisted and scoped **per user** via composite `"<userId>:<key>"` storage — two users reusing the same key never collide.
- Filter runs after `JwtAuthFilter`; oversized keys (>200 chars) return a clean `400`.
- Covered by cross-user independence tests and scheduler double-run safety tests.

### H2 — Transaction velocity limits
- Hourly velocity limits on withdrawals/transfers, configured under `app.transaction.velocity`.
- Defaults: **10 transactions/hour** and **50,000 ZMW/hour** per wallet.
- Breach returns `429 RATE_LIMITED`.

### H3 — Secrets hardening
- No secret defaults outside the dev profile: bare `${JWT_SECRET}` binding makes prod fail fast on missing secrets.
- Admin credential defaults relocated to dev-only profile; `AdminDataInitializer` seeding gated behind the dev profile and the password is never logged.
- All Java-side `@Value` secret fallbacks removed.

### H4 — Email verification gate
- Withdrawals and transfers require a verified email address (`403` before verification).
- Aligned with the Flutter verify-email flow; integration tests updated accordingly.

---

## Phase 0 — Project Foundation ✅

**Goal**: Dependency graph, base configuration, project packaging, code standards.

| Step | Task | Status |
|------|------|--------|
| 0.1 | Expand `pom.xml` (security, actuator, cache, mail, jjwt, springdoc, h2, caffeine) | ✅ |
| 0.2 | Multi-profile config (`application.yml` + dev/prod) | ✅ |
| 0.3 | Base package structure (`com.novawallet`: config/entity/dto/repository/service/controller/security/exception/audit/scheduler) | ✅ |
| 0.4 | Global exception handler — uniform `{status, code, message, timestamp, path}` errors | ✅ |
| 0.5 | `ApiResponse<T>` success envelope | ✅ |
| 0.6 | Flyway baseline migration | ✅ |
| 0.7 | Security headers filter (HSTS, X-Content-Type-Options, X-Frame-Options, CSP) | ✅ |
| 0.8 | Actuator health/info endpoints | ✅ |
| 0.9 | Swagger/OpenAPI config with security scheme | ✅ |
| 0.10 | MDC request tracing (`X-Request-ID`) | ✅ |

---

## Phase 1 — User & Authentication Core ✅

**Goal**: Register, login, JWT issuance, email verification, transaction PIN setup, rate limiting, password management.

| Step | Task | Status |
|------|------|--------|
| 1.1–1.4 | Entities: `User`, `Role`, `Wallet`, `RefreshToken` | ✅ |
| 1.5 | Flyway V1–V3 (users, wallets, refresh tokens) | ✅ |
| 1.6–1.8 | JWT utility, `SecurityFilterChain`, `JwtAuthFilter` | ✅ |
| 1.9–1.12 | Auth DTOs, `AuthService` register/login, account-number generation, `AuthController` | ✅ |
| 1.13–1.14 | PIN endpoint + PIN lockout (15 min after 3 failures) | ✅ |
| 1.15–1.16 | Email verification + password forgot/reset flows | ✅ |
| 1.17 | Strong password validation | ✅ |
| 1.18 | Async `MailService` | ✅ |
| 1.19 | Login rate limiter (email+IP lockout) | ✅ |
| 1.20 | Token-bucket `RateLimitFilter` (global + stricter auth bucket, `X-RateLimit-*` headers) | ✅ |
| 1.21 | Logout (revoke all refresh tokens) | ✅ |
| 1.22–1.23 | Auth service unit tests + controller integration tests | ✅ |

---

## Phase 2 — Transaction Core ✅

**Goal**: Deposit, withdraw, transfer with atomicity, PIN validation, ledger-style records, human-friendly references.

| Step | Task | Status |
|------|------|--------|
| 2.1–2.2 | `Transaction` entity + Flyway V4 | ✅ |
| 2.3 | Transaction DTOs | ✅ |
| 2.4–2.6 | Deposit / withdraw / transfer services (PIN check, balance guard, linked debit/credit pair) | ✅ |
| 2.7 | Atomic reference generator (`NWTX{yyyyMMdd}{seq}`) | ✅ |
| 2.8 | Balance integrity (ledger-verifiable stored balance) | ✅ |
| 2.9 | `TransactionController` endpoints | ✅ |
| 2.10 | Input validation | ✅ |
| 2.11 | Balance-change audit wiring | ✅ |
| 2.12–2.13 | Unit + integration tests | ✅ |

---

## Phase 3 — Transaction History & Querying ✅

**Goal**: Paginated, filterable history; users see only their own data.

| Step | Task | Status |
|------|------|--------|
| 3.1 | `TransactionRepository` with `Specification` filtering | ✅ |
| 3.2 | `TransactionHistoryService` (type/date/status filters, sorted desc) | ✅ |
| 3.3 | `PagedResponse<T>` envelope | ✅ |
| 3.4 | History endpoint with query params | ✅ |
| 3.5 | Ownership checks | ✅ |
| 3.6 | Single lookup by reference | ✅ |
| 3.7 | Wallet balance endpoint | ✅ |
| 3.8 | Pagination/filtering/ownership tests | ✅ |

---

## Phase 4 — Fee Engine & Audit Logging ✅

**Goal**: Configurable fees per transaction type; append-only audit log for balance changes.

| Step | Task | Status |
|------|------|--------|
| 4.1 | `FeeConfiguration` entity | ✅ |
| 4.2 | Flyway V5/V6 (fee configs, audit logs) | ✅ |
| 4.3 | Append-only `AuditLog` entity | ✅ |
| 4.4 | `FeeEngineService` (percentage + flat, min/max clamp, HALF_EVEN rounding) | ✅ |
| 4.5 | Fee deduction in transfer flow (linked FEE record) | ✅ |
| 4.6 | Public fee estimation endpoint | ✅ |
| 4.7 | Async `AuditService` | ✅ |
| 4.8 | `@Audited` AOP annotation | ✅ |
| 4.9 | Admin fee CRUD | ✅ |
| 4.10–4.11 | FeeEngine + AuditService tests | ✅ |

---

## Phase 5 — Idempotency ✅

**Goal**: Duplicate-charge prevention for financial operations.

| Step | Task | Status |
|------|------|--------|
| 5.4 | `IdempotencyKey` entity (status, TTL, optimistic locking) | ✅ |
| 5.5 | Flyway V9 (unique constraint, expiry index) | ✅ |
| 5.6 | `IdempotencyFilter` — atomic INSERT-first, poll-and-replay, 409 on timeout, error recovery | ✅ |
| 5.7 | `IdempotencyService` (acquire/complete/replay/cleanup) | ✅ |
| 5.8 | Nightly TTL cleanup job (04:00 cron) | ✅ |
| 5.9 | `app.idempotency.ttl-hours` config | ✅ |
| 5.10 | Idempotency integration tests (byte-for-byte replay) | ✅ |
| 5.11 | `@EnableScheduling` | ✅ |

> Superseded/extended by **Post-MVP Hardening H1**: keys are now scoped per user via composite `userId:key` storage.

---

## Phase 5.5 — Fintech Hardening ✅

**Goal**: Concurrency, KYC limits, unified errors, CORS, admin pagination, admin audit.

| Step | Task | Status |
|------|------|--------|
| 5.5.1 | Pessimistic locking (`SELECT … FOR UPDATE`, sender-before-receiver order) | ✅ |
| 5.5.2 | KYC tier daily-send/wallet limit enforcement (approved users only) | ✅ |
| 5.5.3 | Unified rate-limit error format (`ErrorResponse`) | ✅ |
| 5.5.4 | CORS whitelist configuration | ✅ |
| 5.5.5 | Admin user-list pagination | ✅ |
| 5.5.6 | Admin action audit logging (freeze/unfreeze/delete with `adminId`) | ✅ |
| 5.5.7 | Regression fixes across test suite | ✅ |

---

## Phase 6 — Notifications, Scheduling & Caching ✅

**Goal**: Async email/SMS alerts, scheduled cleanup jobs, hot-data caching.

| Step | Task | Status |
|------|------|--------|
| 6.1–6.3 | `Notification` + `NotificationAttempt` entities, Flyway V10 | ✅ |
| 6.4 | `NotificationService` (async send, status tracking, up-to-3 retries) | ✅ |
| 6.5 | `SmsService` stub (logs when unconfigured) | ✅ |
| 6.6 | Deposit/withdraw/transfer notification wiring | ✅ |
| 6.7 | Pending-transaction cleanup job (03:00) | ✅ |
| 6.8 | Idempotency key cleanup job (04:00, from Phase 5) | ✅ |
| 6.9 | Expired refresh-token cleanup job (04:30) | ✅ |
| 6.10–6.11 | Caffeine cache config + balance eviction on mutations | ✅ |
| 6.12 | Mail config | ✅ |
| 6.13–6.14 | Notification retry tests + full regression | ✅ |

---

## Phase 7 — Admin Endpoints & Production Hardening ✅

**Goal**: Admin dashboard API, soft delete, soft freeze with reasons, security hardening.

| Step | Task | Status |
|------|------|--------|
| 7.1 | Admin user/wallet listing (paginated, filtered) | ✅ |
| 7.2 | Wallet freeze/unfreeze with reason + audit | ✅ |
| 7.3 | Specification-powered admin transaction search | ✅ |
| 7.4 | Paginated, filterable audit-log viewer | ✅ |
| 7.5 | Full fee CRUD | ✅ |
| 7.6 | `@PreAuthorize("hasRole('ADMIN')")` enforcement | ✅ |
| 7.7 | Admin response DTOs | ✅ |
| 7.8 | Soft delete user (freeze wallet, hide by default) | ✅ |
| 7.9 | Security headers audit (incl. prod HSTS) | ✅ |
| 7.10 | Input sanitization + admin authorization tests (USER → 403) | ✅ |

> The admin API also covers **KYC approve/reject**, exposed in the Flutter admin console.

---

## Phase 8 — Testing & Quality Assurance ✅

**Goal**: Comprehensive coverage beyond happy paths.

| Step | Task | Status |
|------|------|--------|
| 8.1 | Service-layer unit tests | ✅ |
| 8.2 | Security component tests (JWT, filters, rate limiting) | ✅ |
| 8.3 | Fee engine edge cases | ✅ |
| 8.4 | Full transaction flow integration test | ✅ |
| 8.5 | Concurrent transfer test (overdraft prevention, deadlock resilience) | ✅ |
| 8.6 | Idempotency end-to-end tests | ✅ |
| 8.7 | Rate limiting end-to-end (headers, auth-tier limits) | ✅ |
| 8.8 | Refresh token rotation + reuse detection | ✅ |
| 8.9 | Scheduler idempotency | ✅ |
| 8.10 | Admin authorization tests | ✅ |
| 8.11–8.14 | Bug fixes surfaced by testing (transactional rollback, token validation, scheduled queries, test infrastructure) | ✅ |

> Suite has since grown past the original 123 tests to **130/130 passing**.

---

## Phase 9 — Docker, CI/CD & Production Readiness ✅

**Goal**: Containerization, CI pipeline, hardened production configuration.

| Step | Task | Status |
|------|------|--------|
| 9.1 | Multi-stage non-root Dockerfile with healthcheck | ✅ |
| 9.2 | docker-compose (postgres:16 + Mailpit dev profile, env-var secrets) | ✅ |
| 9.3 | Env-var-driven `application-prod.yml` (Flyway validate, HikariCP pool) | ✅ |
| 9.4 | GitHub Actions CI (test → Docker build/push GHCR, layer caching) | ✅ |
| 9.5 | logback JSON logging in prod, colored console in dev, rolling files | ✅ |
| 9.6 | CORS via `APP_CORS_ORIGINS` env var | ✅ |
| 9.7 | `.dockerignore` | ✅ |
| 9.8 | README documentation | ✅ (refreshed at repo root) |
| 9.9 | API curl examples | ✅ |

---

## Phase 10 — Flutter Frontend ✅ SUBSTANTIALLY COMPLETE

**Goal**: Cross-platform mobile app for NovaWallet.

| Step | Task | Status |
|------|------|--------|
| 10.1 | Scaffold with `go_router`, Riverpod, dio | ✅ |
| 10.2 | Auth screens (register, login, PIN setup, forgot/reset password, email verification) | ✅ |
| 10.3 | Dashboard (balance, quick actions, recent transactions) | ✅ |
| 10.4 | Send money flow (account number → amount → PIN confirmation → success) | ✅ |
| 10.5 | Transaction history (paginated, pull-to-refresh, type/date filters) | ✅ |
| 10.6 | Profile (view profile, change PIN, settings) | ✅ |
| 10.7 | Push notifications (Firebase Cloud Messaging) | ⏳ Not implemented |

**Delivered beyond the original scope:**

- Screens for deposit, withdraw, statements, cards, KYC, notifications, security/settings, and an **admin console**.
- Material 3 with three-layer design tokens (primitive → semantic → component) and **active dark mode** (`ThemeMode.system`) plus contrast contract tests.
- Optional **biometric unlock** (`local_auth`, Hive-backed flag): lock screen with retry counter and password fallback; PIN-set flow outranks the lock.
- Offline detection banner, Hive cache boxes, skeleton loaders.
- Polish: page transitions, haptics, pull-to-refresh, count-up balance animation.
- Client hardening: secure token storage with single-flight refresh; idempotency-key headers sent on mutations.
- Configuration: API base URL via `--dart-define=API_BASE_URL` (defaults: `localhost`, or `10.0.2.2` on the Android emulator).

**Verification**: 214/214 tests passing, `flutter analyze` clean.

---

## Phase 11 — Payment Provider Integration ⏳ PENDING

**Goal**: Live mobile-money deposits and payouts for real money in/out.

| Step | Task | Status |
|------|------|--------|
| **11.0** | **Gate 0 — compliance confirmation**: Lipila must confirm in writing that wallet/deposit-withdrawal business models are accepted under its merchant framework (we are a wallet, not e-commerce checkout) | ⏳ Ask at signup — could veto provider choice |
| 11.1 | Mobile-money deposit integration + webhook confirmation | 🟡 **Scaffold complete** (`payment` package, webhook endpoint, idempotent crediting) — awaiting Lipila account/keys. Pre-go-live hardening: **Payment domain entity** (separate from ledger) with full state machine + **unique provider-reference column**, and webhook amount/currency verification against initiated intent |
| 11.2 | ~~Paystack~~ Card payments | ⏳ Deferred — Paystack does not cover Zambia; card collections available via **Lipila** when needed (ZynlePay on card-research list) |
| 11.3 | Withdraw/payout to mobile money | 🟡 **Scaffold complete** (`/wallets/{id}/payouts`, PIN+limits gated, rollback-on-rejection) — awaiting keys |
| 11.4 | Webhook security (signature verification, idempotent processing) | 🟡 **Done in scaffold** — constant-time secret compare + `[LIPILA:<ref>]` idempotent markers; add HMAC if Lipila ships signed webhooks. **Stress test required:** same successful webhook ×100 concurrent ⇒ exactly ONE credit |
| 11.5 | Transaction status sync & reconciliation | ⏳ Pending (`checkStatus` wired). Reconciliation outcomes taxonomy: `AUTO_RESOLVE / ADMIN_REVIEW / RETRY / REFUND`. **Never auto-retry a financial payment across providers** (double-charge risk) |

### Zambia rails recommendation *(updated 2026-08-22)*

**Primary gateway — Lipila** (Zambia-native; supersedes the original Flutterwave recommendation):

- Covers MTN + Airtel + **Zamtel Kwacha** collections *and* disbursements, plus card collections and bank payouts — the only provider found covering all three Zambian operators.
- Self-onboarding sandbox at `dashboard.lipila.dev` (live: `dashboard.lipila.io`) — no sales cycle to start building.
- Official Flutter SDK (`lipila_flutter`) matches our client stack.
- **Verified pricing shape:** per-transaction percentage fee on MoMo collections/disbursements (exact % shown per-transaction in merchant dashboard); tiered settlement charges K5→K200 by settlement amount; no setup/monthly fees advertised.
- Backend integration is **already merged**: `PaymentGateway` SPI + `LipilaGateway` + webhook/payout endpoints behind `app.payments.lipila.*` (disabled until keys are set). Go-live = sign up → set `LIPILA_ENABLED/LIPILA_API_KEY/LIPILA_WEBHOOK_SECRET` env vars.
- Note: disbursements require server IP whitelisting.

**Secondary — Lenco (by BroadPay):** business bank rails, virtual accounts, MoMo transfers (ZM + Malawi). API v2, bearer auth, `collection.successful` webhooks. Adds bank-account payout corridor and multi-currency accounts. Requires Zambian business KYC docs (incorporation cert, PACRA <3 months, directors' IDs, corporate bank details).

**Deferred — international/cross-border:** ~~Flutterwave~~ / Onafriq / DPO become relevant only for expansion beyond Zambia (+Malawi edge case via Lenco) or USD card acceptance at scale. Not on the MVP critical path. Original plan's Paystack assumption remains invalid (no Zambia coverage).

**Cost optimization at volume:** once volume justifies engineering cost, move high-volume corridors direct — **MTN MoMo Collections API** and/or **Airtel Africa OpenAPI** — cutting aggregator percentage fees. Keep Lipila as fallback during any migration.

### Virtual cards — path & verified costs

NovaWallet's cards screen is design-stage demo data (`cards_demo_data.dart`) awaiting a real issuing relationship. Realistic paths:

| Provider | Verified published costs | Notes |
|---|---|---|
| **Fyatu** (stablecoin-funded Visa/MC) | $5/card (Mastercard World USD), $10/card (Visa Platinum USD); min funding $5; funding fee per plan | Fastest self-serve; funding via USDT/USDC adds crypto dependency |
| **Gpaynow-style BaaS** | $1–$50/card creation; 2.5% funding; 1.5% withdrawal of unspent; ~$1/card/mo maintenance | NGN-centric BINs |
| Enterprise issuers (Reap/Rain/Interlace class) | Setup $5K–$80K+, monthly minimums $2K–$20K/mo | Only at serious scale |
| **Union54 lineage** (ChitChat/Zazu's issuer) | ChitChat charges ~$5/mo per virtual card (user-reported) | Union54 paused after fraud incident; watch for revival |

**Recommendation stands: defer virtual cards until after mobile-money launch.** When ready, cheapest credible route is a Fyatu-class partnership (~$5–10/card + funding %) resold inside NovaWallet, or renegotiated BaaS rates once volume proves the product case. Do not block rails on it.

**Mandatory prerequisites before any provider goes live:** steps **11.4 (webhook signature verification + idempotent processing)** and **11.5 (status sync + reconciliation)** must be implemented and tested first — the backend's existing idempotency and audit infrastructure should be reused for this.

---

## Phase 11.5 — Bills, Airtime & Merchant Checkout 📋 PLANNED *(added 2026-08-22)*

**Goal**: Convert NovaWallet from "send money to friends" into "pay for life" — daily-utility spending that works entirely on the Phase 11 rails, no cards needed. This is the highest-leverage build after go-live: it creates daily-use habits and merchant-side network effects before virtual cards ever ship.

| Step | Task | Details | Est. |
|------|------|---------|------|
| 11.5.1 | Bills catalog & service | `bills` package: `BillProvider` entity (ZESCO electricity tokens, DSTV/GOtv, water, school fees), customer-reference validation per provider, payment via Lipila disbursement/collection to biller account, receipt stored as Transaction with `[BILL:<provider>:<custRef>]` marker | M |
| 11.5.2 | Airtime top-up (self + others) | Direct MoMo airtime purchase flows; own-number one-tap + gift-to-number; batch/scheduled top-ups later | S |
| 11.5.3 | Merchant checkout API | `POST /api/v1/checkout` (public, rate-limited): merchant supplies order ref + amount → returns hosted-payment URL / USSD prompt; webhook confirms to merchant `callbackUrl` reusing the existing idempotent webhook pipeline (`[MERCHANT:<orderRef>]` markers) | M |
| 11.5.4 | Merchant onboarding & keys | `merchant` role: register business → issued API key pair (pk_…/sk_…) hashed like refresh tokens; per-merchant fee config rows in existing `fee_configurations`; settlement to merchant wallet or bank payout (Lipila) | M |
| 11.5.5 | Checkout Flutter screens | Pay-from-balance sheet + MoMo-prompt flow + success/receipt screens; QR present-to-pay for physical merchants | M |
| 11.5.6 | Reconciliation job | Extend scheduler pattern: nightly sweep of PENDING bill/checkout transactions vs provider status (`checkStatus`), auto-settle or flag for admin review | S |

**Prerequisites:** Phase 11 live keys (11.1–11.3) · 11.5.6 requires 11.5 status polling. **Compliance note:** merchant settlement = holding third-party funds; keep balances ring-fenced per wallet (already ledger-enforced) and seek BoZ guidance once merchant volume is material.

---

## Phase 12 — Digital Goods & Pooled-Card Payments 📋 PLANNED *(added 2026-08-22)*

**Goal**: Let MoMo-only users (the banked-out 14–25 segment) buy international digital products — Netflix, game currency, Google Play, hosting — paying in ZMW through Phase 11 rails while NovaWallet completes the USD side via **one pooled, good-BIN virtual card** it controls. This converts the founder's own pain point (Airtel Mastercard BIN-rejected on Google Cloud despite funded balance) into the product wedge: users never see issuer declines, prepaid discrimination, or AVS failures because the international leg is handled server-side on a proven BIN with retry logic.

**Why pooled card beats per-user cards:** ~$5 total capital vs $5/user/month; BIN/AVS/retry expertise centralized; zero monthly barrier for teens; fraud surface rate-limited per user on your side.

| Step | Task | Details | Est. |
|------|------|---------|------|
| 12.1 | Pooled card acquisition | Open Fyatu-class issuer account (verified costs: $5/card Mastercard World USD, min funding $5); fund from Phase 11 revenue; document BIN acceptance test suite against top targets (Google, Netflix, Steam, Meta ads) before committing | S |
| 12.2 | Goods catalog & pricing engine | `goods` package: product entities (fixed-price SKUs like Netflix tiers / denominations like Google Play credit), ZMW↔USD pricing with FX buffer % + markup % (target 10–15%) stored in config, price-refresh job | M |
| 12.3 | Order service | Order state machine `QUOTED → PAID → FULFILLING → DELIVERED/FAILED_REFUNDED`; idempotency via `[GOODS:<orderRef>]` transaction markers (same convention as LIPILA/MERCHANT); wallet debit + order row in one TX | M |
| 12.4 | Card payment executor | Server-side executor calling issuer API (JIT auth where supported); bounded retries on soft declines; hard-decline → auto-refund to wallet + alert; full audit trail per attempt | M |
| 12.5 | Fulfillment & delivery | Per-product fulfillment strategy: direct subscription payment (Netflix), code/voucher purchase + in-app delivery (Google Play), receipt-only proof; manual-review fallback queue for stuck orders | M |
| 12.6 | Fraud & spend limits | Reuse velocity-limit infra: per-user daily/monthly goods caps by KYC tier; blocked-MCC list; new-device cooling period for high-value SKUs | S |
| 12.7 | Flutter storefront screens | Catalog grid → product detail (ZMW price incl. fees shown upfront) → PIN confirm → status/receipt; order history tab | M |
| 12.8 | Ops dashboard (admin) | Order monitor, refund tool, margin report, card-balance alerting (auto-pause store when float low) | S |

**Prerequisites:** Phase 11 live (11.1–11.3) · reuses velocity limits, fee engine, idempotency markers, admin console.
**Compliance/fraud notes:** goods purchases are user-own-spending (no third-party settlement → lighter BoZ posture than Phase 11.5 merchants); still enforce KYC-tier caps; watch ZMW→USD volatility — buffer % must be configurable without redeploy.

> **Evolution note:** Phase 12's pooled card is the launch wedge, not the destination. It proves demand and generates the revenue/KYB track record for Phase 13, after which digital-goods becomes one consumer of personal NovaWallet cards. See `docs/CARD-ISSUING-RESEARCH.md`.

---

## Phase 13 — NovaWallet Card Platform (per-user issuing) 📋 RESEARCH TRACK *(added 2026-08-22)*

**Goal**: Every eligible NovaWallet user can create their own virtual Mastercard, funded from their ZMW wallet balance, with full lifecycle controls — spendable at Google/Netflix/Steam and international merchants.

**Architecture principle:** card issuing is a separate domain from payment collection. Backend exposes a dedicated `CardIssuingProvider` SPI (`create/freeze/unfreeze/fund/setLimit/close/getTransactions`) alongside `PaymentGateway` — never merged into it.

| Step | Task | Notes |
|------|------|-------|
| 13.1 | Issuer/BIN-sponsor selection | **Lead: Scale + Mastercard** (verified 31 Mar 2026 announcement covering Zambia; single integration = onboarding+processing+BIN+compliance). Backups: Codego, Fyatu. Diligence bank: docs/CARD-ISSUING-RESEARCH.md §3 |
| 13.2 | Regulatory/KYB approval | BoZ posture assessment; NovaWallet KYC obligations vs provider-managed; legal review before contract |
| 13.3 | Virtual card issuing | Create-card flow behind KYC-tier eligibility; PAN/proxy handling server-side only |
| 13.4 | Wallet funding ↔ card ledger bridge | ZMW→USD conversion policy (spread configurable), fund/debit events mirrored into NovaWallet ledger |
| 13.5 | Authorization webhooks | Real-time auth events → per-card transaction history + push notifications (FCM) |
| 13.6 | Freeze/unfreeze + spending limits | Instant controls; per-user/per-card caps by KYC tier |
| 13.7 | 3-D Secure & security controls | Enrollment flow, decline-handling UX (the Airtel-BIN-rejection lesson: test BINs against Google/Netflix/Steam pre-launch) |
| 13.8 | Card reconciliation & disputes | Auth/clearing/chargeback pipeline mapped to audit trail |
| 13.9 | Flutter card management UI | Replaces demo data in cards_screen (swap-point already isolated in `cards_demo_data.dart`) |
| 13.10 | Physical cards | Later, same platform |

**Gating criteria to start:** Phase 11 live · Phase 12 revenue evidence · Scale/Mastercard KYB approved · unit-economics model from real pricing (issuance/FX/monthly minimums vs teen willingness-to-pay).

---

## Known Remaining Gaps

Stated honestly, consistent with the current codebase:

1. **FCM push notifications** (step 10.7) not implemented.
2. **Payment gateway integration** (Phase 11) not started.
3. `cards_screen.dart` still contains some raw hex colors outside the token system.
4. Core widgets (`PillButton` outlined/text variants, skeleton) read static light tokens in dark mode.
5. **H2-vs-Postgres integration parity run** outstanding — all backend tests run on H2; a Postgres-backed money-path run (dockerized) is queued.
6. Minor queued follow-ups tracked in `TODO.md` (e.g., replacing `IdempotencyFilter` Thread.sleep polling, prod mail credential fail-fast, splitting five oversized screens).

---

## Entity Relationship Summary

```
User (1) ──── (1) Wallet
User (1) ──── (N) RefreshToken
Wallet (1) ──── (N) Transaction (senderWallet)
Wallet (1) ──── (N) Transaction (receiverWallet)
User (1) ──── (N) Notification
Transaction (N) ──── (1) Transaction (relatedTransactionId) — self-referencing debit/credit pairs
FeeConfiguration — standalone, no FK
AuditLog — standalone, entityType + entityId as polymorphic reference
IdempotencyKey — standalone (scoped per user via composite userId:key)
NotificationAttempt (N) ──── (1) Notification
```

---

## Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Wallet balance | DB-stored + ledger-verifiable | Stored with atomic guarded updates; reconcilable against transaction records |
| Money precision | BigDecimal scale=2, HALF_EVEN | Banking standard, ngwee (ZMW cent) precision |
| Concurrency | `@Lock(PESSIMISTIC_WRITE)` + `SELECT FOR UPDATE` | Prevents lost updates on concurrent transfers; sender locked before receiver prevents deadlocks |
| KYC limits | Service-level check before transactions | Sums today's outgoing volume against tier limits; approved users only |
| Velocity limits | Hourly tx-count + amount caps per wallet | Caps blast radius of compromised accounts; returns `429 RATE_LIMITED` |
| Audit writes | `@Async` + independent transaction | Non-blocking; won't roll back with the main transaction |
| Token storage | BCrypt-hashed refresh tokens in DB | Server-side storage enables rotation and reuse detection |
| Refresh tokens | Rotation with family revocation on reuse | Stolen-token detection invalidates the whole family |
| Rate limiting | Token-bucket filter (Caffeine) | Constant memory, burst-tolerant; global + stricter auth buckets |
| Idempotency | DB-backed, atomic INSERT-first, **per-user composite key** | Unique constraint prevents concurrent duplicates; replay returns cached response; users can't collide |
| Email gate | Verified email required for money-out | Blocks throwaway-account abuse before funds can leave |
| Migration strategy | Flyway sequential versioned SQL (V1–V11) | Explicit schema control; no auto-DDL in production |

---

## Implementation Order

```
Phase 0 ─► Phase 1 ─► Phase 2 ─► Phase 3 ─► Phase 4        ALL ✅
                                                    │
                                                    ▼
                                          Phase 5 (Idempotency)          ✅
                                                    │
                                                    ▼
                                        Phase 5.5 (Hardening)            ✅
                                                    │
                                          ┌─────────┼─────────┐
                                          ▼         ▼         ▼
                                    Phase 6   Phase 7   Phase 8         ✅✅✅
                                          │         │         │
                                          └─────────┼─────────┘
                                                    ▼
                                              Phase 9 (Docker/CI)         ✅
                                                    │
                                                    ▼
                                     Post-MVP Hardening (H1–H4)           ✅
                                                    │
                                              ┌─────┴─────┐
                                              ▼           ▼
                                         Phase 10     Phase 11
                                       ✅ (10.7 ⏳)     ⏳ pending
```

Phases 0–5.5 were strictly sequential; 6–8 partially overlapped; 9 ran independently. Remaining work: **10.7 (FCM)** and **Phase 11 (payments)** — with 11.4/11.5 as mandatory prerequisites before any provider goes live.

---

*Historical day-by-day execution schedule (22-day plan, Weeks 1–4) was completed and retired; see the original plan document in git history if needed.*
