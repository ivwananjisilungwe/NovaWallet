# AGENTS.md — novawallet-api (Spring Boot backend)

Read the root `AGENTS.md` first. This file adds backend rules. Existing project rules in `TODO.md`, `SECURITY.md`, `docs/phase-1c/PHASE_1C_DECISION_LOG.md`, `docs/phase-1c/PHASE_1C_IMPLEMENTATION_PLAN.md` (§0, §13 Batch Gate Protocol) still apply; if they conflict with this file, the stricter rule wins and you report the conflict.

## Stack & facts (verified)
- Spring Boot 3.5.3, Java source/release 17 (local runtime may be 21; never claim Java 17 runtime verification — BLK-04).
- PostgreSQL (verified on 16), Flyway migrations in `src/main/resources/db/migration`. Current head: **V16**. V12 does not exist (historical gap) — do not create or renumber it.
- Packages: auth, user, wallet, transaction, payment, kyc, notification, admin, token, security, config, exception.
- Base path `/api/v1`. JWT HS256 with iss/aud/jti; access ≤15 min; refresh rotation; DB-backed access-token revocation (V16).
- Payouts: three-stage (A durable debit/hold → B provider call outside DB transaction → C finalize), `PENDING_RECONCILIATION`, reconciliation scheduler with lease + backoff, Lipila disbursement webhook with durable dedup (`provider_webhook_events`), cleanup job.
- Collections webhook is guarded (503) until the Lipila signing contract exists (BLK-01).
- Email verification enforced for money movement only (EmailVerificationPolicy, DEC-01 Option B).

## Engineering rules
1. **Transactions:** use `TransactionTemplate` for programmatic boundaries; never rely on self-invoked `@Transactional(REQUIRES_NEW)`. `@Modifying` queries must run inside a transaction. Never hold a DB transaction open across an HTTP call to a provider.
2. **Money:** `BigDecimal` with scale 2 (or long ngwee) — follow the existing entity convention; never `double`. Lock order for multi-wallet operations follows the existing deterministic ordering (see `TransactionServiceTransferLockOrderTest`).
3. **Idempotency:** every money-moving POST accepts `Idempotency-Key`, scoped per user, with stored response replay. Follow the existing idempotency table/pattern.
4. **Migrations:** new file `V<next>__snake_case.sql`, additive first (expand → migrate → contract). Never edit an applied migration. Every migration verified on PostgreSQL 16: fresh V1→head and upgrade previous-head→head, recorded in the report.
5. **Security:** ownership checks on every resource (no IDOR); never trust client-supplied user ids; validate DTO bounds; no stack traces or internals in error bodies; redact tokens/PINs/passwords/full phone/ID numbers in logs (`SensitiveDataRedactor`).
6. **External providers:** behind an interface + adapter; sandbox config only; timeouts, retries with backoff only for safe/idempotent calls; map provider codes to internal codes. Do not invent provider endpoints or fields — read `docs/v5/part-08/03_MOBILE_MONEY_INTEGRATION.md` and any provider docs in `docs/providers/`; if absent, BLOCKED.
7. **API contract:** OpenAPI annotations on every endpoint; update `reports/API_ENDPOINT_INVENTORY.md` for every added/changed endpoint; consistent error envelope (the existing global handler).
8. **Audit:** admin and money actions write audit logs (who, what, when, before/after where relevant), without secrets.

## Gates (every backend prompt)
```
./mvnw -B clean test                       # full suite; report counts (baseline 316 after Batch 1)
# PostgreSQL 16 migration check via the existing pgtest profile / disposable container:
#   fresh V1→head, upgrade previous head→head, flyway validate
git diff --check
```
Plus: targeted tests for the change, negative tests (authz, validation, concurrency where relevant), secret scan of the diff. Update the risk traceability matrix only with executable evidence. Write `reports/<PROMPT-ID>_REPORT.md`.

## Never
- Weaken an existing Phase 1A/1B/1C safeguard to make a test pass.
- Delete or skip tests. Disable security in tests outside the existing test config.
- Add Redis/Kafka/RabbitMQ or any new infrastructure unless the prompt says so.
- Touch `novawallet-app/` or `novawallet-admin/`.
