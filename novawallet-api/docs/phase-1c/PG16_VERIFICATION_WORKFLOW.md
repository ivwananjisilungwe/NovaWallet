# PostgreSQL 16 Verification Workflow (Phase 1C / Wave 0)

**Document:** `PG16_VERIFICATION_WORKFLOW.md`  
**Task:** W0-3c / W0-FINAL-DOC — Correct reproducible PostgreSQL 16 fresh/upgrade verification workflow for Wave 0 closure  
**Date:** 2026-11-14 (reconciled to final implemented `pgtest.sh` behavior and verified runtime evidence)  
**Status:** Documentation aligned with implemented artifacts in this workspace (`novawallet-api/scripts/pgtest.sh`, pgtest profile/test, CI job definitions) and with **verified final runtime evidence** recorded in §7. **CI jobs are configured (Temurin 17) but not run in this session.** Distinguishes implemented artifacts, runtime-verified outcomes, and remaining caveats.

## 1. Purpose and scope

This document describes the reproducible PostgreSQL 16 (PG16) verification workflow for NovaWallet API (Phase 1C / Wave 0): fresh `V1→V16` migration, upgrade `previous-head→V16` migration (implemented as **Flyway CLI baseline at target V15 → seed representative data → migrate to V16/head + validate + smoke/data-preservation checks**), Flyway `validate`, and PG16-tagged tests, plus CI job behavior.

**Non-substitutes (hard rule):**
- **H2 is not a substitute for PostgreSQL 16** for migration, locking, constraint, isolation, or Flyway/PostgreSQL-behavior verification. Default `./mvnw` tests run against H2 (`application-test.yml`, `MODE=PostgreSQL`) and explicitly exclude the `postgres` tag by default (`novawallet-api/pom.xml` Surefire config).
- **PostgreSQL 18.x (or any non-16 local instance) is not a substitute** for PG16 parity verification. Local `psql --version` observed as PostgreSQL 18.6 in prior audit evidence (`docs/PROJECT_STATE_AUDIT.md`) does not satisfy a PG16 requirement.

**Runtime-parity caveat:** the upgrade pre-V16 baseline is established via **dockerized Flyway CLI** (`flyway/flyway:10` by default) `migrate -target=15` using mounted SQL migrations and the V12 Java migration provided via a temporary jar built from compiled `target/classes` (**not** by booting the Spring Boot test context with `spring.flyway.target=15`). Fresh/head migration and `flyway.validate()` are exercised through the Spring Boot postgres-tagged smoke test path (`PostgresMigrationSmokeIntegrationTest`, `@ActiveProfiles("pgtest")`, `-Ppgtest`) on the same disposable PG16 database. Both paths are PG16-backed/disposable but are different execution entrypoints/tooling versions.

Sources: `docs/PROJECT_STATE_AUDIT.md`, `novawallet-api/AGENTS.md`, `novawallet-api/pom.xml`, `novawallet-api/src/test/resources/application-test.yml`, `novawallet-api/scripts/pgtest.sh`, `novawallet-api/src/test/resources/application-pgtest.yml`, `novawallet-api/src/test/java/com/novawallet/novawallet_api/db/PostgresMigrationSmokeIntegrationTest.java`, `.github/workflows/api-ci.yml`, `novawallet-api/.github/workflows/ci.yml`, `novawallet-api/docs/phase-1c/PHASE_1C_IMPLEMENTATION_PLAN.md` (§8).

## 2. Implemented artifacts (landed)

