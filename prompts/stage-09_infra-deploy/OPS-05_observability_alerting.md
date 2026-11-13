# OPS-05 — Observability & alerting

**Area:** infra, novawallet-api (metrics/tracing config) · **Needs:** OPS-04

---
Read `infra/AGENTS.md`, `novawallet-api/AGENTS.md`.

Do:
1. **Metrics:** Micrometer → Prometheus. Business metrics: payment intents by type/status/provider, success rate, time-to-final-status, pending/unknown count and age, reconciliation exceptions, daily-proof status, ledger drift, webhook rejects, OTP sends, login failures, KYC queue size, outbox lag/dead letters, AI gateway spend. Technical: latency histograms per route, error rates, DB pool, JVM, scheduler runs.
2. **Logs:** JSON to stdout → Loki (or the simplest equivalent the VM can afford) with retention; request id searchable; the log-leak test from BE-1C-8 runs in CI.
3. **Tracing:** OpenTelemetry auto-instrumentation with sampling; provider calls as spans (no payload capture).
4. **Dashboards as code** (Grafana JSON in `infra/monitoring/dashboards/`): Money flow, Providers, Security, System, Queue/Jobs.
5. **Alerts as code** with severity and runbook link: API down / health failing, error rate, p95 latency, payment success rate drop per provider, unknown-outcome intents older than SLA, daily proof failed, ledger drift ≠ 0, backup failed/old, disk > 80 %, certificate expiry, dead letters > 0, brute-force spikes. Routed to a channel a human sees (email/Telegram/WhatsApp gateway — owner chooses); test alert procedure.
6. External uptime check from outside the server (health + a synthetic login on staging).
7. Resource-light deployment (single small VM): document memory budget; allow a hosted free-tier alternative if the VM is too small.
8. Metrics/Grafana never public: behind auth + IP allow-list.

Gates: local compose stack with monitoring up; screenshots or exported dashboard JSON; each alert rule unit-tested (promtool) or exercised.
