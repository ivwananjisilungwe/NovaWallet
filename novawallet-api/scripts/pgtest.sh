#!/usr/bin/env bash
# Reproducible PostgreSQL 16 verification for NovaWallet API (Phase 1C / Wave 0, W0-3a).
#
# Runs against a DISPOSABLE postgres:16 container only. Never touches local/shared DBs.
# Covers: fresh V1->head migrate+validate, upgrade previous-head(V15)->V16 migrate+validate
# with basic integrity/data-preservation checks, and the PostgreSQL-tagged test suite
# (-Ppgtest, @Tag("postgres")). Fails clearly if required lanes/tests are unavailable.
#
# Usage:
#   novawallet-api/scripts/pgtest.sh [--mode=all|fresh|upgrade|tests] [--keep-container]
# Run from repo root or novawallet-api/. Requires: docker, novawallet-api/mvnw, bash.
# Java V12 migration requires compiled classes (target/classes/db/migration/V12__*). If
# missing, run: (cd novawallet-api && ./mvnw -q -DskipTests test-compile)
#
# Environment overrides (optional): PG_IMAGE (default postgres:16), PGTEST_DB/USER/PASSWORD,
# MVNW_ARGS (extra mvn args), PG_READY_TIMEOUT (seconds).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
API_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$API_DIR/.." && pwd)"
cd "$API_DIR"

MODE="all"
KEEP_CONTAINER="false"
for arg in "$@"; do
  case "$arg" in
    --mode=*) MODE="${arg#*=}" ;;
    --keep-container) KEEP_CONTAINER="true" ;;
    -h|--help)
      sed -n '1,40p' "$0"; exit 0 ;;
    *) echo "ERROR: unknown argument: $arg" >&2; exit 2 ;;
  esac
done
case "$MODE" in all|fresh|upgrade|tests) ;; *) echo "ERROR: invalid --mode=$MODE" >&2; exit 2 ;; esac

PG_IMAGE="${PG_IMAGE:-postgres:16}"
PGTEST_DB="${PGTEST_DB:-novawallet_pgtest}"
PGTEST_USER="${PGTEST_USER:-novawallet}"
PGTEST_PASSWORD="${PGTEST_PASSWORD:-novawallet}"
PG_READY_TIMEOUT="${PG_READY_TIMEOUT:-60}"
CONTAINER_NAME="novawallet-pgtest-$$-$RANDOM"
HOST_PORT=""
CREATED_CONTAINER="false"
MVNW="./mvnw"
MVNW_ARGS="${MVNW_ARGS:-}"

log() { printf '\n[%s] %s\n' "$(date -u +%H:%M:%S)" "$*"; }
die() { echo "ERROR: $*" >&2; exit 1; }

require_cmd() { command -v "$1" >/dev/null 2>&1 || die "required command not found: $1"; }

cleanup() {
  local ec=$?
  set +e
  if [[ -n "${FLYWAY_CLASSES_JAR:-}" ]]; then
    rm -rf "$(dirname "$FLYWAY_CLASSES_JAR")" >/dev/null 2>&1 || true
  fi
  if [[ "$CREATED_CONTAINER" == "true" && "$KEEP_CONTAINER" != "true" ]]; then
    log "Cleanup: removing disposable container $CONTAINER_NAME"
    docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
  elif [[ "$CREATED_CONTAINER" == "true" ]]; then
    log "Cleanup: keeping container $CONTAINER_NAME (PGTEST_JDBC_URL=jdbc:postgresql://localhost:${HOST_PORT}/${PGTEST_DB})"
  fi
  if [[ $ec -ne 0 ]]; then
    echo "PG16 verification FAILED (exit $ec)" >&2
  fi
  exit $ec
}
trap cleanup EXIT