| Artifact | Path | Implemented role / notes |
|---|---|---|
| PG16 verification script | `novawallet-api/scripts/pgtest.sh` | Bash script; starts a **disposable** `postgres:16` container (default), runs PG16 verification modes, exports datasource env vars, and guarantees cleanup (trap removes container unless `--keep-container`). Fails clearly on missing Docker/daemon/image-pull issues, missing V12 compiled class, missing V16 head migration, missing `jar`/Flyway CLI image for upgrade bootstrap (when upgrade runs), or unavailable postgres-tagged test lane (no silent skip/success). Upgrade baseline uses dockerized `flyway/flyway:10` (`migrate -target=15`) with a jar built from compiled `target/classes` (SQL + V12 Java migration). |
| PG16 Spring/test profile | `novawallet-api/src/test/resources/application-pgtest.yml` | `@ActiveProfiles("pgtest")` / `-Dspring.profiles.active=pgtest`; PostgreSQL datasource injectable via env/properties (see §3.2); `spring.jpa.hibernate.ddl-auto=validate`; Flyway enabled from `classpath:db/migration`; mail inert; test-only JWT/admin/KYC/scheduler settings for disposable runs. |
| PostgreSQL-tagged smoke test | `novawallet-api/src/test/java/com/novawallet/novawallet_api/db/PostgresMigrationSmokeIntegrationTest.java` | `@Tag("postgres")`, `@SpringBootTest`, `@ActiveProfiles("pgtest")`. Three tests (see §4.3). Intended for disposable CI/script databases. |
| Maven Surefire + pgtest profile | `novawallet-api/pom.xml` | Default: `surefire.excludedGroups=postgres`, `surefire.groups` empty (H2 suite excludes postgres-tagged tests). Profile `pgtest` (`-Ppgtest`): `surefire.groups=postgres`, `surefire.excludedGroups=none`. |
| CI PG16 jobs (both workflows) | `.github/workflows/api-ci.yml`, `novawallet-api/.github/workflows/ci.yml` | Dedicated PG16 verification jobs added (see §6). Both configure Temurin 17 and run `./scripts/pgtest.sh --mode=all` after `test-compile`. |

## 3. Prerequisites and configuration

| Prerequisite | Requirement | Notes / evidence |
|---|---|---|
| PostgreSQL | **Explicit PostgreSQL 16.x** in a **disposable** container/instance | Script defaults to `PG_IMAGE=postgres:16` and fails if container reports a non-16.x `server_version`. Do **not** use unrelated/shared/local non-disposable instances. |
| Docker | Docker CLI + daemon available; images present or pullable | Script checks `docker info`; attempts `docker pull "$PG_IMAGE"` if missing and **fails** if unavailable/unpullable (no silent skip). Upgrade mode also requires/pulls `FLYWAY_CLI_IMAGE` (default `flyway/flyway:10`) when needed. |
| Java / Maven | Java source/release **17** (`pom.xml`); Maven wrapper `novawallet-api/mvnw` | CI uses Temurin 17 (see §6). **Local limitation (BLK-04):** JDK 17 runtime not verified locally in reported environments; final Wave 0 runtime verification reported on JDK 21 (21.0.12.1, release 17) — **not** Java 17 runtime verification. Do not claim JDK 17 runtime verification without evidence. |
| Compiled V12 Java migration class | `target/classes/db/migration/V12__CreateTransactionReferenceSequence.class` must exist before running script | Script exits with error if missing and instructs: `(cd novawallet-api && ./mvnw -q -DskipTests test-compile)`. Required because V12 is a Java migration (no V12 SQL file in `db/migration`; do not create/renumber V12). CI jobs run `./mvnw -B -q -DskipTests test-compile` before the script. |
| `jar` command (upgrade mode) | `jar` available on `PATH` for building Flyway CLI classes jar from `target/classes` | Required by `--mode=upgrade` / `--mode=all` (script fails if missing when preparing classes jar). Not needed for `--mode=fresh` or `--mode=tests` alone if upgrade baseline not executed. |
| Migration head | `src/main/resources/db/migration/V16__*.sql` present | Script fails if head V16 migration file not found. |
| Secrets/config | No secrets in repo; ephemeral/test-only values only | pgtest profile uses test-only JWT/admin values and injectable datasource credentials; script passes ephemeral DB credentials to disposable container. Never commit real credentials/keys. |

### 3.1 Script usage (`novawallet-api/scripts/pgtest.sh`)

