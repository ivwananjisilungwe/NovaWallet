# OPS-01 — Containers & environments

**Area:** infra, novawallet-api (Dockerfile/compose only) · **Needs:** BE-99

---
Read root `AGENTS.md`, `infra/AGENTS.md`, `docs/v5/part-08/01_SYSTEM_ARCHITECTURE.md`, existing `novawallet-api/Dockerfile`, `docker-compose.yml`, `.env.example`.

Do:
1. **API image:** multi-stage build (JDK 17 build → slim JRE 17 runtime), non-root user, read-only filesystem except a mounted uploads volume and tmp, healthcheck on the actuator health endpoint, JVM memory flags for a small VM, pinned base image digests, `.dockerignore` tight. `hadolint` clean.
2. **Compose files** in `infra/compose/`: `local.yml` (api + postgres 16 + mailhog-like mail catcher, sandbox profile, seeded), `staging.yml` and `production.yml` (api blue + api green + postgres + caddy; no DB port published; named volumes; resource limits; restart policies; log rotation).
3. **Config matrix** `infra/ENVIRONMENTS.md`: every env variable × local/staging/production, where its value comes from, which are secrets. `infra/env/*.example` files only.
4. Spring profiles: `dev`, `sandbox`, `staging`, `prod`. Prod profile: simulator provider impossible to enable, Swagger off, placeholder fees refused, strict secrets validation.
5. Flyway as a separate one-shot `migrate` service/command so migrations run before the new version takes traffic.
6. `infra/scripts/`: `up-local.sh`, `seed-sandbox.sh`, `logs.sh` — `shellcheck` clean.

Gates: `docker compose -f infra/compose/local.yml config` valid; build the image; start local; health green; seeded login works (curl evidence). Report image size.
