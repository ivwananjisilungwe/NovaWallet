# NovaWallet API — Payout Lifecycle & Financial State Transitions

> **Document**: `docs/architecture/payout-lifecycle.md`
> **Scope**: `novawallet-api/` source tree (Phase 1B, branch `feature/phase-1b-safe-payout-processing`)
> **Purpose**: Reference for the Phase 1B safe payout-processing implementation: the three-stage payout lifecycle, reconciliation scheduler, disbursement webhook, dedicated payout data model, audit events, cleanup-job behaviour, and the limitations that genuinely remain. Maintained against the actual source; see `backend-risk-register.md` for risk tracking.

---

## 1. Current Payout Lifecycle (Three-Stage Flow)

### Entry point

```
POST /api/v1/wallets/{walletId}/payouts            (optional Idempotency-Key header)
  → PayoutController.payout()
  → PayoutService.payout(walletId, request, userId, idempotencyKey)
      Stage A: initiatePayout()        [short transaction — TransactionTemplate, PROPAGATION_REQUIRED]
      Stage B: executeDisbursement()   [no transaction]
      Stage C: finalizePayout()        [short transaction — TransactionTemplate, PROPAGATION_REQUIRED]
```

`PayoutService.payout()` itself is **not** `@Transactional`; it chains three independent stages so no DB transaction or wallet lock is held across the external Lipila HTTP call. Before Stage A, a description-marker fast path replays an existing payout when the idempotency key has already been used (see §4).

### Stage A — initiatePayout (durable initiation, short transaction)

| Step | Action |
|---|---|
| A1 | `requirePaymentsEnabled()` — 503 while the Lipila gateway is disabled (`LIPILA_ENABLED=false` by default) |
| A2 | `findOwnedActiveWalletWithLock()` — `SELECT … FOR UPDATE`; validates wallet ownership (403) and `WalletStatus.ACTIVE` (400). The lock is held only for Stage A |
| A3 | Idempotency key scoping — `IdempotencyService.scopedKey(userId, key)` → `<userId>:<rawKey>`; `findByIdempotencyKey` lookup under the wallet lock; `IdempotencyService.tryAcquire` on `idempotency_keys` (same transaction as the debit, so a failed attempt never poisons the key) |
| A4 | `enforceVelocityLimits(userId, amount)` — rolling hourly window (before PIN) |
| A5 | `authService.verifyPin(userId, pin)` |
| A6 | `enforceDailySendLimit(userId, walletId, amount)` — KYC daily-send limit (after PIN) |
| A7 | `feeEngineService.calculateFee(WITHDRAWAL, amount)` |
| A8 | `walletRepository.updateBalance(walletId, -(amount + fee))` — native UPDATE guarded on affected rows; 0 rows → 400 insufficient balance |
| A9 | Persist `PENDING` payout row (`type=WITHDRAWAL`) with the dedicated V14 columns: `providerReference` (`NW:<walletId>:<uuid>`), `externalStatus="Pending"`, `payoutAccount`, `payoutChannel="mobile-money"`, `attemptCount=0`, `idempotencyKey` (user-scoped). The description additionally embeds `[ACCT:…] [LIPILA:<providerReferenceId>] [IDEMPOTENCY:<rawKey>]` for legacy/scan compatibility |
| A10 | If `fee > 0`: persist a linked `FEE` row (`relatedTransactionId`) |
| A11 | Audit `PAYOUT_INITIATED` (best-effort; the idempotency key is deliberately NOT written to the audit trail) |
| A12 | **Commit** → wallet debit durable, lock released |

### Stage B — executeDisbursement (provider call, NO transaction)

- Calls `LipilaGateway.initiateDisbursement()` — HTTP POST `{base-url}/disbursements/mobile-money`, `x-api-key` auth header, optional `callbackUrl` header, connect timeout 5s / read timeout 10s. No DB transaction and no wallet lock are held.
- On a provider response (Successful / Failed / Pending / anything else): returns the `GatewayInitiationResponse` to Stage C.
- On a transport/provider `Exception` (timeout, unreachable, malformed response): `payout()` catches it, synthesises an uncertain `Pending` response, and **Stage C still runs** — the Stage A debit is durable and the record is marked `PENDING_RECONCILIATION` (no auto-refund, no auto-resubmit, no exception escapes).
- JVM `Error`s (e.g. `OutOfMemoryError`) are deliberately **not** swallowed: they propagate and leave the payout `PENDING` with a durable debit for reconciliation (see `PayoutErrorPropagationTest`).
- S10 sanitisation: provider-derived text is redacted (digit runs, 200-char cap) before it reaches audit logs, descriptions, or client responses; client-facing messages are hardcoded strings only.

