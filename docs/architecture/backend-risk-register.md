# NovaWallet — Backend Risk Register

**Document:** `backend-risk-register.md`
**Scope:** Backend (`novawallet-api`) financial, security, architectural, database, and operational risks
**Phase:** Phase 0 audit consolidation (DOC-3)
**Severity scale:** **Critical**, **High**, **Medium**, **Low**
**Verification statuses:** **Confirmed** (reproduced / verified by code inspection) vs **Design Weakness** (architectural concern, not a directly exploitable defect today)
**Classification:** All credentials, secrets, tokens and sensitive values are redacted as `[REDACTED]`. No source code, tests, migrations, or configuration files were modified to produce this document.

---

## 1. Purpose & Scope

This register consolidates every verified finding from the Phase 0 backend audit into a single, severity-ranked, traceable risk inventory. Each entry is actionable: it names the exact file(s), class(es), method(s) and line number(s), describes the actual runtime behaviour, the failure/exploit scenario, the financial/business consequence, a concrete correction, and a suggested verification test case.

Two categories are kept strictly separate:

1. **Confirmed Vulnerabilities / Defects** — behaviours that are demonstrably wrong or exploitable in the current code.
2. **Potential Architectural & Design Weaknesses** — structural or design-level concerns that are not directly exploitable today but materially increase risk as the platform scales or moves to live money.

A summary risk matrix is provided at the top (§2) and repeated at the bottom (§9) for quick reference.

---

## 2. Summary Risk Matrix (Top)

| Risk ID | Title | Category | Severity | Verification Status |
|---|---|---|---|---|
| FIN-01 | Direct unverified wallet deposit endpoint (arbitrary self-crediting) | Financial | Critical | TEMPORARILY DISABLED |
| FIN-02 | Webhook collection callback trusts unverified payload amount | Financial | Critical | TEMPORARILY DISABLED |
| FIN-03 | Payout debits wallet and calls external provider inside single `@Transactional` | Financial | High | Confirmed Vulnerability |
| FIN-04 | Provider timeout/error during payout rolls back debit despite possible disbursement | Financial | High | Confirmed Vulnerability |
| FIN-05 | PendingTransactionCleanupJob marks PENDING payouts FAILED without refund | Financial | High | Confirmed Vulnerability |
| FIN-06 | Internal transfer acquires pessimistic locks in caller-supplied order (deadlock) | Financial | High | VERIFIED |
| FIN-07 | BalanceRecalculationJob overwrites historical snapshots with current balance | Financial | Medium | Confirmed Defect |
| FIN-08 | Single-entry Transaction table + mutable wallets.balance instead of double-entry ledger | Financial | Medium | Design Weakness |
| FIN-09 | Intermediate rounding to scale 2 in percentage fee calculation | Financial | Medium | Design Weakness |
| FIN-10 | Missing payout status polling, disbursement webhook, refund/reversal workflows | Financial | Medium | Design Weakness |
| FIN-11 | Daily send limit per-wallet vs velocity per-user; unverified users bypass tier limits | Financial | Low | Design Weakness |
| SEC-01 | JwtAuthFilter does not check enabled/locked/soft-delete status | Security | High | Confirmed Vulnerability |
| SEC-02 | Password change / admin deactivation do not revoke refresh tokens | Security | High | Confirmed Vulnerability |
| SEC-03 | Plaintext email-verification & password-reset tokens in Notification + logs | Security | High | Confirmed Vulnerability |
| SEC-04 | `spring.profiles.active: dev` hardcoded as default in base application.yml | Security | High | Confirmed Misconfiguration |
| SEC-05 | Webhook auth = static header only; no HMAC/timestamp/nonce; string-match dedup | Security | High | Design Weakness |
| SEC-06 | No logout endpoint exposed despite AuthService.logout() existing | Security | Medium | Confirmed Weakness |
| SEC-07 | Email verification token has no expiration timestamp | Security | Medium | Confirmed Weakness |
| SEC-08 | JwtUtil does not enforce minimum key length/entropy or iss/aud claims | Security | Medium | Confirmed Weakness |
| SEC-09 | RateLimitFilter & LoginRateLimiter in-memory per-JVM, proxy-header unaware | Security | Medium | Design Weakness |
| SEC-10 | Monetary DTOs validate @Positive but lack @Digits/@DecimalMax upper bounds | Security | Medium | Confirmed Weakness |
| SEC-11 | management.info.env.enabled + public Swagger + CORS wildcard headers w/ credentials | Security | Medium | Confirmed Misconfiguration |
| SEC-12 | RequestTracingFilter accepts unvalidated X-Request-ID; KycService trusts client MIME | Security | Low | Confirmed Weakness |
| ARCH-01 | Extensive cross-domain repository injection | Architectural | High | Architectural |
| ARCH-02 | Identity and KYC state coupled in users table / JPA @ManyToOne | Architectural | High | Architectural/Database |
| ARCH-03 | Synchronous in-process side effects without domain events / outbox | Architectural | Medium | Architectural |
| DB-01 | Missing DB CHECK constraints, FK indexes, leftover test table, default mismatch | Database | Medium | Database |

---

## 3. Confirmed Vulnerabilities / Defects

### 3.1 Financial — Confirmed

#### FIN-01 — Direct unverified wallet deposit endpoint (arbitrary self-crediting)

- **Risk ID:** FIN-01
- **Title:** Direct unverified wallet deposit endpoint allows arbitrary self-crediting
- **Category:** Financial
- **Severity:** Critical
- **Verification Status:** TEMPORARILY DISABLED
- **Exact File Path(s):**
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/transaction/controller/TransactionController.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/transaction/service/TransactionService.java`
- **Class & Method Name(s):** `TransactionController.deposit(...)`; `TransactionService.deposit(UUID, DepositRequest, UUID)`
- **Exact Line Numbers:** `TransactionController.java:58-73`; `TransactionService.java:89-141`
- **Actual Code Behaviour:** `POST /api/v1/wallets/{walletId}/deposit` accepts a client-supplied `DepositRequest.amount` and immediately calls `walletRepository.updateBalance(walletId, amount)` (line 102), writes a `Transaction` row of type `DEPOSIT` with status `SUCCESSFUL` (lines 112-123), and notifies the user. There is no payment-provider verification, no collection initiation record, no PIN, no idempotency of its own, and no email-verified gate. The KYC wallet-limit check (`enforceDepositWalletLimit`) runs only after the amount is already accepted and does not prevent crediting.
- **Failure / Exploit Scenario:** Any authenticated user creates a wallet (via KYC approval) and calls the deposit endpoint with an arbitrary amount (e.g. 1,000,000.00), instantly crediting their own balance. The funds appear real in statements and can be transferred/paid out once payouts are enabled, draining the merchant float.
- **Financial / Business Consequence:** Unbounded money creation in the ledger; total liabilities exceed assets; regulatory/fraud exposure; potential total loss of merchant funds once payouts go live.
- **Concrete Recommended Correction:** Remove or gate the endpoint behind a demo-only profile flag; in production, deposits must originate from a verified provider collection (see FIN-02 / Wave 5 W5.2). At minimum require an admin/system principal, an idempotency key, and a provider reference. Add server-side max-amount ceiling.
- **Suggested Verification Test Case:** Integration test asserting that a non-admin JWT calling `POST /api/v1/wallets/{id}/deposit` returns 403/404 in a `prod` profile, and that no `Transaction` row is created. Property test fuzzing `amount` values (negative, zero, huge, fractional) expecting rejection.

#### FIN-02 — Webhook collection callback trusts unverified payload amount

- **Risk ID:** FIN-02
- **Title:** Webhook collection callback trusts unverified payload amount without matching an initiated collection record or currency
- **Category:** Financial
- **Severity:** Critical
- **Verification Status:** TEMPORARILY DISABLED
- **Exact File Path(s):**
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/payment/controller/WebhookController.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/payment/service/WebhookService.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/payment/PaymentGateway.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/payment/LipilaGateway.java`
- **Class & Method Name(s):** `WebhookController.lipilaCollectionCallback(...)`; `WebhookService.processCollectionCallback(GatewayCallbackPayload)`; `PaymentGateway.initiateCollection(...)`; `LipilaGateway.initiateCollection(...)`
- **Exact Line Numbers:** `WebhookController.java:64-99`; `WebhookService.java:93-181`; `PaymentGateway.java:22-29`; `LipilaGateway.java:94-97`
- **Actual Code Behaviour:** `initiateCollection` (the outbound collection call) is never invoked by any application service or controller (verified by grep). The inbound callback `processCollectionCallback` parses `NW:<walletId>:<uuid>` from `payload.referenceId()`, takes the amount directly from `payload.getAmount()` (line 107), and credits it via `transactionService.deposit(...)` (line 124). There is no comparison against an initiated collection record, no currency check (the gateway hardcodes `"ZMW"` only on the payout path), and deduplication relies on `existsByDescription` string matching (lines 116-120). Authentication is only a static `X-Webhook-Secret` header (controller lines 83-99).
- **Failure / Exploit Scenario:** Anyone who obtains the shared webhook secret (or if it is unset/weak) POSTs `{ referenceId: "NW:<victimWalletId>:<any-uuid>", status: "Successful", amount: 1000000 }` and credits arbitrary funds. Because no collection was ever initiated, there is no authoritative amount/currency to compare against. Concurrent duplicates race on the `description` string match.
- **Financial / Business Consequence:** Arbitrary wallet credit by anyone holding (or guessing) the secret; total loss of funds integrity; no reconciliation possible.
- **Concrete Recommended Correction:** Implement Wave 5 W5.1-W5.3: a `Payment` entity with a unique provider reference and initiated amount/currency; an initiation endpoint that creates the record; the webhook must look up the record by `referenceId` and credit only the recorded amount/currency, rejecting mismatches. Add a DB unique constraint on the provider reference. Replace string-match dedup with a unique constraint / idempotency key.
- **Suggested Verification Test Case:** Integration test: callback with amount differing from initiated record → 400/409 and no credit; callback with unknown referenceId → 404; duplicate callback → no second credit (assert balance unchanged and only one `Transaction` row).

