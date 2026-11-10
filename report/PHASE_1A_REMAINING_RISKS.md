# NovaWallet — Phase 1A Remaining Risks

**Document:** PHASE_1A_REMAINING_RISKS.md
**Date:** 2026-11-10
**Branch:** feature/phase-1a-financial-guardrails
**Baseline commit:** 60179fb5b09f0d5a11338f07ed2cb23ef393b1f5
**Scope:** All unresolved risks remaining after Phase 1A completion

---

## 1. Purpose

This document lists all unresolved risks, their severity, impact, recommended mitigation, and proposed phase for resolution. Risks are not hidden because the phase is nearly complete. Every risk identified during Phase 0 audit, Phase 1A implementation, independent verification, and PostgreSQL 16 testing is included.

---

## 2. Risk Summary

| Risk ID | Title | Severity | Status | Proposed Phase |
|---|---|---|---|---|
| R-01 | AdminService wallet status race (no pessimistic lock) | Medium | OPEN | Phase 1B |
| R-02 | BalanceRecalculationJob overwrites historical snapshots | Medium | OPEN | Phase 1B (urgent) |
| R-03 | AccountNumberGenerator TOCTOU race | Medium | OPEN | Phase 1B |
| FIN-01 | Direct deposit containment (not resolved) | Critical | TEMPORARILY DISABLED | Phase 3 (Payment Orchestration) |
| FIN-02 | Collection callback containment (not resolved) | Critical | TEMPORARILY DISABLED | Phase 3 (Payment Orchestration) |
| FIN-03 | Payout debits + HTTP in single transaction | High | OPEN | Phase 3 |
| FIN-04 | Provider timeout rolls back debit | High | OPEN | Phase 3 |
| FIN-05 | Pending cleanup marks FAILED without refund | High | OPEN | Phase 3 |
| FIN-07 | BalanceRecalculationJob snapshot overwrite | Medium | OPEN | Phase 1B (urgent) |
| FIN-08 | No double-entry ledger | Medium | OPEN | Phase 2 |
| FIN-09 | Intermediate rounding in fee calculation | Medium | OPEN | Phase 2 |
| FIN-10 | No payout polling/webhook/refund workflows | Medium | OPEN | Phase 3 |
| FIN-11 | Daily send limit per-wallet vs velocity per-user | Low | OPEN | Phase 1B |
| SEC-01 | JwtAuthFilter no enabled/locked check | High | OPEN | Phase 1B |
| SEC-02 | Password change/deactivation no token revocation | High | OPEN | Phase 1B |
| SEC-03 | Plaintext tokens in notifications/logs | High | OPEN | Phase 1B |
| SEC-04 | Hardcoded dev profile default | High | OPEN | Phase 1B |
| SEC-05 | Webhook auth static header only | High | OPEN | Phase 3 |
| SEC-06 | No logout endpoint | Medium | OPEN | Phase 1B |
| SEC-07 | Email verification token no expiry | Medium | OPEN | Phase 1B |
| SEC-08 | JwtUtil no key length/entropy check | Medium | OPEN | Phase 1B |
| SEC-10 | Monetary DTOs lack @Digits/@DecimalMax | Medium | OPEN | Phase 1B |
| SEC-11 | management.info.env.enabled + public Swagger | Medium | OPEN | Phase 1B |
| SEC-12 | RequestTracingFilter unvalidated X-Request-ID | Low | OPEN | Phase 1B |
| ARCH-01 | Cross-domain repository injection | High | OPEN | Phase 4 |
| ARCH-02 | Identity/KYC coupled in users table | High | OPEN | Phase 4/8 |
| ARCH-03 | Sync side effects without outbox | Medium | OPEN | Phase 5 |
| DB-01 | Missing CHECK constraints, FK indexes | Medium | OPEN | Phase 4 |

---

## 3. Detailed Risk Entries

### R-01 — AdminService Wallet Status Race

- **Severity:** Medium
- **Impact:** Financial integrity. Admin status transitions (freeze/unfreeze) are not serialized with money operations. Concurrent operations can observe inconsistent wallet state. Restore path sets wallet to ACTIVE unconditionally, potentially overriding fraud/freeze states.
- **Evidence:** `AdminService.toggleUserDeletedStatus` uses non-locking `findByUserId` + `save`, while `TransactionService.transfer`, `withdraw`, and `PayoutService.payout` use `findByIdWithLock`.
- **Recommended Mitigation:** Use `WalletRepository.findByIdWithLock` in `toggleUserDeletedStatus`. Add precondition checks. Do not override fraud/freeze states on restore. Clear `freezeReason` on unfreeze.
- **Proposed Phase:** Phase 1B (security/financial hardening)
- **Phase 1A Action:** Documented only. Not implemented (outside Phase 1A scope).

### R-02 — BalanceRecalculationJob Overwrites Historical Snapshots

