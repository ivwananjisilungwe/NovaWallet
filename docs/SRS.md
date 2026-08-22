# NovaWallet — Software Requirements Specification

## 1. Purpose

NovaWallet is a Zambia-first digital wallet platform: a Spring Boot backend managing user accounts, wallet balances, financial transactions, KYC, and payment rails, plus a Flutter mobile client. It implements banking-grade ledger semantics (pessimistic locking, balanceBefore/After pairs, append-only audit) with proper security, validation, and idempotent money movement. Live money flows through **Lipila** (primary gateway: MTN/Airtel/Zamtel MoMo collections & disbursements, card collections), with a phased roadmap toward bills/merchant checkout (Phase 11.5) and digital-goods payments via a pooled good-BIN card (Phase 12).

**Target users**: University students, youth (14–25) without bank accounts, small business owners, and peer-to-peer users in Zambia requiring transfers, bill payments, and online digital-goods purchases using only mobile money.

---

## MVP (Minimum Viable Product)

**Core loop**: User registers → gets a wallet → deposits money → sends to another user → withdraws → sees history.

**Status: MVP is ✅ COMPLETE and verified.** Backend phases 0–9 + hardening wave built and tested (**144 API tests passing**, H2 suite) with **Postgres parity proven** (26/26 money-path suites green on PostgreSQL 16). Flutter client substantially complete (**214 tests passing**, analyzer clean). Live-money enablement (Phase 11) is scaffolded and awaits gateway account credentials only.

### MVP Features (Built, Tested, Working)

