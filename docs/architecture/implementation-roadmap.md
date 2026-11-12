# NovaWallet — Implementation Roadmap (Phased Migration Programme)

**Document:** 5 of the NovaWallet Architecture Series
**Scope:** Complete actionable migration programme, Phase 0 through Phase 9.
**Mandatory progression:** Financial integrity → Modular monolith → Event-driven architecture → Independent microservices.
**Baseline:** Spring Boot 3.5.3, Java 17, PostgreSQL 16, Flyway V1–V11, 130/130 backend tests on H2.

---

## Phase 0 — Architecture Audit (completed baseline & risk register)

**Objectives**
- Establish a verified baseline of the codebase: package layout, schema ownership (V1–V11), cross-domain repository injection map, misplaced schedulers, and the financial-integrity / security risk register (FIN-01…FIN-07, SEC-01…SEC-12).
- Produce the readiness matrix and extraction order (Document 4).

**Dependencies & Preconditions**
- None (entry point).

**Concrete Technical Deliverables**
- Baseline inventory of 179 Java sources under `novawallet-api/src/main/java/com/novawallet/novawallet_api/`.
- Risk register referencing exact files: `transaction/service/TransactionService.java` (deposit/withdraw/transfer), `payment/service/PayoutService.java`, `payment/service/WebhookService.java`, `security/JwtAuthFilter.java`, `security/CustomUserDetailsService.java`, `user/entity/User.java`, `notification/schedule/BalanceRecalculationJob.java`, `notification/schedule/KycExpiryJob.java`, `kyc/service/FileStorageService.java`, `application.yml` (profile default line 6).
- Database ownership map from migrations V1–V11 (tables: `users`, `wallets`, `refresh_tokens`, `kyc_documents`, `transactions`, `fee_configurations`, `audit_logs`, `idempotency_keys`, `notifications`, `notification_attempts`).

**Verification & Acceptance Criteria**
- `./mvnw test` green (130/130) on H2 before any Phase 1 change.
- Document 4 (`microservices-readiness.md`) and this document reviewed and approved.

---

## Phase 1 — Financial Integrity and Security Remediation

**Objectives**
- Close the financial-integrity gaps FIN-01…FIN-07 and security gaps SEC-01…SEC-12 without changing the public API contract.
- Make money movement safe before any ledger or microservice work.

**Dependencies & Preconditions**
- Phase 0 complete. No schema changes required beyond possibly additive columns (none mandated here).

**Concrete Technical Deliverables**
- FIN-01: Gate/disable unverified deposit path — `TransactionService.deposit(...)` (line 90) must not credit funds for users whose email is unverified (align with H4 email gate already enforced in `withdraw`/`transfer`).
- FIN-02: Decouple payout DB transaction from HTTP call — refactor `PayoutService.payout(...)` (line 107) so the gateway call is not inside the same `@Transactional` boundary; persist a PENDING intent first, settle asynchronously (precursor to Phase 3).
- FIN-03: Fix `PendingTransactionCleanupJob.cleanupStalePendingTransactions()` (line 40) — ensure refund/reconciliation of any reserved funds before marking stale PENDING rows FAILED.
- FIN-04: Deterministic UUID lock ordering in `TransactionService.transfer(...)` (lines 227–260) — lock wallets in UUID order (not sender-then-receiver) to eliminate deadlocks; replace `walletRepository.findByUserId(...)` (line 229) initial lookup with a locked read.
- FIN-05: Fix `BalanceRecalculationJob.recalculateMissingBalances()` (line 28) — make it a true reconciliation (not just NULL backfill via `TransactionRepository.recalculateMissingBalances()`, lines 83–96) and log discrepancies.
- FIN-06: Enforce `UserDetails` active/non-deleted status in `JwtAuthFilter.doFilterInternal(...)` (line 37) — reject tokens for deleted/deactivated users (today only `!user.isDeleted()` is set as `accountNonLocked` in `CustomUserDetailsService.loadUserById`, line 51).
- FIN-07: Add `@DecimalMax` bounds on amount DTOs (`DepositRequest`, `WithdrawRequest`, `TransferRequest.amount`, `PayoutRequest.amount`) and token expiry enforcement in `JwtUtil`/`TokenService`.
- SEC-01…SEC-04: Revoke refresh tokens on password change & admin deactivation — `AuthService`/`TokenService` (line 31 repo) plus `AdminService` deactivation path.
- SEC-05: Stop leaking reset/verification tokens in notifications/logs — `NotificationService`, `MailService`, controllers.
- SEC-06: Remove hardcoded `dev` profile default — `application.yml` line 6 (`active: dev`) → require explicit profile; fail fast in prod.
- SEC-07: Add logout endpoint — `AuthController` + `TokenService` revocation.
- SEC-08…SEC-12: Token expiry enforcement, DTO bounds, in-memory rate-limiter hardening notes, proxy-aware remote-addr keying (documented; full Redis-backed in Phase 8).

