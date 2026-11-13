# 02 · Backend completion plan

**Starting point (from the repo):** Phase 0, Phase 1A and Phase 1B Batch 2 are done. Batch 3 has not started. The plan below keeps your roadmap phases and IDs, and adds the endpoints the new app screens need, so the app and backend finish together.

Each work item lists the risks it closes, so you can tick the risk register as you go.

---

## Phase 1B · Batch 3: finish safe payouts (do this first)

| # | Work | Closes | Done when |
|---|---|---|---|
| 1 | **Dedicated payout columns or a `disbursement_requests` table**: `provider_reference`, `external_status`, `payout_account`, `payout_channel`, `attempts`, `next_check_at`. Stop parsing `description`. | Batch 3 item 4 | Migration V14 + no code reads payout data from description |
| 2 | **Reconciliation scheduler**: every minute, pick `PENDING`/`PENDING_RECONCILIATION` payouts with `next_check_at <= now`, call `PaymentGateway.checkStatus()`. SUCCESS → `SUCCESSFUL`. FAILED → `FAILED` plus compensating credit (once). Unknown → back off (1, 2, 5, 10, 30 min, then hourly up to 24 h), then raise an admin alert. | S2, S9, FIN-10 | Integration tests: success, failure+refund, still pending, provider down |
| 3 | **Disbursement webhook** `POST /api/v1/webhooks/lipila/disbursements` with HMAC + timestamp + nonce, stored in `provider_webhook_events`, amount/currency check, idempotent. | FIN-10, SEC-05 | Replay of the same event changes nothing |
| 4 | **Fix `PendingTransactionCleanupJob`**: never mark payouts `FAILED`; route stale payouts to reconciliation. | FIN-05, S8 | Test: stale payout ends in reconciliation, not FAILED |
| 5 | Catch `Throwable` around Stage B, so a JVM `Error` still leads to Stage C or reconciliation. | S7 | Test with a gateway that throws `Error` |
| 6 | Close the Stage A idempotency race for direct service calls (unique constraint on idempotency key + wallet). | S5 | Concurrency test |
| 7 | Map provider messages to safe customer messages (no "Insufficient merchant float"). | S10 | Test on the 400 path |

## Phase 1C · Security fixes (do in parallel with Batch 3)

| # | Work | Closes |
|---|---|---|
| 1 | JwtAuthFilter checks enabled/locked/deleted status | SEC-01 |
| 2 | Revoke refresh tokens on password change, admin deactivation and logout; expose `POST /auth/logout` | SEC-02, SEC-06 |
| 3 | Hash verification/reset tokens; redact them from logs and notifications; add expiry | SEC-03, SEC-07 |
| 4 | Default profile is not `dev`; secrets come from environment/secret manager | SEC-04 |
| 5 | JWT key length check, `iss`/`aud` claims | SEC-08 |
| 6 | Redis-backed, proxy-aware rate limiting | SEC-09 |
| 7 | `@Digits(integer=12, fraction=2)` + `@DecimalMax` on all money DTOs | SEC-10 |
| 8 | Prod: actuator env off, Swagger off, CORS allowlist | SEC-11 |
| 9 | Validate `X-Request-ID`; magic-byte MIME check on KYC uploads | SEC-12 |
| 10 | Fix the transfer deadlock risk with ordered locks if not already done | FIN-06 |
| 11 | Single rounding step in percentage fees | FIN-09 |
| 12 | Per-user aggregate limits; unverified users can't bypass tier limits | FIN-11 |
| 13 | DB CHECK constraints, FK indexes, drop the leftover test table | DB-01 |

## Phase 1D · Endpoints the new app needs (before or alongside Phase 2)

These are marked **NEW** in the screen BUILD.md files. Group them by module.

