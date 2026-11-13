# T4 — Owner gate checklist (you run this, not the agent)

Before committing any step:

**Read**
- [ ] Agent report read. Every `BLOCKED` / `DECISION NEEDED` understood.
- [ ] 🔒 step: T1 review done, verdict APPROVE (after T2 fixes if needed).

**Run yourself**
- Backend: `cd novawallet-api && ./mvnw -B clean test` → counts match report, 0 failures.
- Backend with migrations: start a disposable PostgreSQL 16, run the app or Flyway → fresh and upgrade both OK.
- App: `cd novawallet-app && make gate` (format, analyze, build_runner, test) → 0 issues.
- Admin: `cd novawallet-admin && npm run lint && npm run typecheck && npm test && npm run build`.
- Infra: `docker compose -f infra/... config` OK.

**Look**
- [ ] `git status --short` — only expected files. No `.env`, keystores, dumps, `uploads/` content.
- [ ] `git diff --check` clean.
- [ ] Quick secret grep: `git diff | grep -iE "secret|password|apikey|api_key|token|BEGIN (RSA|EC|OPENSSH)"` — inspect every hit.
- [ ] App/admin: you opened it and clicked through the new screens (light + dark).

**Commit**
```
git add -A   # after checking status
git commit -m "<PROMPT-ID>: <summary>"
```
Push when you're ready. Tick the step in `01_MASTER_ORDER.md`.