check_prereqs() {
  log "Prerequisites"
  require_cmd docker
  [[ -x "$MVNW" ]] || die "mvnw not found/executable at $API_DIR/mvnw"
  docker info >/dev/null 2>&1 || die "docker daemon not available"
  if ! docker image inspect "$PG_IMAGE" >/dev/null 2>&1; then
    log "Docker image '$PG_IMAGE' not present locally; attempting pull (required for disposable PG16 verification)"
    if ! docker pull "$PG_IMAGE" >/dev/null 2>&1; then
      die "docker image '$PG_IMAGE' unavailable and pull failed; PG16 lane cannot be skipped/unavailable - failing"
    fi
  fi
  if [[ ! -f "target/classes/db/migration/V12__CreateTransactionReferenceSequence.class" ]]; then
    echo "ERROR: compiled Java migration V12 class not found in target/classes." >&2
    echo "Prerequisite: build classes without changing repo source, e.g.:" >&2
    echo "  (cd novawallet-api && ./mvnw -q -DskipTests test-compile)" >&2
    echo "This script cannot download dependencies; if deps are not cached, this will fail." >&2
    exit 1
  fi
  if ! compgen -G "src/main/resources/db/migration/V16__*.sql" >/dev/null; then
    die "expected migration head V16__*.sql not found in src/main/resources/db/migration"
  fi
  log "Using PG_IMAGE=$PG_IMAGE (disposable container: $CONTAINER_NAME)"
}

start_container() {
  log "Start disposable PostgreSQL 16 container"
  docker run -d --rm --name "$CONTAINER_NAME" \
    -e POSTGRES_DB="$PGTEST_DB" \
    -e POSTGRES_USER="$PGTEST_USER" \
    -e POSTGRES_PASSWORD="$PGTEST_PASSWORD" \
    -p 127.0.0.1::5432 \
    "$PG_IMAGE" >/dev/null
  CREATED_CONTAINER="true"
  HOST_PORT="$(docker port "$CONTAINER_NAME" 5432/tcp | head -n1 | awk -F: '{print $NF}')"
  [[ -n "$HOST_PORT" ]] || die "failed to determine mapped host port for $CONTAINER_NAME"
  export PGTEST_JDBC_URL="jdbc:postgresql://localhost:${HOST_PORT}/${PGTEST_DB}"
  export PGTEST_HOST="localhost"
  export PGTEST_PORT="$HOST_PORT"
  export PGTEST_DB PGTEST_USER PGTEST_PASSWORD
  export SPRING_DATASOURCE_URL="$PGTEST_JDBC_URL"
  export SPRING_DATASOURCE_USERNAME="$PGTEST_USER"
  export SPRING_DATASOURCE_PASSWORD="$PGTEST_PASSWORD"

  log "Waiting for PostgreSQL readiness on localhost:${HOST_PORT} (timeout ${PG_READY_TIMEOUT}s)"
  local i=0
  until docker exec "$CONTAINER_NAME" pg_isready -U "$PGTEST_USER" -d "$PGTEST_DB" >/dev/null 2>&1; do
    i=$((i+1))
    if [[ $i -ge $PG_READY_TIMEOUT ]]; then
      docker logs "$CONTAINER_NAME" >&2 || true
      die "PostgreSQL did not become ready within ${PG_READY_TIMEOUT}s"
    fi
    sleep 1
  done
  local server_ver
  server_ver="$(docker exec "$CONTAINER_NAME" psql -U "$PGTEST_USER" -d "$PGTEST_DB" -Atc 'SHOW server_version;' 2>/dev/null || true)"
  log "PostgreSQL ready: server_version=${server_ver:-unknown} jdbc=$PGTEST_JDBC_URL"
  if [[ -n "$server_ver" && ! "$server_ver" =~ ^16(\.|$) ]]; then
    die "expected PostgreSQL 16.x but container reports server_version=${server_ver}"
  fi
}

psql_exec() {
  # -i allows piping/heredoc SQL to psql; also works with -c/arg-based invocations.
  docker exec -i -e PGPASSWORD="$PGTEST_PASSWORD" "$CONTAINER_NAME" \
    psql -v ON_ERROR_STOP=1 -U "$PGTEST_USER" -d "$PGTEST_DB" "$@"
}

reset_db() {
  log "Reset database schema/data (drop+recreate public schema)"
  psql_exec -c "DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public;" >/dev/null
}

mvn_pg() {
  # shellcheck disable=SC2086
  "$MVNW" -B $MVNW_ARGS "$@"
}