### Stage C — finalizePayout (settlement, short transaction)

Reloads the payout by id (must be `WITHDRAWAL`), re-acquires the wallet pessimistic lock, and settles from the **persisted** status:

| Provider outcome | Stage C action | Wallet effect | Audit event |
|---|---|---|---|
| `Successful` | Mark `SUCCESSFUL`; append finalization note | Debit preserved | `PAYOUT_SUCCESS` |
| `Failed` | Compensating credit of `amount + fee`; mark `FAILED`; write compensating `DEPOSIT` row | Debit reversed | `PAYOUT_FAILED_COMPENSATED` |
| `Pending` / anything else (uncertain) | Mark `PENDING_RECONCILIATION`; preserve the debit | Debit preserved | `PAYOUT_PENDING_RECONCILIATION` |

**Replay guard (terminal immutability).** Inside the transaction, after the wallet lock: if the persisted status is `SUCCESSFUL` or `FAILED`, the call is a no-op (terminal states are immutable — no double credit). A `PENDING_RECONCILIATION` payout is likewise immune to replayed/stale **live-flow** responses; it can only be resolved by the reconciliation scheduler or the disbursement webhook after a fresh authoritative provider re-query (`reconciliationResolution=true` with `Successful`/`Failed`). An uncertain payout is therefore never failed or refunded without fresh provider confirmation.

### Transaction boundary summary

- **Stage A** (short tx): validation + debit + persist PENDING record → commit → wallet lock released.
- **Stage B** (no tx): Lipila HTTP call only; no DB transaction, no wallet lock.
- **Stage C** (short tx): reload + settle + compensating credit if needed → commit.
- At no point is a DB transaction or wallet lock held across the external HTTP call.

### HTTP status mapping (PayoutController)

| `gatewayStatus` | Meaning | HTTP status | Controller behaviour |
|---|---|---|---|
| `Successful` | payout `SUCCESSFUL` (Stage C) | 201 CREATED | `ResponseEntity.status(CREATED)` |
| `Pending` | payout `PENDING` or `PENDING_RECONCILIATION` | 202 ACCEPTED | `ResponseEntity.status(ACCEPTED)` |
| `Failed` | payout `FAILED`, compensating credit applied | 400 BAD_REQUEST | `BadRequestException` → standard error body (hardcoded message, no provider leakage) |

The `Idempotency-Key` header is optional; omitting it is valid and simply skips idempotency deduplication.

---

## 2. Transaction Status Definitions

Source: `com.novawallet.novawallet_api.transaction.enums.TransactionStatus` (V13 widened the column to `VARCHAR(30)`).

| Value | Meaning |
|---|---|
| `PENDING` | Initial state after the Stage A debit; provider call not yet settled |
| `SUCCESSFUL` | Provider confirmed `Successful` (Stage C). Terminal |
| `FAILED` | Provider returned `Failed`; compensating credit applied (Stage C). Terminal |
| `PENDING_RECONCILIATION` | Provider returned Pending/uncertain or a Stage B transport error; debit preserved; resolved by reconciliation/webhook with an authoritative provider outcome. Non-terminal |

**State machine:** `PENDING → SUCCESSFUL / FAILED / PENDING_RECONCILIATION`; `PENDING_RECONCILIATION → SUCCESSFUL / FAILED` **only** via an authoritative provider re-query (scheduler or disbursement webhook); `SUCCESSFUL`/`FAILED` are terminal and replay-immune.

---

## 3. Reconciliation Scheduler

Source: `payment/schedule/PayoutReconciliationScheduler.java`, `payment/service/PayoutReconciliationService.java`.