**Verification & Acceptance Criteria**
- All existing 130 tests still pass; new tests for FIN-01..FIN-07 and SEC-01..SEC-12.
- Manual verification: unverified user cannot deposit; payout timeout does not silently roll back a provider-accepted payment; transfer deadlock test under concurrent load; deleted user's JWT rejected within one request.

---


## Phase 1A — Financial Guardrails (Completed and Verified)

**Status:** Complete / verified. This is a scoped financial-guardrail slice of Phase 1. It does not complete the broader Phase 1 security and payout work, and no Phase 1B work was performed.

**Branch:** `feature/phase-1a-financial-guardrails`
**Baseline commit:** `60179fb`

### Scope completed

| Risk ID | Phase 1A disposition | Summary |
|---|---|---|
| FIN-01 | TEMPORARILY DISABLED | Direct deposit endpoint remains registered and preserves ownership/active checks, but `TransactionService.deposit` now throws `ServiceUnavailableException` (HTTP 503) before any balance update, transaction, audit record, or notification. No admin/public equivalent exists. |
| FIN-02 | TEMPORARILY DISABLED | Webhook secret authentication and constant-time comparison are unchanged. `WebhookService.processCollectionCallback` validates, parses the reference, returns 200 for non-Successful statuses, normalizes the amount, looks up the wallet, then throws `ServiceUnavailableException` (HTTP 503) before crediting. The credit path and duplicate-description deduplication were removed. |
| FIN-06 | VERIFIED | Transfer locking is canonicalized by ascending wallet UUID using `findByIdWithLock`; locked rows are mapped back to sender/receiver; sender active status is rechecked under lock; atomic debit/credit and rollback are preserved. No isolation downgrade. |
| FIN-12 | MITIGATED | Transaction references now use DB-backed `transaction_reference_seq` via `nextval`, with range guard `1..99,999,999`. Migration V12 streams references, ignores non-conforming rows with a warning, fails closed at capacity, restarts to `max+1`, and does not rewrite historical rows. Streaming uses a nonzero fetch size, autocommit guard, and try-with-resources. **PostgreSQL 16 verified**: fresh migration, historical references, sequence initialization, malformed references, concurrent generation (8x100=800 unique, 0 duplicates), uniqueness enforcement, sequence exhaustion, and migration failure/recovery all passed. Java generator tested against PostgreSQL 16 (16x250=4000 unique, 0 duplicates, 1831ms). |
| OPS-01 | VERIFIED | Maven Wrapper 3.3.4 script-only configuration was restored with Maven 3.9.16 and a pinned SHA-256 from the official ASF SHA-512. Clean-cache bootstrap and tampered-checksum failure were verified; `mvnw` mode is `755`; wrapper properties remain untracked by repository policy. |

### Evidence

- Full regression: 175 tests, 0 failures, 0 errors, 0 skipped on H2 with JDK 17 via `./mvnw clean test`.
- Baseline before Phase 1A: 144 tests with 8 failures caused by duplicate transaction references.
- Targeted containment/locking/migration tests passed in their respective runs.
- No source, test, migration, POM, `.mvn`, or `.gitignore` files were modified by this documentation task.

### Explicit non-goals / not performed

- No Phase 1B, payment orchestration, double-entry ledger, modular-monolith, event-driven/outbox, or microservice extraction work.
- FIN-01 and FIN-02 remain contained rather than resolved; verified collection initiation and verification are not implemented.
- FIN-12 PostgreSQL 16 verification: PASSED. All 8 migration tests and 3 Java generator tests passed against PostgreSQL 16.15. No corrections were required.
- Residual low security findings and out-of-scope observations are recorded in `backend-risk-register.md` §10.4.


## Phase 1B — Safe Payout Processing (three-stage refactor)

**Status:** In Progress / Batch 2 Implemented (Batch 3 planned).
**Branch:** `feature/phase-1b-safe-payout-processing`

