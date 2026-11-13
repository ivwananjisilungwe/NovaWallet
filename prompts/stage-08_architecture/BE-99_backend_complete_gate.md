# BE-99 — Backend complete: contract freeze & hand-over to the app 🔒

**Area:** novawallet-api, docs/api · **Needs:** ARC-02 · This is the gate between the backend track and the app track.

---
Read root `AGENTS.md`, `novawallet-api/AGENTS.md`, `docs/api/CONTRACT_TARGET.md`, `docs/v5/SCREEN_INDEX.md`. No new features in this prompt.

Do:
1. **Coverage proof:** update `docs/api/CONTRACT_TARGET.md` → for every one of the 127 app screens and 16 admin screens: each endpoint it needs → EXISTS (controller file) / BLOCKED (reason, feature flag name) / MISSING. Zero MISSING allowed; BLOCKED ones must work against the sandbox simulator.
2. **Freeze the contract:** export OpenAPI to `docs/api/openapi.json` (deterministic ordering), write `docs/api/CONTRACT.md` (auth flows, token lifetimes, headers `Idempotency-Key`, `X-Request-Id`, `X-App-Version`, `X-Device-Id`; pagination; money format in ngwee; date/time format; error envelope; every error code from `ERROR_CODES.md` with the user-facing meaning; feature flags; rate-limit behaviour; pending/unknown payment semantics with polling guidance). Add a CI check that fails when the API changes without updating the snapshot.
3. **Sandbox data & tooling:** a `sandbox` profile + seed script creating demo users at each KYC level, wallets with history (all transaction types and statuses incl. pending/unknown), notifications, goals, a chilimba group, bills catalogue, notices, staff users for each admin role. The simulator provider scripted by magic amounts/numbers documented in `docs/api/SANDBOX.md` (e.g. amount ending .01 → decline, .02 → timeout-unknown).
4. **HTTP collection:** `docs/api/novawallet.http` (or Postman JSON) covering every endpoint with the seeded users.
5. **Full regression:** complete suite on H2 + PostgreSQL 16, migration fresh/upgrade, architecture tests, dependency vulnerability report, secret scan, performance smoke (key endpoints p95), and a 30-minute soak with the simulator (no stuck intents, daily proof passes).
6. `reports/BACKEND_COMPLETE_REPORT.md`: what is live-ready vs simulator-only (blocked on external contracts), remaining risks, and the exact command to start the backend locally for app development (`docker compose` with the sandbox profile).

Acceptance: an app developer can run one command, log in as a seeded user, and exercise every screen's API. Stop.