#### FIN-03 — Payout debits wallet and calls external provider inside single @Transactional

- **Risk ID:** FIN-03
- **Title:** Payout workflow debits wallet and invokes external HTTP provider inside a single `@Transactional` method
- **Category:** Financial
- **Severity:** High
- **Verification Status:** Confirmed Vulnerability
- **Exact File Path(s):** `novawallet-api/src/main/java/com/novawallet/novawallet_api/payment/service/PayoutService.java`
- **Class & Method Name(s):** `PayoutService.payout(UUID, PayoutRequest, UUID)`
- **Exact Line Numbers:** `PayoutService.java:106-212` (method); `@Transactional` at 107; `walletRepository.updateBalance` at 125; `paymentGateway.initiateDisbursement` at 133 (via 222-239)
- **Actual Code Behaviour:** The entire method is annotated `@Transactional` (line 107). It acquires a `PESSIMISTIC_WRITE` row lock on the wallet (line 111 via `findByIdWithLock`), debits the wallet (line 125), then calls `paymentGateway.initiateDisbursement` (line 133) which performs an HTTP POST with up to 5s connect + 10s read timeout (LipilaGateway lines 44-47). The DB row lock is held for the duration of the network I/O (up to 15s).
- **Failure / Exploit Scenario:** Under concurrent payouts from the same wallet, all but one block on the pessimistic lock for up to 15s, exhausting the Hikari pool (10 connections) and servlet threads. A slow/gateway timeout stalls the whole payment path. The lock is held across a network boundary, violating the "no I/O inside transactions" rule.
- **Financial / Business Consequence:** Throughput collapse; pool exhaustion → cascading 503s for unrelated endpoints; potential deadlock with other transactions wanting the wallet row.
- **Concrete Recommended Correction:** Split into two phases: (1) a short transaction that validates guards, debits the wallet, and persists a `PENDING` payout intent (with provider reference); (2) an out-of-transaction HTTP call; (3) a follow-up transaction recording the result and, on failure, a compensating credit. Use an outbox/saga pattern. Never hold a DB lock across network I/O.
- **Suggested Verification Test Case:** Load test: N concurrent payouts on one wallet must not exhaust the connection pool; assert max lock-hold time < 100ms. Chaos test: gateway returns after 12s → local intent remains PENDING, no pool starvation.

#### FIN-04 — Provider timeout/error during payout rolls back debit despite possible disbursement

- **Risk ID:** FIN-04
- **Title:** External provider timeout or network error during payout triggers full database rollback of wallet debit even if Lipila accepted and disbursed the payout
- **Category:** Financial
- **Severity:** High
- **Verification Status:** Confirmed Vulnerability
- **Exact File Path(s):** `novawallet-api/src/main/java/com/novawallet/novawallet_api/payment/service/PayoutService.java`
- **Class & Method Name(s):** `PayoutService.payout(...)`; `PayoutService.initiateDisbursement(...)`
- **Exact Line Numbers:** `PayoutService.java:199-240` (helpers); catch block 232-239; class javadoc 50-54
- **Actual Code Behaviour:** `initiateDisbursement` (lines 222-239) catches any `RuntimeException` from `paymentGateway.initiateDisbursement`, logs it, and rethrows as `BadRequestException`. Because the method is `@Transactional` (FIN-03), the exception rolls back the wallet debit (line 125) and any `Transaction` rows. If Lipila actually accepted the request before the timeout, real money leaves the merchant float while NovaWallet has no local record and the customer's balance is restored.
- **Failure / Exploit Scenario:** Gateway accepts the disbursement, then the response is lost to a network blip/timeout. NovaWallet rolls back → customer keeps the money in-wallet AND the provider pays out → double spend / unbacked payout. Or the reverse: customer is charged nothing locally but money leaves the float → orphaned funds.
- **Financial / Business Consequence:** Lost merchant float; unrecoverable discrepancy between provider and ledger; requires manual reconciliation; regulatory reporting impact.
- **Concrete Recommended Correction:** Persist a `PENDING` payout intent (with provider reference) in a committed transaction before the HTTP call; on timeout, leave the intent `PENDING` and surface it to reconciliation (poll `checkStatus`, implement disbursement webhook). Never roll back a debit that may have been accepted externally. Add a reconciliation job.
- **Suggested Verification Test Case:** Test: mock gateway throws `ResourceAccessException` after recording the call → assert a `PENDING` `Transaction` row exists with the provider reference and the wallet remains debited. Assert `checkStatus` is invoked by a reconciliation job within SLA.

#### FIN-05 — PendingTransactionCleanupJob marks PENDING payouts FAILED without refund

- **Risk ID:** FIN-05
- **Title:** `PendingTransactionCleanupJob` marks `PENDING` transactions older than 24h as `FAILED` without refunding/reversing debited payout funds
- **Category:** Financial
- **Severity:** High
- **Verification Status:** Confirmed Vulnerability
- **Exact File Path(s):**
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/transaction/schedule/PendingTransactionCleanupJob.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/transaction/repository/TransactionRepository.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/payment/service/PayoutService.java`
- **Class & Method Name(s):** `PendingTransactionCleanupJob.cleanupStalePendingTransactions()`; `TransactionRepository.findPendingOlderThan(...)`; `PayoutService.payout(...)`
- **Exact Line Numbers:** `PendingTransactionCleanupJob.java:38-55`; `TransactionRepository.java:67-70` (actually 76-81) `findPendingOlderThan`; `PayoutService.payout` debits at line 125
- **Actual Code Behaviour:** `PayoutService.payout` debits the wallet immediately (line 125) even when the gateway returns `Pending` (status stays `PENDING`, line 147). `PendingTransactionCleanupJob` (lines 38-55) selects rows with status `PENDING` older than 24h (`findPendingOlderThan`, lines 76-81) and sets their status to `FAILED` (lines 49-52) without restoring the debited balance or writing a compensating credit.
- **Failure / Exploit Scenario:** A payout that stays `PENDING` (gateway never calls back, no status polling exists — FIN-10) is silently flipped to `FAILED` after 24h. The customer's debited balance is never restored, permanently orphaning customer funds. Conversely, if the provider later completes it, money moves with no local record.
- **Financial / Business Consequence:** Permanent loss of customer funds (orphaned debits); unreconciled provider-side completions; customer disputes and regulatory exposure.
- **Concrete Recommended Correction:** Before marking a stale `PENDING` payout `FAILED`, verify provider status via `checkStatus`; if still pending/unknown, do not fail it automatically (escalate to ops). If failing, issue a compensating wallet credit and a reversal `Transaction` row, and emit an audit entry. Make the job idempotent and auditable.
- **Suggested Verification Test Case:** Test: seed a `PENDING` payout row 25h old with a known wallet debit; run the job with a mocked gateway returning `Pending` → row remains `PENDING` and balance unchanged. With gateway returning `Failed` → row becomes `FAILED` and balance restored. Assert audit row written.

#### FIN-06 — Internal transfer acquires pessimistic locks in caller-supplied order

- **Risk ID:** FIN-06
- **Title:** Internal wallet transfer acquires pessimistic locks in caller-supplied order, enabling database deadlocks on concurrent opposite-direction transfers
- **Category:** Financial
- **Severity:** High
- **Verification Status:** VERIFIED
- **Exact File Path(s):** `novawallet-api/src/main/java/com/novawallet/novawallet_api/transaction/service/TransactionService.java`
- **Class & Method Name(s):** `TransactionService.transfer(TransferRequest, UUID)`
- **Exact Line Numbers:** `TransactionService.java:227-248`
- **Actual Code Behaviour:** The method locks the sender wallet first (line 243, `findByIdWithLock(senderWallet.getId())`) then the receiver wallet second (line 247, `findByIdWithLock(receiverWalletId)`). Lock order is determined by the transfer direction supplied by the caller, not by a global canonical order (e.g. UUID comparison).
- **Failure / Exploit Scenario:** User A transfers to user B while user B transfers to user A concurrently. Thread 1 locks wallet A then waits for wallet B; thread 2 locks wallet B then waits for wallet A → classic deadlock. PostgreSQL detects the deadlock and aborts one transaction (rolling back the debit), surfacing as a 500 to the user and a partial failure. Repeated under load this causes transaction failures and retries.
- **Financial / Business Consequence:** Failed transfers under concurrent load; potential partial state if exception handling is incomplete; poor UX and support load; possible retry storms.
- **Concrete Recommended Correction:** Acquire locks in a canonical global order (e.g. compare wallet UUIDs and lock the lower UUID first, then the higher), independent of transfer direction. Document the ordering. Add a deadlock-retry policy at the service boundary as a safety net.
- **Suggested Verification Test Case:** `ConcurrentTransferIntegrationTest` extended: two threads performing A→B and B→A simultaneously for N iterations; assert zero deadlocks/timeouts and final balances equal expected. Measure lock wait time.

#### FIN-07 — BalanceRecalculationJob overwrites historical snapshots with current balance

- **Risk ID:** FIN-07
- **Title:** `BalanceRecalculationJob` overwrites missing historical `balance_before`/`balance_after` snapshots with the wallet's current balance
- **Category:** Financial
- **Severity:** Medium
- **Verification Status:** Confirmed Defect
- **Exact File Path(s):**
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/notification/schedule/BalanceRecalculationJob.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/transaction/repository/TransactionRepository.java`
- **Class & Method Name(s):** `BalanceRecalculationJob.recalculateMissingBalances()`; `TransactionRepository.recalculateMissingBalances()`
- **Exact Line Numbers:** `BalanceRecalculationJob.java:26-33`; `TransactionRepository.java:83-96`
- **Actual Code Behaviour:** The native UPDATE (lines 83-96) sets `balance_before` and `balance_after` for any row where either is NULL to the wallet's *current* balance (via the correlated subquery on `wallets.balance`), not the balance at the time the transaction occurred.
- **Failure / Exploit Scenario:** Any historical transaction missing snapshots (e.g. legacy/imported rows) gets its audit snapshot rewritten to the present balance, destroying the ability to reconstruct the ledger at a point in time. Re-running the job makes the corruption permanent and idempotent.
- **Financial / Business Consequence:** Loss of audit trail integrity; inability to reconcile historical statements; forensic/compliance exposure.
- **Concrete Recommended Correction:** Do not backfill with the current balance. Either compute the correct historical values by replaying the ledger in chronological order (per wallet), or leave them NULL and mark them as "unavailable". Add a NOT NULL constraint only after backfill is verified. Add a unit test asserting the job never changes a non-null snapshot.
- **Suggested Verification Test Case:** Test: insert a transaction with NULL snapshots for a wallet whose current balance differs from the historical value; run the job; assert snapshots remain NULL (or are computed correctly by replay) and that no existing non-null snapshot changed.