### Objectives
- Decouple the external Lipila HTTP call from the wallet debit transaction (address FIN-03 / FIN-04).
- Make the wallet debit durable before the provider call; settle the outcome in a follow-up short transaction.
- Eliminate auto-refund of uncertain amounts; route timeouts/uncertain results to `PENDING_RECONCILIATION` (Batch 3 reconciliation resolves them).
- Add idempotency on payout initiation and payout audit events.

### Batch 2 — Three-stage payout (Implemented)

| Batch | Item | Status | Summary |
|-------|------|--------|---------|
| Batch 2.1 | Stage A — `initiatePayout()` | Implemented | Short `@Transactional` stage: payments-enabled gate, wallet ownership/`ACTIVE` (pessimistic `FOR UPDATE` lock), email verification, PIN, velocity + daily-send limits, fee calculation, wallet debit (`amount + fee`), persist `PENDING` payout row with `[LIPILA:<ref>] [IDEMPOTENCY:<key>]` description, commit + release lock. |
| Batch 2.2 | Stage B — `executeDisbursement()` | Implemented | No-transaction stage: `LipilaGateway.initiateDisbursement()` (5s connect / 10s read timeout). Transport exception → caught by `payout()`, synthesised to `Pending` (uncertain); Stage A debit stays durable; **Stage C still runs** and marks `PENDING_RECONCILIATION` (record does **not** stay `PENDING`). |
| Batch 2.3 | Stage C — `finalizePayout()` | Implemented | Short `@Transactional` stage: `Successful` → `SUCCESSFUL` + `PAYOUT_SUCCESS` audit; `Failed` → compensating credit of `amount + fee` + `FAILED` + `PAYOUT_FAILED_COMPENSATED` audit; `Pending`/uncertain → `PENDING_RECONCILIATION` (debit preserved, no auto-refund) + `PAYOUT_PENDING_RECONCILIATION` audit. |
| Batch 2.4 | Idempotency | Implemented | `IdempotencyFilter` (scoped `<userId>:<key>`, response caching/replay) + `PayoutService` description-marker scan (`findByDescriptionContaining("[IDEMPOTENCY:<key>]")`) to return existing payout without re-debiting. |
| Batch 2.5 | `TransactionStatus.PENDING_RECONCILIATION` | Implemented | New enum value added to `TransactionStatus`; gateway mapping (`PENDING`/`PENDING_RECONCILIATION` → "Pending" response status). |

**Entry point:** `PayoutController.payout()` (`POST /wallets/{walletId}/payouts`, optional `Idempotency-Key` header) chains `initiatePayout → executeDisbursement → finalizePayout`.

### Batch 3 — Payout hardening (Implemented, pending final acceptance)

| Item | Status | Summary |
|------|--------|---------|
| Reconciliation scheduler | Implemented (fail-closed) | `PayoutReconciliationScheduler` + `PayoutReconciliationService`: atomic claim with 5-min lease, provider calls outside transactions, backoff ladder (1/2/5/10/30/60 min), 24h park-and-alert with dedup, exception isolation. Disabled by default (`matchIfMissing=false`); enabled per environment via `app.scheduler.payout-reconciliation.enabled`. |
| Disbursement webhook | Implemented | `POST /api/v1/webhooks/lipila/disbursements` (HMAC per Lipila Standard Webhooks scheme, timestamp window, durable event-id dedup, reference/amount match, authoritative provider re-query, quarantine/deferred paths). HMAC required by default. |
| Cleanup job alignment | Implemented | `PendingTransactionCleanupJob` no longer marks payouts `FAILED` without refund; routes stale payouts to reconciliation. |
| Dedicated payout columns | Implemented (V14) | `provider_reference`, `external_status`, `payout_account`, `payout_channel`, `attempt_count`, `next_status_check_at`, `idempotency_key` columns + indexes (V14, V15). |
| Provider webhook events | Implemented (V15) | `provider_webhook_events` table with unique `(provider, event_id)` and quarantine status. |

### Verification & Acceptance Criteria
- `./mvnw test` green; no regression in existing transfer/withdraw/deposit paths.
- Concurrency test: N concurrent payouts on one wallet do not exhaust the connection pool; lock-hold time < 100ms.
- Timeout test: gateway unreachable → Stage A commits (debit durable, record `PENDING`), Stage B transport exception caught and synthesised to `Pending`, Stage C runs and marks `PENDING_RECONCILIATION` (debit preserved, no auto-refund).
- Provider-failure test: mock gateway returns `Failed` → compensating credit restores balance, row `FAILED`, audit recorded.
- Idempotency test: repeated `Idempotency-Key` → no second debit, cached response returned.
- Batch 3 items are explicitly **not** part of Batch 2.

