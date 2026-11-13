# ARC-01 — Modular monolith & schema boundaries 🔒

**Area:** novawallet-api · **Needs:** AI-04 (all backend features in place)

---
Follow `prompts/stage-02_backend-app-api/_COMMON_FEATURE_RULES.md`. Use T5 plan-first. Read `implementation-roadmap.md` Phase 4, `domain-boundaries.md`, `microservices-readiness.md`, `docs/v5/part-08/01_SYSTEM_ARCHITECTURE.md`, module map diagram in `docs/v5/part-10/`.

Goal: enforce module boundaries inside the single deployable. **No behaviour change, no API change.**

Do:
1. Modules: identity (auth, users, devices, PIN), kyc, wallet, ledger, payments (orchestration + providers), transfers, bills, savings (goals, chilimba, budgets), notifications, support, risk, ai, admin, platform (config, audit, files, security). Each exposes a small public API package; internals are package-private.
2. **Enforce** with architecture tests (ArchUnit or Spring Modulith — pick one, justify): no cross-module access to internals/repositories/entities; no cycles; only the ledger module writes ledger tables; only payments talks to providers.
3. Replace cross-module entity references with ids + module API calls (e.g. KYC decoupled from the `User` entity as the roadmap says).
4. Schema ownership: document which module owns each table; cross-module foreign keys listed and justified or removed in later migrations; optional per-module PostgreSQL schemas only if the plan shows a safe expand → migrate → contract path.
5. Move schedulers to their owning modules; single place for scheduling config and leases.
6. Refactor in small steps, full suite after each module. Zero test deletions.

Evidence: architecture tests green, endpoint inventory unchanged (diff the OpenAPI before/after — must be identical), performance sanity unchanged.
