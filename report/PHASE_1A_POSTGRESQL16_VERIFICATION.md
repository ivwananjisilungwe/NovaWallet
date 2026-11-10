# NovaWallet — Phase 1A PostgreSQL 16 Verification Report

**Document:** PHASE_1A_POSTGRESQL16_VERIFICATION.md
**Date:** 2026-11-10
**Branch:** feature/phase-1a-financial-guardrails
**Baseline commit:** 60179fb5b09f0d5a11338f07ed2cb23ef393b1f5
**Status:** VERIFIED — PostgreSQL 16 runtime evidence obtained

---

## 1. Environment Details

### 1.1 PostgreSQL 16 Provisioning

- **Method:** Isolated disposable Docker container (`postgres:16`)
- **Container name:** novawallet-pg16-test
- **Port binding:** 127.0.0.1:5433 → 5432 (localhost only)
- **Database:** novawallet_test_pg16
- **User:** postgres
- **Password:** Randomly generated, stored in `/tmp/opencode/pg16_env.sh` (mode 600), not printed or persisted in repository
- **Volume:** None (ephemeral, no persistent volume)
- **Cleanup:** Container removed after verification

### 1.2 Version Confirmation

```
SELECT version();
```
Output: `PostgreSQL 16.15 (Debian 16.15-1.pgdg13+2) on x86_64-pc-linux-gnu, compiled by gcc (Debian 14.2.0-19) 14.2.0, 64-bit`

```
SHOW server_version;
```
Output: `16.15 (Debian 16.15-1.pgdg13+2)`

**Confirmed: PostgreSQL 16.15.** Not 18.6 or any other version.

### 1.3 Tooling

- **Docker:** 29.9.3
- **JDK:** OpenJDK 17.0.13 (Temurin) at /tmp/opencode/jdk17/jdk-17.0.13+11
- **Maven:** 3.9.16 (via mvnw)
- **PostgreSQL client:** psql 18.6 (client only; server is 16.15)

---

## 2. FIN-12 Implementation Summary

### 2.1 V12 Migration (`V12__CreateTransactionReferenceSequence.java`)

- **Package:** `db.migration`
- **Extends:** `BaseJavaMigration`
- **Pattern:** `^NWTX[0-9]{8}[0-9]{8}$`
- **MAX_CAPACITY:** 99,999,999
- **FETCH_SIZE:** 1000
- **Behavior:**
  1. Fails closed if connection auto-commit is enabled.
  2. Streams `SELECT reference FROM transactions` with fetch size 1000.
  3. Computes max suffix among conforming references.
  4. Counts and ignores non-conforming rows with WARN log.
  5. Fails closed if `maxSuffix >= MAX_CAPACITY`.
  6. Creates sequence `transaction_reference_seq START WITH 1 INCREMENT BY 1`.
  7. Restarts sequence to `maxSuffix + 1`.
  8. Never rewrites historical rows.

### 2.2 TransactionReferenceGenerator

- **Package:** `com.novawallet.novawallet_api.transaction.service`
- **Injection:** `TransactionRepository` (constructor-injected)
- **generate():** Calls `transactionRepository.nextReferenceSequence()` (native `SELECT nextval('transaction_reference_seq')`), validates range [1, 99,999,999], returns `NWTX` + `yyyyMMdd` + 8-digit zero-padded sequence.
- **No JVM-local counters:** No AtomicLong, @PostConstruct, or date-based reset.

### 2.3 Database Uniqueness

- **V6 migration:** `CONSTRAINT uq_transactions_reference UNIQUE (reference)` on `reference VARCHAR(30) NOT NULL`
- **JPA entity:** `@Column(nullable = false, unique = true, length = 30)`

---

## 3. Test Results

### Test A — Fresh Database Migration

**Result: PASSED**

- All 12 Flyway migrations (V1–V12) applied successfully.
- V12 created `transaction_reference_seq` in PostgreSQL 16.
- Sequence metadata inspected: data_type, start_value, increment confirmed.
- First `nextval` returned 1 (empty database).
- No errors or warnings (except expected "Seeded to start at 1").

### Test B — Historical Transaction References

**Result: PASSED**

- Migrations V1–V11 applied, then historical data inserted.
- Historical references: `NWTX{today}00000018`, `NWTX{yesterday}00000042`, `NWTX{today}00000007`, `NWTXLEGACY-1`
- V12 applied successfully.
- Sequence first value: 43 (= max suffix 42 + 1). Correct.
- All 4 historical references preserved unchanged.
- 50 subsequent nextval values (43–92) never collided with historical references.

### Test C — Sequence Initialization

**Result: PASSED**

- Inserted references with suffixes 1 and 99.
- V12 applied. First nextval returned 100 (= max suffix 99 + 1). Correct.
- Max suffix calculated correctly from conforming references.

