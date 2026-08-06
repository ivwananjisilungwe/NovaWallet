# NovaWallet — Repository Scan Report

> Generated: 2026-08-06 · Methodology: repo-scan (classify surface → detect embedded 3rd-party → score modules → flag risks)

## 1. What this is

**NovaWallet** — a digital wallet backend (Zambia-focused: mobile money, KYC, virtual cards) built as a **Spring Boot 3.5.3 / Java 17** REST API with PostgreSQL 16 and Flyway migrations. Per the SRS, the **MVP is complete** — Phases 0–9 built and tested. Remaining work: Flutter mobile app + Flutterwave payment integration.

## 2. Repository surface

| Category | Count | Notes |
|---|---|---|
| Project code (main) | 122 Java files, ~8,100 LOC | 18 packages under `com.novawallet.novawallet_api` |
| Tests | 29 classes, ~4,100 LOC | Integration-heavy (H2); surefire reports present |
| Migrations | 10 Flyway scripts (V1–V10) | Clean sequential schema evolution |
| Config | 3 yml + logback + Dockerfile + compose | dev/prod profile split, env-var driven |
| Embedded 3rd-party | **0** | All dependencies Maven-managed — nothing vendored |
| Build artifacts | `target/` 3.6 MB | Compiled classes + surefire reports (git-ignored) |
| Documentation | Spec, SRS, user flows, phase reports | Plus 40 Stitch UI design prompts in `.planning/` |
| CI/CD | 1 workflow | `novawallet-api/.github/workflows/ci.yml` |

## 3. Dependencies

Spring Web / Data-JPA / Security / Validation / AOP / Cache / Mail / Actuator, **PostgreSQL 16**, **Flyway**, **JJWT 0.12.7**, **Caffeine**, **springdoc 2.8.9**, **Lombok**, **logstash-logback 8.0**. Test: H2, spring-security-test. All current-generation, nothing EOL.

## 4. Module verdicts

| Module | LOC (pre-refactor) | Verdict |
|---|---|---|
| transaction | 1,248 | 🟢 Core Asset — `TransactionService` was 443 LOC (split target) |
| admin | 890 | 🟢 Core Asset — `AdminController` was 436 LOC (split target) |
| kyc | 890 | 🟢 Core Asset |
| auth | 762 | 🟢 Core Asset |
| notification | 703 | 🟢 Core Asset |
| fee / idempotency / wallet / user / security / audit / token | — | 🟢 Core Asset |
| config / exception / bootstrap / common / filter | — | 🟢 Core Asset (small, fine) |

No Extract/Rebuild/Deprecate candidates beyond the two splits. No duplicated wrappers, no dead-weight modules, no vendored code.

## 5. Structural risks (post-2026-08-06 hardening)

| Severity | Finding | Status |
|---|---|---|
| 🔴 | No git repository — no history, no rollback | ✅ Fixed — `git init` + baseline commit |
| 🟠 | Default admin password `Admin@123` in prod config | ✅ Fixed — now fail-fast `${APP_ADMIN_PASSWORD}` |
| 🟠 | Default DB password `changeme_in_production` in compose | ✅ Fixed — `${DB_PASSWORD:?}` fail-fast |
| 🟠 | Hardcoded JWT dev-secret fallback in base `application.yml` | ⚠️ Acceptable — dev-only; prod overrides with `${JWT_SECRET}` (required) |
| 🟡 | God-class risk: `TransactionService` (443), `AdminController` (436) | ✅ Fixed — both split into cohesive units |
| 🟡 | No frontend in repo | 📌 Planned — Flutter app; active Stitch UI design in `.planning/` |
| 🟢 | CI existed but unverified recent | ✅ Verified — `ci.yml` runs `mvn test`, builds/pushes image to GHCR |

## 6. What's done well

- ✅ **No SQL injection** — all 11 `@Query` calls use parameter binding (`:prefix`, `:id`), zero string-concatenated SQL
- ✅ **No secrets in Java source** — clean scan
- ✅ **No TODO/FIXME** anywhere
- ✅ **Prod config env-var driven** (`${JWT_SECRET}`, `${DATABASE_PASSWORD}`, now `${APP_ADMIN_PASSWORD}`)
- ✅ **Strong test discipline** — concurrency, idempotency, rate-limit, refresh-rotation integration tests
- ✅ **Banking-grade concerns first-class** — idempotency keys, audit log aspect, pessimistic locking, atomic balance updates, KYC tier limits
- ✅ **Dockerized** — multi-stage build, non-root user, healthchecks, named volumes, mailpit dev profile

## 7. Recommended next steps

1. **Push to remote** (GitHub) — the CI workflow will verify everything on the first push
2. **Wire the SMS provider** or document the stub as intentional pre-launch
3. **Add `dependency-check` / Dependabot** to the CI for CVE scanning
4. **Set up the remote `.env`** from `.env.example` before any real deploy