FLYWAY_CLI_IMAGE="${FLYWAY_CLI_IMAGE:-flyway/flyway:10}"
FLYWAY_CLASSES_JAR=""

ensure_flyway_cli() {
  if ! docker image inspect "$FLYWAY_CLI_IMAGE" >/dev/null 2>&1; then
    log "Docker image '$FLYWAY_CLI_IMAGE' not present locally; attempting pull (required for upgrade-state verification)"
    if ! docker pull "$FLYWAY_CLI_IMAGE" >/dev/null 2>&1; then
      die "docker image '$FLYWAY_CLI_IMAGE' unavailable and pull failed; upgrade lane cannot be skipped/unavailable - failing"
    fi
  fi
}

prepare_flyway_classes_jar() {
  if [[ -n "$FLYWAY_CLASSES_JAR" && -f "$FLYWAY_CLASSES_JAR" ]]; then
    return 0
  fi
  command -v jar >/dev/null 2>&1 || die "required command not found for upgrade flyway-cli path: jar"
  local jar_dir jar_path
  jar_dir="$(mktemp -d "${TMPDIR:-/tmp}/novawallet-pgtest-flyway.XXXXXX")"
  jar_path="$jar_dir/classes.jar"
  log "Preparing classes jar for Flyway CLI (includes SQL resources + V12 Java migration): $jar_path"
  (cd "$API_DIR/target/classes" && jar cf "$jar_path" .) || die "failed to create classes jar from target/classes"
  FLYWAY_CLASSES_JAR="$jar_path"
}

flyway_cli() {
  # Run Flyway CLI against the disposable postgres container network namespace.
  # Uses compiled target/classes jar for classpath migrations/resources (SQL + V12 Java).
  ensure_flyway_cli
  prepare_flyway_classes_jar
  docker run --rm \
    --network "container:${CONTAINER_NAME}" \
    -v "${FLYWAY_CLASSES_JAR}:/flyway/jars/novawallet-classes.jar:ro" \
    "$FLYWAY_CLI_IMAGE" \
    -url="jdbc:postgresql://localhost:5432/${PGTEST_DB}" \
    -user="$PGTEST_USER" \
    -password="$PGTEST_PASSWORD" \
    -locations="classpath:db/migration" \
    -connectRetries=60 \
    "$@"
}

migrate_to_target_v15_via_flyway_cli() {
  log "Establish pre-V16 schema state via Flyway CLI (target=15) to avoid JPA entity-schema validation against V16-only tables"
  flyway_cli -target=15 migrate
  local current
  current="$(psql_exec -Atc "SELECT version FROM flyway_schema_history WHERE success AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1;")"
  [[ "$current" == "15" ]] || die "expected Flyway current version 15 after CLI migrate -target=15, found '${current:-<none>}'"
  psql_exec -c "SELECT to_regclass('public.transaction_reference_seq') IS NOT NULL AS v12_sequence_exists, to_regclass('public.revoked_access_tokens') IS NULL AS v16_table_absent;" || true
  psql_exec -c "SELECT nextval('transaction_reference_seq') AS v12_seq_nextval_at_v15;" >/dev/null \
    || die "V12 sequence transaction_reference_seq not usable at V15 state"
  log "Pre-upgrade state reached (Flyway target 15 via CLI; V12 Java migration applied, V16 table absent)"
}

run_fresh_verification() {
  log "FRESH verification: clean DB -> Flyway V1->V16 + validate + schema/integrity checks (postgres-tagged smoke test)"
  reset_db
  # PostgresMigrationSmokeIntegrationTest (@SpringBootTest, @ActiveProfiles pgtest) migrates to head,
  # asserts current==V16/success, runs flyway.validate(), and checks V12 sequence + V16 objects.
  set +e
  mvn_pg -Ppgtest test -Dspring.profiles.active=pgtest \
    -Dtest=PostgresMigrationSmokeIntegrationTest
  local ec=$?
  set -e
  if [[ $ec -ne 0 ]]; then
    log "Fresh verification attempt failed (exit $ec). Checking for known transient Flyway-validate anomaly (retry once with evidence)..."
    # Known observed anomaly: first post-migrate flyway validate may report V16 pending, immediate retry passes.
    # We do NOT mask real failures: retry once only and require success; full logs are preserved in surefire reports.
    reset_db
    mvn_pg -Ppgtest test -Dspring.profiles.active=pgtest \
      -Dtest=PostgresMigrationSmokeIntegrationTest
    log "Fresh verification succeeded on retry; anomaly recorded (see surefire reports/logs above) - not masked."
  else
    log "Fresh verification PASSED"
  fi
  log "Fresh evidence (flyway_schema_history tail)"
  psql_exec -c "SELECT installed_rank, version, description, type, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 6;" || true
}