### 3.2 Security — Confirmed

#### SEC-01 — JwtAuthFilter does not check enabled/locked/soft-delete status

- **Risk ID:** SEC-01
- **Title:** `JwtAuthFilter` authenticates requests from valid JWTs without checking `userDetails.isEnabled()`, `isAccountNonLocked()`, or soft-delete status
- **Category:** Security
- **Severity:** High
- **Verification Status:** Confirmed Vulnerability
- **Exact File Path(s):**
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/security/JwtAuthFilter.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/user/service/CustomUserDetailsService.java` (referenced)
- **Class & Method Name(s):** `JwtAuthFilter.doFilterInternal(...)`; `CustomUserDetailsService.loadUserById(...)`
- **Exact Line Numbers:** `JwtAuthFilter.java:36-68`
- **Actual Code Behaviour:** On a valid JWT, the filter loads `UserDetails` and sets the authentication in the security context (lines 45-61) without ever calling `userDetails.isEnabled()`, `isAccountNonLocked()`, `isAccountNonExpired()`, or `isCredentialsNonExpired()`. Soft-deleted/deactivated users therefore retain active API access until the JWT expires (15 min).
- **Failure / Exploit Scenario:** An admin deactivates a compromised or terminated user; the user's already-issued access token keeps working for up to 15 minutes, allowing continued money movement and data access. Combined with SEC-02 (no refresh-token revocation), the user can also mint new access tokens.
- **Financial / Business Consequence:** Deactivated/compromised accounts remain operable; fraud and data-exfiltration window; undermines admin freeze controls.
- **Concrete Recommended Correction:** After `loadUserById`, check `isEnabled()` and `isAccountNonLocked()`; if false, do not set authentication (and optionally clear the context), causing downstream 401/403. Add `isAccountNonExpired`/`isCredentialsNonExpired` checks. Ensure `CustomUserDetailsService` maps soft-delete/locked flags onto the `UserDetails`.
- **Suggested Verification Test Case:** Test: deactivate a user, use their (still-valid) access token → expect 401/403 on a protected endpoint. Test: locked user → 403. Regression test in `AdminSecurityIntegrationTest`.

#### SEC-02 — Password change / admin deactivation do not revoke refresh tokens

- **Risk ID:** SEC-02
- **Title:** Password change and admin user deactivation do not revoke active refresh tokens
- **Category:** Security
- **Severity:** High
- **Verification Status:** Confirmed Vulnerability
- **Exact File Path(s):**
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/user/service/UserService.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/admin/service/AdminService.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/auth/repository/RefreshTokenRepository.java` (referenced)
- **Class & Method Name(s):** `UserService.changePassword(...)` (lines 63-73); `AdminService.deactivateUser(...)` (lines 72-107); `RefreshTokenRepository.revokeAllByUserId(...)`
- **Exact Line Numbers:** `UserService.java:63-73`; `AdminService.java:72-107`
- **Actual Code Behaviour:** Neither flow calls `refreshTokenRepository.revokeAllByUserId(...)`. `AuthService.logout()` (line 278) exists but is not invoked from these flows, and no logout endpoint is exposed (SEC-06). Refresh tokens issued before the password change/deactivation therefore remain valid and can mint new access tokens.
- **Failure / Exploit Scenario:** A user changes their password after a suspected compromise, but the attacker's stolen refresh token keeps working. An admin deactivates a user, but the user's refresh token keeps minting access tokens (compounding SEC-01).
- **Financial / Business Consequence:** Account takeover persists past password reset/deactivation; fraud window extends indefinitely (until token expiry/family revocation triggers).
- **Concrete Recommended Correction:** Call `refreshTokenRepository.revokeAllByUserId(userId)` in `UserService.changePassword` and `AdminService.deactivateUser`. Add a logout endpoint (SEC-06) and revoke on password reset too.
- **Suggested Verification Test Case:** Test: change password → old refresh token rejected on `/auth/refresh` (401). Test: admin deactivates user → refresh token rejected. Add to `RefreshTokenRotationIntegrationTest`.

#### SEC-03 — Plaintext email-verification & password-reset tokens in Notification + logs

- **Risk ID:** SEC-03
- **Title:** Plaintext email verification tokens and password reset tokens are embedded inside in-app `Notification` messages and exposed via `/api/v1/notifications`, and logged in full by `MailService` when SMTP is unconfigured
- **Category:** Security
- **Severity:** High
- **Verification Status:** Confirmed Vulnerability
- **Exact File Path(s):**
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/auth/service/AuthService.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/notification/service/MailService.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/notification/controller/NotificationController.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/notification/dto/NotificationResponse.java`
- **Class & Method Name(s):** `AuthService.register(...)` (lines 92-94); `AuthService.forgotPassword(...)` (lines 220-222); `MailService` (lines 105-113)
- **Exact Line Numbers:** `AuthService.java:92-94, 220-222`; `MailService.java:105-113`
- **Actual Code Behaviour:** `register` stores the verification token in a `Notification` record whose body contains `"Token: " + user.getVerificationToken()` (lines 92-94). `forgotPassword` does the same with the reset token (lines 220-222). These records are readable via `GET /api/v1/notifications`. Additionally, when SMTP is unconfigured, `MailService` logs the full token (lines 105-113). Tokens are also stored in plaintext in the `users` table (SEC-09 in D1 report).
- **Failure / Exploit Scenario:** Any authenticated user (or anyone with DB read access / log access) reads another user's verification/reset token from their notification inbox or logs, then calls `/email/verify` or `/password/reset` to take over the account. Because verification tokens never expire (SEC-07), the window is unbounded.
- **Financial / Business Consequence:** Account takeover; bypass of email-verification gate; unauthorised money movement; regulatory exposure.
- **Concrete Recommended Correction:** Never persist tokens in notification bodies — send only a generic "check your email" message. Redact tokens from all logs (log a hash or last 4 chars only). Hash tokens at rest in the DB (store a BCrypt/SHA-256 hash, compare in constant time) and enforce single-use + expiry. Remove token from `NotificationResponse`.
- **Suggested Verification Test Case:** Test: after registration, `GET /api/v1/notifications` body must not contain the verification token substring. Test: `MailService` log output (captured) must not contain the token. Security scan: grep for token in notification table.

#### SEC-04 — `spring.profiles.active: dev` hardcoded as default

- **Risk ID:** SEC-04
- **Title:** `spring.profiles.active: dev` is hardcoded as the default in base `application.yml`, risking accidental activation of `AdminDataInitializer`, SQL debug logging, and dev settings if `SPRING_PROFILES_ACTIVE` is omitted
- **Category:** Security
- **Severity:** High
- **Verification Status:** Confirmed Misconfiguration
- **Exact File Path(s):**
  - `novawallet-api/src/main/resources/application.yml`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/bootstrap/AdminDataInitializer.java`
- **Class & Method Name(s):** `AdminDataInitializer.run(...)` (lines 58-101)
- **Exact Line Numbers:** `application.yml:6`; `AdminDataInitializer.java:58-101`
- **Actual Code Behaviour:** Base `application.yml` line 6 sets `spring.profiles.active: dev`. If `SPRING_PROFILES_ACTIVE` is not overridden (e.g. a misconfigured deployment), the dev profile activates, seeding an admin from `APP_ADMIN_PASSWORD`, enabling SQL debug logging, and applying dev settings. Prod profile is never auto-selected.
- **Failure / Exploit Scenario:** An operator deploys the container without setting `SPRING_PROFILES_ACTIVE=prod`; the app boots in dev mode, seeds a known admin account (if env vars present) or uses dev defaults, logs SQL (potentially including data), and applies dev CORS/limits. Combined with the wrong healthcheck path, this may go unnoticed.
- **Financial / Business Consequence:** Accidental admin seeding; verbose logging of financial data; weakened security posture in production; audit/compliance failure.
- **Concrete Recommended Correction:** Remove the hardcoded `active: dev` from base `application.yml`; default to no profile (fail-fast requiring explicit profile) or default to `prod`. Make dev opt-in via `SPRING_PROFILES_ACTIVE=dev`. Document the requirement. Add a startup guard that refuses to boot with dev profile when a prod marker (e.g. `APP_ENV=prod`) is set.
- **Suggested Verification Test Case:** Test: boot with no `spring.profiles.active` → application context fails to load or selects prod. Test: boot with `dev` profile and `APP_ENV=prod` → startup failure. CI check that base YAML contains no `active:` key.

#### SEC-06 — No logout endpoint exposed

