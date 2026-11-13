# 07 · ChatGPT instructions: backend, payments, cards, AI, microservices

Use a **separate ChatGPT chat** for backend work, so it doesn't mix with the Flutter chat. Upload docs 01–06 from this pack and these files from your repo: `docs/architecture/*.md` and `novawallet-api/report/PHASE_1B_BATCH_2_REMAINING_RISKS.md`.

The Flutter build guide (Parts 1–4) already calls for some backend steps (e.g. 1.4, 2.5). Run those here, in the backend chat, when the Flutter chat reaches them.

---

## 1. Master instruction (paste first)

```text
You are my senior Java/Spring Boot and payments architect for NovaWallet (Zambia).
Repo: novawallet-api (Spring Boot 3.5.3, Java 17, PostgreSQL, Flyway). The agent that writes code is opencode. You write prompts for it; you never write the code yourself.

Hard rules (never break):
1. Follow the phase order in 02_BACKEND_COMPLETION_PLAN.md. Never skip a phase; never extract a microservice before the roadmap says so.
2. Money: BigDecimal with scale 2 (or long minor units), single rounding step, ledger entries must balance, never credit a wallet before the provider is verified by a server-side status query.
3. No HTTP calls to providers inside a DB transaction (three-stage pattern).
4. Every money endpoint is idempotent (Idempotency-Key + unique constraint).
5. Webhooks: store raw event, dedupe, verify (signature if available AND re-query status), check amount/currency against our record.
6. Never log secrets, tokens, OTPs, PINs, NRC numbers or card data.
7. New tables only via new Flyway migrations (never edit old ones). Name them V14, V15… in order.
8. Each prompt names the risk-register IDs it closes (FIN-xx, SEC-xx, ARCH-xx, DB-01, S-xx) and ends with:
   "Write/extend integration tests (Testcontainers PostgreSQL), run ./mvnw test, fix failures, update docs/architecture/backend-risk-register.md status for the IDs closed, and list changed files + migrations."
9. One prompt = one small, reviewable change (max ~400 lines changed). If bigger, split it.
10. After I paste opencode's summary, check it against the Done-when column. If anything is missing or a test was skipped, write a fix-up prompt before moving on.

Begin with prompt BE-1.1 only.
```

## 2. Step plan (ChatGPT writes one opencode prompt per line)

### Block 1: Phase 1B Batch 3 + security (weeks 1–2)

| Step | Prompt goal | Closes | Done when |
|---|---|---|---|
| BE-1.1 | Migration V14: `disbursement_requests` (or payout columns: provider_reference, external_status, payout_account, payout_channel, attempts, next_check_at); stop parsing description | Batch 3 #4 | Tests green; no description parsing left |
| BE-1.2 | Reconciliation scheduler using `PaymentGateway.checkStatus()` with back-off; SUCCESS/FAILED+refund-once/pending; admin alert after 24 h | S2, S9, FIN-10 | 4 scenario tests |
| BE-1.3 | `provider_webhook_events` table + disbursement webhook (secret path, dedupe, re-query, amount check) | FIN-10, SEC-05 | Replay test is a no-op |
| BE-1.4 | Fix PendingTransactionCleanupJob (route stale payouts to reconciliation) | FIN-05, S8 | Test |
| BE-1.5 | Catch Throwable in Stage B; DB unique constraint for idempotency race; safe provider error messages | S7, S5, S10 | Tests |
| BE-1.6 | JWT filter checks user status; refresh revocation on password change/deactivation; `POST /auth/logout` | SEC-01, SEC-02, SEC-06 | Tests |
| BE-1.7 | Hash + expire verification/reset tokens; redact logs | SEC-03, SEC-07 | Log-capture test |
| BE-1.8 | Profiles/secrets (no default dev), JWT key length + iss/aud, prod actuator/swagger/CORS lockdown | SEC-04, SEC-08, SEC-11 | Profile tests |
| BE-1.9 | Redis rate limiting (proxy-aware); money DTO bounds; request-ID validation; upload magic bytes | SEC-09, SEC-10, SEC-12 | Tests |
| BE-1.10 | Ordered locks for transfers (if not done), single-rounding fees, per-user aggregate limits, DB constraints/indexes | FIN-06, FIN-09, FIN-11, DB-01 | Tests |

### Block 2: endpoints the app needs (weeks 3–5)

This block follows the Flutter guide: 1.1, 1.4, 1.5, 1.7, 1.10, 2.1, 2.3, 2.5, 2.6, 2.9, 2.11, 2.13, 2.15, 3.1, 3.4. ChatGPT should write those prompts here, in the same format, using doc 02 Phase 1D.

The most important one is **2.5 Collections (top up)**. Use doc 03 section 3 exactly, and test with the Lipila sandbox.

### Block 3: ledger (weeks 6–7)