- **Enablement (fail-closed):** `@ConditionalOnProperty(name = "app.scheduler.payout-reconciliation.enabled", havingValue = "true", matchIfMissing = false)` — the scheduler bean is not registered unless explicitly enabled. Disabled in `application-dev.yml`, bound to `APP_SCHEDULER_PAYOUT_RECONCILIATION_ENABLED` (default `false`) in `application-prod.yml`, disabled in tests (`app.scheduler.payout-reconciliation.enabled=false`). Fixed delay 60s, initial delay 30s; a failed pass is logged and never kills the scheduler thread.
- **Batch:** `BATCH_SIZE = 25` due payouts per pass (PageRequest-bounded).
- **Eligibility:** `WITHDRAWAL` rows with a provider reference in `PENDING`/`PENDING_RECONCILIATION` whose next check is due, older than the 5-minute in-flight grace window (`GRACE_MINUTES`) and younger than the 24-hour horizon (`HORIZON_HOURS`).
- **Atomic claim** (`claimPayoutForReconciliation`): one conditional UPDATE — `version = :expectedVersion` (optimistic predicate), `attemptCount < 96`, next check due — that increments `attempt_count`, sets `next_status_check_at = claimTime + 5m` (the **lease**), and bumps `version`. Losing concurrent workers affect 0 rows and are skipped (`Outcome.SKIPPED`), so the provider is polled at most once per lease window.

### Lease vs. retry eligibility (F1 verification)

The lease and the retry schedule share one column, `transactions.next_status_check_at`, but they are used at **different phases** and never conflict:

- **Lease (in-flight ownership).** At claim time the row's `next_status_check_at` is set to `claimTime + 5m`. While it is in the future the payout is **not** selected by `findDuePayoutsForReconciliation` (which requires `next_status_check_at <= now`), so no other worker can claim it. The five-minute window is deliberately longer than the gateway timeouts (5s connect / 10s read) so a single in-flight provider call cannot be raced.
- **Retry eligibility (backoff).** `applyPayoutReconciliationBackoff` is called **only after** the attempt has completed (uncertain outcome, failed status check, or failed finalization). It replaces the lease with `claimTime + backoffMinutes(attempt)` (1m, 2m, 5m, 10m, 30m, then hourly), i.e. it **releases** the lease and schedules the next attempt. Because the backoff is applied only after completion, there is no point at which a retry timestamp is due while the original worker still holds an active lease: the lease (`T0+5m`) is the gate during in-flight, and it is overwritten by the (earlier) backoff only once the worker is finished.
- **Recovery.** If the worker dies before releasing, the lease simply expires at `T0+5m` and any later worker can reclaim (`next_status_check_at <= now`). No automatic refund or failure occurs.
- **Stale-worker protection.** `applyPayoutReconciliationBackoff` is keyed on `attempt_count = :expectedAttempt AND next_status_check_at <= :leasedUntil` (the values the claim produced). A worker whose payout was re-claimed elsewhere sees a higher attempt count and a different lease value and affects 0 rows (verified by `PayoutReconciliationBackoffTest.staleWorker_cannotRescheduleOverNewerLease` and `lease_notInvalidatedByDueRetryTimestamp`).
- **Whole-second precision.** `PayoutReconciliationService` truncates `now` to whole seconds so the stored lease and the bound parameter agree exactly, preventing a sub-second mismatch from making the ownership predicate fail for a row this worker actually owns.

Verified by `PayoutReconciliationBackoffTest` (11 tests) and `PayoutReconciliationServiceTest` (F1 scenarios).
- **Provider call outside transactions:** `paymentGateway.checkStatus()` runs with no transaction active (verified by `PayoutReconciliationBackoffTest.concurrentWorkers_providerPolledOnce` and `DisbursementWebhookServiceTest.providerStatusQuery_runsOutsideDatabaseTransaction`).
- **Settlement:** `finalizePayout(..., reconciliationResolution=true)` — idempotent and replay-guarded (compensating credit at most once).
- **Backoff ladder (F1 — as implemented):** when an attempt produces no authoritative outcome (uncertain provider status, failed status check, or failed finalization), the worker releases its lease via `applyPayoutReconciliationBackoff`, whose predicate (`attempt_count = :attempt AND next_status_check_at = :leasedUntil`) matches only the row state this worker's claim produced. The next check is scheduled at **claimTime + backoffMinutes(attempt)**: attempt 1 → **1 minute**, 2 → **2 minutes**, 3 → **5 minutes**, 4 → **10 minutes**, 5 → **30 minutes**, 6+ → **hourly**. The F1 correction replaced the earlier “reschedule if due” predicate, which was already false once the claim wrote its lease and therefore never applied the ladder (the row silently stayed on the flat 5-minute lease).
- **Stale-worker safety:** the lease-ownership predicate blocks a worker whose payout was re-claimed elsewhere (attempt count and lease value both changed), and blocks un-parking (the 2999 sentinel never equals a lease value). Verified by `PayoutReconciliationBackoffTest.staleWorker_cannotRescheduleOverNewerLease`.
- **24-hour parking and alerting:** payouts past the horizon are swept by `alertExpiredPayouts` → conditional `parkPayoutForManualInvestigation` (next check pushed to the 2999-12-31 23:59 sentinel; status and funds untouched — never failed, never refunded) → `PAYOUT_RECONCILIATION_ALERT` audit + one admin notification. The conditional updates make park-and-alert fire **exactly once per payout**; reconciliation, the cleanup job, and concurrent instances cannot un-park or re-alert.
- **Manual investigation path:** parked payouts are resolved by operations (provider portal / support); no automatic refund or failure exists by design.

