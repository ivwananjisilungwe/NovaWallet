# Common rules for every backend feature prompt (BE-1D, LED, PAY, AI, ARC)

1. Read root `AGENTS.md`, `novawallet-api/AGENTS.md`, `docs/DECISIONS.md`, `docs/api/CONTRACT_TARGET.md`, and for each endpoint the **screens that use it**: open their `BUILD.md` ("Data and backend", "States", "Rules") via `docs/v5/SCREEN_INDEX.md`. The response must carry every field those screens show. Do not guess fields — derive them from the screens.
2. **Mini-plan first** in `reports/<ID>_PLAN.md`: endpoints (method, path, request, response, error codes), tables/migrations, files, tests, decisions needed. If a required decision is missing in `docs/DECISIONS.md`, stop after the plan.
3. Build in thin vertical slices: migration → entity/repository → service (ownership, validation, idempotency, audit) → controller (OpenAPI annotations) → tests.
4. Every endpoint: authentication, ownership check, DTO bounds, rate limit class (BE-1C-7), error codes from `docs/api/ERROR_CODES.md` (add new codes there), audit where it changes state.
5. Money-moving endpoints: `Idempotency-Key`, email-verified policy, KYC tier/limits, PIN, deterministic locks, and (after LED-03) ledger postings. Never hold a DB transaction across a provider call.
6. Tests: happy path, validation, authz/IDOR, idempotent replay, concurrency where money moves, and one test per error code.
7. Update `reports/API_ENDPOINT_INVENTORY.md` and tick the rows in `docs/api/CONTRACT_TARGET.md`.
8. Gates from `novawallet-api/AGENTS.md` (full suite + PostgreSQL 16 fresh/upgrade + diff check). Write `reports/<ID>_REPORT.md`. Stop. No commit.