| Module | Endpoints | Screens |
|---|---|---|
| identity | `POST /auth/otp/send`, `POST /auth/otp/verify`, register with `otpToken` and `dateOfBirth`, login with `{identifier,password}`, `POST /auth/device/verify`, `POST /pin/set`, `POST /pin/verify` (server attempts + lockout), `GET/DELETE /devices`, `POST /auth/logout` | B01–B07, C01–C07, O04, O05 |
| app | `GET /app/config` (min version, maintenance, feature flags, biometric threshold), `GET /app/notices` | A01, E05, P05, P06 |
| kyc | `GET /kyc/tiers`, reason codes in `/kyc/status`, document types | D01–D09 |
| payments | **`POST /wallets/{id}/collections`** (top up; replaces disabled deposit, closes FIN-01/FIN-02 properly), `GET /payment-methods`, `GET /payouts/name-check`, `GET/POST/DELETE /payment-accounts` | F01–F08, G07, I01–I03, O06 |
| wallet | `GET /users/lookup?phone=`, `POST /contacts/match`, transactions filters (`q`, `type`, `category`, dates), `PATCH /transactions/{ref}/category`, receipt PDF, `POST /payment-requests` (+ pay/decline), `POST /splits`, `GET /qr/me`, `POST /qr/resolve` | G01–G09, H01–H03, J01–J07 |
| notifications | `GET /notifications` (list), `PUT /notifications/{id}/read`, `POST /devices/push-token`, preferences | E06, E07, O07 |
| insights + savings | `GET /insights/summary`, budgets CRUD, goals CRUD + deposit/withdraw, round-ups, cash entries, chilimba (tracker) | M01–M09 |
| bills + store | `GET /billers`, `POST /bills/validate`, `POST /bills/pay`, saved billers, airtime/bundles, store products/orders | K00–K06 |
| cards | waitlist now; issuer endpoints later (doc 04) | L01–L07 |
| ai | `POST /ai/chat`, `GET /ai/tips` (doc 05) | N01, N02 |
| support + privacy | support cases + messages, privacy consents + export, referrals, account close checks | J06, O09–O13 |
| admin | see the admin console guide (Part 7) | X01–X16 |

## Phase 2 · Double-entry ledger (as in your roadmap)

Tables: `ledger_accounts`, `journal_entries`, `ledger_postings`, `accounting_periods`. Seed the chart of accounts below, backfill history, dual-write, and run continuous reconciliation. Closes **FIN-08**.

| Account | Type | Use |
|---|---|---|
| `customer_wallet:{walletId}` | Liability | Each customer's money |
| `goal:{goalId}` | Liability | Savings goal sub-accounts (M04–M06) |
| `card:{cardId}` | Liability | If cards are pre-funded |
| `clearing:lipila:collections` | Asset | Money Lipila holds for us from top ups |
| `clearing:lipila:payouts` | Asset | Float used to pay out |
| `clearing:bills:lenco` | Asset | Bill float |
| `fee_income` | Revenue | Fees |
| `suspense` | Liability | Unknown/mismatched money while being investigated |

Example: a top up of K 250 posts debit `clearing:lipila:collections` K 250, credit `customer_wallet` K 250, and only after the provider confirms.

## Phase 3 · Payment orchestration (as in your roadmap)

`payment_intents`, `collection_requests`, `disbursement_requests`, `provider_webhook_events`, `provider_routes` (admin kill switch), a provider router (Lipila now; direct MTN/Airtel later), status polling and HMAC. See doc 03 for the flows.

## Phase 4 · Modular monolith and schema isolation

Move each module's tables into its own Postgres schema. Replace cross-module repository use with module ports and ArchUnit rules (ARCH-01). Split identity and KYC (ARCH-02).

## Phase 5 · Outbox and events

Add an `outbox` table, a relay worker and RabbitMQ. Notifications, insights and audit become event consumers (ARCH-03).

## Phases 6–8 · Extract services

Extract in this order: notification, then payment, then identity/KYC (doc 01, section 2).

## Phase 9 · Production engineering

Observability, load tests (500 concurrent payouts, p95 < 300 ms on reads), chaos tests on the provider timeout path, disaster recovery drill, penetration test before going live, and runbooks for "provider down", "stuck payouts" and "webhook storm".

---

## Order of work (suggested 10–12 weeks for one developer with opencode)

| Week | Focus |
|---|---|
| 1–2 | Batch 3 + Phase 1C security fixes |
| 3 | Identity endpoints (OTP, PIN, devices, login by phone) + app config |
| 4 | Collections (top up) end to end in Lipila sandbox |
| 5 | Notifications list, lookups, payment requests, QR |
| 6–7 | Phase 2 ledger (dual-write + reconciliation) |
| 8 | Bills (aggregator sandbox) + insights/savings endpoints |
| 9 | Admin backend (auth, RBAC, maker-checker, provider routes) |
| 10 | AI gateway (read-only tools) |
| 11–12 | Phase 3 orchestration tables, hardening, pen test fixes, pilot |

Phases 4–8 come after the pilot, when real usage shows where the load is.
