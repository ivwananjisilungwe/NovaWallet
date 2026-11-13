# NovaWallet

Student-first mobile wallet for Zambia: send, receive, top up, withdraw, and budget, with mobile money underneath.

**New here?** Read [`StartHere.md`](StartHere.md), then follow [`prompts/01_MASTER_ORDER.md`](prompts/01_MASTER_ORDER.md). Agent rules: [`AGENTS.md`](AGENTS.md). Build progress: [`reports/opencode/PROGRESS.md`](reports/opencode/PROGRESS.md).

## Folder map
| Folder | What |
|---|---|
| `novawallet-api/` | Spring Boot 3.5 backend (Java 17 target), PostgreSQL 16, Flyway |
| `novawallet-app/` | Flutter mobile app (not started) |
| `novawallet-admin/` | React + Vite + TypeScript admin console (not started) |
| `infra/` | Docker, Caddy, CI/CD, monitoring (not started) |
| `docs/` | SRS, designs (`docs/v5`, index in `docs/v5/SCREEN_INDEX.md`), provider docs, decisions |
| `reports/` | Verification reports; per-prompt reports in `reports/opencode/` |
| `prompts/` | Ordered build prompts |

## Run the API locally
Needs a full JDK 17+ (not just a JRE) and Docker.

```bash
cd novawallet-api
cp .env.example .env          # fill in local-only values; never commit .env
docker compose up -d          # PostgreSQL
./mvnw spring-boot:run        # dev profile per application.yml
./mvnw -B clean test          # full test suite (H2 in PostgreSQL mode)
```

If `java -version` shows a runtime without a compiler, point Maven at a JDK, e.g.
`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw -B clean test`.

## Status
Sandbox only. Not production-ready and not a compliance claim; launch depends on provider contracts, licensing and legal review (see `prompts/02_DECISIONS_AND_BLOCKERS.md`).