**Invocation:**
```bash
novawallet-api/scripts/pgtest.sh [--mode=all|fresh|upgrade|tests] [--keep-container]
```
- Run from repo root or `novawallet-api/` (script `cd`s to API dir).
- `--mode` (default `all`): `fresh` | `upgrade` | `tests` | `all` (fresh + upgrade + tests). Invalid mode/unknown args fail (exit 2); `-h|--help` prints header.
- `--keep-container`: skip container removal on exit (logs JDBC URL for manual inspection). Default behavior removes the disposable container via cleanup trap.

**Environment overrides (optional):** `PG_IMAGE` (default `postgres:16`), `FLYWAY_CLI_IMAGE` (default `flyway/flyway:10`, used for upgrade baseline bootstrap), `PGTEST_DB` (default `novawallet_pgtest`), `PGTEST_USER` (default `novawallet`), `PGTEST_PASSWORD` (default `novawallet`), `PG_READY_TIMEOUT` seconds (default `60`), `MVNW_ARGS` (extra Maven args).

**Runtime behavior (implemented):**
- Creates disposable container `novawallet-pgtest-$$-$RANDOM` with random host port mapped to 5432; waits for `pg_isready` (timeout fails with container logs). Script fails if reported `server_version` is not 16.x.
- Exports for Maven/tests: `PGTEST_JDBC_URL`, `PGTEST_HOST`, `PGTEST_PORT`, `PGTEST_DB`, `PGTEST_USER`, `PGTEST_PASSWORD`, and `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`.
- Resets schema between steps as needed (`DROP SCHEMA public CASCADE; CREATE SCHEMA public;`).
- For upgrade baseline, builds a temporary classes jar from compiled `target/classes` and runs dockerized Flyway CLI (`flyway/flyway:10` by default) with `-locations=classpath:db/migration` and `-target=15`.
- **Fails on unavailable/skip conditions (no silent success):** missing prerequisites, Docker/image/PG16 readiness failures, non-16 server version, missing V12 class/V16 migration/`jar` (upgrade path), migration/validate/test failures, or required postgres-tagged tests unavailable in `tests`/`all` tagged-test phase (script counts `@Tag("postgres")` markers in `src/test`; zero => non-zero exit).
- **Validate-once retry behavior (evidence-preserving):** fresh and upgrade-to-head smoke-test phases retry once on failure to accommodate a known transient Flyway-validate anomaly (first post-migrate validate may report V16 pending; immediate retry may pass). The script requires retry success and records an anomaly note when the retry path succeeds; it does **not** mask persistent failures. A passing retry can coincide with intermittent causes beyond that specific anomaly, so retry-pass runs should be interpreted with recorded logs/evidence.
- **Security/operational hardening notes (implemented behavior, not remediated here):** image tags default to unpinned/mutable (`postgres:16`, `flyway/flyway:10`; CI/workflow actions also unpinned) — supply-chain mutability caveat; DB password is passed on Flyway CLI/docker command line (argv exposure risk in local process listings/log contexts); `MVNW_ARGS` is intentionally unquoted in Maven invocations; pgtest profile/workflow wiring include committed test-only defaults. Treat as disposable/test-lane constraints and harden separately if required.

### 3.2 pgtest datasource/profile activation

`application-pgtest.yml` resolves datasource as:
- URL: `${PGTEST_JDBC_URL:jdbc:postgresql://${PGTEST_HOST:localhost}:${PGTEST_PORT:5432}/${PGTEST_DB:novawallet_pgtest}}`
- Username: `${PGTEST_USER:${SPRING_DATASOURCE_USERNAME:novawallet}}`
- Password: `${PGTEST_PASSWORD:${SPRING_DATASOURCE_PASSWORD:novawallet}}`

Script exports both `PGTEST_*` and `SPRING_DATASOURCE_*`; tests/script runs activate `-Dspring.profiles.active=pgtest` and Maven profile `-Ppgtest` as implemented.

## 4. Verification modes and implemented checks

All PG16 verification must run against **PostgreSQL 16.x** and be recorded with: PG server version, exact commands, environment (JDK/Maven/Docker image), exit codes, Flyway result, and test counts. Runtime verification status is tracked in §7.