| Area | What | Why MVP |
|------|------|---------|
| **Auth** | Register, login, JWT + refresh token rotation, PIN (4-6 digit, 3-attempt lockout), email verification, password reset | Users must identify securely |
| **Wallet** | Auto-generated account number (NW-prefix), BigDecimal balance, ACTIVE/FROZEN status, DB-level negative balance guard | Every user needs a wallet |
| **Deposit** | Add funds atomically, create DEPOSIT transaction, balanceBefore/balanceAfter audit trail | Money must enter the system |
| **Transfer** | Peer-to-peer send with atomicity, pessimistic locking, PIN verification, fee deduction | Core value: sending money |
| **Withdraw** | Deduct funds with PIN check, balance guard, WITHDRAWAL record | Money must leave the system |
| **History** | Paginated transaction list with type/date/status filters, lookup by reference | Users see what happened |
| **Fees** | Configurable % + flat fee per transaction type, min/max clamping, HALF_EVEN rounding | Platform sustainability |
| **Idempotency** | Idempotency-Key header, atomic INSERT-first, poll-and-replay, 24hr TTL cleanup | Prevents duplicate charges |
| **Audit** | Async append-only audit log, AOP @Audited annotation | Every balance change recorded |
| **Rate Limiting** | Token-bucket filter (Caffeine), 100 req/min default, 10 req/min on auth, 429 with Retry-After | Brute-force protection |
| **Notifications** | Async email + SMS on transactions (Africa's Talking stub), 3 retry logic, scheduled retry job | Users get alerts |
| **Scheduling** | 5 cron jobs: pending txn cleanup (3am), idempotency cleanup (4am), refresh token cleanup (4:30am), KYC retry (2am) | System self-maintenance |
| **Caching** | Caffeine cache for wallet balances (10 min write expiry), evicted on every mutation | Performance |
| **KYC** | Tier-based daily send limits and balance caps, document upload scaffold | Regulatory compliance |
| **Admin API** | User mgmt, wallet freeze/unfreeze with reason, transaction search, audit log view, fee CRUD, role enforcement | Platform moderation |
| **Docker/CI** | Multi-stage Dockerfile, docker-compose (API + PostgreSQL + Mailpit), GitHub Actions CI, JSON logging | Production deployment |

### What's NOT MVP (Post-MVP / V2)

| Feature | Why Post-MVP | Required Before Launch? |
|---------|--------------|------------------------|
| **Flutterwave / Paystack gateways** | Superseded — **Lipila is primary** (Zambia-native, all 3 MoMo operators, Flutter SDK, self-onboard sandbox); Paystack has no Zambia coverage. International rails deferred | No (Lipila scaffold merged; keys unblock) |
| ~~Flutter Mobile App~~ | ✅ Substantially BUILT: auth/dashboard/send/history/cards/KYC/notifications/admin screens, dark mode (system-following), biometric unlock, offline banner — 214 tests green | Ship-blocking polish: FCM push only |
| **Bills & Airtime + Merchant Checkout** | Phase 11.5 — converts app to daily-use "pay for life" | No (post-rails) |
| **Digital Goods Store** | Phase 12 — pooled-card model for game/streaming purchases by MoMo-only youth | No (post-revenue) |
| **Chilemba/Group Savings** | Differentiator feature, not core wallet functionality | No |
| **School Fee Payments** | Folded into Phase 11.5 bills catalog | No |
| **Split Bills / Shared Expenses** | Nice-to-have for students | No |
| **Virtual Cards (per-user issuing)** | Deferred; Phase 12 pooled-card model serves the need at ~$5 total instead of $5/user/month | No |
| **Admin Dashboard UI** | Admin API exists, curl sufficient for launch | No |
| **AI Features** | Roadmapped in docs/AI-STRATEGY.md (insights → fraud → assistant) | No |

### MVP Completion Checklist

- [x] User registers with email + phone, gets JWT
- [x] User sets transaction PIN (4-6 digits)
- [x] User gets a wallet with unique account number
- [x] User deposits money (mock ledger; live via Lipila scaffold once keys set)
- [x] User sends money to another user (atomic, with fee)
- [x] User withdraws money (mock ledger; payout rails scaffolded)
- [x] User views transaction history with filters
- [x] Duplicate requests blocked by idempotency (per-user scoped)
- [x] Every balance change is audit-logged
- [x] Rate limiting + hourly velocity limits prevent abuse
- [x] 144 backend tests passing · 214 Flutter tests passing · PG parity verified
- [x] Docker + CI/CD ready for deployment

---

## 2. Scope

NovaWallet is a wallet platform: a RESTful Spring Boot API consumed by a Flutter mobile client over HTTP returning JSON. The system handles:

- User registration and authentication (JWT + rotating refresh tokens, biometric unlock on client)
- Wallet creation and balance management
- Deposit, withdrawal, transfer, and payout operations with velocity controls
- Transaction history with filtering and pagination
- Fee calculation (configurable per transaction type, admin CRUD)
- Per-user idempotency (duplicate charge prevention)
- Payment rails via the Lipila gateway abstraction (MoMo collections/disbursements, card collections) with idempotent webhook processing
- Scheduled transaction settlement and cleanup
- Audit logging of all balance changes
- Caching for frequently read data
- KYC identity verification (multi-tier with daily/balance caps)

Virtual card issuing per-user is superseded by the Phase 12 pooled-card digital-goods model (see PROJECT-PLAN).

---

## 3. Functional Requirements

### 3.1 User System

| ID | Requirement | Priority |
|----|------------|----------|
| U1 | Users register with first name, last name, email, password, and phone number | High |
| U2 | Passwords stored as BCrypt hash, never in plain text | High |
| U3 | Users log in with email + password, receive a JWT | High |
| U4 | JWT expires after configurable duration (default 1 hour) | High |
| U5 | Users can set a 4-6 digit transaction PIN for financial operations | High |
| U6 | PIN hashed with BCrypt, rate-limited to 3 failed attempts per 15 minutes | High |
| U7 | Users have roles: USER (default) and ADMIN | Medium |
| U8 | Optional biometric unlock (device fingerprint/Face ID via local_auth): flag default OFF, capability-gated toggle, lock screen with retry + password fallback; mandatory PIN-setup flow outranks biometric gate | Medium |
| U9 | Outgoing money operations require a verified email address (403 until verified) | High |

### 3.2 Wallet System

| ID | Requirement | Priority |
|----|------------|----------|
| W1 | Each user gets one wallet, created after KYC approval (not on registration) | High |
| W2 | Wallet has a unique auto-generated account number (prefix "NW" + 10 digits) | High |
| W3 | Wallet tracks balance as BigDecimal (never double/float) | High |
| W4 | Wallet can be ACTIVE or FROZEN | Medium |
| W5 | Negative balances are prohibited at the database and application level | High |
| W6 | Wallet balance can only change through deposit, withdraw, or transfer operations | High |
| W7 | Wallet has a maximum balance cap determined by the user's KYC tier | High |

### 3.3 Transaction System

| ID | Requirement | Priority |
|----|------------|----------|
| T1 | Deposit: adds money to a wallet, creates a DEPOSIT transaction record | High |
| T2 | Withdrawal: validates PIN, checks sufficient balance, deducts money, creates WITHDRAWAL record | High |
| T3 | Transfer: validates PIN, prevents self-transfer, checks balance (amount + fee), deducts sender, credits receiver, creates DEBIT/CREDIT/FEE transaction records | High |
| T4 | All money operations are atomic — partial failure rolls back entirely (@Transactional) | High |
| T5 | Transaction records include: id, type, amount, description, status, timestamp, sender wallet, receiver wallet | High |
| T6 | Transactions can be PENDING, SUCCESSFUL, or FAILED | Medium |
| T7 | Gateway deposits (Lipila collections) are credited only after webhook confirmation with a valid shared secret; duplicate callbacks are ignored idempotently via `[GATEWAY:<referenceId>]` ledger markers | High |
| T8 | Hourly velocity limits on outgoing operations: max transactions/hour and max amount/hour (configurable via `app.transaction.velocity`), enforced before PIN verification, returning 429 RATE_LIMITED when breached | High |
| T9 | Payouts to mobile money follow the same guardrail stack as withdrawals (PIN, velocity, daily KYC limit, email gate); provider rejection rolls back the local debit entirely and returns 400; gateway disabled → 503 SERVICE_UNAVAILABLE | Medium |

### 3.4 Fee Engine

| ID | Requirement | Priority |
|----|------------|----------|
| F1 | Configurable percentage fee per transaction type | Medium |
| F2 | Configurable flat fee per transaction type | Medium |
| F3 | Total fee = percentage fee + flat fee | Medium |
| F4 | Fees rounded to 2 decimal places (ZMW ngwee precision) | Medium |
| F5 | Fee deducted from sender's wallet along with transfer amount | Medium |
| F6 | Fee estimation endpoint (GET, no side effects) | Low |
| F7 | Flutterwave processing fees tracked separately from platform fees | Medium |

### 3.5 Idempotency

| ID | Requirement | Priority |
|----|------------|----------|
| I1 | Deposit, withdrawal, transfer, and payout endpoints accept an Idempotency-Key header | High |
| I2 | If a request with an existing key is received, return the cached response. Keys are **scoped per authenticated user** (stored as `"<userId>:<key>"`); anonymous endpoints keep global scope | High |
| I3 | Idempotency records expire after 24 hours | Medium |
| I4 | Expired records cleaned up by a scheduled job | Low |
| I5 | Gateway webhooks deduplicated via deterministic ledger markers (`[LIPILA:<referenceId>]`); losing concurrent requests replay the cached response after bounded exponential-backoff polling, else 409 IDEMPOTENCY_IN_FLIGHT | High |

### 3.6 Scheduled Jobs

| ID | Requirement | Priority |
|----|------------|----------|
| S1 | Daily at 3:00 AM: mark PENDING transactions older than 24 hours as FAILED | Medium |
| S2 | Daily at 4:00 AM: delete idempotency records older than 24 hours | Low |
| S3 | Jobs must be idempotent (running twice produces same result) | Medium |
| S4 | Provider status polling for PENDING gateway transactions (reconciliation sweep) | Medium — Phase 11.5.6 |

### 3.11 Client Application (Flutter)

| ID | Requirement | Priority |
|----|------------|----------|
| C1 | Material 3 client with three-layer design tokens (primitive→semantic→component); dark mode follows the device system setting (`ThemeMode.system`), light mode unchanged | High |
| C2 | Dark and light text contrast ≥ 4.5:1, enforced by automated contract tests | Medium |
| C3 | Session tokens stored in device secure storage; single-flight refresh-token rotation on 401 with one retry | High |
| C4 | Money mutations send per-user idempotency keys; operation key rotates on non-ambiguous failures | High |
| C5 | API base URL resolves: `--dart-define=API_BASE_URL` override → Android emulator `10.0.2.2` → localhost; no machine-specific addresses committed | High |
| C6 | Offline connectivity is surfaced via a persistent banner; skeleton loaders on async content | Medium |
| C7 | Biometric unlock per U8; PIN pad flows for financial confirmation with operation-key rotation on retryable failures | High |
| S5 | Daily at 2:00 AM: retry failed KYC verification callbacks | Low |

### 3.7 Audit Logging

| ID | Requirement | Priority |
|----|------------|----------|
| A1 | Every balance change is recorded in an append-only audit log | High |
| A2 | Audit log entries contain: entity type, entity ID, action, old value, new value, performed by, IP address, timestamp | High |
| A3 | No delete or update operations on audit logs | High |
| A4 | Audit log writes are asynchronous (non-blocking) | Medium |
| A5 | KYC document access and verification actions are audit-logged | High |
| A6 | Virtual card creation, freeze, unfreeze, and termination are audit-logged | Medium |

### 3.8 Rate Limiting

| ID | Requirement | Priority |
|----|-------------|----------|
| R1 | API requests are rate-limited per user/IP using the token-bucket algorithm | High |
| R2 | Configurable rate limits: default 100 requests per minute per user | Medium |
| R3 | Rate limit headers (X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset) returned on every response | Medium |
| R4 | Exceeding the rate limit returns 429 Too Many Requests with a Retry-After header | High |
| R5 | Rate limits are enforced via a filter before request reaches controllers | High |
| R6 | Auth endpoints have stricter limits (e.g., 10 requests per minute) to prevent brute-force attacks | High |

### 3.9 Refresh Token Authentication

| ID | Requirement | Priority |
|----|-------------|----------|
| RT1 | Login returns both an access token (short-lived, 15 minutes) and a refresh token (long-lived, 7 days) | High |
| RT2 | Access tokens are JWTs with short expiry | High |
| RT3 | Refresh tokens are stored server-side as hashed values in the database | High |
| RT4 | POST /api/v1/auth/refresh accepts a refresh token and returns a new access token + rotated refresh token | High |
| RT5 | Refresh tokens are single-use — a new refresh token is issued on every refresh | High |
| RT6 | If a refresh token is reused (stolen token detected), all refresh tokens for that user are invalidated | High |
| RT7 | Refresh tokens expire after 7 days of inactivity | Medium |

### 3.10 Notification Service

| ID | Requirement | Priority |
|----|-------------|----------|
| N11 | Transaction notifications are sent via email and SMS | Medium |
| N12 | Email notifications sent on deposit, withdrawal, and transfer completion | Medium |
| N13 | SMS notifications via Africa's Talking API for transaction alerts | Medium |
| N14 | Notification delivery is asynchronous — does not block the API response | Medium |
| N15 | Failed notifications are retried up to 3 times, then logged | Low |
| N16 | KYC approval/rejection notifications sent to user | Medium |
| N17 | Virtual card transaction alerts sent via push notification | Low |

### Layer Architecture

- **Controller Layer**: REST endpoints, request validation, response formatting
- **Service Layer**: Business logic, transaction management, authorization
- **Repository Layer**: Database access via Spring Data JPA
- **Entity Layer**: JPA entities with Flyway migrations

## 8. Future Scope

Sequenced per docs/PROJECT-PLAN.md:

- **Phase 11 (go-live)**: Lipila credentials → live MoMo collections/payouts; reconciliation job for PENDING payouts
- **Phase 11.5**: bills & airtime catalog, merchant checkout API + onboarding, hosted payment page
- **Phase 12**: digital-goods storefront fulfilled via pooled good-BIN card (Netflix/game currency for MoMo-only users)
- **Later**: NovaWallet Gateway merchant product · QR present-to-pay · recurring payments/standing orders · multi-currency wallets (USD) · Kubernetes scaling · FCM push notifications (10.7) · AI roadmap per docs/AI-STRATEGY.md

---

## GitHub Repo Description

> **NovaWallet** — A Zambia-first digital wallet: Spring Boot API + Flutter app. Banking-grade ledger (pessimistic locking, audit trail), per-user idempotency, velocity limits, KYC tiers, biometric unlock, dark mode, and Lipila mobile-money rails (MTN/Airtel/Zamtel) behind a provider-agnostic gateway abstraction.

**Topics**: `flutter` `spring-boot` `fintech` `digital-wallet` `mobile-app` `dart` `java` `jwt-authentication` `postgresql` `flyway` `rest-api` `zambia` `lipila` `mobile-money`
