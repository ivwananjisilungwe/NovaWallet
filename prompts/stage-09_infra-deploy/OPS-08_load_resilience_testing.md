# OPS-08 — Load, failure and recovery testing

**Area:** infra, test tooling · **Needs:** OPS-07

---
Do (staging/sandbox only):
1. **Load model** in `infra/loadtest/` (k6 or Gatling): realistic student mix — mostly balance/activity reads, bursts at month-start (allowance day), top-ups, P2P sends, bill payments. Targets: 500 concurrent users, 50 money ops/s peak on the pilot VM; p95 reads < 300 ms, money ops < 800 ms (excluding provider time).
2. **Correctness under load:** after every run: ledger trial balance zero, wallet = ledger, no negative balances, no stuck intents, idempotency honoured, daily proof passes. A run that is fast but wrong fails.
3. **Failure injection** with the simulator and compose tooling: provider timeouts/5xx, duplicate and delayed webhooks, DB restart mid-traffic, API container kill during a payout (each stage), slow DB, disk full on uploads, clock skew, outbox dispatcher crash, SMS provider down (OTP degradation path).
4. **Recovery drills:** blue-green switch under load with zero failed requests; rollback under load; restore backup into a new instance and resume (ties to OPS-04).
5. **Capacity notes:** DB connections, JVM heap, CPU per ops/s; the point at which the growth plan (two app VMs, read replica) triggers.
6. Report `reports/LOAD_RESILIENCE_REPORT.md` with graphs/numbers, bottlenecks found, fixes done or filed.
