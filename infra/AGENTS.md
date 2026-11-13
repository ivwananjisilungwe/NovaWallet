# AGENTS.md — infra (deployment, CI/CD, operations)

Read the root `AGENTS.md` first. Deployment strategy source: `docs/v5/part-08/01_SYSTEM_ARCHITECTURE.md` and the OPS prompts (Docker, Caddy, GitHub Actions, blue-green → rolling → canary, expand-migrate-contract, Play staged rollout). Diagrams: `docs/v5/part-10/` (D25 security zones, D26 CI/CD).

## Rules
1. **Infrastructure as code.** Everything reproducible from the repo: `infra/` holds compose files, Caddyfile, scripts, CI workflows, runbooks. No hand-configured servers without a script + doc.
2. **Environments:** `local`, `staging`, `production`. Staging mirrors production topology at smaller size. Production config never in the repo; templates only (`*.example`).
3. **Secrets:** GitHub Actions secrets / server env files with 600 permissions / a secret manager. Never echo secrets in CI logs. Rotate on exposure.
4. **Deploys:** build once, promote the same image digest staging → production. Migrations run as a separate step before traffic switch, always backward compatible (expand → migrate → contract). Blue-green with health checks and instant rollback to the previous digest.
5. **Data:** PostgreSQL backups encrypted, off-site, tested by restore drills. No production data in staging/local.
6. **Network:** only 80/443 public (Caddy). DB, admin API paths, metrics not exposed publicly. SSH key-only, no root login, firewall default deny, fail2ban or equivalent, automatic security updates.
7. **Observability:** structured JSON logs with request id (no PII/secrets), metrics, uptime checks, alerts that go to a human.
8. **The agent never connects to a real server or runs deploy commands.** It writes scripts, configs, and runbooks; the owner executes them and pastes outputs back if needed.

## Gates
Lint configs (`docker compose config`, `caddy validate` via container, `actionlint`, `shellcheck`, `hadolint`). Local dry run with `docker compose` where possible. Report exactly what was validated locally vs. what the owner must run on the server.