### Test D — Malformed Historical References

**Result: PASSED**

- Inserted: 2 valid (`...00000005`, `...00000010`) + 3 non-conforming (`NWTXLEGACY-1`, `INVALID-REF`, `NWTX{today}ABC`)
- V12 logged: "Ignored 3 transaction rows whose reference does not match ^NWTX[0-9]{8}[0-9]{8}$"
- Sequence seeded to start at 11 (= max valid suffix 10 + 1). Correct.
- Valid references not accidentally ignored.
- Historical data preserved.

### Test E — Concurrent Generation (JDBC)

**Result: PASSED**

- Workers: 8
- Calls per worker: 100
- Total generated: 800
- Unique references: 800
- Duplicate count: 0
- Errors: 0
- Duration: 4570ms
- Database: PostgreSQL 16.15
- No duplicate references, no missing values, no connection failures.

### Test F — Uniqueness Enforcement

**Result: PASSED**

- Inserted a transaction with reference `NWTX2026010100000001`.
- Attempted duplicate insert with same reference.
- Database rejected duplicate via `uq_transactions_reference` unique constraint.
- Application error handling does not silently treat duplicate as successful.

### Test G — Sequence Exhaustion

**Result: PASSED**

- Moved sequence to 99,999,999 via `setval('transaction_reference_seq', 99999999, true)`.
- Next `nextval` returned 100,000,000 (beyond MAX_CAPACITY).
- Java generator correctly throws `IllegalStateException` with message "Transaction reference sequence value out of range: 100000000".
- No invalid reference returned.
- No transaction created, no balance modified.

### Test H — Migration Failure and Recovery

**Result: PASSED**

- Inserted a reference with suffix 99,999,999 (at capacity).
- V12 failed with `IllegalStateException: No remaining capacity for transaction reference sequence (maxSuffix=99999999)`.
- Flyway recorded the failed migration (success=false).
- Sequence was NOT created (migration failed before CREATE SEQUENCE).
- Fail-closed behavior confirmed: no partial sequence, no incorrect results.

---

## 4. Java Generator Tests Against PostgreSQL 16

### Concurrency Test (Spring Boot Integration)

**Result: PASSED**

- Threads: 16
- Per thread: 250
- Total: 4,000
- Unique: 4,000
- Duplicates: 0
- Errors: 0
- Duration: 1,831ms
- All references match pattern `^NWTX\d{8}\d{8}$`
- Database: PostgreSQL 16.15

### Exhaustion Guard Test

**Result: PASSED**

- Sequence moved to 99,999,999 via SQL.
- `generator.generate()` threw `IllegalStateException`.
- Message: "Transaction reference sequence value out of range: 100000000"
- No invalid reference returned.

### Format Test

**Result: PASSED**

- First generated reference matches pattern `^NWTX\d{8}\d{8}$`.
- Suffix is 00000001 (first value from fresh sequence).

---

## 5. SQL Evidence Summary

### Sequence Creation (V12)

```sql
CREATE SEQUENCE transaction_reference_seq START WITH 1 INCREMENT BY 1;
SELECT setval('transaction_reference_seq', <maxSuffix + 1>, false);
```

### Sequence Query

```sql
SELECT nextval('transaction_reference_seq');  -- Returns next value atomically
```

### Uniqueness Constraint (V6)

```sql
CONSTRAINT uq_transactions_reference UNIQUE (reference)
```

### Migration Failure (Test H)

```
FlywayException: Migration failed
Caused by: IllegalStateException: No remaining capacity for transaction reference sequence (maxSuffix=99999999)
```

---

## 6. Corrections Made

**None.** All PostgreSQL 16 tests passed without requiring any code changes. The FIN-12 implementation is compatible with PostgreSQL 16 as-is.

---

## 7. Conclusion

**FIN-12 PostgreSQL 16 Verification: PASSED**

- Fresh migration: ✅
- Historical references: ✅
- Sequence initialization: ✅
- Malformed references: ✅
- Concurrent generation: ✅ (800 unique, 0 duplicates)
- Uniqueness enforcement: ✅
- Sequence exhaustion: ✅
- Migration failure/recovery: ✅
- Java generator concurrency: ✅ (4000 unique, 0 duplicates)
- Java generator exhaustion: ✅
- Java generator format: ✅

No corrections were required. The implementation is PostgreSQL 16 compatible.

---

## 8. Cleanup

- Temporary test files removed (Pg16TransactionReferenceTest.java, Pg16TransactionReferenceGeneratorTest.java, application-pg16.yml).
- PostgreSQL 16 container removed after verification.
- Password file (/tmp/opencode/pg16_env.sh) to be deleted.
- No repository modifications from testing (only documentation updates).