### 4.1 Fresh migration: `V1 → V16` (head) — implemented

**Script mode:** `--mode=fresh` (also runs in `--mode=all`)

**Implemented flow (`run_fresh_verification`):**
1. Reset disposable DB (`public` schema dropped/recreated).
2. Run postgres-tagged smoke test: `./mvnw -B [-Ppgtest] test -Dspring.profiles.active=pgtest -Dtest=PostgresMigrationSmokeIntegrationTest` (`mvn_pg -Ppgtest ...`).
3. **Validate-once anomaly handling:** if the attempt fails, script logs a check for a known transient Flyway-validate anomaly (first post-migrate `flyway.validate()` may report V16 pending; immediate retry may pass), resets DB, retries the same smoke test **once**, and only succeeds if retry passes (records anomaly). **Caveat:** retry-once must not mask persistent failures and can mislabel true intermittency if the retry passes; treat any retry/success-after-retry as requiring log/surefire review. If first attempt passes, no retry.
4. Prints evidence tail from `flyway_schema_history` (best-effort).

**Coverage provided by smoke test on fresh/head migrate:** Flyway current is V16 + `SUCCESS`, `flyway.validate()` succeeds, V12 sequence exists/usable, V16 `revoked_access_tokens` integrity objects exist (see §4.3).

### 4.2 Upgrade migration: pre-V16 (V15 target) → V16 — implemented

**Script mode:** `--mode=upgrade` (also runs in `--mode=all`)

**Implemented flow (`run_upgrade_verification`):**
1. Reset disposable DB.
2. **V15 bootstrap (not a Spring Boot `target=15` test):** run dockerized `flyway/flyway:10` (`FLYWAY_CLI_IMAGE`) with `migrate -target=15` against the disposable DB, using `-locations=classpath:db/migration` and a jar built from compiled `target/classes` (includes SQL migrations and the V12 Java migration class). Script asserts Flyway current version is `15`, checks V12 sequence exists and V16 table absent, and confirms `nextval('transaction_reference_seq')` is usable at V15.
3. Seed representative valid synthetic data at V15 state for preservation checks (2 users, 2 wallets, and a best-effort transaction row when `transactions` table/schema allows; implemented as `ON CONFLICT DO NOTHING` inserts).
4. Apply upgrade to head (V16) and validate by running full `PostgresMigrationSmokeIntegrationTest` (`mvn_pg -Ppgtest test -Dspring.profiles.active=pgtest -Dtest=PostgresMigrationSmokeIntegrationTest`); on failure, retries the same full smoke test **once** **without dropping data** (to accommodate known transient Flyway-validate anomaly) and requires retry success. **Caveat:** retry-once must not mask persistent failures and can mislabel intermittency if the retry passes; review logs/surefire for any retry.
5. Record evidence and run integrity/data-preservation checks: `flyway_schema_history` entries for V12/V15/V16 (where present), counts, `revoked_access_tokens` existence/indexes/constraints, `nextval('transaction_reference_seq')`, and assertions that seeded users/wallets (2/2) are preserved (script fails if preservation checks fail).

**"Previous head" definition (implemented):** pre-V16 baseline is explicitly **Flyway target 15** established via dockerized **Flyway CLI 10** (`flyway/flyway:10` by default) `migrate -target=15` as executed by the script (**not** a Spring Boot test bootstrap at `spring.flyway.target=15`); migrate-to-head/validate runs through the app test path (Spring Boot/Flyway + smoke test). **Parity caveat:** Flyway CLI 10 for the V15 baseline vs app/Flyway for head/fresh validation — not a claim of identical engine/runtime behavior; record tool/image versions in any run evidence.

**Scope limitation:** seed/preservation coverage is limited to representative synthetic data at V15 (2 users, 2 wallets, conditional/best-effort transaction insert) with hard preservation assertions on seeded users/wallets (2/2) and targeted post-upgrade integrity signals — not exhaustive across all domain tables/state.

### 4.3 Flyway validate and PG16-tagged tests — implemented

