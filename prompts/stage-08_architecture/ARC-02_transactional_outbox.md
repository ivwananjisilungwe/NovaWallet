# ARC-02 — Transactional outbox & domain events 🔒

**Area:** novawallet-api · **Needs:** ARC-01

---
Follow `_COMMON_FEATURE_RULES.md`. Read `implementation-roadmap.md` Phase 5.

Goal: side effects (notifications, push, emails, risk signals, insights updates, webhooks to partners) happen reliably **after** the business transaction commits, without a message broker.

Do:
1. `outbox_events` table (id, aggregate, type, payload JSON with schema version, created, available_at, attempts, status, lease). Events are written **in the same DB transaction** as the business change.
2. Dispatcher: polls with `FOR UPDATE SKIP LOCKED`, lease + backoff + max attempts → dead-letter state with admin visibility; at-least-once delivery; handlers idempotent (processed-events table per consumer).
3. Event catalogue `docs/architecture/events.md`: name, version, payload fields (no secrets, minimal PII), producers, consumers. Start with: UserRegistered, KycStatusChanged, TransferCompleted, PaymentIntentStatusChanged, PayoutUnknownOutcome, GoalDeposited, SecurityEvent, CaseUpdated.
4. Migrate existing direct calls to events one consumer at a time: notifications/push/email first, then risk signals, then insights caches. Money correctness must never depend on an outbox consumer.
5. Ordering: per-aggregate ordering key; document what is and is not ordered.
6. Operations: metrics (lag, dead letters), cleanup job, replay tool for a dead-lettered event (admin, audited).
7. No Kafka/RabbitMQ. Document how the dispatcher could later publish to a broker (Phase 6+).

Tests: crash after commit before dispatch → event still delivered; duplicate delivery → handler idempotent; poison event → dead letter; concurrency with two dispatcher instances (PostgreSQL 16).