- **Risk ID:** SEC-06
- **Title:** No logout endpoint exposed in `AuthController` despite `AuthService.logout()` existing
- **Category:** Security
- **Severity:** Medium
- **Verification Status:** Confirmed Weakness
- **Exact File Path(s):**
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/auth/controller/AuthController.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/auth/service/AuthService.java`
- **Class & Method Name(s):** `AuthService.logout(UUID)` (line 278); `AuthController` (no logout mapping)
- **Exact Line Numbers:** `AuthService.java:278`
- **Actual Code Behaviour:** `AuthService.logout()` exists and revokes refresh tokens, but `AuthController` exposes no `POST /api/v1/auth/logout` (or similar) endpoint, so clients cannot trigger server-side revocation. Verified by grep across controllers.
- **Failure / Exploit Scenario:** A user cannot terminate their session server-side; logout is purely client-side (token deletion), leaving refresh tokens valid and reusable if intercepted.
- **Financial / Business Consequence:** Stolen refresh tokens remain usable until natural expiry/family revocation; no user-initiated containment.
- **Concrete Recommended Correction:** Expose `POST /api/v1/auth/logout` (JWT-authenticated) calling `AuthService.logout(userId)`. Document token clearing on client. Add to OpenAPI.
- **Suggested Verification Test Case:** Test: logout with valid JWT → subsequent `/auth/refresh` with the old refresh token returns 401. Test endpoint appears in Swagger and requires auth.

#### SEC-07 — Email verification token has no expiration

- **Risk ID:** SEC-07
- **Title:** Email verification token has no expiration timestamp
- **Category:** Security
- **Severity:** Medium
- **Verification Status:** Confirmed Weakness
- **Exact File Path(s):**
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/user/entity/User.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/auth/service/AuthService.java`
- **Class & Method Name(s):** `User` (verificationToken field, line 73); `AuthService.register(...)` / `AuthService.verifyEmail(...)` (lines 101-116)
- **Exact Line Numbers:** `User.java:73`; `AuthService.java:101-116` (verified range 84-99 for register; verify-email method around 101-116)
- **Actual Code Behaviour:** `User` has `verificationToken` (line 73) but no `verificationTokenExpiresAt` (contrast `passwordResetExpiresAt` at line 79). `register` generates a token with no expiry; `verifyEmail` accepts it at any later time.
- **Failure / Exploit Scenario:** A leaked verification token (SEC-03) can be used indefinitely to verify an email / unlock money movement, and to satisfy the email-verified gate for transfers/withdrawals/payouts.
- **Financial / Business Consequence:** Unbounded window for email-verification bypass; supports account takeover and money movement on unverified identities.
- **Concrete Recommended Correction:** Add `verificationTokenExpiresAt` (e.g. 24h) to `User`; set it in `register`; reject expired tokens in `verifyEmail` (400/410). Add a Flyway migration. Hash the token at rest (SEC-03).
- **Suggested Verification Test Case:** Test: verify with an expired token → 400/410 and email remains unverified. Test: token expiry timestamp is persisted and within 24h of registration.

#### SEC-08 — JwtUtil does not enforce minimum key length/entropy or iss/aud

- **Risk ID:** SEC-08
- **Title:** `JwtUtil` does not enforce minimum key length/entropy at startup or validate `iss`/`aud` claims
- **Category:** Security
- **Severity:** Medium
- **Verification Status:** Confirmed Weakness
- **Exact File Path(s):** `novawallet-api/src/main/java/com/novawallet/novawallet_api/security/JwtUtil.java`
- **Class & Method Name(s):** `JwtUtil` (lines 13-76)
- **Exact Line Numbers:** `JwtUtil.java:13-76`
- **Actual Code Behaviour:** The utility parses/signs JWTs without validating issuer/audience claims and without a startup check that the signing key meets a minimum length/entropy. A weak or misconfigured secret could be accepted silently.
- **Failure / Exploit Scenario:** An operator sets a short/low-entropy `JWT_SECRET`; tokens become brute-forceable offline. Tokens minted by another service sharing the same key (no `iss`/`aud` distinction) are accepted.
- **Financial / Business Consequence:** Token forgery / account takeover if the secret is weak or shared across environments; cross-environment token reuse.
- **Concrete Recommended Correction:** Add a `@PostConstruct` (or config validation) that fails startup if the key is shorter than 256 bits / low entropy. Add `iss` and `aud` claims on issue and validate them on parse. Document key rotation.
- **Suggested Verification Test Case:** Test: boot with a 16-char secret → startup failure. Test: token without expected `iss`/`aud` → rejected. Test: token from a different issuer → 401.

#### SEC-10 — Monetary DTOs lack @Digits/@DecimalMax upper bounds

- **Risk ID:** SEC-10
- **Title:** Monetary request DTOs validate `@Positive` but lack `@Digits(integer=13, fraction=2)` / `@DecimalMax` upper bounds
- **Category:** Security
- **Severity:** Medium
- **Verification Status:** Confirmed Weakness
- **Exact File Path(s):**
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/transaction/dto/DepositRequest.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/transaction/dto/WithdrawRequest.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/transaction/dto/TransferRequest.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/payment/dto/PayoutRequest.java`
- **Class & Method Name(s):** `DepositRequest`, `WithdrawRequest`, `TransferRequest`, `PayoutRequest` (amount fields)
- **Exact Line Numbers:** field declarations in each DTO (verify per file)
- **Actual Code Behaviour:** Amount fields carry `@Positive` (and possibly `@NotNull`) but no `@Digits` precision or `@DecimalMax` ceiling, so a client can submit extremely large or high-precision values. `BigDecimal.setScale(2, HALF_EVEN)` is applied later, but the input is not bounded before reaching the service/DB (`DECIMAL(15,2)`).
- **Failure / Exploit Scenario:** A request with `amount = 999999999999999999.999999` either overflows the DB column (error) or, combined with FIN-01, credits an unbounded amount. High-precision inputs cause unexpected rounding. Negative/zero already rejected by `@Positive`.
- **Financial / Business Consequence:** DoS via oversized payloads; potential DB errors; supports arbitrary-credit exploits (FIN-01/FIN-02).
- **Concrete Recommended Correction:** Add `@Digits(integer = 13, fraction = 2)` (or stricter) and `@DecimalMax("9999999999999.99")` to each amount field. Add `@Size`/max on description/narration strings. Validate at controller boundary (`@Valid` already present).
- **Suggested Verification Test Case:** Validation tests: amount with 3 decimals → 400; amount exceeding ceiling → 400; amount = 0/-1 → 400; valid max → 201. Add to `TransactionEdgeCaseEndpointIntegrationTest`.

#### SEC-11 — management.info.env.enabled + public Swagger + CORS wildcard headers w/ credentials

- **Risk ID:** SEC-11
- **Title:** `management.info.env.enabled: true` with unauthenticated `/actuator/info`, Swagger UI public in all profiles, and CORS wildcard headers with credentials
- **Category:** Security
- **Severity:** Medium
- **Verification Status:** Confirmed Misconfiguration
- **Exact File Path(s):**
  - `novawallet-api/src/main/resources/application.yml`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/config/SecurityConfig.java`
  - `novawallet-api/src/main/resources/application-prod.yml`
- **Class & Method Name(s):** `SecurityConfig` filter chain (lines 104-154)
- **Exact Line Numbers:** `application.yml:34-36`; `SecurityConfig.java:104-113, 124-154`; `application-prod.yml:139-140`
- **Actual Code Behaviour:** `/actuator/info` is exposed and `management.info.env.enabled: true` (lines 34-36) can leak environment details. Swagger UI is permitted in all profiles (SecurityConfig lines 104-113). CORS is configured with wildcard headers and `allowCredentials=true` (lines 124-154; prod defaults lines 139-140).
- **Failure / Exploit Scenario:** Information disclosure via `/actuator/info`; Swagger UI exposes the full API surface in production (aids attackers); over-broad CORS with credentials enables cross-origin token theft if an origin is misconfigured.
- **Financial / Business Consequence:** Information disclosure; expanded attack surface; potential credential theft via CORS misconfiguration.
- **Concrete Recommended Correction:** Disable `management.info.env.enabled` in prod (or gate `/actuator/info` behind auth). Restrict Swagger to non-prod profiles. Tighten CORS to an explicit origin list and specific headers; never combine wildcard headers with credentials.
- **Suggested Verification Test Case:** Test: in prod profile, `/actuator/info` returns 401/404 and Swagger returns 404. CORS preflight from an unlisted origin → no `Access-Control-Allow-Origin`. SecurityConfig test.

#### SEC-12 — RequestTracingFilter unvalidated X-Request-ID; KycService trusts client MIME