**Flyway validate:** enforced by `PostgresMigrationSmokeIntegrationTest#migrationsAppliedToHeadAndSchemaIsValid()` via `flyway.validate()` after migration to head (fresh and upgrade-to-head phases). Script/CI treat non-zero outcomes as failure.

**Postgres-tagged tests (implemented, 3 tests, 1 class):** `PostgresMigrationSmokeIntegrationTest` (`@Tag("postgres")`, `@ActiveProfiles("pgtest")`)
- `migrationsAppliedToHeadAndSchemaIsValid`: asserts current Flyway version is **16**, state **SUCCESS**, and `flyway.validate()` does not throw.
- `v12TransactionReferenceSequenceExistsAndIsUsable`: asserts `transaction_reference_seq` exists (`to_regclass` not null) and `nextval(...)` returns a positive value.
- `v16RevokedAccessTokensTableHasExpectedIntegrityObjects`: asserts `revoked_access_tokens` table exists, exactly 1 PK, FK on `user_id` (>=1), and indexes `idx_revoked_access_tokens_expires_at` and `idx_revoked_access_tokens_user_id` exist.

**Maven/profile behavior (`pom.xml`):**
- Default: `surefire.excludedGroups=postgres`, `surefire.groups` empty — postgres-tagged tests excluded from normal H2 suite.
- `-Ppgtest`: `surefire.groups=postgres`, `surefire.excludedGroups=none`.

**Script tagged-test mode:** `--mode=tests` (and `all`) resets DB and runs `./mvnw -B $MVNW_ARGS -Ppgtest test -Dspring.profiles.active=pgtest -Dgroups=postgres`, but **only if** at least one `@Tag("postgres")` marker is discovered in `src/test`; otherwise exits with error (lane unavailable, cannot be claimed successful). **Caveat:** marker-discovery count and executed-test count can differ in reporting; executed Surefire/test totals in run logs are the evidence of what ran.

**Scope limitation:** postgres-tagged PG16 lane currently consists of a single smoke test class (**1 class / 3 tests**) covering migration/head/validate and key V12/V16 integrity signals, with limited behavioral breadth beyond migration smoke checks.

**Direct command shape (aligned with implementation, run only against disposable PG16):** `./mvnw -Ppgtest test -Dspring.profiles.active=pgtest -Dgroups=postgres` (or with `-Dtest=...` as used by script). CI/script provide `PGTEST_*` / `SPRING_DATASOURCE_*`.

## 5. Cleanup and disposable-only guarantees

- **Disposable only:** verification uses ephemeral Docker containers created per script run. Never run destructive migration verification against shared, persistent, or production-like databases.
- **Teardown:** cleanup trap removes the created container on exit unless `--keep-container` is set; failures exit non-zero. Trap also removes temporary Flyway classes jar directory when created.
- **No local shared instance reliance:** do not depend on a developer's always-on local PostgreSQL service being PG16; prefer script-controlled container lifecycle.
- **Data safety:** no real money, no live keys, sandbox/test-only configuration (`novawallet-api/AGENTS.md`, root `AGENTS.md`).

## 6. CI PG16 job behavior (implemented)

Dedicated PG16 verification jobs are implemented in **both** workflow files (trigger/path conditions differ per workflow):

| Workflow | Job | Runs-on / timeout | Key steps | Artifacts / gating |
|---|---|---|---|---|
| `.github/workflows/api-ci.yml` (`API CI`, monorepo-root workflow per DEC-24 option A note) | `pg16` — **"PostgreSQL 16 verification (JDK 17)"** | `ubuntu-latest`, 30 min, `working-directory: novawallet-api` | Checkout; setup JDK 17 Temurin (cache maven, `novawallet-api/pom.xml`); `./mvnw -B -q -DskipTests test-compile`; `./scripts/pgtest.sh --mode=all` with `PG_IMAGE: postgres:16` | Upload `novawallet-api/target/surefire-reports/` as `surefire-reports-pg16` (`if: always()`, 7 days). No build job in this workflow; separate `test` job runs default H2 suite. |
| `novawallet-api/.github/workflows/ci.yml` (`NovaWallet CI`) | `pg16` — **"PostgreSQL 16 verification (JDK 17)"** | `ubuntu-latest` | Checkout; setup JDK 17 Temurin (cache maven, `novawallet-api/pom.xml`); `./mvnw -B -q -DskipTests test-compile` (working-dir `novawallet-api`); `./scripts/pgtest.sh --mode=all` with `PG_IMAGE: postgres:16` (working-dir `novawallet-api`) | Upload `novawallet-api/target/surefire-reports/` as `surefire-reports-pg16` (`if: always()`, 7 days). **Build gated:** `build` job `needs: [test, pg16]` (push to main/master). |

