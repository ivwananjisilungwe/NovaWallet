# AGENTS.md — NovaWallet (repository root)

You are working in the NovaWallet monorepo: a student-first mobile wallet for Zambia. This file applies everywhere. Each sub-project has its own AGENTS.md with more rules; **read the one for the folder you touch before doing anything.**

| Folder | What | Its rules |
|---|---|---|
| `novawallet-api/` | Spring Boot 3.5 backend, PostgreSQL, Flyway | `novawallet-api/AGENTS.md` |
| `novawallet-app/` | Flutter mobile app | `novawallet-app/AGENTS.md` |
| `novawallet-admin/` | React + Vite + TS admin console | `novawallet-admin/AGENTS.md` |
| `infra/` | Docker, Caddy, CI/CD, monitoring | `infra/AGENTS.md` |
| `docs/` | SRS, architecture, v5 designs (`docs/v5/part-01..10`), decisions | read-only unless a prompt says update |
| `reports/` | Batch reports, risk matrix, endpoint inventory | update only as instructed |
| `prompts/` | This prompt pack | read-only |

## Universal hard rules
1. **Git writes only on explicit owner request**: do not commit, push, merge, rebase, reset, stash-drop, tag, or force anything unless the owner explicitly asks for it in this session. When asked, confirm the exact commands and scope first, then execute. Leave changes in the working tree by default.
2. **Stay in scope.** Only touch folders the prompt names. If a fix is needed elsewhere, report it; don't do it.
3. **Never invent external facts**: provider APIs, signature schemes, fees, limits, legal rules, regulator requirements. If missing, write `BLOCKED: <what is needed>` and stop that part.
4. **Decisions**: read `docs/DECISIONS.md`. If the prompt needs a decision that is not recorded, stop and ask with options + your recommendation.
5. **No secrets** in code, tests, logs, docs, or reports. Use env vars and `*.example` files.
6. **No real money, no live keys.** Sandbox/test modes only unless the prompt is `LAUNCH-02` and the owner has provided live config outside the repo.
7. **Evidence over claims.** Never write "tests pass" without running them and quoting counts. Never claim production readiness, regulatory compliance, or certification.
8. **Money**: integer minor units (ngwee) or BigDecimal with explicit scale on the server; never floating point. Every money mutation is idempotent and auditable.
9. **Privacy**: minimum data, mask phone/ID numbers in UI and logs, no PII to third-party AI or analytics without a recorded decision.
10. When finished, **stop** and report (format below). Do not start the next prompt.

## Report format (end every session with this)
```
## Report — <prompt id>
Done: <bullets>
Files changed: <count + list of key files>
Evidence: <commands run, exit codes, test counts, migration checks>
Blocked / not done: <BLOCKED: … items>
Decisions needed: <DEC-xx questions with recommendation>
Risks noticed (not fixed): <bullets with suggested prompt>
Next: <next prompt id from prompts/01_MASTER_ORDER.md>
```
