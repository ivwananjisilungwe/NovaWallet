# Architecture Overview

## High‑Level Diagram
```
[Flutter Mobile App]  <--->  REST API (Spring Boot)  <--->  PostgreSQL Database
```
- **Flutter Mobile App** (Dart 3.12.2) communicates with the backend via HTTPS JSON requests.
- **Backend** exposes a set of REST endpoints under `/api/**` handling authentication, wallet operations, KYC, virtual cards, and admin tasks.
- **Security**: JWT tokens issued on login, validated on each request by `JwtAuthFilter`. Rate‑limiting via `LoginRateLimiter` and `RateLimitFilter`.
- **Data Layer**: JPA entities map to PostgreSQL tables, managed by Flyway migrations.
- **Audit & Idempotency**: AOP‑based audit logs (`@Audited`) record critical actions. Idempotency keys ensure safe retries for payment‑related requests.
- **Async Processing**: Scheduled jobs (e.g., `BalanceRecalculationJob`, `KycExpiryJob`) run via Spring’s `@Scheduled`.
- **Admin Panel**: Separate web UI (outside this repo) interacts with admin endpoints for confirming deposits/withdrawals.

## Component Interaction Flow (Deposit Example)
1. Mobile app calls `POST /api/payments/mobile-money/initiate`.
2. Backend creates a `MobileMoneyRequest` (status *pending*).
3. Admin confirms via admin panel → `PUT /api/admin/deposits/{id}/confirm`.
4. Backend creates a `LedgerEntry` (+deposit) and a `Transaction` record, updates wallet balance.
5. Notification service pushes a push/email notification to the user.

## Future Extensions (Phase 2+)
- **Mobile Money Providers**: Integration with real Airtel/Mtn APIs.
- **Payment Gateway**: Flutterwave for card top‑up and withdrawals.
- **Card Issuer**: Union54 integration for real virtual cards.
- **Analytics & Fraud**: Real‑time fraud detection, usage analytics.