- **Risk ID:** SEC-12
- **Title:** `RequestTracingFilter` accepts unvalidated `X-Request-ID` header into MDC logs (log injection); `KycService` trusts client-supplied MIME header without magic-byte inspection
- **Category:** Security
- **Severity:** Low
- **Verification Status:** Confirmed Weakness
- **Exact File Path(s):**
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/filter/RequestTracingFilter.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/kyc/service/KycService.java`
- **Class & Method Name(s):** `RequestTracingFilter` (line 33); `KycService` (upload handling)
- **Exact Line Numbers:** `RequestTracingFilter.java:33`
- **Actual Code Behaviour:** The filter copies the incoming `X-Request-ID` header verbatim into the MDC (line 33) without validation/sanitisation, enabling log injection (CRLF/newline injection into structured logs). `KycService` validates the file's MIME type from the client-supplied `Content-Type`/filename without inspecting magic bytes.
- **Failure / Exploit Scenario:** An attacker sends `X-Request-ID: foo\n[ERROR] fake entry` to forge log lines, hindering forensics. A user uploads a non-image file with a spoofed MIME type, bypassing the upload allowlist (jpeg/png/webp/pdf) and potentially storing executable content.
- **Financial / Business Consequence:** Log-integrity compromise; stored malicious file content; potential XSS if files are served inline.
- **Concrete Recommended Correction:** Validate/sanitise `X-Request-ID` (allowlist `[A-Za-z0-9-]{1,64}`, else generate a UUID). Inspect magic bytes (Apache Tika or manual) for KYC uploads and reject mismatches. Serve uploaded files with `Content-Disposition: attachment` and a safe content type.
- **Suggested Verification Test Case:** Test: `X-Request-ID` containing newline → replaced with generated UUID; log output contains no injected newline. Test: upload a `.exe` renamed to `.pdf` → 400.

### 3.3 Architectural / Database — Confirmed

#### ARCH-01 — Extensive cross-domain repository injection

- **Risk ID:** ARCH-01
- **Title:** Extensive cross-domain repository injection prevents independent module testing and service extraction
- **Category:** Architectural
- **Severity:** High
- **Verification Status:** Architectural (Confirmed by code inspection)
- **Exact File Path(s):** `novawallet-api/src/main/java/com/novawallet/novawallet_api/admin/service/AdminService.java`; `novawallet-api/src/main/java/com/novawallet/novawallet_api/transaction/service/TransactionService.java`; `novawallet-api/src/main/java/com/novawallet/novawallet_api/payment/service/PayoutService.java`; `novawallet-api/src/main/java/com/novawallet/novawallet_api/payment/service/WebhookService.java`; `novawallet-api/src/main/java/com/novawallet/novawallet_api/kyc/service/AdminKycService.java`; `novawallet-api/src/main/java/com/novawallet/novawallet_api/transaction/service/TransactionLimitService.java`
- **Class & Method Name(s):** Service constructors (cross-domain repository fields)
- **Exact Line Numbers:** constructor injection blocks (various)
- **Actual Code Behaviour:** Services inject repositories from other domains (e.g. `PayoutService` injects `UserRepository`, `WalletRepository`, `TransactionRepository`, `TransactionLimitService`, `FeeEngineService`, `AuthService`; `WebhookService` injects `WalletRepository`, `TransactionRepository`, `TransactionService`). This couples modules at the data layer.
- **Failure / Exploit Scenario:** A change to one domain's schema/repo ripples across services; unit tests must mock many collaborators; extracting a service (e.g. payments) later requires untangling data access. Not directly exploitable today.
- **Financial / Business Consequence:** Slower, riskier changes to money paths; higher regression probability; blocks future microservice extraction.
- **Concrete Recommended Correction:** Introduce domain service interfaces / facades; inject service-level ports rather than repositories across domains; keep repository access inside the owning domain. Refactor incrementally with tests green.
- **Suggested Verification Test Case:** Architecture test (ArchUnit) asserting that `payment.*` does not import `*.repository.*` from other domains; service unit tests with a single collaborator.

#### ARCH-02 — Identity and KYC state coupled in users table

- **Risk ID:** ARCH-02
- **Title:** Identity and KYC state are coupled in the `users` table and via JPA `@ManyToOne User` in `KycDocument`
- **Category:** Architectural / Database
- **Severity:** High
- **Verification Status:** Architectural/Database (Confirmed by schema/code inspection)
- **Exact File Path(s):**
  - `novawallet-api/src/main/resources/db/migration/V5__add_kyc_tables.sql`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/kyc/entity/KycDocument.java`
- **Class & Method Name(s):** `KycDocument` (lines 35-37)
- **Exact Line Numbers:** `KycDocument.java:35-37`
- **Actual Code Behaviour:** KYC state (tier, status, documents) lives in/on the `users` table and `KycDocument` holds a `@ManyToOne User` (lines 35-37). Identity and KYC lifecycle are not separate bounded contexts.
- **Failure / Exploit Scenario:** Schema changes to KYC require user-table migrations; KYC re-approval creates duplicate wallets (D1 S14) because there is no uniqueness on `wallets.user_id`. Hard to evolve KYC independently (e.g. re-KYC, multiple documents).
- **Financial / Business Consequence:** Duplicate-wallet bugs; migration risk; blocks KYC re-review flows required for compliance.
- **Concrete Recommended Correction:** Separate KYC into its own aggregate (KycProfile/KycDocument) referencing `userId` by value (not JPA entity), with its own table and lifecycle. Add unique constraint on `wallets.user_id`.
- **Suggested Verification Test Case:** Migration test: re-running KYC approval for a user does not create a second wallet; ArchUnit test forbidding `KycDocument` from holding a `@ManyToOne User`.

#### ARCH-03 — Synchronous in-process side effects without domain events / outbox

- **Risk ID:** ARCH-03
- **Title:** Synchronous in-process side effects (`NotificationService`, `MailService`, `SmsService`) inside core transaction flows without domain events or a transactional outbox
- **Category:** Architectural
- **Severity:** Medium
- **Verification Status:** Architectural (Confirmed by code inspection)
- **Exact File Path(s):** `novawallet-api/src/main/java/com/novawallet/novawallet_api/transaction/service/TransactionService.java` (lines 128-138, 300-330); `novawallet-api/src/main/java/com/novawallet/novawallet_api/notification/service/NotificationService.java`; `novawallet-api/src/main/java/com/novawallet/novawallet_api/notification/MailService.java`; `novawallet-api/src/main/java/com/novawallet/novawallet_api/notification/service/SmsService.java`
- **Class & Method Name(s):** `TransactionService.deposit(...)`, `TransactionService.transfer(...)`, `NotificationService.sendBoth(...)`
- **Exact Line Numbers:** `TransactionService.java:128-138` (deposit notification); transfer notification ~300-330
- **Actual Code Behaviour:** Core transaction methods call `notificationService.sendBoth(...)` synchronously within the `@Transactional` flow (deposit lines 131-138). If notification/SMTP fails, the exception can roll back the financial transaction. There is no outbox or async dispatch.
- **Failure / Exploit Scenario:** SMTP outage causes a successful deposit to roll back (money not credited) or a slow mail server stalls the transaction. Notifications are coupled to money-movement success.
- **Financial / Business Consequence:** Money movement fails due to notification infrastructure; inconsistent user-facing state; retry storms.
- **Concrete Recommended Correction:** Decouple via a transactional outbox: write an outbox event in the same DB transaction as the money movement; a separate worker dispatches notifications. Never let notification I/O fail a financial transaction.
- **Suggested Verification Test Case:** Test: mock `MailService` to throw → deposit still returns 201 and `Transaction` row is committed. Outbox table contains a pending notification event.

#### DB-01 — Missing DB CHECK constraints, FK indexes, leftover test table, default mismatch

- **Risk ID:** DB-01
- **Title:** Lack of DB `CHECK` constraints (`balance >= 0`, `amount > 0`), missing FK indexes (`kyc_documents.reviewed_by`, `transactions.related_transaction_id`, `idempotency_keys.user_id`), no FK on `related_transaction_id` or `notifications.user_id`, leftover `schema_version_test` table in `V1__init_schema.sql`, and Java vs SQL default mismatch on `User.kycTier` (`0` vs `1`)
- **Category:** Database
- **Severity:** Medium
- **Verification Status:** Database (Confirmed by schema inspection)
- **Exact File Path(s):**
  - `novawallet-api/src/main/resources/db/migration/V1__init_schema.sql`
  - `novawallet-api/src/main/resources/db/migration/V3__create_wallets_table.sql`
  - `novawallet-api/src/main/resources/db/migration/V5__add_kyc_tables.sql`
  - `novawallet-api/src/main/resources/db/migration/V6__create_transactions_table.sql`
  - `novawallet-api/src/main/resources/db/migration/V9__create_idempotency_keys_table.sql`
  - `novawallet-api/src/main/resources/db/migration/V10__create_notifications_tables.sql`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/user/entity/User.java`
- **Class & Method Name(s):** `User` (kycTier default); migration files above
- **Exact Line Numbers:** `V6__create_transactions_table.sql` (status default 'SUCCESSFUL', no CHECK on amount); `V3__create_wallets_table.sql` (non-unique user_id index); `V1__init_schema.sql` (schema_version_test); `User.java` (kycTier default 0 vs SQL 1)
- **Actual Code Behaviour:** Schema lacks DB-level guards: no `CHECK (balance >= 0)`, no `CHECK (amount > 0)`; FK indexes missing on several columns; `related_transaction_id` and `notifications.user_id` have no FK; `schema_version_test` junk table remains; `User.kycTier` Java default `0` differs from SQL default `1`.
- **Failure / Exploit Scenario:** A bug or race that bypasses application-level guards could persist negative balances or zero/negative amounts; missing indexes cause slow FK checks and queries; orphaned rows; Java/SQL default mismatch causes inconsistent tier assignment depending on insert path.
- **Financial / Business Consequence:** Data-integrity erosion; slow queries under load; orphaned records; inconsistent KYC tiering affecting limits.
- **Concrete Recommended Correction:** Add a new Flyway migration adding CHECK constraints, FKs and indexes; drop `schema_version_test`; align defaults (prefer Java `1` or SQL `0` per business rule). Add `wallets.user_id` UNIQUE constraint (also fixes ARCH-02 duplicate wallet).
- **Suggested Verification Test Case:** Migration test on Postgres 16: insert negative balance → constraint violation; insert amount 0 → violation; FK delete cascade behaviour; index existence check via `pg_indexes`. Default-value test for new User.

---

## 4. Potential Architectural & Design Weaknesses

### 4.1 Financial — Design Weaknesses

#### FIN-08 — Single-entry Transaction table + mutable wallets.balance

- **Risk ID:** FIN-08
- **Title:** Single-entry `Transaction` table and mutable `wallets.balance` rather than an immutable double-entry ledger enforcing `Total Debits = Total Credits`
- **Category:** Financial
- **Severity:** Medium
- **Verification Status:** Design Weakness
- **Exact File Path(s):**
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/transaction/entity/Transaction.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/wallet/entity/Wallet.java`
- **Class & Method Name(s):** `Transaction` (lines 23-79); `Wallet` (lines 21-67)
- **Exact Line Numbers:** `Transaction.java:23-79`; `Wallet.java:21-67`
- **Actual Code Behaviour:** A single `Transaction` row per operation carries `balanceBefore`/`balanceAfter` and a signed `amount`; `Wallet.balance` is a mutable column updated in place. There is no immutable ledger of paired debit/credit entries and no invariant `sum(debits) = sum(credits)` enforceable at the DB level.
- **Failure / Exploit Scenario:** A partial write, a missed update, or a manual DB edit desyncs `wallets.balance` from the sum of transactions. Reconciliation requires replaying history; there is no single source of truth.
- **Financial / Business Consequence:** Silent drift between stored balance and ledger; hard to audit; reconciliation burden; regulatory exposure.
- **Concrete Recommended Correction:** Evolve toward a double-entry ledger: an immutable `ledger_entry` table with paired debit/credit rows per operation, and derive `wallets.balance` (or a materialised view) from it. At minimum, add a periodic reconciliation job comparing `wallets.balance` to `SUM` of transactions and alerting on drift. Keep current schema for MVP but document the migration path.
- **Suggested Verification Test Case:** Reconciliation test: for every wallet, `wallets.balance ==` last `balance_after` of its latest transaction; alert if mismatch. Invariant test in CI.