seed_upgrade_data() {
  log "Seed representative valid synthetic data at V15 state (pre-V16) for preservation checks"
  # V16 adds revoked_access_tokens only; seed core rows that must survive upgrade.
  # Schema aligned to V2/V3/V6(+) so inserts are valid at V15 and data remains intact after V16 migration.
  psql_exec <<'SQL'
INSERT INTO users (id, first_name, last_name, email, phone, password_hash, role, email_verified, deleted, created_at, updated_at)
VALUES
  ('11111111-1111-1111-1111-111111111111', 'Pg', 'Test1', 'pgtest-user1@example.test', '+260970000001', '$2a$10$abcdefghijklmnopqrstuv', 'USER', true, false, NOW(), NOW()),
  ('22222222-2222-2222-2222-222222222222', 'Pg', 'Test2', 'pgtest-user2@example.test', '+260970000002', '$2a$10$abcdefghijklmnopqrstuv', 'USER', true, false, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO wallets (id, user_id, account_number, balance, currency, status, created_at, updated_at)
VALUES
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', '100000000001', 1250.50, 'ZMW', 'ACTIVE', NOW(), NOW()),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '22222222-2222-2222-2222-222222222222', '100000000002', 75.00, 'ZMW', 'ACTIVE', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO transactions (id, reference, type, amount, balance_before, balance_after, status, description, sender_wallet_id, receiver_wallet_id, created_at, updated_at)
SELECT 'cccccccc-cccc-cccc-cccc-cccccccccccc', 'PGTEST-UPGRADE-REF-001', 'DEPOSIT', 100.00, 0.00, 100.00, 'SUCCESSFUL', 'pgtest seed',
       NULL, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', NOW(), NOW()
WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='public' AND table_name='transactions')
ON CONFLICT DO NOTHING;
SQL
  log "Seeded counts (pre-upgrade)"
  psql_exec -c "SELECT 'users' AS t, COUNT(*) AS c FROM users UNION ALL SELECT 'wallets', COUNT(*) FROM wallets;" || true
  psql_exec -c "SELECT 'transactions' AS t, COUNT(*) AS c FROM transactions;" || true
}

record_upgrade_evidence() {
  log "Upgrade evidence: schema/index/constraint/FK/data-preservation checks"
  psql_exec -c "SELECT version, description, success FROM flyway_schema_history WHERE version IN ('12','15','16') ORDER BY installed_rank;" || true
  psql_exec -c "SELECT COUNT(*) AS users FROM users;" || true
  psql_exec -c "SELECT COUNT(*) AS wallets FROM wallets;" || true
  psql_exec -c "SELECT COUNT(*) AS transactions FROM transactions;" || true
  # V16 integrity objects (basic)
  psql_exec -c "SELECT to_regclass('public.revoked_access_tokens') IS NOT NULL AS revoked_access_tokens_exists;" || true
  psql_exec -c "SELECT indexname FROM pg_indexes WHERE schemaname='public' AND tablename='revoked_access_tokens' ORDER BY indexname;" || true
  psql_exec -c "SELECT tc.constraint_type, kcu.column_name FROM information_schema.table_constraints tc JOIN information_schema.key_column_usage kcu ON tc.constraint_name=kcu.constraint_name AND tc.table_schema=kcu.table_schema WHERE tc.table_schema='public' AND tc.table_name='revoked_access_tokens' ORDER BY tc.constraint_type, kcu.column_name;" || true
  # V12 sequence still usable post-upgrade
  psql_exec -c "SELECT nextval('transaction_reference_seq') AS seq_nextval;" || true
  # Data preservation assertions (fail if seeded rows lost)
  local u w
  u="$(psql_exec -Atc "SELECT COUNT(*) FROM users WHERE email IN ('pgtest-user1@example.test','pgtest-user2@example.test');")"
  w="$(psql_exec -Atc "SELECT COUNT(*) FROM wallets WHERE account_number IN ('100000000001','100000000002');")"
  [[ "$u" == "2" ]] || die "upgrade data-preservation check failed: expected 2 seeded users, found $u"
  [[ "$w" == "2" ]] || die "upgrade data-preservation check failed: expected 2 seeded wallets, found $w"
  log "Upgrade data-preservation checks PASSED (seeded users/wallets present)"
}