---

## Phase 2 — Double-Entry Accounting Ledger

**Objectives**
- Introduce a real double-entry ledger alongside the existing balance-based model, without breaking existing `wallets`/`transactions`.
- Enforce `Total Debits = Total Credits` and provide reconciliation.

**Dependencies & Preconditions**
- Phase 1 complete (money movement safe).

**Concrete Technical Deliverables**
- Additive Flyway migrations: `ledger_accounts`, `journal_entries`, `ledger_postings`, `accounting_periods` (new migration files, e.g. V12+).
- Chart of accounts seeded (cash, wallet liability, fee revenue, suspense).
- `Total Debits = Total Credits` DB-level / service-level enforcement.
- Historical backfill migration preserving existing `wallets` and `transactions` (every existing transaction becomes a balanced journal entry).
- Dual-write: `TransactionService` writes both `transactions` row and ledger postings in the same `@Transactional`.
- Continuous reconciliation job (extends `BalanceRecalculationJob` logic) comparing wallet balance vs ledger position.

**Verification & Acceptance Criteria**
- Reconciliation job reports zero discrepancies on backfilled data.
- New money movements produce balanced postings; a forced imbalance throws and rolls back.
- Existing 130 tests still pass.

---

## Phase 3 — Payment Orchestration & Multi-Provider Readiness

**Objectives**
- Replace the current thin payment layer (zero owned tables) with durable payment orchestration entities and a multi-provider router.
- Make payouts and collections auditable, reconcilable, and provider-agnostic.

**Dependencies & Preconditions**
- Phase 1 complete. Phase 2 recommended (ledger postings for payment settlement).

**Concrete Technical Deliverables**
- New tables: `payment_intents`/`collection_requests`, `disbursement_requests`, `provider_webhook_events` (Flyway V13+).
- Complete collection initiation workflow: `WebhookController` + `WebhookService` route by `referenceId` (NW:<walletId>:<uuid>) but persist a `provider_webhook_events` row + idempotency check before crediting.
- Two-phase payout: reservation (debit wallet into pending) + async disbursement execution via `PaymentGateway.initiateDisbursement(...)`; settle on callback/poll.
- Webhook HMAC/replay protection + amount/currency verification (extend `WebhookService.processCollectionCallback`, line 93, and `normalizeAmount`, line 174).
- Status polling scheduler using `PaymentGateway.checkStatus(...)` (line 45 of `PaymentGateway.java`).
- Provider router supporting Lipila, MTN Mobile Money, Airtel Money, Zamtel Kwacha, and Bank transfers (implementations of `PaymentGateway`).
- `PayoutService.payout(...)` (line 107) no longer calls the gateway inside the DB tx; uses the orchestration tables.

**Verification & Acceptance Criteria**
- Replay of a webhook is a no-op (idempotent).
- A simulated provider timeout leaves a PENDING intent that reconciles via `checkStatus`.
- Amount/currency mismatch is rejected with 400 and no credit.
- Multi-provider routing unit tests.

---

## Phase 4 — Modular Monolith & Schema Isolation

**Objectives**
- Enforce strict module boundaries and relocate misplaced schedulers and KYC coupling, preparing for extraction without deploying new services.

**Dependencies & Preconditions**
- Phases 1–3 complete.

**Concrete Technical Deliverables**
- ArchUnit rules forbidding cross-domain repository injection (e.g. `payment` may not reference `WalletRepository` directly; must use a Wallet & Ledger API/command).
- Relocate `BalanceRecalculationJob` from `notification.schedule` to `transaction.schedule` (owns `TransactionRepository`).
- Relocate `KycExpiryJob` from `notification.schedule` to `kyc.schedule` (owns `UserRepository`/`KycDocumentRepository`).
- Decouple KYC fields from `User` entity (`user/entity/User.java` lines 81–106) and `users` table (V5 columns) into a `kyc_profiles` table in the `kyc` schema; `UserRepository.findByKycStatusAndKycApprovedAtBefore(...)` (line 34) moves to a KYC repository.
- PostgreSQL schema-per-domain: `identity`, `ledger`, `payment`, `kyc`, `notification`, `admin`; FKs only within schema; cross-schema refs by UUID.
- Replace `AdminKycService`'s direct `WalletRepository.save(...)` (line 141) with a Wallet & Ledger command/event (`KycApproved` → provisioning).
- `FileStorageService` keeps local FS here; S3/MinIO deferred to Phase 8.