- **Severity:** Medium (recommend elevated to High for audit integrity)
- **Impact:** Financial audit integrity. The job sets `balance_before` and `balance_after` to the wallet's current balance for any transaction row where either is NULL. This destroys the ability to reconstruct the ledger at a point in time. For transfers, it always selects the sender wallet balance (COALESCE sender first), making both snapshots identical.
- **Evidence:** `BalanceRecalculationJob.recalculateMissingBalances()` calls `TransactionRepository.recalculateMissingBalances()` which executes a native UPDATE setting both columns to `COALESCE((SELECT balance FROM wallets WHERE id = t.sender_wallet_id), (SELECT balance FROM wallets WHERE id = t.receiver_wallet_id))`.
- **Schedule:** Daily at 03:00 (`@Scheduled(cron = "0 0 3 * * ?")`). No `@Profile` guard. Runs in production.
- **Recommended Mitigation:** Either (a) disable the job immediately, or (b) redesign to compute correct historical values by replaying the ledger in chronological order per wallet, or (c) leave snapshots NULL and mark as "unavailable". Add a NOT NULL constraint only after verified backfill.
- **Proposed Phase:** Phase 1B (urgent — should be disabled or fixed before production use)
- **Phase 1A Action:** Documented only. Job not run during verification.

### R-03 — AccountNumberGenerator TOCTOU Race

- **Severity:** Medium
- **Impact:** Account creation. The existence check (`walletRepository.existsByAccountNumber`) is vulnerable to TOCTOU races. Two concurrent KYC approvals can both pass the check, then one insert fails on the unique constraint. The retry loop does not catch `DataIntegrityViolationException`, so the failure propagates.
- **Evidence:** `AccountNumberGenerator.generate()` uses random 10-digit generation with existence check. DB unique index (`idx_wallets_account_number`) guarantees uniqueness at DB level, but the application does not handle constraint violations gracefully.
- **Recommended Mitigation:** Wrap insert in try/catch for `DataIntegrityViolationException` and retry. Or use a database sequence for account numbers. Increase retry attempts or use a larger random space.
- **Proposed Phase:** Phase 1B
- **Phase 1A Action:** Documented only. Not implemented.

### FIN-01 — Direct Deposit Containment (Not Resolved)

- **Severity:** Critical
- **Impact:** Money creation. Endpoint remains registered but returns 503. No verified collection workflow exists.
- **Status:** TEMPORARILY DISABLED
- **Recommended Mitigation:** Implement verified collection initiation (Phase 3).
- **Proposed Phase:** Phase 3 (Payment Orchestration)

### FIN-02 — Collection Callback Containment (Not Resolved)

- **Severity:** Critical
- **Impact:** Arbitrary crediting. Callback authenticates via static secret, validates payload, then returns 503. No collection initiation/verification workflow.
- **Status:** TEMPORARILY DISABLED
- **Recommended Mitigation:** Implement collection initiation, provider verification, amount/currency match, idempotency persistence (Phase 3).
- **Proposed Phase:** Phase 3 (Payment Orchestration)

### FIN-03 — Payout Transaction Boundary

- **Severity:** High
- **Impact:** Connection pool exhaustion, deadlock risk. Payout debits wallet and calls HTTP provider inside single @Transactional.
- **Recommended Mitigation:** Split into two phases: short transaction (debit + PENDING intent), out-of-transaction HTTP call, follow-up transaction (settle). Use outbox/saga.
- **Proposed Phase:** Phase 3 (Payment Orchestration)

### FIN-04 — Provider Timeout During Payout

- **Severity:** High
- **Impact:** Double-spend or orphaned funds. Gateway timeout rolls back debit even if provider accepted.
- **Recommended Mitigation:** Persist PENDING intent before HTTP call. On timeout, leave PENDING and reconcile. Never roll back a debit that may have been accepted externally.
- **Proposed Phase:** Phase 3 (Payment Orchestration)

### FIN-05 — Pending Cleanup Without Refund

- **Severity:** High
- **Impact:** Permanent loss of customer funds. Stale PENDING payouts marked FAILED without refund.
- **Recommended Mitigation:** Verify provider status before failing. Issue compensating credit on failure. Make job idempotent and auditable.
- **Proposed Phase:** Phase 3 (Payment Orchestration)

### FIN-07 — BalanceRecalculationJob (same as R-02)

- **Severity:** Medium
- **Status:** Confirmed Defect
- **See:** R-02 above.

### FIN-08 — No Double-Entry Ledger

- **Severity:** Medium
- **Impact:** No audit trail of debits/credits. Single-entry transaction table.
- **Proposed Phase:** Phase 2 (Double-Entry Accounting Ledger)

### FIN-09 — Intermediate Rounding in Fee Calculation

- **Severity:** Medium
- **Impact:** Financial accuracy. Percentage fee calculation rounds intermediate values.
- **Proposed Phase:** Phase 2

### FIN-10 — No Payout Polling/Webhook/Refund Workflows

- **Severity:** Medium
- **Impact:** No reconciliation possible for pending/failed payouts.
- **Proposed Phase:** Phase 3

### FIN-11 — Daily Send Limit Per-Wallet vs Velocity Per-User

- **Severity:** Low
- **Impact:** Unverified users may bypass tier limits.
- **Proposed Phase:** Phase 1B

### SEC-01 — JwtAuthFilter No Enabled/Locked Check

