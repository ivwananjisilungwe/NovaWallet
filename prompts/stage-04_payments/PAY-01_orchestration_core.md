# PAY-01 — Payment orchestration core 🔒

**Area:** novawallet-api · **Needs:** LED-04 · **Screens (behaviour source):** F05–F08, G06, I03, P03, X10

---
Follow `prompts/stage-02_backend-app-api/_COMMON_FEATURE_RULES.md`. Use T5 plan-first. Read `implementation-roadmap.md` Phase 3, `payout-lifecycle.md`, `docs/v5/part-08/03_MOBILE_MONEY_INTEGRATION.md`, the state diagrams in `docs/v5/part-10/`.

Goal: one provider-agnostic engine for every external money movement, so Lipila today and other providers later plug in without touching business flows.

Do:
1. **Model:** `payment_intents` (id, type COLLECTION | DISBURSEMENT | BILL | AIRTIME | CARD_FUND, wallet, amount, fee, currency, provider, status, idempotency key, references, timestamps, failure code) and `payment_attempts` (per provider call: request id, provider reference, raw status, timings). Immutable history of status changes.
2. **State machine** (explicit, tested, illegal transitions rejected): CREATED → FUNDS_HELD (outbound) → SUBMITTED → PENDING → SUCCEEDED | FAILED | UNKNOWN(PENDING_RECONCILIATION) → …; terminal states final; every transition posts the right ledger entries.
3. **Provider SPI:** `PaymentProvider` interface (capabilities, initiateCollection, initiateDisbursement, queryStatus, nameCheck, parseWebhook/verify, map error codes). Routing by network/type/config with a **kill switch per provider and per operation** (X10) and health tracking (auto-pause on error-rate threshold → alert).
4. **Sandbox simulator provider** (non-production profiles only): scriptable outcomes — success, decline, timeout-then-success, timeout-unknown, duplicate webhook, late webhook, amount mismatch. This is what the app and tests run against.
5. Generic webhook pipeline: raw-body capture, signature verification delegated to the provider adapter, durable dedup (`provider_webhook_events`), idempotent state transitions.
6. Unified reconciliation scheduler (generalise the existing payout one: lease, backoff, max age → manual queue).
7. Customer-facing status + stable error codes mapped to the messages in `03_MOBILE_MONEY_INTEGRATION.md` §6.
8. Admin endpoints for X10 (provider status, toggle with maker-checker, recent failures).

No existing endpoint behaviour changes in this prompt. Tests: state-machine table tests, simulator scenarios end to end with ledger assertions, kill switch, routing.
