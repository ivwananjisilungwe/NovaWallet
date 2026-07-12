# NovaWallet API

A production-grade digital wallet backend API built with Spring Boot, enabling secure financial transactions, user management, and mobile money-style operations for university students and small businesses in Zambia.

**Status**: Phases 0–6 ✅ Complete | **112 tests passing** | [Phase Progress](#phase-progress) below

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.5.3 |
| **Database** | PostgreSQL 16 (H2 for tests) |
| **Migrations** | Flyway (V1–V10) |
| **Security** | Spring Security + JWT (jjwt 0.12.x) with refresh token rotation |
| **API Docs** | SpringDoc OpenAPI (Swagger UI) |
| **Cache** | Caffeine (wallet balances, 10-min write expiry) |
| **Async** | Spring `@Async` (email, SMS notifications) |
| **Scheduling** | Spring `@Scheduled` (5 cron jobs) |
| **Build** | Maven |

---

## Features

### Phase 1 — User & Authentication ✅
- ✅ User registration & JWT authentication (access + refresh token rotation)
- ✅ Secure PIN management with BCrypt + 3-attempt lockout (15-min cooldown)
- ✅ Email verification (token-based)
- ✅ Password reset flow
- ✅ Digital wallet creation with auto-generated account numbers (NW prefix)
- ✅ User profile management (get/update)
- ✅ Admin user management (list, get, soft-delete/restore)
- ✅ MDC request tracing (X-Request-ID)
- ✅ Comprehensive error handling with uniform JSON responses (7 exception types)
- ✅ Swagger/OpenAPI documentation

### Phase 2 — Transactions ✅
- ✅ Deposit funds with atomic balance update
- ✅ Withdraw funds with fee deduction
- ✅ Peer-to-peer transfers between wallets
- ✅ PIN validation before all financial operations
- ✅ DB-level negative balance guard (`WHERE balance + :amount >= 0`)
- ✅ Pessimistic write locking (`SELECT ... FOR UPDATE`) prevents race conditions
- ✅ Consistent lock ordering prevents deadlocks
- ✅ `@Transactional` ensures atomicity (crash between debit/credit = rollback)

### Phase 3 — History & Querying ✅
- ✅ Paginated transaction history with filtering (type, date range, status)
- ✅ Single transaction lookup by reference
- ✅ Wallet balance query
- ✅ Ownership verification (users can only see their own transactions)

### Phase 4 — Fee Engine & Audit Logging ✅
- ✅ Configurable fee engine (percentage and/or flat amounts)
- ✅ Min/max clamping on fee amounts
- ✅ `@Async` audit service with `REQUIRES_NEW` propagation (won't roll back with main txn)
- ✅ `@Audited` annotation + AOP aspect for declarative auditing
- ✅ Fee deduction on withdrawals and transfers

### Phase 5 — Idempotency ✅
- ✅ `Idempotency-Key` header support on all mutating endpoints
- ✅ Atomic INSERT-first pattern (DB unique constraint prevents concurrent duplicates)
- ✅ Poll-and-replay: duplicate requests replay cached response byte-for-byte
- ✅ 409 Conflict if the initial request is still processing
- ✅ Non-2xx responses release the lock (client can retry)
- ✅ Expired records cleaned up at 4 AM daily
- ✅ Same pattern used by Stripe, PayPal, and major payments APIs

### Phase 5.5 — Fintech Hardening ✅
- ✅ Pessimistic write locking on all wallet balance operations
- ✅ KYC tier limit enforcement (daily send limit, wallet balance cap)
- ✅ Unified error response format across all filters and handlers
- ✅ CORS configuration for frontend access
- ✅ Admin pagination on user list
- ✅ Admin actions logged to audit trail (freeze, unfreeze, soft-delete)
- ✅ `@Version` column incremented in native SQL updates (optimistic locking defense)

### Phase 6 — Notifications, Scheduling & Caching ✅
- ✅ Email + SMS notifications for all financial events (async)
- ✅ Notification retry logic (3 retries with 15-min interval)
- ✅ Africa's Talking SMS stub (logs to console when no API key)
- ✅ Transaction notifications: deposit, withdraw, transfer sent/received
- ✅ KYC approval/rejection notifications
- ✅ Wallet status change notifications
- ✅ Caffeine cache for wallet balances (evicted on every mutation)
- ✅ 5 scheduled maintenance jobs (midnight → 4:30 AM window)

---

## Project Structure

```
novawallet-api/
├── src/main/java/com/novawallet/novawallet_api/
│   ├── admin/                  # Admin user/wallet management
│   ├── audit/                  # Audit logging (entity, service, aspect)
│   ├── auth/                   # Authentication, PIN, email verification
│   ├── common/                 # Shared DTOs (ApiResponse, PagedResponse)
│   ├── config/                 # Security, OpenAPI, async, caching, audit
│   ├── exception/              # Global error handling (7+ exception types)
│   ├── fee/                    # Fee engine (entity, repository, service, config)
│   ├── filter/                 # Request tracing, rate limiting, idempotency
│   ├── idempotency/            # Idempotency (entity, service, filter, schedule)
│   ├── kyc/                    # KYC (entity, service, controller, document storage)
│   ├── notification/           # Email/SMS (entity, service, schedule)
│   ├── security/               # JWT filter, JWT util, custom user details
│   ├── token/                  # Refresh token (entity, service, schedule)
│   ├── transaction/            # Transaction (entity, service, controller, schedule)
│   ├── user/                   # User management (controller, service, entity)
│   └── wallet/                 # Wallet management (controller, service, entity)
├── src/main/resources/
│   ├── application.yml         # Base configuration
│   ├── application-dev.yml     # Development profile (PostgreSQL)
│   ├── application-prod.yml    # Production profile
│   └── db/migration/           # Flyway SQL migrations (V1–V10)
├── src/test/java/
│   ├── .../auth/               # Auth unit & integration tests (16)
│   ├── .../config/             # Test configuration
│   ├── .../endpoint/           # End-to-end endpoint tests (6 scenarios)
│   ├── .../notification/       # Notification service tests (7)
│   ├── .../transaction/        # Transaction tests
│   └── .../fee/                # Fee engine tests
└── docs/
    ├── phase-5-idempotency.md  # Instructor doc: why idempotency matters
    ├── phase-6-notifications.md# Instructor doc: async notifications
    └── ...                     # Instructor guides for each phase
```

---

## Quick Start

### Prerequisites

- Java 17+
- PostgreSQL 16+
- Maven

### Setup

```bash
# Clone the repo
git clone https://github.com/ivwananjisilungwe/NovaWallet.git
cd NovaWallet/novawallet-api

# Create the database
createdb -U postgres novawallet

# Run with dev profile (Flyway migrates automatically)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Run Tests

```bash
mvn test
# Expected: 112 tests, 0 failures, 0 errors
```

### Verify it's running

```bash
curl http://localhost:8080/api/actuator/health
# {"status":"UP"}

curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"Str0ng!Pass","fullName":"Alice Banda","phone":"+260970000001"}'

# Full flow: register → verify email → login → deposit → transfer → withdraw
```

---

## API Documentation

Once running, visit:
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **API Docs**: http://localhost:8080/api/api-docs

### Idempotency

All mutating endpoints accept an `Idempotency-Key` header. Send the same key twice → get the same response twice (only one transaction executed).

```bash
curl -X POST http://localhost:8080/api/v1/wallets/{walletId}/deposit \
  -H "Authorization: Bearer <token>" \
  -H "Idempotency-Key: unique-key-123" \
  -H "Content-Type: application/json" \
  -d '{"amount": 50.00}'
```

---

## Phase Progress

| Phase | Description | Status |
|---|---|---|
| **0** | Project Foundation (Spring Boot, Flyway, security, error handling, Swagger) | ✅ Complete |
| **1** | User & Auth (register, login, JWT, PIN, email verify, password reset, wallet, admin) | ✅ Complete |
| **2** | Transactions (deposit, withdraw, transfer with pessimistic locking + atomic balance) | ✅ Complete |
| **3** | History & Querying (paginated history, filtering, single lookup, balance) | ✅ Complete |
| **4** | Fee Engine & Audit (percentage+flat fees, min/max clamping, async audit trail) | ✅ Complete |
| **5** | Idempotency (Idempotency-Key header, INSERT-first pattern, poll-and-replay) | ✅ Complete |
| **5.5** | Fintech Hardening (pessimistic locking, KYC enforcement, CORS, pagination, audit) | ✅ Complete |
| **6** | Notifications, Scheduling & Caching (email+SMS, 5 cron jobs, Caffeine cache) | ✅ Complete |
| **7** | Admin Endpoints (transaction search, audit log view, fee CRUD) | ⏳ Partial |
| **8** | Testing & QA (edge case coverage, concurrency tests) | ⏳ Partial |
| **9** | Docker & CI/CD (Dockerfile, docker-compose, GitHub Actions) | ❌ Pending |
| **10** | Flutter Mobile App | ❌ Pending |
| **11** | Flutterwave Payment Gateway | ❌ Pending |

---

## Key Architecture Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Balance integrity | `UPDATE ... WHERE balance + amount >= 0` | DB-level negative guard — no race condition possible |
| Concurrency | `@Lock(PESSIMISTIC_WRITE)` | Prevents phantom reads on wallet balance |
| Transfer atomicity | Class-level `@Transactional` | Debit + credit in one DB transaction — crash = rollback |
| Money precision | `BigDecimal` scale=2, `HALF_EVEN` | Fintech standard, avoids floating-point errors |
| Idempotency | INSERT-first, unique constraint | Atomic acquire without distributed locks |
| Audit logging | `@Async` + `REQUIRES_NEW` | Won't roll back if main transaction fails |
| Notifications | `@Async` thread pool | API response doesn't wait for email/SMS |
| Password storage | BCrypt via Spring Security | Industry standard for credential hashing |
| Rate limiting | Caffeine + token bucket | In-memory, per-endpoint quotas |

---

## Author

**Ivwananji Silungwe**

---

## License

This project is for educational and development purposes.