#### FIN-09 — Intermediate rounding to scale 2 in percentage fee calculation

- **Risk ID:** FIN-09
- **Title:** Intermediate rounding to scale 2 in percentage fee calculation before adding flat fee
- **Category:** Financial
- **Severity:** Medium
- **Verification Status:** Design Weakness
- **Exact File Path(s):** `novawallet-api/src/main/java/com/novawallet/novawallet_api/fee/service/FeeEngineService.java`
- **Class & Method Name(s):** `FeeEngineService.calculateFee(FeeType, BigDecimal)`
- **Exact Line Numbers:** `FeeEngineService.java:44-60` (specifically line 55)
- **Actual Code Behaviour:** Line 55 computes `percentagePart = amount * percentage / 100` with `setScale(2, HALF_EVEN)` *before* adding `flatFee` (line 56). This intermediate rounding can introduce a 1-ngwee discrepancy versus computing the full expression then rounding once.
- **Failure / Exploit Scenario:** For specific amounts/percentages, the fee differs by 0.01 from the mathematically correct value; over millions of transactions this accumulates and complicates reconciliation with provider fee schedules.
- **Financial / Business Consequence:** Micro-discrepancies in fee revenue; reconciliation noise; potential customer disputes.
- **Concrete Recommended Correction:** Compute `totalFee = (amount * percentage / 100) + flat` in full precision, apply min/max clamping, then round once to scale 2 at the end. Use `MathContext`/`BigDecimal` without intermediate `setScale`. Add unit tests for boundary amounts.
- **Suggested Verification Test Case:** Unit test: for a grid of amounts (e.g. 0.01..999.99) and a percentage producing a non-terminating decimal, assert `calculateFee` equals the single-rounding reference. Property-based test.

#### FIN-10 — Missing payout status polling, disbursement webhook, refund/reversal workflows

- **Risk ID:** FIN-10
- **Title:** Missing payout status polling (`checkStatus` never invoked in production), missing disbursement webhook endpoint, and missing refund/reversal workflows
- **Category:** Financial
- **Severity:** Medium
- **Verification Status:** Design Weakness
- **Exact File Path(s):**
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/payment/LipilaGateway.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/payment/PaymentGateway.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/payment/service/PayoutService.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/payment/controller/WebhookController.java`
- **Class & Method Name(s):** `LipilaGateway.checkStatus(...)` (line 108); `PaymentGateway.checkStatus(...)` (line 45); `PayoutService` (no polling/reversal methods)
- **Exact Line Numbers:** `LipilaGateway.java:108`; `PaymentGateway.java:45`
- **Actual Code Behaviour:** `checkStatus` is implemented (LipilaGateway line 108) but never invoked anywhere in production code (verified by grep). There is no disbursement webhook endpoint (WebhookController only handles collections). No refund/reversal workflow exists for any transaction type.
- **Failure / Exploit Scenario:** PENDING payouts never settle (compounding FIN-05). A payout that the provider completes has no inbound signal to flip the local row to SUCCESSFUL. No way to reverse an erroneous credit/debit.
- **Financial / Business Consequence:** Stuck PENDING rows; orphaned funds; no customer self-service refunds; manual ops burden.
- **Concrete Recommended Correction:** Implement a scheduled polling job calling `checkStatus` for PENDING payouts; add a disbursement webhook endpoint with the same auth model; implement a reversal workflow (compensating transactions) with admin authorisation and audit. Document SLAs.
- **Suggested Verification Test Case:** Integration test: a PENDING payout whose `checkStatus` returns `Successful` → row becomes SUCCESSFUL. Test: disbursement webhook with valid secret → status update. Test: reversal creates a compensating row and restores balance.

#### FIN-11 — Daily send limit per-wallet vs velocity per-user; unverified users bypass tier limits

- **Risk ID:** FIN-11
- **Title:** Daily send limit aggregates per wallet while velocity limits aggregate per user; unverified users bypass tier daily send limits until KYC tier config applies
- **Category:** Financial
- **Severity:** Low
- **Verification Status:** Design Weakness
- **Exact File Path(s):** `novawallet-api/src/main/java/com/novawallet/novawallet_api/transaction/service/TransactionLimitService.java`
- **Class & Method Name(s):** `TransactionLimitService` (daily send limit + velocity methods)
- **Exact Line Numbers:** entire service (verify per method)
- **Actual Code Behaviour:** Daily send limits are enforced per wallet (`enforceDailySendLimit(userId, walletId, amount)`), while velocity limits aggregate per user across wallets. Users with multiple wallets (possible via ARCH-02 duplicate-wallet bug) effectively multiply their daily limit. Unverified users (tier 0/Basic) may bypass tier caps until KYC tier config is applied.
- **Failure / Exploit Scenario:** A user with N wallets sends N × daily limit per day. Unverified accounts move funds beyond intended tier caps.
- **Financial / Business Consequence:** Limit enforcement ineffective; regulatory/AML exposure; excessive money movement on unverified identities.
- **Concrete Recommended Correction:** Make daily send limits per-user (aggregate across all user wallets), consistent with velocity limits. Apply tier limits from KYC approval; default unverified users to the most restrictive tier (or block money movement entirely until verified). Add tests.
- **Suggested Verification Test Case:** Test: user with two wallets cannot exceed the per-user daily limit across both. Test: unverified user attempting a transfer → rejected by email-verified gate (already) and tier cap.

### 4.2 Security — Design Weaknesses

#### SEC-05 — Webhook authentication = static header only; no HMAC/timestamp/nonce; string-match dedup

- **Risk ID:** SEC-05
- **Title:** Webhook authentication relies solely on static `X-Webhook-Secret` header comparison without payload HMAC signing, timestamp tolerance window, or replay nonce table; deduplication checks `existsByDescription` string matching
- **Category:** Security
- **Severity:** High
- **Verification Status:** Design Weakness
- **Exact File Path(s):**
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/payment/controller/WebhookController.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/payment/service/WebhookService.java`
- **Class & Method Name(s):** `WebhookController.verifySecret(...)` (lines 77-99); `WebhookService.processCollectionCallback(...)` (lines 114-121)
- **Exact Line Numbers:** `WebhookController.java:77-99`; `WebhookService.java:114-121`
- **Actual Code Behaviour:** Authentication is a constant-time comparison of a shared secret header (controller lines 77-99). There is no HMAC over the payload, no timestamp tolerance, no replay nonce table. Deduplication uses `transactionRepository.existsByDescription(description)` (lines 114-121), a string match with no DB unique constraint.
- **Failure / Exploit Scenario:** If the secret leaks (logs, config, repo), an attacker can forge callbacks indefinitely (compounding FIN-02). Replayed callbacks are only deduplicated by a string match that can race or be bypassed by altering the description. No replay protection.
- **Financial / Business Consequence:** Forged credits; replay attacks; no non-repudiation.
- **Concrete Recommended Correction:** Implement HMAC-SHA256 signature over the raw payload with a timestamp and nonce; enforce a tolerance window (e.g. 5 min) and a nonce table with unique constraint; replace string-match dedup with a unique provider-reference constraint (FIN-02). Document the Lipila signature scheme once confirmed with the provider.
- **Suggested Verification Test Case:** Test: callback with valid secret but tampered body → rejected (once HMAC added). Test: replayed callback (same nonce) → 200 no-op. Test: callback older than tolerance → 400.

#### SEC-09 — RateLimitFilter & LoginRateLimiter in-memory per-JVM, proxy-header unaware