**Verification & Acceptance Criteria**
- ArchUnit tests pass in CI.
- `./mvnw test` green (all tests updated for relocated schedulers).
- No cross-schema FKs; integration test boots against schema-per-module DB.

---

## Phase 5 — Event-Driven Architecture & Transactional Outbox

**Objectives**
- Decouple side effects (notifications, audit, KYC wallet provisioning, payment settlement) from synchronous transaction paths via domain events and a Transactional Outbox.

**Dependencies & Preconditions**
- Phase 4 complete (boundaries + schema isolation).

**Concrete Technical Deliverables**
- Domain events: `UserRegistered`, `EmailVerified`, `PasswordChanged`, `UserDeactivated`; `WalletCreated`, `WalletFrozen`, `WalletUnfrozen`, `DepositCompleted`, `WithdrawalCompleted`, `TransferCompleted`, `TransactionFailed`, `BalanceChanged`; `KycSubmitted`, `KycApproved`, `KycRejected`, `KycExpired`; `PaymentCollection*`, `PaymentDisbursement*`, `WebhookReceived/Verified`; `NotificationSent/Failed/Read`.
- `outbox_events` table (id, aggregate_type, aggregate_id, event_type, payload JSON, created_at, published_at).
- Outbox relay publisher (polls unpublished rows, publishes to RabbitMQ, marks published).
- Idempotent consumer inbox table (`processed_events` with event_id PK) for at-least-once consumers.
- RabbitMQ broker integration (spring-amqp).
- Replace synchronous `NotificationService.sendBoth(...)` calls in `TransactionService` (deposit line 118, withdraw line 161, transfer notification block) and `AdminKycService` (line 148) with outbox writes.
- Replace synchronous `AuditService.recordAction(...)` calls with an audit projection consuming events.
- `KycApproved` event drives wallet provisioning (remove direct `WalletRepository.save` from `AdminKycService`).

**Verification & Acceptance Criteria**
- Money movement transaction commits without calling Notification/Audit synchronously; outbox row present in same tx.
- Relay publishes to RabbitMQ; consumer processes idempotently (duplicate event → no-op).
- End-to-end test: deposit → notification row created via event within SLA.

---

## Phase 6 — Notification Service Extraction

**Objectives**
- Extract the `notification` module into the first standalone deployable microservice, consuming RabbitMQ domain events with its own database.

**Dependencies & Preconditions**
- Phase 5 complete (outbox + events proven). Phase 4 schedulers relocated.

**Concrete Technical Deliverables**
- New service `notification-service` (Spring Boot) with its own PostgreSQL DB containing `notifications` and `notification_attempts` (migrated from monolith via Flyway; no FKs to other tables).
- Consumer of: `DepositCompleted`, `WithdrawalCompleted`, `TransferCompleted`, `TransactionFailed`, `KycApproved`, `KycRejected`, `PasswordChanged`, `EmailVerified`, `UserRegistered`.
- REST API `/api/v1/notifications/**` served by the new service; API Gateway routes notification paths to it.
- Remove notification tables/code from monolith (or keep read-only projection temporarily).
- `NotificationRetryJob` moves into the new service.

**Verification & Acceptance Criteria**
- Notification service deploys independently; monolith continues to operate if it is down (events buffered in RabbitMQ).
- No data loss on notification service restart (inbox idempotency).
- End-to-end: transfer in monolith → notification delivered by extracted service.

---

## Phase 7 — Payment Service Extraction

**Objectives**
- Extract the `payment` orchestration module into a standalone microservice communicating with Wallet & Ledger strictly via idempotent signed commands and outbox events; zero shared tables.

**Dependencies & Preconditions**
- Phase 3 payment-orchestration tables proven; Phase 5 outbox contracts proven; Phase 6 extraction pattern proven.

