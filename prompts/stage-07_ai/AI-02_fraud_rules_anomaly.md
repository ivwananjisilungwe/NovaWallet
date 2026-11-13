# AI-02 — Fraud rules engine & anomaly signals 🔒

**Area:** novawallet-api · **Needs:** AI-01 · **Screens:** X02 alerts, X04, X08

---
Follow `_COMMON_FEATURE_RULES.md`. Read `docs/AI-STRATEGY.md` Phase B and `05_AI_FEATURES.md` Phase C. Rules first; no ML model in this prompt.

Do:
1. **Risk engine** evaluated synchronously before money leaves (transfer, payout, withdraw, bill, card fund) and asynchronously after events: returns ALLOW / STEP_UP (PIN + OTP) / HOLD_FOR_REVIEW / BLOCK with reason codes.
2. **Rules** (config-driven, versioned, each with tests): velocity by amount/count, new device + large payout, new recipient + large amount, rapid in-then-out (mule pattern), many senders to one receiver, repeated failed PINs then success, payout to a number seen on many accounts, account age, night-time anomalies vs own baseline, PIN reset cooling-off.
3. **Signals store:** per-user rolling features (counts, sums, distinct recipients/devices) updated transactionally or via scheduled aggregation.
4. **Case workflow:** HOLD creates a review item for admins (X02 alerts, X04/X08 context) with approve/reject under maker-checker; customer sees an honest pending state; SLA timers; release or reverse via ledger recipes.
5. **Shadow mode** per rule: log what it would have done before enforcing; report false-positive estimates.
6. Explainability: every decision stored with rule ids and feature values (no PII in logs).
7. Legal note: suspicious-activity reporting obligations are for counsel/compliance (LAUNCH-01); provide the data export they will need, do not auto-file anything.

Tests: each rule boundary, shadow vs enforce, hold → release/reverse ledger assertions, no bypass via idempotent replay, performance budget (< 30 ms added at p95).