**Behavioral guarantees (from script + CI steps):** runs on Temurin 17; executes `--mode=all` (fresh, upgrade, tagged tests) against disposable PG16 container; `test-compile` ensures V12 class prerequisite; script fails on missing/unavailable Docker/image/PG16 readiness, missing prerequisites, migration/validate/test failures, or unavailable postgres-tagged tests (no success on skip/unavailable). **CI execution/results are not evidenced in this session.** CI/runtime image mutability is not pinned to immutable digests in workflow config (supply-chain hardening opportunity, not a claim of compromise).

## 7. Evidence and limitations (factual / as reported)

| Item | Evidence | Status / limitation |
|---|---|---|
| `pgtest.sh` implementation | `novawallet-api/scripts/pgtest.sh` in workspace | **Implemented and runtime-verified (final).** Modes `all/fresh/upgrade/tests`, explicit `postgres:16` default, disposable container lifecycle/cleanup, fresh V1→V16 + validate, upgrade V15 pre-state via **dockerized `flyway/flyway:10` CLI `migrate -target=15`** (temp classes jar from compiled `target/classes`, incl. V12 Java migration) with V15/V12-usable/V16-absent checks + seed + migrate-to-V16 + head/smoke checks + data-preservation assertions, V12 compiled-class prerequisite, retry-once on validate anomaly with evidence, fails if postgres-tagged tests unavailable/skip; exports `PGTEST_*` and `SPRING_DATASOURCE_*`. |
| `--mode=upgrade` runtime (final) | Verified run on disposable **PG16.15**, **EXIT 0** (execution JDK **21.0.12.1**, release 17; **not** JDK 17 runtime) | **VERIFIED.** V1→V15 established (incl. V12 JDBC migration), seed **2 users / 2 wallets / 1 transaction**, migrate-to-V16 smoke **3/0/0/0**, `flyway_schema_history` V12/V15/V16 **success**, V16 `revoked_access_tokens` + indexes/PK/FK present, `nextval('transaction_reference_seq')` **3**, seeded data preserved, cleanup completed. |
| `--mode=all` runtime (final) | Verified run on disposable **PG16.15**, **EXIT 0** (execution JDK **21.0.12.1**, release 17; **not** JDK 17 runtime) | **VERIFIED.** Fresh **3/0/0/0**, upgrade **3/0/0/0**, tagged suite **3/0/0/0**, cleanup completed. Minor anomalies (informational): one readiness log `server_version=unknown` but PG16.15 confirmed; marker-count reporting differed but 3 tests executed; unrelated lingering container removed manually post-run. |
| pgtest config + postgres-tagged smoke test (prior) | Reported (W0-3b): `PostgresMigrationSmokeIntegrationTest` (3 tests) passed on disposable **PG16.16** via `./mvnw -Ppgtest test -Dgroups=postgres` **EXIT 0** (JDK 21 with release 17, reported) | **Reported pass (prior, not rerun in this closure step).** Tagged lane remains 1 smoke class / 3 tests. |
| Default H2 suite (prior) | W0-1 baseline reported **366/0/0/0** | **Reported prior evidence, not rerun.** H2 is not a PG16 substitute. |
| CI PG16 jobs | `.github/workflows/api-ci.yml` and `novawallet-api/.github/workflows/ci.yml` define `pg16` jobs (Temurin 17, `./scripts/pgtest.sh --mode=all`) | **Configured/implemented in repo; CI jobs not run in this session.** No CI execution claimed. |
| JDK 17 local runtime | `docs/PROJECT_STATE_AUDIT.md`, `HANDOFF.md`, `novawallet-api/AGENTS.md` (BLK-04) and final execution context | **Local JDK 17 runtime blocked/unavailable here.** Temurin 17 CI route available but unexecuted in-session. Final verified runs used JDK 21.0.12.1 — not Java 17 runtime verification. |
| H2 / PG18 not substitutes | `docs/PROJECT_STATE_AUDIT.md`, `novawallet-api/AGENTS.md`, `pom.xml`, `application-test.yml` | Default suite is H2; local PG 18.6 in audit does not satisfy PG16 parity. |
| Validate retry-once behavior | Script retries once on failure in fresh/upgrade-to-head validation paths and records anomaly on retry success | **Implemented; caveat:** narrow heuristic that must not mask persistent failures and can mislabel intermittency if retry passes. Review logs/surefire for any retry. |
| Flyway CLI vs app Flyway parity | Upgrade baseline uses `flyway/flyway:10` CLI `migrate -target=15`; head validation runs via app test path (Spring Boot/Flyway) | **Parity caveat:** tool/version/runtime paths differ (CLI 10 for V1→V15 baseline vs app Flyway for V16 head). Record versions in evidence. |
| Seed/preservation breadth | `seed_upgrade_data` + `record_upgrade_evidence` assert seeded users/wallets (2/2) and run targeted integrity checks | **Limited breadth:** representative synthetic seed/checks only (not exhaustive schema/data integrity). |
| Images/actions and hardening | Script uses `PG_IMAGE=postgres:16`, `FLYWAY_CLI_IMAGE=flyway/flyway:10` defaults; workflows/actions as configured | **Informational (security review):** unpinned images/actions and minor script hardening items noted; not changed in this doc scope. |
| Historical PG16 checks | `PHASE_1C_BATCH_0_REPORT.md`, `PHASE_1C_BATCH_1_REPORT.md` | Prior evidence, **not rerun** in this session. |

