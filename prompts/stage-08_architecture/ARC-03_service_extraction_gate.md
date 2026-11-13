# ARC-03 — Service extraction gate (do NOT run unless triggered)

**Area:** docs only · **Needs:** production running for months · Roadmap Phases 6–8

---
Read `implementation-roadmap.md` Phases 6–8 and `microservices-readiness.md`.

Microservices add cost and risk. A single well-structured deployable is right for the pilot and well beyond. This prompt produces a **decision document**, not code.

Write `docs/architecture/extraction-assessment.md`:
1. Measured triggers (with real numbers from monitoring): deploy contention between teams, a module with a very different scaling profile (e.g. notifications volume), provider isolation needs for uptime, compliance isolation (card data), team size > 1 squad.
2. For each candidate (notifications → payments → identity/KYC, in the roadmap's order): current coupling (from the architecture tests), data ownership, events already published, expected benefit, cost (infra, on-call, distributed transactions), risk.
3. Verdict per candidate: extract now / not yet, with the trigger that would change it.
4. If any "extract now": a step plan (strangler pattern, outbox → broker, contract tests, dual-run, rollback).

Stop. The owner decides. Extraction itself is a new project with its own prompts.
