# NovaWallet — Software Requirements Specification

## 1. Purpose

NovaWallet is a digital wallet backend API that manages user accounts, wallet balances, financial transactions, identity verification (KYC), and virtual card issuing. It simulates real banking backend logic with proper security, validation, and audit trails. Phases integrate with Flutterwave for live mobile money deposits, withdrawals, and card payments.

**Target users**: University students, small business owners, and peer-to-peer users in Zambia requiring shared expense management, school fee payments, and person-to-person transfers.

---

## MVP (Minimum Viable Product)

**Core loop**: User registers → gets a wallet → deposits money → sends to another user → withdraws → sees history.

**Status: MVP is ✅ COMPLETE.** Phases 0–9 built and tested (123 tests passing). Remaining work (Flutter app, Flutterwave) enhances the MVP but is not required for it to function as a wallet backend.

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
| **Flutter Mobile App** | API works without it. Users can interact via curl/any HTTP client | No |
| **Flutterwave Payment Gateway** | Deposit/withdraw are mock without it. Needed for real money movement | **Yes — blocks real use** |
| **Chilemba/Group Savings** | Differentiator feature, not core wallet functionality | No |
| **School Fee Payments** | Requires onboarding real schools — operational overhead | No |
| **Split Bills / Shared Expenses** | Nice-to-have for students | No |
| **Virtual Cards** | Speculative feature, requires card network partnership | No |
| **Admin Dashboard UI** | Admin API exists, curl is sufficient for launch | No |
| **AI Features** | Over-engineering for MVP | No |

### MVP Completion Checklist

- [x] User registers with email + phone, gets JWT
- [x] User sets transaction PIN (4-6 digits)
- [x] User gets a wallet with unique account number
- [x] User deposits money (mock)
- [x] User sends money to another user (atomic, with fee)
- [x] User withdraws money (mock)
- [x] User views transaction history with filters
- [x] Duplicate requests blocked by idempotency
- [x] Every balance change is audit-logged
- [x] Rate limiting prevents brute-force attacks
- [x] 123 tests passing
- [x] Docker + CI/CD ready for deployment

---

## 2. Scope

NovaWallet is a RESTful API. There is no frontend. All interactions happen through HTTP endpoints returning JSON. The system handles:

- User registration and authentication
- Wallet creation and balance management
- Deposit, withdrawal, and transfer operations
- Transaction history with filtering and pagination
- Fee calculation (configurable per transaction type)
- Idempotency (duplicate charge prevention)
- Scheduled transaction settlement and cleanup
- Audit logging of all balance changes
- Caching for frequently read data
- KYC identity verification (multi-tier)
- Virtual card issuing and management
- Flutterwave payment gateway integration (mobile money, card, transfers)

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
| T7 | Flutterwave deposits are created as PENDING and marked SUCCESSFUL only after webhook confirmation | High |

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
| I1 | Deposit, withdrawal, and transfer endpoints accept an Idempotency-Key header | High |
| I2 | If a request with an existing key is received, return the cached response | High |
| I3 | Idempotency records expire after 24 hours | Medium |
| I4 | Expired records cleaned up by a scheduled job | Low |
| I5 | Flutterwave webhooks deduplicated via `flw-ref` header | High |

### 3.6 Scheduled Jobs

| ID | Requirement | Priority |
|----|------------|----------|
| S1 | Daily at 3:00 AM: mark PENDING transactions older than 24 hours as FAILED | Medium |
| S2 | Daily at 4:00 AM: delete idempotency records older than 24 hours | Low |
| S3 | Jobs must be idempotent (running twice produces same result) | Medium |
| S4 | Every 15 minutes: poll Flutterwave for pending transfer status updates | Medium |
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

- Paystack integration (alternative payment gateway)
- QR code payments
- Recurring payments / standing orders
- Kubernetes deployment with horizontal scaling
- Push notifications to mobile app
- Business/merchant accounts with sub-wallets
- Multi-currency wallet support (USD, EUR, GBP)

---

## GitHub Repo Description

> **NovaWallet** — A production-grade digital wallet backend API built with Spring Boot. Supports user registration, wallet management, peer-to-peer transfers, deposits, withdrawals, configurable fees, idempotency, audit logging, and Flutterwave integration.

**Topics**: `flutter` `spring-boot` `fintech` `digital-wallet` `mobile-app` `dart` `java` `jwt-authentication` `postgresql` `flyway` `rest-api` `zambia` `flutterwave`
