# OPS-03 — CI/CD: build once, blue-green deploy, safe migrations 🔒

**Area:** infra, .github · **Needs:** OPS-02

---
Read `infra/AGENTS.md`, the CI/CD diagram in `docs/v5/part-10/` (D26).

Do:
1. **CI (every PR):** API tests (H2 + PostgreSQL 16 service container), architecture tests, OpenAPI snapshot check, dependency vulnerability scan, secret scan (gitleaks), image build + scan (trivy), `actionlint`. Required checks documented for branch protection (owner enables).
2. **Build once:** on merge to main, build the image, tag with the git SHA, push to a registry (GHCR), record the digest. The **same digest** is promoted staging → production.
3. **Deploy to staging (automatic):** SSH with a deploy key from GitHub secrets → `infra/scripts/deploy.sh <digest>`:
   pull → run Flyway migrate (backward-compatible only) → start the idle colour → wait for health + smoke tests → switch Caddy upstream → keep the old colour for instant rollback → stop it after a soak period.
4. **Deploy to production (manual approval)** via a protected GitHub environment; same script; a pre-deploy DB backup step (OPS-04) and a post-deploy smoke + daily-proof check.
5. `rollback.sh`: switch back to the previous colour/digest in seconds; documents when a migration blocks rollback and why expand → migrate → contract prevents that.
6. **Migration policy check** in CI: fail on destructive statements (DROP/ALTER TYPE/NOT NULL without default…) unless the PR carries an explicit `contract-phase` label.
7. Later strategies documented, not built: rolling (two VMs) and canary (weighted upstreams) with the triggers for adopting them.
8. `infra/DEPLOY_RUNBOOK.md`: first deploy, routine deploy, rollback, hotfix.

Gates: workflows linted; `deploy.sh` exercised locally against the compose stack (blue → green → rollback) with output in the report. The agent never deploys to a real server.