- **Severity:** High
- **Impact:** Deactivated users retain API access until JWT expires.
- **Proposed Phase:** Phase 1B

### SEC-02 — No Token Revocation on Password Change/Deactivation

- **Severity:** High
- **Impact:** Stolen refresh tokens remain valid after password change.
- **Proposed Phase:** Phase 1B

### SEC-03 — Plaintext Tokens in Notifications/Logs

- **Severity:** High
- **Impact:** Account takeover via token leakage.
- **Proposed Phase:** Phase 1B

### SEC-04 — Hardcoded Dev Profile Default

- **Severity:** High
- **Impact:** Accidental dev mode in production.
- **Proposed Phase:** Phase 1B

### SEC-05 — Webhook Auth Static Header Only

- **Severity:** High
- **Impact:** No HMAC/timestamp/nonce protection.
- **Proposed Phase:** Phase 3

### SEC-06 — No Logout Endpoint

- **Severity:** Medium
- **Impact:** Users cannot terminate sessions server-side.
- **Proposed Phase:** Phase 1B

### SEC-07 — Email Verification Token No Expiry

- **Severity:** Medium
- **Impact:** Unbounded window for verification bypass.
- **Proposed Phase:** Phase 1B

### SEC-08 — JwtUtil No Key Length/Entropy Check

- **Severity:** Medium
- **Impact:** Weak secrets accepted silently.
- **Proposed Phase:** Phase 1B

### SEC-10 — Monetary DTOs Lack Bounds

- **Severity:** Medium
- **Impact:** Unbounded amounts, potential DB overflow.
- **Proposed Phase:** Phase 1B

### SEC-11 — management.info.env.enabled + Public Swagger

- **Severity:** Medium
- **Impact:** Information disclosure, expanded attack surface.
- **Proposed Phase:** Phase 1B

### SEC-12 — RequestTracingFilter Unvalidated X-Request-ID

- **Severity:** Low
- **Impact:** Log injection.
- **Proposed Phase:** Phase 1B

### ARCH-01 — Cross-Domain Repository Injection

- **Severity:** High
- **Impact:** Coupling, slower changes, blocks extraction.
- **Proposed Phase:** Phase 4

### ARCH-02 — Identity/KYC Coupled in Users Table

- **Severity:** High
- **Impact:** Duplicate wallets, migration risk.
- **Proposed Phase:** Phase 4/8

### ARCH-03 — Sync Side Effects Without Outbox

- **Severity:** Medium
- **Impact:** Money movement fails due to notification infrastructure.
- **Proposed Phase:** Phase 5

### DB-01 — Missing CHECK Constraints, FK Indexes

- **Severity:** Medium
- **Impact:** Data integrity at DB level.
- **Proposed Phase:** Phase 4

---

## 4. Security Findings Classification (Phase 1A Review)

| Finding | Classification | Notes |
|---|---|---|
| Unused WebhookService fields (transactionRepository, transactionService) | Harmless cleanup | Dead code after containment. No security impact. |
| Outdated webhook Javadoc | Harmless cleanup | Documentation inaccuracy. No functional impact. |
| Callback validation ordering (secret → service → payload → walletId → status → amount → wallet lookup → 503) | Correctness issue (minor) | Amount validation occurs before unknown-wallet 404 on Successful callbacks. No security vulnerability. |
| UUID validation in extractWalletId | Security concern (minor) | Parses segments[1] as UUID. Third segment not validated. No exploitable vulnerability. |

---

## 5. Phase 1A Scope Compliance

All work performed was within Phase 1A scope:
- FIN-01: Contained (not resolved)
- FIN-02: Contained (not resolved)
- FIN-06: Verified
- FIN-12: Mitigated + PostgreSQL 16 verified
- OPS-01: Verified

No prohibited work was performed:
- No double-entry ledger
- No payment orchestration
- No collection initiation state machine
- No payout lifecycle redesign
- No new payment-provider integrations
- No RabbitMQ/Kafka/Redis
- No microservice extraction
- No frontend changes
- No Java/Spring upgrades
- No unrelated migrations or refactoring

---

## 6. Unresolved Risks Summary

**Total unresolved risks:** 28 (including 3 new observations from Phase 1A verification: R-01, R-02, R-03)

**By severity:**
- Critical: 2 (FIN-01, FIN-02 — contained)
- High: 9 (FIN-03, FIN-04, FIN-05, SEC-01, SEC-02, SEC-03, SEC-04, SEC-05, ARCH-01, ARCH-02)
- Medium: 13 (R-01, R-02, R-03, FIN-07, FIN-08, FIN-09, FIN-10, SEC-06, SEC-07, SEC-08, SEC-10, SEC-11, ARCH-03, DB-01)
- Low: 3 (FIN-11, SEC-12)

**Recommended immediate actions (before production):**
1. Disable or redesign BalanceRecalculationJob (R-02) — can corrupt audit history.
2. Fix AdminService wallet status race (R-01) — can cause inconsistent state.
3. Fix AccountNumberGenerator TOCTOU (R-03) — can fail under concurrency.