---

## 4. Idempotency

Three layers protect against duplicate payouts (documented precedence):

1. **`IdempotencyFilter`** (servlet filter registered after `JwtAuthFilter`): on mutating requests carrying an `Idempotency-Key` header, atomically acquires a scoped key `<userId>:<rawKey>` (`idempotency_keys` unique constraint), processes the request, caches the 2xx response body, and replays it verbatim on retry. Non-2xx outcomes delete the PROCESSING record so the client can retry. A record still PROCESSING after bounded exponential polling returns 409 `IDEMPOTENCY_IN_FLIGHT`. Keys longer than 200 chars are rejected with 400.
2. **Service-level guard** (`PayoutService`): a description-marker scan (`findByDescriptionContainingByUserId("[IDEMPOTENCY:<rawKey>]", userId)`, scoped to the user's wallets) plus a dedicated `idempotency_key` column lookup under the Stage A wallet lock; concurrent losers either replay the winner's payout (`IdempotentReplayException`) or hit the V14 unique index (`DataIntegrityViolationException` → full Stage A rollback → replay the winner).
3. **Database backstop:** V14 unique index on `transactions.idempotency_key` (per-user scoped values) — the durable guarantee that no two payouts share a key.

**Replay policy (accepted design — F4):** replaying a key returns the **original** payout response even when the request payload differs (e.g. a different amount): the key identifies the operation, not the payload, and no payload-hash comparison is performed. This is financially safe — a second wallet debit never occurs — and is asserted by `PayoutIdempotencyHttpTest.sameKeyDifferentPayload_returnsOriginal`. Cross-user replay is impossible because every layer scopes by user.

---

## 5. Disbursement Webhook

Source: `payment/controller/WebhookController.java`, `payment/service/WebhookService.java`.

- **Endpoint:** `POST /api/v1/webhooks/lipila/disbursements` (`permitAll` — no user context exists for provider callbacks). Optional legacy `X-Webhook-Secret` header is verified constant-time when supplied.
- **Required headers:** `webhook-id` (or `X-Webhook-Event-Id`), `webhook-timestamp` (or `X-Webhook-Timestamp`), `webhook-signature` (or `X-Webhook-Signature`). Body is consumed raw (`@RequestBody String`) so the HMAC is computed over the exact bytes received.
- **Processing order and transaction boundaries (F2):**
  1. Shared-secret configuration check — 503 fail-closed when no webhook secret is configured.
  2. Payload validation — 400 when `referenceId`/`status` are missing.
  3. Timestamp window — rejects timestamps older than `app.payments.lipila.webhook-timestamp-window-seconds` (default 300s) or more than 120s in the future. Malformed/unparseable timestamps → **400** (P1B-CLOSE-01).
  4. HMAC — Lipila **Standard Webhooks** scheme: HMAC-SHA256 over `{webhook-id}.{webhook-timestamp}.{rawBody}`, secret base64-decoded (raw UTF-8 fallback for legacy plaintext secrets), output base64 with `v1,` prefix; multiple space-delimited signatures accepted (key rotation); comparison via constant-time `MessageDigest.isEqual`. Required by default (`app.payments.lipila.webhook-hmac-required=true`) — invalid/missing signature → 401; set to `false` to log-only.
  5. Malformed JSON → **400** (F3; parsing happens manually for the raw body, so the controller guards it explicitly and nothing is persisted or finalized).
  6. **Durable event claim** — `provider_webhook_events` insert in a REQUIRES_NEW transaction that commits immediately; a duplicate `(provider, event_id)` fails the unique constraint → `DUPLICATE` (durable across restarts).
  7. Reference/amount validation (short transactions for event-state writes only): unknown reference → `QUARANTINED`; null amount on a terminal callback → `QUARANTINED` (P1B-CLOSE-02); amount ≠ persisted payout amount (scale-2 compare) → `QUARANTINED`; `Pending` interim status → `DEFERRED`.
  8. **Authoritative provider re-query — outside any transaction** (F2): `checkStatus` is called with no DB connection held. Provider failure → `DEFERRED` (payout untouched).
  9. **Finalization** — one short transaction in which `finalizePayout(..., reconciliationResolution=true)` settles the payout (replay-guarded) and the event is marked `PROCESSED`: both commit atomically. Finalization failure → `DEFERRED`.
- **Replay protection:** timestamp window + durable event dedup + the payout's terminal-state guard — a webhook can never independently create a successful financial state.

---

## 6. Collections Webhook (contained)

`POST /api/v1/webhooks/lipila/collections` processes `Pending`/`Failed` notifications as no-op acknowledgements and **rejects every `Successful` callback with HTTP 503** ("Collection callbacks are temporarily disabled until verified collection initiation is implemented"). No collection callback can credit a wallet; the shared-secret configuration is still required (503 when absent). Upgrading collections to the disbursement HMAC/timestamp/dedup/amount-verification standard is Phase 1C work (register risk SEC-05/R-09; closure P1B-CLOSE-09).

---

## 7. Dedicated Payout Data (Migrations V13–V15)

Applied on both H2 (tests) and PostgreSQL 16 (verified fresh + upgrade; `pg16_verification_report.md`):

| Migration | Change |
|---|---|
| **V13** | `transactions.status` → `VARCHAR(30)` (accommodates `PENDING_RECONCILIATION`); metadata-only widening on PostgreSQL 16 |
| **V14** | Payout columns on `transactions`: `provider_reference` (indexed), `external_status` (indexed), `payout_account`, `payout_channel` (indexed), `attempt_count INTEGER NOT NULL DEFAULT 0`, `next_status_check_at` (indexed), `idempotency_key` (**unique** per-user-scoped index — the durable dedup backstop) |
| **V15** | `provider_webhook_events` table (unique `(provider, event_id)`; `processing_status` RECEIVED/PROCESSED/QUARANTINED/DEFERRED), `CHECK (attempt_count >= 0)`, composite `idx_transactions_reconciliation_due (next_status_check_at, status, provider_reference)`. V15 deliberately does **not** recreate the single-column `provider_reference` index already created by V14 |

Historical payout rows keep their description markers; V14 columns are populated for all new payouts (a one-time historical backfill remains outstanding — see Limitations).

---

## 8. Audit Events

All audit writes go through `AuditService.recordAction` (`@Async`, REQUIRES_NEW) and are best-effort: a failed audit write never fails a payout. Provider-derived text is sanitised before persistence (S10).

| Event | When | Recorded via |
|---|---|---|
| `PAYOUT_INITIATED` | Stage A commit (initiation) | `AuditService.recordAction` → `audit_logs` row (idempotency key deliberately excluded) |
| `PAYOUT_SUCCESS` | Stage C: provider `Successful` | `AuditService.recordAction` |
| `PAYOUT_FAILED_COMPENSATED` | Stage C: provider `Failed` + compensating credit | `AuditService.recordAction` |
| `PAYOUT_PENDING_RECONCILIATION` | Stage C: provider Pending/uncertain | `AuditService.recordAction` |
| `PAYOUT_RECONCILIATION_ALERT` | 24h park-and-alert sweep | `AuditService.recordAction` |
| `PAYOUT_WEBHOOK_QUARANTINED` | Disbursement webhook quarantine (unknown reference / null amount / amount mismatch) | `AuditService.recordAction` |

---

## 9. Cleanup Job (PendingTransactionCleanupJob)

Daily at 03:00 (`@Scheduled(cron = "0 0 3 * * ?")`), reaps `PENDING` transactions older than 24 hours:

- **Payout rows** (`provider_reference IS NOT NULL`): routed into reconciliation — their next status check is made due immediately via the conditional `reschedulePayoutReconciliation` (`next_status_check_at` null or past). They are **never** marked `FAILED` and never refunded by this job, and a payout already parked for manual investigation is skipped (the sentinel is in the future). A payout reaches `FAILED` only through an authoritative provider outcome with a compensating credit — never through this job (risk FIN-05, mitigated; tests: `PendingTransactionCleanupJobTest`).
- **Legacy non-payout pendings** (deposits/transfers awaiting legacy webhook confirmation): retain the historical `FAILED`-after-24h behaviour.

## 10. Known Limitations (genuine, as of Phase 1B)

| # | Limitation | Impact |
|---|---|---|
| L1 | No historical backfill of V14 columns for pre-V14 payout rows | Historical payouts keep description markers only; `provider_reference`/`external_status`/`idempotency_key` stay NULL until a one-time backfill job is run |
| L2 | Quarantined webhook events have no admin alert | Quarantine is logged + audited; only parked payouts alert admins (closure P1B-CLOSE-08) |
| L3 | Platform-wide ZMW assumption; callback payload carries no currency field | Currency check to be added when Lipila sends one (closure P1B-CLOSE-03) |
| L4 | Collections callbacks contained (503) until verified collection initiation | No unverified credits possible today; Phase 1C upgrade (SEC-05 / P1B-CLOSE-09) |
| L5 | Reconciliation scheduler disabled by default | Enablement per environment plus a reconciliation runbook and staging profile are operational prerequisites (P1B-CLOSE-06) |
| L6 | Flyway checksum validation and Java 17 runtime not verified in this environment | `--release 17` compilation verified under JDK 21/25; Flyway CLI unavailable — run `flyway validate` and a JDK 17 runtime check in CI before production (P1B-CLOSE-07, R-10 environment items) |
| L7 | HTTP-level idempotency tests cover 2 of 6 recommended scenarios | Replay and same-key-different-payload covered end-to-end; cross-user key, concurrent HTTP, retry-after-timeout/restart, and replay-while-awaiting-reconciliation remain service-level only (P1B-CLOSE-05) |
| L8 | Idempotency replay returns the original response for a different payload | Accepted design (§4); no payload-hash enforcement exists and none is claimed |
| L9 | Balances are single-entry with derived snapshots | No double-entry ledger exists (FIN-08); Phase 2 work |

No reconciliation auto-refund, no payout auto-failure, no Phase 1C/1D functionality, and no production-readiness claim is made in this document.

---

## References

| File | Relevance |
|---|---|
| `PayoutController.java` | Entry point; `POST /wallets/{walletId}/payouts`; optional `Idempotency-Key`; 201/202/400 mapping |
| `PayoutService.java` | Three-stage orchestration; idempotency (filter + scan + unique index); compensating credit; replay guard; audit |
| `PayoutReconciliationService.java` / `PayoutReconciliationScheduler.java` | Fail-closed scheduler; claim/lease/backoff (F1); park-and-alert; provider calls outside transactions |
| `WebhookController.java` / `WebhookService.java` | Disbursement webhook (raw-body HMAC, timestamp, durable dedup, quarantine/deferred, F2 boundary, F3 400s); collections containment |
| `Transaction.java` / `TransactionStatus.java` | Entity incl. V14 payout columns; `PENDING_RECONCILIATION` |
| `TransactionRepository.java` | Reconciliation queries, version-predicate claim, lease-ownership backoff release, conditional park |
| `PendingTransactionCleanupJob.java` | Stale payouts routed to reconciliation; legacy pendings still failed by age |
| `IdempotencyFilter.java` / `IdempotencyService.java` | Scoped key acquisition, response caching/replay |
| `LipilaGateway.java` / `PaymentGateway.java` | HTTP client (5s connect / 10s read); `initiateDisbursement`, `checkStatus` |
| `db/migration/V13–V15` | Status widening; payout columns + unique idempotency index; webhook event store + reconciliation indexes |
| `backend-risk-register.md` | FIN-01…FIN-12, SEC-01…SEC-12 risk tracking; closure findings tracked as P1B-CLOSE-01…10 in `reports/PHASE_1B_FINAL_REMAINING_RISKS.md` |

---

> **Read-only-provenance note**: This document was corrected in the Phase 1B remediation cycle (D1) to match the actual source. Prior revisions described the Batch-2-only state (no scheduler, no disbursement webhook, no dedicated payout columns) and are superseded by this version.
