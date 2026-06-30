# NovaWallet — Software Requirements Specification

## 1. Purpose

NovaWallet is a digital wallet backend API that manages user accounts, wallet balances, and financial transactions. It simulates real banking backend logic with proper security, validation, and audit trails. Later phases integrate with real payment providers (Flutterwave, Paystack, Stripe) for live mobile money and card payments.

**Target users**: University students, small business owners, and peer-to-peer users in Zambia requiring shared expense management, school fee payments, and person-to-person transfers.

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
| W1 | Each user gets one wallet on registration | High |
| W2 | Wallet has a unique auto-generated account number (prefix "NW" + 10 digits) | High |
| W3 | Wallet tracks balance as BigDecimal (never double/float) | High |
| W4 | Wallet can be ACTIVE or FROZEN | Medium |
| W5 | Negative balances are prohibited at the database and application level | High |
| W6 | Wallet balance can only change through deposit, withdraw, or transfer operations | High |

### 3.3 Transaction System

| ID | Requirement | Priority |
|----|------------|----------|
| T1 | Deposit: adds money to a wallet, creates a DEPOSIT transaction record | High |
| T2 | Withdrawal: validates PIN, checks sufficient balance, deducts money, creates WITHDRAWAL record | High |
| T3 | Transfer: validates PIN, prevents self-transfer, checks balance (amount + fee), deducts sender, credits receiver, creates DEBIT/CREDIT/FEE transaction records | High |
| T4 | All money operations are atomic — partial failure rolls back entirely (@Transactional) | High |
| T5 | Transaction records include: id, type, amount, description, status, timestamp, sender wallet, receiver wallet | High |
| T6 | Transactions can be PENDING, SUCCESSFUL, or FAILED | Medium |

### 3.4 Fee Engine

| ID | Requirement | Priority |
|----|------------|----------|
| F1 | Configurable percentage fee per transaction type | Medium |
| F2 | Configurable flat fee per transaction type | Medium |
| F3 | Total fee = percentage fee + flat fee | Medium |
| F4 | Fees rounded to 2 decimal places (ZMW ngwee precision) | Medium |
| F5 | Fee deducted from sender's wallet along with transfer amount | Medium |
| F6 | Fee estimation endpoint (GET, no side effects) | Low |

### 3.5 Idempotency

| ID | Requirement | Priority |
|----|------------|----------|
| I1 | Deposit, withdrawal, and transfer endpoints accept an Idempotency-Key header | High |
| I2 | If a request with an existing key is received, return the cached response | High |
| I3 | Idempotency records expire after 24 hours | Medium |
| I4 | Expired records cleaned up by a scheduled job | Low |

### 3.6 Scheduled Jobs

| ID | Requirement | Priority |
|----|------------|----------|
| S1 | Daily at 3:00 AM: mark PENDING transactions older than 24 hours as FAILED | Medium |
| S2 | Daily at 4:00 AM: delete idempotency records older than 24 hours | Low |
| S3 | Jobs must be idempotent (running twice produces same result) | Medium |

### 3.7 Audit Logging

| ID | Requirement | Priority |
|----|------------|----------|
| A1 | Every balance change is recorded in an append-only audit log | High |
| A2 | Audit log entries contain: entity type, entity ID, action, old value, new value, performed by, IP address, timestamp | High |
| A3 | No delete or update operations on audit logs | High |
| A4 | Audit log writes are asynchronous (non-blocking) | Medium |

## 4. Non-Functional Requirements

| ID | Requirement | Priority |
|----|------------|----------|
| N1 | API responses use consistent JSON format with proper HTTP status codes | High |
| N2 | All error responses follow a uniform structure: status, code, message, timestamp, path | High |
| N3 | Passwords and PINs never appear in logs, responses, or stack traces | High |
| N4 | Input validation rejects malformed requests with 400 Bad Request | High |
| N5 | Endpoints protected by JWT authentication (except auth endpoints) | High |
| N6 | API documented via OpenAPI/Swagger at /swagger-ui.html | Medium |
| N7 | Application logs include request ID (MDC) for request tracing | Medium |
| N8 | Health check endpoint for monitoring (/actuator/health) | Medium |
| N9 | Database schema managed via versioned Flyway migrations | High |
| N10 | Unit and integration tests cover core business logic | High |

## 5. Architecture

- **Language**: Java 17
- **Framework**: Spring Boot 3+
- **Build**: Maven
- **Database**: PostgreSQL (production), H2 (development/test)
- **Migrations**: Flyway
- **Auth**: JWT (jjwt library)
- **Caching**: Spring Cache abstraction (in-memory)
- **API Docs**: SpringDoc OpenAPI

### Layer Architecture

```
Controller (HTTP) → Service (Business Logic) → Repository (Data Access)
     ↓                    ↓                        ↓
  DTOs            Entities + DTOs             JPA/Hibernate
```

## 6. Constraints

- Money values use BigDecimal with scale 2 (rounding mode HALF_EVEN)
- No negative balance in wallets at any time
- All balance changes happen inside @Transactional boundaries
- Entities never exposed directly to API consumers (DTOs used instead)
- Idempotency keys guarantee at-most-once processing for financial operations

## 7. Future Scope (Post-MVP)

- Flutterwave/Paystack integration for real mobile money deposits
- Webhook handling for payment status updates
- Email/SMS notification service for transaction alerts
- Refresh token rotation for enhanced JWT security
- Rate limiting with token bucket algorithm
- Kubernetes deployment with horizontal scaling

---

## GitHub Repo Description

> **NovaWallet** — A full-stack digital wallet mobile app: Flutter frontend + Spring Boot backend. Users register, manage wallet balances, send/receive money, and view transaction history with JWT security and transaction PIN. The backend is a production-grade REST API handling deposits, withdrawals, peer-to-peer transfers, configurable fees, idempotency, audit logging, and scheduled job processing. Designed for students and small businesses in Zambia. Later connects to Flutterwave and Paystack for live mobile money payments.

**Topics**: `flutter` `spring-boot` `fintech` `digital-wallet` `mobile-app` `dart` `java` `jwt-authentication` `postgresql` `flyway` `rest-api` `zambia` `flutterwave`