- **Risk ID:** SEC-09
- **Title:** `RateLimitFilter` and `LoginRateLimiter` are in-memory per-JVM and rely on `request.getRemoteAddr()` without proxy-header trust configuration, failing under horizontal scaling or reverse proxies
- **Category:** Security
- **Severity:** Medium
- **Verification Status:** Design Weakness
- **Exact File Path(s):**
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/security/RateLimitFilter.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/security/LoginRateLimiter.java`
- **Class & Method Name(s):** `RateLimitFilter` (lines 82-88); `LoginRateLimiter`
- **Exact Line Numbers:** `RateLimitFilter.java:82-88`
- **Actual Code Behaviour:** Counters are held in in-memory Caffeine caches; keys are derived from `request.getRemoteAddr()` (lines 82-88). No `server.forward-headers-strategy` is configured anywhere.
- **Failure / Exploit Scenario:** Behind a reverse proxy, all traffic shares one `remoteAddr` (the proxy's), causing either global throttling (DoS) or, if the proxy forwards `X-Forwarded-For` untrusted, trivial bypass by spoofing the header. Multi-instance deployments do not share state, so limits are per-instance.
- **Financial / Business Consequence:** Rate limiting ineffective in production topology; brute-force/DoS exposure.
- **Concrete Recommended Correction:** Configure `server.forward-headers-strategy=framework` (or `native`) and trust only the proxy. Either move counters to a shared store (Redis) or document single-instance deployment. Add tests with `X-Forwarded-For`.
- **Suggested Verification Test Case:** Test: two requests with different spoofed `X-Forwarded-For` from a trusted proxy → separate buckets. Test: untrusted proxy → `remoteAddr` used. Load test across 2 instances sharing a limit.

---

## 5. Risk Register Notes

- **Confirmed vs Design Weakness:** Entries marked "Confirmed" are reproducible from the current code; entries marked "Design Weakness"/"Architectural"/"Database" require a change in design or scale to become exploitable but are tracked here because they materially affect financial correctness.
- **Line numbers** are as of the Phase 0 audit snapshot (source dated 2026-10-19 → 2026-10-31). Re-verify with `grep`/`read` before editing.
- **No source code was modified** to produce this document. Corrections are recommendations only.
- **Secrets:** All sensitive values are represented as `[REDACTED]`; no actual credentials appear in this document.

---

## 6. Recommended Prioritisation

1. **Block live money until fixed:** FIN-01, FIN-02, FIN-04, FIN-05, SEC-01, SEC-02, SEC-03, SEC-04.
2. **Fix before scaling beyond single instance:** FIN-03, FIN-06, SEC-09, SEC-11.
3. **Fix before audit/compliance sign-off:** FIN-07, FIN-08, DB-01, SEC-07, SEC-08, SEC-10, SEC-12.
4. **Track as roadmap items:** FIN-09, FIN-10, FIN-11, SEC-05, SEC-06, ARCH-01, ARCH-02, ARCH-03.

---

## 7. Cross-References

- `docs/D1-RELEASE-READINESS-ASSESSMENT.md` — release blockers and security findings (§7, §12).
- `docs/TECHNICAL-REPORT.md` — architecture and verification evidence.
- `docs/SRS.md` — Wave 5 (W5.1–W5.5) payment requirements.
- `novawallet-api/SECURITY.md` — security documentation (outdated; see D1 §3.2).

---

## 8. Verification Matrix (Suggested Test Coverage Summary)

| Risk ID | Suggested Test Type | Owner |
|---|---|---|
| FIN-01 | Integration (authz) + property (amount fuzzing) | Backend |
| FIN-02 | Integration (amount/currency mismatch, duplicate, unknown wallet) | Backend |
| FIN-03 | Load + chaos (pool exhaustion, timeout) | Backend/Perf |
| FIN-04 | Integration (timeout → PENDING intent retained) | Backend |
| FIN-05 | Scheduled-job integration (refund on fail) | Backend |
| FIN-06 | Concurrency integration (opposite-direction) | Backend |
| FIN-07 | Scheduled-job integration (no snapshot corruption) | Backend |
| FIN-08 | Reconciliation invariant test | Backend |
| FIN-09 | Unit + property (single rounding) | Backend |
| FIN-10 | Integration (polling, webhook, reversal) | Backend |
| FIN-11 | Integration (per-user aggregate limits) | Backend |
| SEC-01 | Integration (deactivated/locked token) | Backend |
| SEC-02 | Integration (refresh revocation) | Backend |
| SEC-03 | Integration + log capture (token redaction) | Backend |
| SEC-04 | Boot/profile integration | Backend/DevOps |
| SEC-05 | Integration (HMAC, replay, tolerance) | Backend |
| SEC-06 | Integration (logout revocation) | Backend |
| SEC-07 | Integration (expired verification token) | Backend |
| SEC-08 | Boot validation + JWT parse tests | Backend |
| SEC-09 | Integration (proxy headers, multi-instance) | Backend |
| SEC-10 | Validation integration | Backend |
| SEC-11 | Profile-based integration (actuator/swagger/cors) | Backend |
| SEC-12 | Integration (log injection, magic bytes) | Backend |
| ARCH-01 | ArchUnit | Backend |
| ARCH-02 | Migration integration | Backend |
| ARCH-03 | Integration (notification failure isolation) | Backend |
| DB-01 | Postgres migration integration | Backend/DBA |

---

## 9. Summary Risk Matrix (Bottom)

| Risk ID | Title | Category | Severity | Verification Status |
|---|---|---|---|---|
| FIN-01 | Direct unverified wallet deposit endpoint (arbitrary self-crediting) | Financial | Critical | TEMPORARILY DISABLED |
| FIN-02 | Webhook collection callback trusts unverified payload amount | Financial | Critical | TEMPORARILY DISABLED |
| FIN-03 | Payout debits wallet and calls external provider inside single `@Transactional` | Financial | High | Confirmed Vulnerability |
| FIN-04 | Provider timeout/error during payout rolls back debit despite possible disbursement | Financial | High | Confirmed Vulnerability |
| FIN-05 | PendingTransactionCleanupJob marks PENDING payouts FAILED without refund | Financial | High | Confirmed Vulnerability |
| FIN-06 | Internal transfer acquires pessimistic locks in caller-supplied order (deadlock) | Financial | High | VERIFIED |
| FIN-07 | BalanceRecalculationJob overwrites historical snapshots with current balance | Financial | Medium | Confirmed Defect |
| FIN-08 | Single-entry Transaction table + mutable wallets.balance instead of double-entry ledger | Financial | Medium | Design Weakness |
| FIN-09 | Intermediate rounding to scale 2 in percentage fee calculation | Financial | Medium | Design Weakness |
| FIN-10 | Missing payout status polling, disbursement webhook, refund/reversal workflows | Financial | Medium | Design Weakness |
| FIN-11 | Daily send limit per-wallet vs velocity per-user; unverified users bypass tier limits | Financial | Low | Design Weakness |
| SEC-01 | JwtAuthFilter does not check enabled/locked/soft-delete status | Security | High | Confirmed Vulnerability |
| SEC-02 | Password change / admin deactivation do not revoke refresh tokens | Security | High | Confirmed Vulnerability |
| SEC-03 | Plaintext email-verification & password-reset tokens in Notification + logs | Security | High | Confirmed Vulnerability |
| SEC-04 | `spring.profiles.active: dev` hardcoded as default in base application.yml | Security | High | Confirmed Misconfiguration |
| SEC-05 | Webhook auth = static header only; no HMAC/timestamp/nonce; string-match dedup | Security | High | Design Weakness |
| SEC-06 | No logout endpoint exposed despite AuthService.logout() existing | Security | Medium | Confirmed Weakness |
| SEC-07 | Email verification token has no expiration timestamp | Security | Medium | Confirmed Weakness |
| SEC-08 | JwtUtil does not enforce minimum key length/entropy or iss/aud claims | Security | Medium | Confirmed Weakness |
| SEC-09 | RateLimitFilter & LoginRateLimiter in-memory per-JVM, proxy-header unaware | Security | Medium | Design Weakness |
| SEC-10 | Monetary DTOs validate @Positive but lack @Digits/@DecimalMax upper bounds | Security | Medium | Confirmed Weakness |
| SEC-11 | management.info.env.enabled + public Swagger + CORS wildcard headers w/ credentials | Security | Medium | Confirmed Misconfiguration |
| SEC-12 | RequestTracingFilter accepts unvalidated X-Request-ID; KycService trusts client MIME | Security | Low | Confirmed Weakness |
| ARCH-01 | Extensive cross-domain repository injection | Architectural | High | Architectural |
| ARCH-02 | Identity and KYC state coupled in users table / JPA @ManyToOne | Architectural | High | Architectural/Database |
| ARCH-03 | Synchronous in-process side effects without domain events / outbox | Architectural | Medium | Architectural |
| DB-01 | Missing DB CHECK constraints, FK indexes, leftover test table, default mismatch | Database | Medium | Database |

---

*End of register.*
---

## Appendix A — TASK-H Test Execution Findings (FIN-12, OPS-01)

The following two findings were discovered during TASK-H test execution and are appended here so the register stays synchronized with the test evidence. They are not part of the original Phase 0 audit set.

### FIN-12 — TransactionReferenceGenerator seeds an in-memory AtomicLong once per JVM (@PostConstruct), causing reference collisions across instances/tests sharing DB state

- **Risk ID:** FIN-12
- **Title:** In-memory `AtomicLong` reference counter seeded once per JVM in `@PostConstruct` collides on `transactions.reference` when instances/tests share DB state
- **Category:** Financial / Concurrency
- **Severity:** High
- **Verification Status:** MITIGATED
- **Exact File Path(s):**
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/transaction/service/TransactionReferenceGenerator.java`
  - `novawallet-api/src/main/java/com/novawallet/novawallet_api/transaction/repository/TransactionRepository.java` (referenced `findMaxReferenceByPrefix`)
- **Class & Method Name(s):** `TransactionReferenceGenerator.initCounterFromDatabase()` (`@PostConstruct`); `TransactionReferenceGenerator.generate()`; `TransactionRepository.findMaxReferenceByPrefix(String)`
- **Exact Line Numbers:** `TransactionReferenceGenerator.java:19-74` (class); `:30-46` (`initCounterFromDatabase`); `:52-73` (`generate`).
- **Actual Code Behaviour:** The `counter` is an instance-level `AtomicLong` (line 23) and `initCounterFromDatabase()` runs exactly once per JVM at startup (line 28 `@PostConstruct`), reading the current day's max reference from the DB and seeding the counter. `generate()` (lines 52-73) increments this local counter and only resets it to 0 on a date change, without re-reading the DB. There is no DB-level uniqueness guard beyond the `transactions.reference` column constraint and no cross-instance coordination (no DB sequence, no `SELECT ... FOR UPDATE`, no distributed lock).
- **Failure / Exploit Scenario:** When two application instances (or a test JVM and a running instance, or parallel test contexts) share the same PostgreSQL database, each JVM seeds its counter independently at startup and then increments locally. Two concurrent `generate()` calls can produce the identical reference string, and the second `INSERT` into `transactions` fails with `DataIntegrityViolationException` on the unique `transactions.reference` constraint. The same happens when tests reuse persisted DB state across runs, because the counter restarts from the DB's max at boot but races with other writers. The date-change branch resets the counter to 0 without consulting the DB, which can also collide with references already written for that day by another instance.
- **Financial / Business Consequence:** Transaction creation fails mid-flow (deposit/transfer/payout/webhook callback), leaving wallets debited/credited inconsistently or requests returning 500; under load this surfaces as intermittent `DataIntegrityViolationException` failures and potential partial state. Breaks horizontal scaling and CI reproducibility.
- **Concrete Recommended Correction:** Replace the in-memory `AtomicLong` with a database-backed sequence (PostgreSQL `SEQUENCE` per day or a dedicated `transaction_reference_sequence` table with `SELECT ... FOR UPDATE` / `INSERT ... ON CONFLICT`), or enforce uniqueness via a DB sequence and retry on conflict. At minimum, catch `DataIntegrityViolationException` in the calling service and regenerate with a retry/backoff, and re-seed from the DB on every date change. Add a Flyway migration for the sequence table. Keep the human-readable `NWTXyyyyMMdd00000001` format.
- **Suggested Verification Test Case:** Integration test starting two application contexts against the same Postgres (Testcontainers) and concurrently generating N references, asserting all distinct and zero `DataIntegrityViolationException`. Test: reuse a DB with pre-existing references for today → new refs continue above the max. Test: date rollover does not collide.