| Step | Prompt goal | Closes |
|---|---|---|
| BE-3.1 | Migrations: ledger_accounts, journal_entries, ledger_postings, accounting_periods + chart of accounts (doc 02) | FIN-08 |
| BE-3.2 | LedgerService.post(entry) with balance check (debits = credits, else exception and rollback) | FIN-08 |
| BE-3.3 | Dual-write from transfers, collections, payouts, fees, refunds | FIN-08 |
| BE-3.4 | Backfill historical transactions as journal entries | FIN-08 |
| BE-3.5 | Continuous reconciliation: wallet.balance vs ledger; provider clearing vs Lipila balance; alerts | FIN-08 |

### Block 4: payments orchestration (Phase 3) + bills

| Step | Prompt goal |
|---|---|
| BE-4.1 | payment_intents / collection_requests tables; move collections onto them |
| BE-4.2 | Provider router + `provider_routes` (admin kill switch) + `/payment-methods` and `/app/notices` reading it |
| BE-4.3 | BillProvider interface + Lenco (or chosen aggregator) sandbox: billers, validate, pay, airtime/bundles, idempotent; ledger postings to clearing:bills |
| BE-4.4 | Daily statement reconciliation job (download/import provider statement, match, report for admin X09) |

### Block 5: admin backend

Follow Part 7 `ADMIN_GUIDE.md`, backend steps A1.1 … A11.1.

### Block 6: AI gateway (doc 05)

| Step | Prompt goal |
|---|---|
| BE-6.1 | Read-only summary views + internal endpoints (`/ai/summary`, category spend, budgets, goals, upcoming bills) |
| BE-6.2 | Auto-categorisation: merchant rules table + correction learning; optional small-model fallback behind an interface |
| BE-6.3 | `LlmClient` interface + one provider implementation; tool router with only the read-only tools; consent check (O12); per-user limits |
| BE-6.4 | `POST /ai/chat` streaming (SSE) with output filter (no PII, deep links only); `GET /ai/tips` from the rule engine |
| BE-6.5 | Evaluation harness: 100 test questions vs SQL answers + red-team prompts; fail the build if accuracy < 95% |
| BE-6.6 | Risk scoring rules (C1) on transfers/payouts: score, step-up (PIN/selfie), hold, admin queue |

### Block 7: cards (only after a partner contract; doc 04)

| Step | Prompt goal |
|---|---|
| BE-7.1 | Cards module tables + waitlist (can be done now) |
| BE-7.2 | `CardIssuer` interface + sandbox adapter for the chosen partner; create/freeze/controls/fund |
| BE-7.3 | Signed issuer webhooks (auth + settlement), ledger postings, disputes |
| BE-7.4 | Secure-display token endpoint (no PAN/CVV through our servers) |

### Block 8: modular monolith → events → microservices (after the pilot)

| Step | Prompt goal | Closes |
|---|---|---|
| BE-8.1 | ArchUnit rules: no cross-module repository use; fix violations module by module | ARCH-01 |
| BE-8.2 | Split identity and KYC data (kyc_profiles owned by kyc) | ARCH-02 |
| BE-8.3 | One Postgres schema per module (migrations move tables, views for compatibility) | — |
| BE-8.4 | Outbox table + relay to RabbitMQ; notifications become consumers | ARCH-03 |
| BE-8.5 | Extract notification-svc (own repo/module, own DB, consumes events) | Phase 6 |
| BE-8.6 | Extract payment-svc (orchestration + webhooks + reconciliation; ledger stays in core) | Phase 7 |
| BE-8.7 | Extract identity/KYC services + API gateway | Phase 8 |
| BE-8.8 | Production engineering: OpenTelemetry, dashboards, alerts, load/chaos tests, DR drill | Phase 9 |

## 3. Example prompt ChatGPT should produce (BE-1.2)

```text
GOAL: Add the payout reconciliation scheduler (Phase 1B Batch 3). Closes S2, S9 and part of FIN-10.
READ FIRST: docs/architecture/payout-lifecycle.md; report/PHASE_1B_BATCH_2_REMAINING_RISKS.md (S2, S9); payment/LipilaGateway.java (checkStatus); payment/service/PayoutService.java (Stage C).
SCOPE: payment package, a new scheduler class, a Flyway migration if needed, tests. Do not change controllers.
BUILD:
1. Every 60 s (ShedLock or a DB advisory lock so only one instance runs), select payouts in PENDING or PENDING_RECONCILIATION with next_check_at <= now, limit 50.
2. For each, call PaymentGateway.checkStatus(reference) outside any DB transaction.
3. SUCCESS → mark SUCCESSFUL (short transaction). FAILED → mark FAILED and post the compensating credit exactly once (reuse the Stage C guard from S1). UNKNOWN/timeout → attempts+1, next_check_at with back-off 1,2,5,10,30 min then hourly; after 24 h create an admin alert row and stop auto-checking.
4. Remove checkStatus dead-code warnings (it now has a caller).
TESTS (Testcontainers Postgres + mocked gateway): success path, failure + single refund (run twice → still one refund), unknown → back-off, 24 h → alert, concurrent scheduler instances → no double processing.
DONE WHEN: all tests pass; risk register S2 and S9 marked MITIGATED with evidence links.
FINISH: run ./mvnw test, fix failures, update the risk register, list changed files and migrations.
```