## 8. Reporting checklist (for any PG16 verification run)

When claiming PG16 verification, include:
- Date/time, operator/CI run ID, branch/commit (if known and authorized to report)
- PostgreSQL image/tag and reported server version (must be 16.x), plus Flyway CLI image/version when upgrade mode is used
- Java/Maven versions and exact commands (script mode/flags, env overrides)
- Fresh `V1→V16` result (pass/fail, retry/anomaly if any, Flyway/test summary)
- Upgrade **V15 baseline via Flyway CLI `migrate -target=15` → seed → V16** result (pass/fail, pre-state checks, seeded preservation checks, retry/anomaly if any) or explicit "not run" with reason
- `flyway validate` result (as asserted by smoke test / observed)
- PG16-tagged test command and counts (run/fail/error/skip)
- Cleanup confirmation (container/resources removed, or intentionally kept with `--keep-container`)
- Any anomalies with logs/surefire reports (including retry-once occurrences, readiness-log oddities, or marker/execution count discrepancies)

## 9. Related documents

- `novawallet-api/AGENTS.md` (gates, migration/PG16 rules)
- `novawallet-api/docs/phase-1c/PHASE_1C_IMPLEMENTATION_PLAN.md` (§8 test strategy, DEC-11)
- `novawallet-api/docs/phase-1c/PHASE_1C_BATCH_0_REPORT.md`, `PHASE_1C_BATCH_1_REPORT.md`, `PHASE_1C_BATCH_2_REPORT.md`
- `docs/PROJECT_STATE_AUDIT.md`, `docs/PROJECT_ROADMAP.md`, `HANDOFF.md`
- `novawallet-api/pom.xml` (Surefire `postgres` tag exclusion/inclusion, `pgtest` profile)
- `.github/workflows/api-ci.yml`, `novawallet-api/.github/workflows/ci.yml` (CI PG16 jobs)