### OPS-01 — Missing `.mvn/wrapper/maven-wrapper.properties` breaks `./mvnw` in CI

- **Risk ID:** OPS-01
- **Title:** Maven Wrapper configuration (`.mvn/wrapper/maven-wrapper.properties`) is absent, so `./mvnw` cannot bootstrap Maven in CI
- **Category:** Operational / Build
- **Severity:** Medium
- **Verification Status:** VERIFIED
- **Exact File Path(s):**
  - `novawallet-api/mvnw` (wrapper script present)
  - `novawallet-api/mvnw.cmd` (wrapper script present)
  - `novawallet-api/.mvn/` (missing — no `wrapper/maven-wrapper.properties`)
- **Class & Method Name(s):** n/a (build tooling)
- **Exact Line Numbers:** n/a
- **Actual Code Behaviour:** The repository ships `mvnw`/`mvnw.cmd` but no `.mvn/wrapper/maven-wrapper.properties` (and no wrapper jar). Running `./mvnw` therefore fails to locate the Maven distribution and cannot bootstrap; CI steps that invoke `./mvnw` (instead of a pre-installed `mvn`) fail before tests run. Verified: `find novawallet-api -name "maven-wrapper.properties"` returns no results.
- **Failure / Exploit Scenario:** Any CI job or developer invoking `./mvnw test`/`./mvnw verify` fails immediately with a wrapper bootstrap error; teams fall back to system Maven (version drift) or skip the build. Docker build stage that relies on the wrapper also breaks.
- **Financial / Business Consequence:** Build/CI unreliability; delayed releases; inconsistent Maven versions across environments.
- **Concrete Recommended Correction:** Commit `.mvn/wrapper/maven-wrapper.properties` (pinning a Maven version, e.g. 3.9.x) plus the wrapper jar under `.mvn/wrapper/`, or remove the wrapper scripts and standardize on system Maven in CI/Docker. Update `.github/workflows/ci.yml` and `Dockerfile` to use the chosen approach consistently.
- **Suggested Verification Test Case:** CI step: `./mvnw -version` succeeds on a clean checkout. Test: Docker build reaches the test stage without wrapper errors.

---

*End of register (updated with TASK-H findings FIN-12, OPS-01).*

---

## 10. Phase 1A Implementation Status (Financial Guardrails)

**Branch:** `feature/phase-1a-financial-guardrails`
**Baseline commit:** `60179fb`
**Scope:** Financial guardrails only. No Phase 1B, payment orchestration, ledger, modular-monolith, eventing, or microservice work was performed.

### 10.1 Status summary

| Risk ID | Phase 1A status | Evidence / notes |
|---|---|---|
| FIN-01 | TEMPORARILY DISABLED | Direct deposit endpoint preserved but no longer credits. Ownership/active checks remain; service throws `ServiceUnavailableException` (HTTP 503) before any balance update, transaction, audit, or notification. No admin/public equivalent exists. Remaining risk: verified collection workflow is not implemented. |
| FIN-02 | TEMPORARILY DISABLED | Secret authentication and constant-time comparison are unchanged. `WebhookService.processCollectionCallback` validates the payload, parses and validates the wallet reference, returns 200 for non-Successful statuses, normalizes the amount, looks up the wallet, then throws `ServiceUnavailableException` (HTTP 503) before crediting. No `transactionService.deposit` call remains. Remaining risk: no collection initiation/verification workflow and no authoritative amount/currency match. |
| FIN-06 | VERIFIED | `TransactionService.transfer` acquires pessimistic locks in ascending UUID order using `findByIdWithLock`, maps locked rows back to sender/receiver, rechecks sender active status under lock, and preserves atomic debit/credit with rollback. No isolation downgrade. Residual retry/backoff policy is not part of Phase 1A. |
| FIN-12 | MITIGATED | `TransactionReferenceGenerator` now allocates from DB-backed `transaction_reference_seq` via `nextval`, with range guard `1..99,999,999`. Migration V12 scans all references, ignores non-conforming rows with `WARN`, fails closed at `>=99,999,999`, restarts to `max+1`, and does not rewrite historical rows. Streaming fix uses nonzero fetch size, autocommit guard, and try-with-resources. **PostgreSQL 16 verified**: fresh migration, historical references, sequence initialization, malformed references (3 ignored), concurrent generation (8 workers x 100 = 800 unique, 0 duplicates), uniqueness enforcement, sequence exhaustion, and migration failure/recovery all passed. Java generator tested against PostgreSQL 16 (16 threads x 250 = 4000 unique, 0 duplicates, 1831ms). |
| OPS-01 | VERIFIED | Maven Wrapper 3.3.4 script-only configuration restored with Maven 3.9.16 and pinned SHA-256 from the official ASF SHA-512. Clean-cache run and tampered-checksum failure were verified; `mvnw` mode is `755`; wrapper properties remain untracked/ignored by design. |

### 10.2 Implementation details

- **FIN-01 files:** `transaction/controller/TransactionController.java`, `transaction/service/TransactionService.java`. `deposit(...)` still calls `findActiveWalletWithLock(walletId, userId)`, then throws `ServiceUnavailableException("Direct wallet deposits are unavailable until verified payment collection is implemented")`. No `walletRepository.updateBalance`, `Transaction` save, audit record, or notification occurs.
- **FIN-02 files:** `payment/controller/WebhookController.java`, `payment/service/WebhookService.java`. The callback still authenticates the static `X-Webhook-Secret` header with constant-time comparison, parses `NW:<walletId>:<uuid>`, validates amount/UUID, returns 200 for non-Successful statuses, and looks up the wallet before throwing `ServiceUnavailableException`. The previous `transactionRepository.existsByDescription` deduplication and `transactionService.deposit` credit path were removed. `DepositRequest` and `TransactionService` imports were removed from `WebhookService`.
- **FIN-06 files:** `transaction/service/TransactionService.java`, `transaction/repository/WalletRepository.java` (existing `findByIdWithLock`). The transfer path detaches the unlocked sender entity, locks the lower UUID first and higher UUID second, maps the locked rows back to sender/receiver, rechecks sender active status, and preserves atomic debit/credit plus rollback. `EntityManager` was added for the detach guard.
- **FIN-12 files:** `transaction/repository/TransactionRepository.java` (`nextReferenceSequence()`), `transaction/service/TransactionReferenceGenerator.java`, `src/main/java/db/migration/V12__CreateTransactionReferenceSequence.java`. The generator no longer uses `AtomicLong`, `@PostConstruct`, date-based reset, or `findMaxReferenceByPrefix`; it calls `nextval('transaction_reference_seq')` and enforces the `1..99,999,999` range. Historical rows are never rewritten.
- **OPS-01 files:** `.mvn/wrapper/maven-wrapper.properties` (untracked by repository policy) plus `mvnw`/`mvnw.cmd`. Maven 3.9.16 and SHA-256 pinning were verified; `./mvnw clean test` on JDK 17 passed 175/175 tests with exit code 0.

### 10.3 Test evidence

- Full regression: 175 tests, 0 failures, 0 errors, 0 skipped on H2 with JDK 17 via `./mvnw clean test`.
- Baseline before Phase 1A: 144 tests with 8 failures caused by duplicate reference collisions.
- FIN-01 containment tests: `TransactionEdgeCaseEndpointIntegrationTest` verifies unavailable 503 responses, zero balance change, and zero transaction rows for direct, idempotent, non-owner, and frozen-wallet cases.
- FIN-02 containment tests: `PaymentWebhookEndpointIntegrationTest` verifies valid-secret Successful callbacks return 503 with no credit/no rows, duplicate replay containment, arbitrary large amounts, unknown wallets, and constant-time secret rejection.
- FIN-06 locking tests: `TransactionServiceTransferLockOrderTest` and `ConcurrentTransferIntegrationTest` verify canonical lock ordering and opposite-direction transfer behavior.
- FIN-12 tests (H2): `TransactionReferenceSequenceMigrationTest` (seed above historical max, empty database first value, 20,000 valid + 20,000 ignored rows) and `V12MigrationStreamingGuardTest` (autocommit guard, fetch-size guard, valid streaming order).
   - FIN-12 tests (PostgreSQL 16): `Pg16TransactionReferenceTest` (8 tests: fresh migration, historical references, sequence init, malformed refs, concurrent generation 8x100=800 unique, uniqueness enforcement, sequence exhaustion, migration failure/recovery) and `Pg16TransactionReferenceGeneratorTest` (3 tests: Java concurrency 16x250=4000 unique, exhaustion guard, format). All passed against PostgreSQL 16.15.
- OPS-01 verification: clean-cache wrapper bootstrap and tampered-checksum failure.

### 10.4 Residual risks and out-of-scope observations

- FIN-01/FIN-02 are contained, not resolved: no verified collection initiation, provider verification, amount/currency match, idempotency persistence, or replay protection beyond the existing static secret.
- FIN-06 has no automatic deadlock retry/backoff; canonical ordering is the Phase 1A mitigation.
- FIN-12 PostgreSQL 16 verification: PASSED. All 8 migration tests (fresh, historical, sequence init, malformed, concurrent, uniqueness, exhaustion, failure/recovery) and 3 Java generator tests (concurrency 4000 unique, exhaustion guard, format) passed against PostgreSQL 16.15. No corrections were required.
- Security review result for Phase 1A: PASS WITH ISSUES, with three low findings: unused webhook fields/`transactionRepository`/`transactionService`; stale Javadoc; amount validation occurs before unknown-wallet 404 on Successful callbacks.
- Out-of-scope observations: `AdminService.toggleUserDeletedStatus` has a status race without a pessimistic lock; `BalanceRecalculationJob` naming/scope; `AccountNumberGenerator` uniqueness was not reviewed.
- Cosmetic audit-message fix is not part of Phase 1A.