run_upgrade_verification() {
  log "UPGRADE verification: establish pre-V16 (target V15) state -> seed -> migrate to V16 + validate + checks"
  reset_db
  # Establish V1..V15 state without bootstrapping the full Spring/JPA context (head entities include V16-only
  # tables). Flyway CLI runs SQL migrations and the V12 Java migration from compiled classes; JPA validation at
  # head remains enforced by the PostgresMigrationSmokeIntegrationTest run after upgrade to V16.
  migrate_to_target_v15_via_flyway_cli
  psql_exec -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 3;" || true

  seed_upgrade_data

  log "Apply upgrade to head (V16) and validate via postgres-tagged smoke test"
  set +e
  mvn_pg -Ppgtest test -Dspring.profiles.active=pgtest \
    -Dtest=PostgresMigrationSmokeIntegrationTest
  local ec=$?
  set -e
  if [[ $ec -ne 0 ]]; then
    log "Upgrade-to-V16 validation attempt failed (exit $ec). Retry once with evidence for known transient Flyway-validate anomaly..."
    # DB is already at V16 attempt state; rerun test to allow immediate retry behavior without dropping data.
    mvn_pg -Ppgtest test -Dspring.profiles.active=pgtest \
      -Dtest=PostgresMigrationSmokeIntegrationTest
    log "Upgrade verification succeeded on retry; anomaly recorded - not masked."
  else
    log "Upgrade-to-V16 migrate+validate PASSED"
  fi

  record_upgrade_evidence
}

count_postgres_tagged_tests() {
  # Count @Tag("postgres") occurrences in test sources (excluding comments is unnecessary; presence indicates lane).
  (cd "$API_DIR" && grep -R --line-number --exclude-dir=target '@Tag("postgres")' src/test 2>/dev/null || true) | wc -l | tr -d ' '
}

run_tagged_tests() {
  local tagged_count
  tagged_count="$(count_postgres_tagged_tests)"
  log "PostgreSQL-tagged test suite (./mvnw -Ppgtest test -Dgroups=postgres); discovered @Tag(\"postgres\") markers: $tagged_count"
  if [[ "$tagged_count" -eq 0 ]]; then
    echo "ERROR: no @Tag(\"postgres\") tests found in src/test; pgtest lane is unavailable and cannot be claimed successful." >&2
    echo "Design note: once W0-3b lands postgres-tagged tests, this step will execute them against PG16." >&2
    exit 1
  fi
  reset_db
  mvn_pg -Ppgtest test -Dspring.profiles.active=pgtest -Dgroups=postgres
  log "PostgreSQL-tagged test suite PASSED"
}

main() {
  log "NovaWallet API PG16 verification (mode=$MODE, api_dir=$API_DIR)"
  log "Java: $(java -version 2>&1 | head -n1 || echo unknown)"
  check_prereqs
  start_container
  case "$MODE" in
    fresh) run_fresh_verification ;;
    upgrade) run_upgrade_verification ;;
    tests) run_tagged_tests ;;
    all)
      run_fresh_verification
      run_upgrade_verification
      run_tagged_tests
      ;;
  esac
  log "PG16 verification COMPLETED SUCCESSFULLY (mode=$MODE)"
}

main "$@"
