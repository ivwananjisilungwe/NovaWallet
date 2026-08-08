# NovaWallet — AGENTS.md

Project instructions for AI agents (opencode and Claude Code). Read this before starting any task.

## What this repo is

Full-stack digital wallet for Zambian students/small businesses:

| Directory | Stack | Agent | Verify with |
|---|---|---|---|
| `novawallet-api/` | Java 17 · Spring Boot 3.5.3 · PostgreSQL · Flyway | `backend` | `./mvnw test` |
| `novawallet-app/` | Flutter 3.x · Dart · Riverpod 2.6 · go_router | `mobile` | `flutter analyze && flutter test` |

## How to work in this repo

1. Read this file first. Identify which subfolder(s) the task touches.
2. Use the `orchestrator` agent, which reads `AGENTS.md`/`CLAUDE.md`, detects the stack, loads the matching skills, then delegates to the right subagent:
   - Flutter/`novawallet-app` → **frontend + backend skills** (mobile stack)
   - Spring Boot/`novawallet-api` → **backend skills**
   - Both → dispatch in parallel, one specialist per subfolder
3. Load and follow the skills' instructions before/as you work.

## Skills to use per stack

| Work area | Skills |
|---|---|
| Mobile (Flutter) | `ui-ux-pro-max`, `ui-styling`, `sleek-design-mobile-apps`, `design-system`, `e2e-testing`, `browser-qa`, `error-handling`, `codehealth-mcp`, `tdd-workflow` |
| Backend (Spring Boot) | `error-handling`, `ai-regression-testing`, `codehealth-mcp`, `production-audit`, `gsd-secure-phase`, `tdd-workflow` |
| Architecture / onboarding | `codebase-onboarding`, `architecture-decision-records`, `graphify` |
| Debugging | `agent-introspection-debugging`, `click-path-audit`, `ai-regression-testing` |
| Security review | `gsd-secure-phase`, `production-audit` |

## Conventions

- **Never write tests without running them.** Backend: `./mvnw test` (dev profile uses H2). Mobile: `flutter test`.
- **Security first**: no hardcoded secrets, PIN-protected transfers lock wallets with pessimistic locking (`SELECT ... FOR UPDATE`), idempotency keys for financial ops.
- **API style**: `/api/v1/...`, JSON envelope responses, JWT access + refresh rotation, rate limiting.
- **Follow existing structure**: `novawallet-api/src/main/java` packages (controller/service/repository/security) and `novawallet-app/lib` (`core/`, `features/`, `models/`, `app/`).
- Do not add new frameworks/dependencies the repo doesn't already use.
- Financial code is sensitive: transfers, fees, wallet balance changes, idempotency, and locking must stay intact — never silently drop a check.
- Docs to consult: `README.md`, `SRS.md`, `SPECIFICATION.md`, `FULLSTACK-SCAN-REPORT.md`.

## Do not

- Read `.env`, `.env.*`, `.secrets` files (denied).
- Remove or bypass existing tests. Keep coverage for auth, transactions, KYC, admin endpoints.
- Commit unless explicitly asked.