**Concrete Technical Deliverables**
- New service `payment-service` with its own DB (`payment_intents`/`collection_requests`, `disbursement_requests`, `provider_webhook_events`).
- Commands to Wallet & Ledger: `ReserveFunds`, `CreditWallet`, `DebitWallet` (idempotent, correlation IDs) replacing direct `WalletRepository`/`TransactionRepository` mutations.
- Consumes `WalletCreated`, `UserRegistered`; publishes `PaymentCollectionCompleted/Failed`, `PaymentDisbursementSettled/Failed`.
- Webhook endpoint moves to payment-service; HMAC verification stays in service.
- `PayoutService`/`WebhookService` removed from monolith.

**Verification & Acceptance Criteria**
- Zero shared tables between payment-service and Wallet & Ledger.
- Timeout scenario reconciles via `checkStatus` polling without money loss.
- Load test: concurrent payouts do not corrupt balances.

---

## Phase 8 — Identity and KYC Service Extraction

**Objectives**
- Extract Identity Service and KYC Service, with object storage (S3/MinIO) replacing local filesystem `FileStorageService`, Redis-backed rate limiting and token revocation, and API Gateway routing.

**Dependencies & Preconditions**
- Phase 4 KYC decoupled from `User`; Phase 5 events published; Phase 7 proven.

**Concrete Technical Deliverables**
- `identity-service`: owns `users`, `refresh_tokens`; issues JWT; Redis revocation cache; publishes `UserRegistered`/`EmailVerified`/`PasswordChanged`/`UserDeactivated`/`RefreshTokenRevoked`.
- `kyc-service`: owns `kyc_documents`, `kyc_profiles`; `FileStorageService` replaced by S3/MinIO client; publishes `KycSubmitted/Approved/Rejected/Expired`; consumes `UserRegistered`; `KycApproved` triggers Wallet & Ledger provisioning via event.
- Admin KYC approval moved to kyc-service (or admin BFF composing calls).
- API Gateway routes `/auth`, `/users`, `/kyc` paths.
- `CustomUserDetailsService` replaced by gateway-level JWT validation + Redis status cache.

**Verification & Acceptance Criteria**
- Identity service down does not break already-issued token validation (cached JWKS/public key).
- KYC document upload/download works against S3/MinIO; local filesystem no longer used.
- Wallet provisioning occurs asynchronously on `KycApproved` event; no direct DB write across services.

---

## Phase 9 — Production Engineering, Observability & Future Capabilities

**Objectives**
- Harden the now-distributed system for production: gateway, tracing, metrics, logging, CI/CD gates, and readiness seams for future features (Virtual Cards, Budgeting/Savings vaults, Nova AI).

**Dependencies & Preconditions**
- Phases 6–8 complete (services extracted).

**Concrete Technical Deliverables**
- API Gateway (Spring Cloud Gateway or Kong) with TLS termination, path-based routing, JWT validation, rate limiting.
- OpenTelemetry distributed tracing across all services (trace context propagation via RabbitMQ headers).
- Prometheus + Grafana dashboards: financial KPIs (transaction volume, success rate, balance reconciliation), SLOs (latency, error rate, availability), payment provider health.
- Centralized structured log aggregation (ELK/Loki) with correlation IDs (`X-Request-ID` from `RequestTracingFilter`).
- CI/CD deployment gates: ArchUnit, contract tests (Pact) between services, migration checks, security scan.
- Readiness seams: Virtual Cards (card-issuing provider integration), Budgeting/Savings vaults (new ledger accounts + goals), Nova AI (AI advisor service consuming transaction events) — documented as future phases, not implemented here.

**Verification & Acceptance Criteria**
- Distributed trace spans a full request across gateway → service → DB → event consumer.
- Grafana shows financial reconciliation dashboard with zero drift.
- CI pipeline blocks merge on contract-test or ArchUnit failure.
- Runbook for service outage (which service, impact, mitigation).

---

## Summary Timeline (indicative)

| Phase | Name | Gate to next |
|---|---|---|
| 0 | Architecture Audit | Baseline verified |
| 1 | Financial Integrity & Security | Money movement safe |
| 2 | Double-Entry Ledger | Balanced postings + reconciliation |
| 3 | Payment Orchestration | Durable intents + multi-provider |
| 4 | Modular Monolith | ArchUnit green, schema-per-module |
| 5 | Event-Driven + Outbox | Events flowing in-process |
| 6 | Extract Notification | First service live |
| 7 | Extract Payment | Zero shared tables |
| 8 | Extract Identity & KYC | Redis + S3/MinIO live |
| 9 | Production Engineering | Tracing/metrics/CI gates live |

No phase may be skipped. Extraction (Phases 6–8) is only permitted after Phases 1–5 are complete and verified.
