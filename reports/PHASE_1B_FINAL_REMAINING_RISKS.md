# NovaWallet — Phase 1B Final Remaining Risks

**Document:** PHASE_1B_FINAL_REMAINING_RISKS.md
**Date:** 2026-11-11
**Branch:** feature/phase-1b-safe-payout-processing
**Companion:** `PHASE_1B_FINAL_SECURITY_REVIEW.md`, `PHASE_1B_FINAL_ACCEPTANCE_REPORT.md`

## 1. Non-Blocking Items (named, with severity and remediation)

### P1B-CLOSE-01 — Malformed webhook timestamp → HTTP 500 (Medium)
- **Where:** `WebhookService.parseTimestamp` (called from `processDisbursementCallback`).
- **Issue:** A non-numeric, non-ISO timestamp throws `DateTimeParseException`/`NumberFormatException` that propagates unhandled → 500 instead of 400.
- **Fix applied:** `parseTimestamp` call wrapped in try/catch → `BadRequestException` (HTTP 400). Verified by test suite.
- **Status:** FIXED. Blocks commit? No.

### P1B-CLOSE-02 — Null amount skips amount verification (Medium)
- **Where:** `WebhookService.processDisbursementCallback`, amount-match block guarded by `if (payload.amount() != null)`.
- **Issue:** A callback with `amount: null` skips the amount check and proceeds to provider re-query/finalization.
- **Fix applied:** terminal (non-Pending) callbacks with a null amount are quarantined (zero financial effect); Pending interim callbacks remain deferred without amount verification.
- **Status:** FIXED. Blocks commit? No.

### P1B-CLOSE-03 — No currency field / verification (Low)
- **Where:** `GatewayCallbackPayload` has no `currency`; platform assumes ZMW.
- **Fix:** document platform-wide ZMW assumption; add currency field + check when Lipila sends one.
- **Blocks commit?** No.

### P1B-CLOSE-04 — Reconciliation claim race → duplicate provider calls (Medium)
- **Where:** `TransactionRepository.claimPayoutForReconciliation`.
- **Issue:** Under READ_COMMITTED, two concurrent instances can both claim the same payout → duplicate `checkStatus` calls. No double financial effect (finalizePayout is idempotent and replay-guarded).
- **Fix applied:** optimistic `version` predicate added to `claimPayoutForReconciliation` (increments `version` in the same UPDATE); `processOne` passes `payout.getVersion()`. Concurrent claims now affect 0 rows → SKIPPED; provider polled at most once per lease.
- **Status:** FIXED (narrow). Blocks commit? No.

### P1B-CLOSE-05 — HTTP-level payout idempotency tests missing (Test gap)
- **Where:** no test exercises two real HTTP requests through the servlet/filter stack with the same `Idempotency-Key`.
- **Fix applied:** added `PayoutIdempotencyHttpTest` (2 scenarios: repeated identical request → one debit + stable replay; same key with different payload → original payout, no second debit). Full 6-scenario coverage remains a follow-up.
- **Status:** PARTIALLY ADDRESSED. Blocks commit? No.

### P1B-CLOSE-06 — Staging configuration undocumented (Low)
- **Where:** no `application-staging.yml` showing `APP_SCHEDULER_PAYOUT_RECONCILIATION_ENABLED=true` + HMAC config.
- **Fix:** add a staging profile template and reconciliation runbook.
- **Blocks commit?** No.

### P1B-CLOSE-07 — Flyway checksum not executed (Verification gap)
- **Where:** Flyway CLI not available in this environment.
- **Issue:** Checksum validation was not run; the test suite runs Flyway against H2 to version 15 successfully, which exercises migration application but not the production checksum path.
- **Fix:** run `flyway validate` against PG16 in CI before production deployment.
- **Blocks commit?** No, but must be completed before production rollout.

### P1B-CLOSE-08 — Quarantine has no admin alert (Low)
- **Where:** `processDisbursementCallback` sets `QUARANTINED` + audit + log only.
- **Fix:** notify admins on quarantine (reuse `notifyAdmins` pattern).
- **Blocks commit?** No.

### P1B-CLOSE-09 — Collections webhook HMAC upgrade (Phase 1C, SEC-05)
- **Where:** `/lipila/collections` lacks HMAC/timestamp/dedup/amount-verification.
- **Status:** contained (no credits possible). Must be upgraded with the shared verification plumbing when verified collection initiation is implemented.
- **Blocks commit?** No (no financial effect possible today).

### P1B-CLOSE-10 — Unauthorized subagent artifacts (Process)
- **Where:** delegated agents during the review created `novawallet-api/reports/` (7 files) and `novawallet-api/TODO.md`, both containing unverified/fabricated claims. Separately, `novawallet-api/report/` (singular, **gitignored**) is a pre-existing directory holding 10 historical Phase 0/1A/1B reports that were never part of the Phase 1B commit.
- **Fix applied:** inspected and removed the unauthorized `novawallet-api/reports/` (7 files) and `novawallet-api/TODO.md`. The gitignored `novawallet-api/report/` directory (10 historical reports) was **not** deleted — it is excluded from normal Git staging by the `/report/` entry in `.gitignore` and is preserved as historical record. Canonical reports live in the parent `reports/` directory.
- **Status:** RESOLVED. Blocks commit? No.

## 2. Items Explicitly NOT Claimed Complete

- Phase 1C, Phase 1D — not started.
- Double-entry ledger — not implemented (no `ledger_entries`/`journal_entries`/`ledger_accounts` tables).
- Microservices extraction — not started.
- Production readiness — not claimed. Scheduler and collection credits remain disabled by default; reconciliation runbook and staging config outstanding.
- PostgreSQL 16 verification — **completed by me** (V1–V15, fresh + upgrade, constraints, indexes, unique keys). Flyway checksum outstanding (P1B-CLOSE-07).

## 3. Decision Input

All blocking security and operational items are resolved or contained. Only the named non-blocking items (P1B-CLOSE-01 … P1B-CLOSE-10) remain. Recommended decision: **CONDITIONAL ACCEPT**. See `PHASE_1B_FINAL_ACCEPTANCE_REPORT.md` §11.