# Phase 1C Batch 4 Plan: Database Integrity and Audit Immutability

Date: 2026-11-14 · Prompt: BE-1C-4 · Mode: **PLAN ONLY** (no source, migration, test, or git change made; no Maven run; no database touched)
Decision dependencies: **DEC-05** (decided: idempotency-key scope, no composite unique), **DEC-06** (decided: DB-level revoke of UPDATE/DELETE, trigger fallback only if a separate migration user is impossible), **DEC-11** (open: `pgtest`), plus new items in §8.

## 0. Assumptions and constraints

- **Migration numbering.** Head on disk is **V16** (`V16__create_revoked_access_tokens.sql`). **V12 is a Java migration** (`src/main/java/db/migration/V12__CreateTransactionReferenceSequence.java`) and is never recreated. **BE-1C-3 may take V17**. This plan therefore uses **V18–V23** and says so everywhere. If BE-1C-3 does not take V17, every number shifts down by one with the same names. An applied migration is never renumbered.
- **H2 is still in the loop.** `src/test/resources/application-test.yml:4` points the full suite at H2 (`MODE=PostgreSQL`), and `application-test.yml:20-22` enables Flyway on `classpath:db/migration`. Every V18–V22 statement must therefore run on H2 **and** PostgreSQL 16, or the whole Spring context fails. V15 already records this constraint (`V15__provider_webhook_events_and_reconciliation_indexes.sql:29-32`). The only PostgreSQL-only piece is the audit immutability step, which is a Java migration that branches on the database product (§5.6). Nothing in this plan uses `NOT VALID`, `CONCURRENTLY`, partial indexes, or `DO $$` blocks.
- **Money is not repaired by migrations.** Any row that violates a new constraint stops the migration (or the precheck) and goes to the owner. No migration silently rewrites `wallets` or `transactions` values.
- **Single deployment role.** The app and Flyway both connect as `novawallet`, and that role is also the Postgres image's bootstrap user (§3.5). This is the decisive finding for DEC-06.

## 1. What I read (T5 item 1)

- Rules and gates: `AGENTS.md`, `novawallet-api/AGENTS.md` (rules 4, 5, 7 and the gate block), `docs/DECISIONS.md` (DEC-05, DEC-06, DEC-11, DEC-33, BLK-04/BLK-06), `prompts/templates/T5_plan_first.md`, `prompts/stage-01_backend-security/_COMMON_BATCH_RULES.md`, `prompts/stage-01_backend-security/BE-1C-4_database_integrity.md`.
- Style model and program docs: `novawallet-api/docs/phase-1c/PHASE_1C_BATCH_2_PLAN.md`, `PHASE_1C_IMPLEMENTATION_PLAN.md` (§5 NEW-DB-01..04, §7 Batch 4, §8.3, §9, §13), `PHASE_1C_RISK_TRACEABILITY_MATRIX.md` (DB-01 row at line 299), `novawallet-api/docs/architecture/backend-risk-register.md` (DB-01 block, lines 528-549).
- Every migration: `V1` to `V11`, `V13` to `V16` (SQL) and `src/main/java/db/migration/V12__CreateTransactionReferenceSequence.java`.
- All 12 JPA entities, all 20 enums that the entities use, the repositories that declare queries, the audit writer, the fee engine, the KYC approval path, the webhook and payout writers, the Flyway and datasource configuration, `docker-compose.yml`, and the existing migration test `V16RevokedAccessTokensMigrationTest`.

## 2. Verified facts that constrain the plan

| # | Fact | Evidence |
|---|---|---|
| F1 | Head is V16. There is no V17 on disk. V12 is Java and creates `transaction_reference_seq`. | `db/migration/` listing; `V12…java:85` |
| F2 | `wallets.balance` has no CHECK. Non-negativity lives only in the conditional UPDATE. The entity javadoc claims the DB enforces it. | `V3…sql:5`; `WalletRepository.java:50-58` (predicate at :56); `Wallet.java:18` (false claim) |
| F3 | `wallets.user_id` is NOT NULL with a plain index, not UNIQUE. The code assumes one wallet per user. | `V3…sql:3, :16`; `WalletRepository.java:23` (`Optional<Wallet> findByUserId`); `Wallet.java:14-17` |
| F4 | No `wallets.currency`, `wallets.status`, or `freeze_reason` CHECK. | `V3…sql:6-8` |
| F5 | `transactions.amount` has no CHECK. `transactions.status` has no CHECK. | `V6…sql:5, :8`; `V13…sql:1` |
| F6 | Fee rows are written only when fee > 0, so `amount > 0` does not break current write paths. | `TransactionService.java:157`, `:320`; `PayoutService.java:333`; DTOs use `@Positive` (`TransferRequest.java:24`, `WithdrawRequest.java:19`, `DepositRequest.java:18`) |
| F7 | `transactions.related_transaction_id` is written by the transfer, fee and payout paths. It has no FK and no index. | `V6…sql:12` (no FK at :19-20); `TransactionService.java:312, :316, :330`; `PayoutService.java:343, :515` |
| F8 | `kyc_documents.reviewed_by` has an FK and no index. | `V5…sql:23, :27`; index list `V5…sql:30-32` |
| F9 | `idempotency_keys.user_id` has no FK and no index. | `V9…sql:8`; `IdempotencyKey.java:44-45` |
| F10 | `notifications.user_id` and `notification_attempts.notification_id` have no FK (the second has an index). | `V10…sql:3, :23, :30`; `NotificationAttempt.java:27-28` |
| F11 | `users.kyc_approved_by` is a UUID with no FK. | `V5…sql:8`; `User.java:104-105` |
| F12 | `users.kyc_tier` has SQL default 1 and is nullable. The Java default is 0. The domain treats 0 as unverified. | `V5…sql:3`; `User.java:88-90`; `KycExpiryJob.java:41` (sets 0); `KycStatusResponse.java:15` ("0 = unverified") |
| F13 | `users.email_verified`, `users.deleted`, `users.pin_attempts` are nullable in SQL. The entity says `deleted` is `nullable=false`, and `@SQLRestriction("deleted = false")` silently hides NULL rows. | `V2…sql:9, :12, :16`; `User.java:56-58, :68-70, :108-111`; `User.java:26` |
| F14 | `version` columns: nullable in `users`, `wallets`, `kyc_documents`, `idempotency_keys`; NOT NULL in `transactions`. | `V2…sql:18`; `V3…sql:9`; `V5…sql:24`; `V9…sql:12`; `V6…sql:13` |
| F15 | `refresh_tokens.revoked` is nullable in SQL. The entity says `nullable=false`. | `V4…sql:6`; `RefreshToken.java:42-44` |
| F16 | `schema_version_test` is a leftover table. No Java, YAML, or test code references it. Only docs do. | `V1…sql:1-4`; `current-architecture.md:144, :161, :176` |
| F17 | `idx_wallets_user_id` becomes redundant once a UNIQUE on `user_id` exists. `idx_notifications_user_id` is a left prefix of `idx_notifications_user_read (user_id, read_at)`. | `V3…sql:16`; `V10…sql:17`; `V11…sql:3` |
| F18 | `idx_transactions_payout_channel` and `idx_transactions_external_status` have no reads. `payout_channel` is written (`"mobile-money"`) and `external_status` is written and updated, but neither is filtered on in any repository query. | `V14…sql:15-16`; `PayoutService.java:327`, `:325, :478, :500, :529`; `TransactionRepository.java:139-181` (no such filter) |
| F19 | The reconciliation query filters on `type`, `status`, `next_status_check_at`, `provider_reference`, `created_at`. The existing index covers `(next_status_check_at, status, provider_reference)` only. | `TransactionRepository.java:175-181`; `V15…sql:37-38` |
| F20 | Audit search filters on `action`, `performed_by`, `entity_type` with a created_at range. No index leads with `action`. | `AdminService.java:176, :182, :190, :196`; `V8…sql:14-16` |
| F21 | `audit_logs` has no DB-level protection. The writer only INSERTs through `save()`. `AuditLog` has no setters. The javadoc says "No update or delete operations are permitted". | `AuditService.java:55`; `AuditLog.java:11, :93-101`; `AuditLogRepository.java:12` |
| F22 | **The app role owns the schema and is a superuser.** `DATABASE_USERNAME` is `novawallet`, the same as the Postgres image's `POSTGRES_USER`. Flyway uses the same datasource. A REVOKE against that role has no effect on it. | `docker-compose.yml:41`, `:93`; `application-prod.yml:10-13, :27-29` |
| F23 | No `db-roles.md` exists. No `scripts/` directory exists. No `pgtest` profile exists in code. `pgtest` appears only in docs. | `ls docs/architecture`; `ls novawallet-api`; grep for `pgtest` returns only `AGENTS.md`, `PHASE_1C_IMPLEMENTATION_PLAN.md`, `PHASE_1C_BATCH_0_REPORT.md` |
| F24 | Nothing deletes audit rows. The only DELETEs are on `idempotency_keys`, `refresh_tokens`, `revoked_access_tokens`, and `notifications` (`deleteOlderThan` has no caller). | `IdempotencyKeyRepository.java:38`; `RefreshTokenRepository.java:27`; `RevokedAccessTokenRepository.java:45`; `NotificationRepository.java:31-33` (no caller found in `src/main/java` or `src/test/java`) |
| F25 | DB uniqueness for DB-29 already exists on the scoped key. Authenticated idempotency keys are stored as `"<userId>:<key>"`, so the global UNIQUE is effectively per user. | `IdempotencyService.java:44-53`; `PayoutService.java:241`; `V9…sql:14`; `V14…sql:18`; `V6…sql:18`; `V15…sql:17` |
| F26 | A DB constraint violation currently surfaces as a 500. No handler maps `DataIntegrityViolationException` or SQLState. The catch-all returns "An unexpected error occurred". | `GlobalExceptionHandler.java:158-170` (grep for `DataIntegrity` returns nothing) |
| F27 | The enum sets are complete for the lists in §3. | `TransactionStatus.java:14-25`; `TransactionType.java:12-21`; `WalletStatus.java:9-13`; `FreezeReason.java` (3 constants); `KycStatus.java:11-19`; `KycDocumentStatus.java`; `KycDocumentType.java:6-11`; `NotificationType.java` (11 constants); `NotificationStatus.java:10-16`; `NotificationChannel.java` (EMAIL, SMS); `ProviderWebhookEventStatus.java:6-18` (6 constants); `IdempotencyStatus.java` (PROCESSING, COMPLETED); `FeeType.java:10-16`; `Role.java:9-13` |
| F28 | Fee unit is unclear. The formula divides a percentage by 100. The seeded TRANSFER percentage is `0.0100`, which the code reads as 0.01% of the amount. The entity javadoc says "percentage". | `FeeEngineService.java:53-55`; `V7…sql:16` (`0.0100`); `FeeConfiguration.java:15` |
| F29 | The `pgtest` profile is referenced as available in the Batch 0 and plan docs, but the repo has no profile file. DEC-11 is still OPEN. | F23; `docs/DECISIONS.md:21` (DEC-11 OPEN) |
| F30 | Baseline count conflict: `novawallet-api/AGENTS.md:26` says 316 after Batch 1. `PHASE_1C_IMPLEMENTATION_PLAN.md` §2 says 243. Live count must be taken at the start of implementation. | as cited |
| F31 | NEW-DB-01..04 are not in the traceability matrix or the risk register (grep count 0 in both). The implementation plan §5 says they must be added in the owning batch. | `PHASE_1C_RISK_TRACEABILITY_MATRIX.md`, `backend-risk-register.md` (grep) |

## 3. Findings by category (T5 items 2 and 5 inputs)

### 3.1 Missing CHECK constraints

| Table.column | Rule | Current state | Proposed | Precheck (§5.0) |
|---|---|---|---|---|
| `wallets.balance` | `>= 0` | none (F2) | `ck_wallets_balance_non_negative` | `wallets_negative_balance` |
| `wallets.status` | `IN ('ACTIVE','FROZEN')` | none (F4) | `ck_wallets_status` | `wallets_bad_status` |
| `wallets.freeze_reason` | NULL or `IN (SUSPICIOUS_ACTIVITY, ADMIN_ACTION, USER_REQUEST)` | none (F4) | `ck_wallets_freeze_reason` | `wallets_bad_freeze_reason` |
| `wallets.currency` | `= 'ZMW'` | none (F4) | `ck_wallets_currency` | `wallets_bad_currency` |
| `transactions.amount` | `> 0` | none (F5); writers use positive values (F6) | `ck_transactions_amount_positive` | `transactions_non_positive_amount` |
| `transactions.type` | `IN (DEPOSIT, WITHDRAWAL, TRANSFER_DEBIT, TRANSFER_CREDIT, FEE)` | none | `ck_transactions_type` | `transactions_bad_type` |
| `transactions.status` | `IN (PENDING, SUCCESSFUL, FAILED, PENDING_RECONCILIATION)` | none (F5) | `ck_transactions_status` | `transactions_bad_status` |
| `transactions.balance_before/after` | NULL or `>= 0` | none | `ck_transactions_balance_snapshot_non_negative` | `transactions_negative_snapshot` |
| `users.kyc_status` | `IN (NOT_SUBMITTED, PENDING, APPROVED, REJECTED)` | none | `ck_users_kyc_status` | `users_bad_kyc_status` |
| `users.role` | `IN (USER, ADMIN)` | none | `ck_users_role` | `users_bad_role` |
| `users.kyc_tier`, `users.pin_attempts` | `>= 0` | none | `ck_users_kyc_tier_non_negative`, `ck_users_pin_attempts_non_negative` | `users_negative_tier_or_pin` |
| `kyc_documents.status` | `IN (PENDING, APPROVED, REJECTED)` | none | `ck_kyc_documents_status` | `kyc_documents_bad_status` |
| `kyc_documents.file_size` | `> 0` | none | `ck_kyc_documents_file_size_positive` | `kyc_documents_non_positive_size` |
| `kyc_documents.document_type` | `IN (...)` | **deferred** (DEC-33 will add NRC_FRONT/NRC_BACK; a CHECK now would block the agreed change) | — | — |
| `fee_configurations.transaction_type` | `IN (TRANSFER, WITHDRAWAL, DEPOSIT)` | none | `ck_fee_config_type` | `fee_bad_type` |
| `fee_configurations.percentage_fee`, `flat_fee`, `min_fee`, `max_fee` | `>= 0` (upper bound waits for DEC-36) | none | `ck_fee_config_*_non_negative`, `ck_fee_config_min_le_max` | `fee_negative_or_inverted` |
| `notifications.status`, `channel` | `IN (...)` | none | `ck_notifications_status`, `ck_notifications_channel` | `notifications_bad_status_or_channel` |
| `notifications.type` | `IN (11 constants)` | **deferred to BE-1D** (types will grow; each addition would need a migration) | — | — |
| `notifications.retry_count`, `max_retries` | `>= 0` | none | `ck_notifications_retry_non_negative` | `notifications_null_retry` |
| `idempotency_keys.status` | `IN (PROCESSING, COMPLETED)` | none | `ck_idempotency_keys_status` | `idempotency_bad_status` |
| `provider_webhook_events.processing_status` | `IN (6 constants)` | none (F27) | `ck_provider_webhook_events_status` | `webhook_bad_status` |

Existing check worth keeping: `chk_transactions_attempt_count_non_negative` (`V15…sql:25-27`). No change.

### 3.2 FK indexes and missing foreign keys

Missing indexes on existing FK columns: `kyc_documents.reviewed_by` (F8), `transactions.related_transaction_id` (F7, once it gets an FK), `idempotency_keys.user_id` (F9, once it gets an FK), `users.kyc_approved_by` (F11, once it gets an FK).

Missing FKs (F7, F9, F10, F11): `transactions.related_transaction_id`, `idempotency_keys.user_id`, `notifications.user_id`, `users.kyc_approved_by`, and `notification_attempts.notification_id` (`ON DELETE CASCADE`).

Intentionally **no** FK: `audit_logs.entity_id` and `audit_logs.performed_by` (polymorphic and history-preserving; an FK would also block future user removal). Recorded as a decision in §8.

Query-driven indexes (only if EXPLAIN in §10 shows the planner needs them):
- Reconciliation and pending-payout scan (F19): `(type, status, next_status_check_at)`.
- Audit search (F20): `(action, created_at)`, `(performed_by, created_at)`, `(entity_type, created_at)`.

### 3.3 Nullability and default mismatches (entity vs schema)

| Item | Entity | Schema | Consequence | Proposed |
|---|---|---|---|---|
| `users.kyc_tier` default | `0` (`User.java:88-90`, `@Builder.Default`) | `DEFAULT 1`, nullable (`V5…sql:3`) | Latent: JPA always writes 0, so the SQL default only affects native inserts. Tier semantics conflict (F12). | **DEC-35** (recommend 0). Then `SET DEFAULT 0` and `SET NOT NULL` |
| `users.deleted` | `nullable=false` (`User.java:108-111`) | nullable (`V2…sql:16`) | A NULL row disappears from every query (`@SQLRestriction`). Silent. | `SET NOT NULL` after backfill |
| `users.email_verified`, `users.pin_attempts` | primitive | nullable | Hibernate would fail to load NULL into a primitive | `SET NOT NULL` after backfill |
| `refresh_tokens.revoked` | `nullable=false` (`RefreshToken.java:42-44`) | nullable (`V4…sql:6`) | Same as above | `SET NOT NULL` after backfill |
| `version` on `users`, `wallets`, `kyc_documents`, `idempotency_keys` | `@Version Integer` | nullable (F14) | Optimistic-lock column may be NULL, which weakens concurrency control | `SET NOT NULL` after backfill (`transactions` is already NOT NULL) |
| `wallets.user_id` uniqueness | Javadoc says one wallet per user (`Wallet.java:14-17`) | no UNIQUE (F3) | Concurrent approval can create two (NEW-FIN-01) | `uq_wallets_user_id` (V20), after BE-1C-3 code is merged |
| `wallets` non-negative | Javadoc says the DB prevents negatives (`Wallet.java:18`) | not true (F2) | Documentation is wrong | Add CHECK (V18) and correct the javadoc |
| `kyc_documents.file_size`, `mime_type` | `Long`, `length=50` | BIGINT, VARCHAR(50) | Consistent | none |
| `transactions.amount` precision | `precision=15, scale=2` | `DECIMAL(15,2)` | Consistent | none |
| `fee_configurations.percentage_fee` | `precision=5, scale=4` | `DECIMAL(5,4)` | Consistent, but the unit is unclear (F28) | DEC-36 |

Hibernate `ddl-auto: validate` (`application-prod.yml:23`) does not check NULL-ability or defaults. The gap is covered by the PostgreSQL integration test in §7.

### 3.4 Leftover tables, columns, and indexes

- **`schema_version_test`** (`V1…sql:1-4`). Unreferenced in code and tests (F16). Drop in V23. Do not edit V1.
- **`idx_wallets_user_id`** (`V3…sql:16`). Redundant after `uq_wallets_user_id`. Drop in V23.
- **`idx_notifications_user_id`** (`V10…sql:17`). Left prefix of `idx_notifications_user_read` (`V11…sql:3`). Drop in V23.
- **`idx_transactions_payout_channel`** (`V14…sql:16`) and **`idx_transactions_external_status`** (`V14…sql:15`). Written but never filtered on (F18). Drop only after `pg_stat_user_indexes.idx_scan = 0` for a full observation window on staging (§10 step 9). Conditional, not in the default V23.
- **`NotificationRepository.deleteOlderThan`** (`NotificationRepository.java:31-33`). Dead code with no caller (F24). Not a schema issue. Report it, do not delete it in this batch.
- **`transactions.payout_channel`**: written as `"mobile-money"` (`PayoutService.java:327`), never read. Column stays; no drop in this batch.
- **`description`-based payout markers** (`TransactionRepository.java:57-58` LIKE query): legacy but still read. Out of scope.

### 3.5 Audit immutability (DEC-06), and the stop point

What exists: nothing at the database layer. The app only INSERTs (F21), and nothing deletes audit rows (F24). Application-only immutability is therefore correct today but not enforced.

What DEC-06 requires: DB-level revoke of UPDATE and DELETE from the **application role**.

**Why a REVOKE alone does not work here (F22):**
1. The application role is the table owner. An owner can re-grant to itself.
2. The application role is the Postgres bootstrap superuser (`POSTGRES_USER`, `docker-compose.yml:93`). A superuser bypasses every privilege check, so REVOKE is ignored.
3. The same role runs Flyway. Migrations run with DDL rights, which can `ALTER TABLE` or `DROP TRIGGER`.

What works:
- **Separate roles**: a migration owner (runs Flyway, owns the tables) and a non-owner, non-superuser application role (gets SELECT and INSERT on `audit_logs`, plus whatever else it needs). The REVOKE then applies to a real restriction.
- **A trigger** that raises an error on UPDATE, DELETE, and TRUNCATE. Triggers fire for superusers too. The owner can still disable one with `ALTER TABLE … DISABLE TRIGGER`, so the trigger is defence in depth, not proof against a DBA.

**This affects deployment** (the owner and infra), so per the BE-1C-4 prompt this batch **stops at §8 DEC-06a before writing V22**. Changes outside `novawallet-api/` (`docker-compose.yml`, the production DB bootstrap, `infra/`) are not made by this batch.

### 3.6 DB-29 (duplicate financial effects)

No DDL. Current uniqueness is already designed correctly and is the closure evidence:
- `transactions.reference` UNIQUE (`V6…sql:18`).
- `transactions.idempotency_key` UNIQUE index (`V14…sql:18`), populated with the scoped key (`PayoutService.java:241`).
- `idempotency_keys.idempotency_key` UNIQUE (`V9…sql:14`), scoped as `"<userId>:<key>"` (`IdempotencyService.java:44-53`).
- `provider_webhook_events (provider, event_id)` UNIQUE (`V15…sql:17`).

A composite `UNIQUE (sender, receiver, type, amount, reference)` is rejected (DEC-05; plan §9.3). Closing DB-29 needs owner acknowledgement only (§8 DEC-05 ack).

### 3.7 Other findings (not fixed here; reported)

- **F26** A DB constraint violation returns 500 with a generic body, so the new constraints would surface as 500s. §6 fixes this.
- **F28** Fee unit ambiguity (DEC-36). Affects the CHECK range and the financial meaning of `0.0100`.
- **F29** `pgtest` profile absent (DEC-11).
- **F30** Baseline test count conflict (316 vs 243).
- **F31** NEW-DB-01..04 missing from the register and matrix. Needs adding in the owning batch.
- **Wallet javadoc** (`Wallet.java:18`) says the DB prevents negatives. False until V18.
- **`AuditLog` javadoc** (`AuditLog.java:11`) is accurate for the app, not for the DB.

## 4. Files to change (T5 item 2)

All paths are in `novawallet-api/` unless noted. Nothing outside that folder changes in this batch, except the TODO and tracking files named under "Out of scope".

**New**
| File | Purpose |
|---|---|
| `scripts/db/precheck_v18.sql` | Read-only pre-migration checks (§5.0). Every row must return 0 violations |
| `src/main/resources/db/migration/V18__integrity_check_constraints.sql` | CHECK constraints (§5.1) |
| `src/main/resources/db/migration/V19__integrity_not_null_and_foreign_keys.sql` | Backfill of NULL flags, NOT NULL, FKs (§5.2) |
| `src/main/resources/db/migration/V20__wallets_user_id_unique.sql` | `UNIQUE (user_id)` on `wallets` (§5.3) |
| `src/main/resources/db/migration/V21__query_and_fk_indexes.sql` | FK and query indexes (§5.4) |
| `src/main/java/db/migration/V22__AuditLogImmutability.java` | Trigger and REVOKE, PostgreSQL only (§5.5). **Blocked by DEC-06a** |
| `src/main/resources/db/migration/V23__contract_drop_leftovers.sql` | Contract step (§5.7) |
| `docs/architecture/db-roles.md` | Role model required by BE-1C-4 item 3 |
| `src/test/resources/application-pgtest.yml` | `pgtest` profile (reads `SPRING_DATASOURCE_*`). Needs DEC-11 confirmed |
| `src/test/java/.../db/MigrationFreshPostgresTest.java` | `@Tag("postgres")` |
| `src/test/java/.../db/MigrationUpgradePostgresTest.java` | `@Tag("postgres")` |
| `src/test/java/.../db/ConstraintIntegrityPostgresTest.java` | `@Tag("postgres")` |
| `src/test/java/.../db/AuditImmutabilityPostgresTest.java` | `@Tag("postgres")`, blocked by DEC-06a |
| `src/test/java/.../db/MigrationPortabilityH2Test.java` | H2 run of V18 to V21 and the V22 skip path |
| `src/test/java/.../exception/DataIntegrityExceptionMappingTest.java` | SQLState to HTTP mapping |
| `src/test/resources/db/fixtures/batch4_seed.sql` | Seed rows for the upgrade test, including the "clean" and "dirty" variants |

**Modified**
| File | Change |
|---|---|
| `src/main/java/com/novawallet/novawallet_api/exception/GlobalExceptionHandler.java` | New `DataIntegrityViolationException` handler keyed on SQLState (§6). Lines near `:158` untouched |
| `src/main/java/com/novawallet/novawallet_api/wallet/entity/Wallet.java` | Correct javadoc at `:14-19` (the DB claim becomes true only after V18). No logic change |
| `src/main/java/com/novawallet/novawallet_api/user/entity/User.java` | `kycTier` default only after DEC-35 (`:88-90`). `deleted` stays `nullable=false` |
| `src/main/resources/application-prod.yml` | Only if DEC-06a adds a Flyway placeholder for the app role name (`spring.flyway.placeholders`) |
| `docs/architecture/backend-risk-register.md` | DB-01 status, NEW-DB-01..04 entries, **at the Batch Gate only, with evidence** |
| `docs/phase-1c/PHASE_1C_RISK_TRACEABILITY_MATRIX.md` | Same, with executable evidence only |

**Out of scope (report, do not do):**
- `docker-compose.yml`, production DB bootstrap, `infra/`: role separation (DEC-06a). Owner and infra.
- `TODO.md` at repo root: `_COMMON_BATCH_RULES.md` step 3 asks for batch checkboxes there, but the BE-1C-4 prompt names only `novawallet-api` scope (rule 2 of root AGENTS.md). **Confirm before touching.**
- `novawallet-app/`, `novawallet-admin/`: not touched.
- `NotificationRepository.deleteOlderThan`: dead code, reported only.
- `reports/API_ENDPOINT_INVENTORY.md`: no endpoint changes in this batch, so no update.

## 5. Migrations: exact SQL and expand → migrate → contract (T5 item 3)

Flyway runs each SQL migration in one transaction on PostgreSQL. A failed migration rolls back, and the version is not recorded. This is the basis for the "fails safely" requirement.

### 5.0 Precheck (read-only, run before V18, owner or CI, never inside Flyway)

`scripts/db/precheck_v18.sql`. Every `violations` value must be 0. Any non-zero value stops the batch and goes to the owner.

```sql
-- scripts/db/precheck_v18.sql  (read-only; every violations value must be 0)
SELECT 'wallets_negative_balance' AS check_name, COUNT(*) AS violations FROM wallets WHERE balance < 0
UNION ALL SELECT 'wallets_duplicate_user', COUNT(*) FROM (SELECT user_id FROM wallets GROUP BY user_id HAVING COUNT(*) > 1) d
UNION ALL SELECT 'wallets_bad_status', COUNT(*) FROM wallets WHERE status NOT IN ('ACTIVE','FROZEN')
UNION ALL SELECT 'wallets_bad_freeze_reason', COUNT(*) FROM wallets WHERE freeze_reason IS NOT NULL AND freeze_reason NOT IN ('SUSPICIOUS_ACTIVITY','ADMIN_ACTION','USER_REQUEST')
UNION ALL SELECT 'wallets_bad_currency', COUNT(*) FROM wallets WHERE currency <> 'ZMW'
UNION ALL SELECT 'wallets_null_version', COUNT(*) FROM wallets WHERE version IS NULL
UNION ALL SELECT 'transactions_non_positive_amount', COUNT(*) FROM transactions WHERE amount <= 0
UNION ALL SELECT 'transactions_bad_type', COUNT(*) FROM transactions WHERE type NOT IN ('DEPOSIT','WITHDRAWAL','TRANSFER_DEBIT','TRANSFER_CREDIT','FEE')
UNION ALL SELECT 'transactions_bad_status', COUNT(*) FROM transactions WHERE status NOT IN ('PENDING','SUCCESSFUL','FAILED','PENDING_RECONCILIATION')
UNION ALL SELECT 'transactions_negative_snapshot', COUNT(*) FROM transactions WHERE balance_before < 0 OR balance_after < 0
UNION ALL SELECT 'transactions_orphan_related', COUNT(*) FROM transactions t WHERE t.related_transaction_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM transactions r WHERE r.id = t.related_transaction_id)
UNION ALL SELECT 'users_bad_kyc_status', COUNT(*) FROM users WHERE kyc_status NOT IN ('NOT_SUBMITTED','PENDING','APPROVED','REJECTED')
UNION ALL SELECT 'users_bad_role', COUNT(*) FROM users WHERE role NOT IN ('USER','ADMIN')
UNION ALL SELECT 'users_negative_tier_or_pin', COUNT(*) FROM users WHERE kyc_tier < 0 OR pin_attempts < 0
UNION ALL SELECT 'users_null_flags_tier_version', COUNT(*) FROM users WHERE email_verified IS NULL OR deleted IS NULL OR pin_attempts IS NULL OR kyc_tier IS NULL OR version IS NULL
UNION ALL SELECT 'users_orphan_kyc_approver', COUNT(*) FROM users u WHERE u.kyc_approved_by IS NOT NULL AND NOT EXISTS (SELECT 1 FROM users a WHERE a.id = u.kyc_approved_by)
UNION ALL SELECT 'kyc_documents_bad_status', COUNT(*) FROM kyc_documents WHERE status NOT IN ('PENDING','APPROVED','REJECTED')
UNION ALL SELECT 'kyc_documents_non_positive_size', COUNT(*) FROM kyc_documents WHERE file_size <= 0
UNION ALL SELECT 'kyc_documents_orphan_reviewer', COUNT(*) FROM kyc_documents d WHERE d.reviewed_by IS NOT NULL AND NOT EXISTS (SELECT 1 FROM users u WHERE u.id = d.reviewed_by)
UNION ALL SELECT 'kyc_documents_null_version', COUNT(*) FROM kyc_documents WHERE version IS NULL
UNION ALL SELECT 'fee_bad_type', COUNT(*) FROM fee_configurations WHERE transaction_type NOT IN ('TRANSFER','WITHDRAWAL','DEPOSIT')
UNION ALL SELECT 'fee_negative_or_inverted', COUNT(*) FROM fee_configurations WHERE percentage_fee < 0 OR flat_fee < 0 OR min_fee < 0 OR max_fee < 0 OR (min_fee IS NOT NULL AND max_fee IS NOT NULL AND min_fee > max_fee)
UNION ALL SELECT 'notifications_bad_status_or_channel', COUNT(*) FROM notifications WHERE status NOT IN ('PENDING','SENT','FAILED') OR channel NOT IN ('EMAIL','SMS')
UNION ALL SELECT 'notifications_negative_retry', COUNT(*) FROM notifications WHERE retry_count < 0 OR max_retries < 0
UNION ALL SELECT 'notifications_orphan_user', COUNT(*) FROM notifications n WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = n.user_id)
UNION ALL SELECT 'notification_attempts_orphan', COUNT(*) FROM notification_attempts a WHERE NOT EXISTS (SELECT 1 FROM notifications n WHERE n.id = a.notification_id)
UNION ALL SELECT 'idempotency_bad_status', COUNT(*) FROM idempotency_keys WHERE status NOT IN ('PROCESSING','COMPLETED')
UNION ALL SELECT 'idempotency_orphan_user', COUNT(*) FROM idempotency_keys k WHERE k.user_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM users u WHERE u.id = k.user_id)
UNION ALL SELECT 'idempotency_null_version', COUNT(*) FROM idempotency_keys WHERE version IS NULL
UNION ALL SELECT 'refresh_tokens_null_revoked', COUNT(*) FROM refresh_tokens WHERE revoked IS NULL
UNION ALL SELECT 'webhook_bad_status', COUNT(*) FROM provider_webhook_events WHERE processing_status NOT IN ('RECEIVED','PROCESSED','DUPLICATE','QUARANTINED','DEFERRED','REJECTED');
```

Also record the row counts of `wallets`, `transactions`, `audit_logs` before and after (for the "no data loss" check).

### 5.1 V18__integrity_check_constraints.sql (expand, additive)

```sql
-- Phase 1C Batch 4 (BE-1C-4), expand. Precondition: scripts/db/precheck_v18.sql returns 0 for every check.
ALTER TABLE wallets ADD CONSTRAINT ck_wallets_balance_non_negative CHECK (balance >= 0);
ALTER TABLE wallets ADD CONSTRAINT ck_wallets_status CHECK (status IN ('ACTIVE', 'FROZEN'));
ALTER TABLE wallets ADD CONSTRAINT ck_wallets_freeze_reason
    CHECK (freeze_reason IS NULL OR freeze_reason IN ('SUSPICIOUS_ACTIVITY', 'ADMIN_ACTION', 'USER_REQUEST'));
ALTER TABLE wallets ADD CONSTRAINT ck_wallets_currency CHECK (currency = 'ZMW');

ALTER TABLE transactions ADD CONSTRAINT ck_transactions_amount_positive CHECK (amount > 0);
ALTER TABLE transactions ADD CONSTRAINT ck_transactions_type
    CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_DEBIT', 'TRANSFER_CREDIT', 'FEE'));
ALTER TABLE transactions ADD CONSTRAINT ck_transactions_status
    CHECK (status IN ('PENDING', 'SUCCESSFUL', 'FAILED', 'PENDING_RECONCILIATION'));
ALTER TABLE transactions ADD CONSTRAINT ck_transactions_balance_snapshot_non_negative
    CHECK ((balance_before IS NULL OR balance_before >= 0) AND (balance_after IS NULL OR balance_after >= 0));

ALTER TABLE users ADD CONSTRAINT ck_users_kyc_status
    CHECK (kyc_status IN ('NOT_SUBMITTED', 'PENDING', 'APPROVED', 'REJECTED'));
ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'));
ALTER TABLE users ADD CONSTRAINT ck_users_kyc_tier_non_negative CHECK (kyc_tier >= 0);
ALTER TABLE users ADD CONSTRAINT ck_users_pin_attempts_non_negative CHECK (pin_attempts >= 0);

ALTER TABLE kyc_documents ADD CONSTRAINT ck_kyc_documents_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'));
ALTER TABLE kyc_documents ADD CONSTRAINT ck_kyc_documents_file_size_positive CHECK (file_size > 0);

ALTER TABLE fee_configurations ADD CONSTRAINT ck_fee_config_type
    CHECK (transaction_type IN ('TRANSFER', 'WITHDRAWAL', 'DEPOSIT'));
ALTER TABLE fee_configurations ADD CONSTRAINT ck_fee_config_percentage_non_negative CHECK (percentage_fee >= 0);
ALTER TABLE fee_configurations ADD CONSTRAINT ck_fee_config_flat_non_negative CHECK (flat_fee >= 0);
ALTER TABLE fee_configurations ADD CONSTRAINT ck_fee_config_min_non_negative CHECK (min_fee IS NULL OR min_fee >= 0);
ALTER TABLE fee_configurations ADD CONSTRAINT ck_fee_config_max_non_negative CHECK (max_fee IS NULL OR max_fee >= 0);
ALTER TABLE fee_configurations ADD CONSTRAINT ck_fee_config_min_le_max
    CHECK (min_fee IS NULL OR max_fee IS NULL OR min_fee <= max_fee);

ALTER TABLE notifications ADD CONSTRAINT ck_notifications_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'));
ALTER TABLE notifications ADD CONSTRAINT ck_notifications_channel CHECK (channel IN ('EMAIL', 'SMS'));
ALTER TABLE notifications ADD CONSTRAINT ck_notifications_retry_non_negative
    CHECK (retry_count >= 0 AND max_retries >= 0);

ALTER TABLE idempotency_keys ADD CONSTRAINT ck_idempotency_keys_status CHECK (status IN ('PROCESSING', 'COMPLETED'));

ALTER TABLE provider_webhook_events ADD CONSTRAINT ck_provider_webhook_events_status
    CHECK (processing_status IN ('RECEIVED', 'PROCESSED', 'DUPLICATE', 'QUARANTINED', 'DEFERRED', 'REJECTED'));
```

Note: `ck_fee_config_percentage_non_negative` deliberately has no upper bound until DEC-36 is answered.

### 5.2 V19__integrity_not_null_and_foreign_keys.sql (migrate NULLs, then expand)

Backfills only non-financial flags and version counters, to the value the entity already assumes. `kyc_tier` is included only if DEC-35 answers 0. The precheck must show the counts first.

```sql
-- Phase 1C Batch 4 (BE-1C-4), migrate-then-expand. Backfill is limited to flags and version counters.
UPDATE users SET email_verified = FALSE WHERE email_verified IS NULL;
UPDATE users SET deleted = FALSE WHERE deleted IS NULL;
UPDATE users SET pin_attempts = 0 WHERE pin_attempts IS NULL;
UPDATE users SET version = 0 WHERE version IS NULL;
UPDATE wallets SET version = 0 WHERE version IS NULL;
UPDATE kyc_documents SET version = 0 WHERE version IS NULL;
UPDATE idempotency_keys SET version = 0 WHERE version IS NULL;
UPDATE refresh_tokens SET revoked = FALSE WHERE revoked IS NULL;
-- kyc_tier: only after DEC-35. Recommended value 0.
-- UPDATE users SET kyc_tier = 0 WHERE kyc_tier IS NULL;
-- ALTER TABLE users ALTER COLUMN kyc_tier SET DEFAULT 0;   -- DEC-35

ALTER TABLE users ALTER COLUMN email_verified SET NOT NULL;
ALTER TABLE users ALTER COLUMN deleted SET NOT NULL;
ALTER TABLE users ALTER COLUMN pin_attempts SET NOT NULL;
ALTER TABLE users ALTER COLUMN version SET NOT NULL;
ALTER TABLE wallets ALTER COLUMN version SET NOT NULL;
ALTER TABLE kyc_documents ALTER COLUMN version SET NOT NULL;
ALTER TABLE idempotency_keys ALTER COLUMN version SET NOT NULL;
ALTER TABLE refresh_tokens ALTER COLUMN revoked SET NOT NULL;
-- ALTER TABLE users ALTER COLUMN kyc_tier SET NOT NULL;    -- DEC-35

ALTER TABLE transactions ADD CONSTRAINT fk_transactions_related_transaction
    FOREIGN KEY (related_transaction_id) REFERENCES transactions (id);
ALTER TABLE notifications ADD CONSTRAINT fk_notifications_user
    FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE idempotency_keys ADD CONSTRAINT fk_idempotency_keys_user
    FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE users ADD CONSTRAINT fk_users_kyc_approved_by
    FOREIGN KEY (kyc_approved_by) REFERENCES users (id);
ALTER TABLE notification_attempts ADD CONSTRAINT fk_notification_attempts_notification
    FOREIGN KEY (notification_id) REFERENCES notifications (id) ON DELETE CASCADE;
```

Note on the `transactions` self-FK: in the transfer path, the credit row is persisted before the debit row's `related_transaction_id` is updated (`TransactionService.java:312-317`), and fee rows point at already-persisted parents (`TransactionService.java:320-330`). The FK should hold under Hibernate's flush order. This is a test item (§7), not assumed.

### 5.3 V20__wallets_user_id_unique.sql (expand, hard dependency)

```sql
-- Phase 1C Batch 4 (BE-1C-4). HARD DEPENDENCY: BE-1C-3 idempotent wallet creation must be merged first,
-- otherwise concurrent KYC approval fails with a 500 instead of a controlled retry (IMPLEMENTATION_PLAN §10).
ALTER TABLE wallets ADD CONSTRAINT uq_wallets_user_id UNIQUE (user_id);
```

### 5.4 V21__query_and_fk_indexes.sql (expand, additive)

```sql
-- Phase 1C Batch 4 (BE-1C-4). FK indexes are unconditional. Query indexes are included only if §10 step 6 EXPLAIN shows the need.
CREATE INDEX idx_kyc_documents_reviewed_by ON kyc_documents (reviewed_by);
CREATE INDEX idx_transactions_related_transaction_id ON transactions (related_transaction_id);
CREATE INDEX idx_idempotency_keys_user_id ON idempotency_keys (user_id);
CREATE INDEX idx_users_kyc_approved_by ON users (kyc_approved_by);

-- Conditional on EXPLAIN evidence (F19, F20):
CREATE INDEX idx_transactions_payout_due ON transactions (type, status, next_status_check_at);
CREATE INDEX idx_audit_logs_action_created_at ON audit_logs (action, created_at);
CREATE INDEX idx_audit_logs_performed_by_created_at ON audit_logs (performed_by, created_at);
CREATE INDEX idx_audit_logs_entity_type_created_at ON audit_logs (entity_type, created_at);
```

### 5.5 V22 (Java): audit immutability, PostgreSQL only. **Blocked by DEC-06a**

Implemented as `src/main/java/db/migration/V22__AuditLogImmutability.java`, the same pattern as V12. It checks `getDatabaseProductName()`. On anything other than PostgreSQL it logs and returns, so H2 is unaffected.

Statements, in order:

```sql
CREATE OR REPLACE FUNCTION nw_reject_audit_log_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs is append-only (% rejected)', TG_OP
        USING ERRCODE = 'insufficient_privilege';
END;
$$;

CREATE TRIGGER trg_audit_logs_no_update_delete
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION nw_reject_audit_log_mutation();

CREATE TRIGGER trg_audit_logs_no_truncate
    BEFORE TRUNCATE ON audit_logs
    FOR EACH STATEMENT EXECUTE FUNCTION nw_reject_audit_log_mutation();

-- Only when a non-owner application role is configured (DEC-06a). The name must match ^[a-z_][a-z0-9_]{0,62}$ and must not equal the migration owner.
REVOKE UPDATE, DELETE, TRUNCATE ON audit_logs FROM "<app_role>";
```

The app role name comes from a Flyway placeholder (for example `${audit_app_role}`, set from `DB_APP_ROLE`). Read it through the Flyway configuration API. Confirm the exact call against the Flyway version in `pom.xml` at implementation time. If the placeholder is empty, skip the REVOKE, log a WARN, and keep the trigger.

Compatibility: `AuditService.java:55` (`save`) and `AuditLog` (no setters, `AuditLog.java:93-101`) need no change. Nothing else writes to or deletes from `audit_logs` (F24).

### 5.6 Expand → migrate → contract summary

| Step | Migration | Type | Data change | Gate before it runs |
|---|---|---|---|---|
| Migrate (outside Flyway) | `scripts/db/precheck_v18.sql` | read-only | none | Every violations value = 0, or owner runbook |
| Expand | V18 | CHECK constraints | none | precheck |
| Migrate + expand | V19 | backfill of flags and version counters, NOT NULL, FKs | backfill NULLs only | precheck; DEC-35 if `kyc_tier` included |
| Expand | V20 | UNIQUE `wallets.user_id` | none | BE-1C-3 idempotent creation merged; precheck duplicates = 0 |
| Expand | V21 | indexes | none (CREATE INDEX takes a write lock during build; acceptable at pre-launch volume) | §10 EXPLAIN for query indexes |
| Expand (PG only) | V22 | trigger, REVOKE | none | DEC-06a |
| Contract | V23 | drop `schema_version_test`, `idx_wallets_user_id`, `idx_notifications_user_id` | none | code verified to not reference them (F16, F17) |
| Contract (conditional) | V23 or later | drop `idx_transactions_payout_channel`, `idx_transactions_external_status` | none | `pg_stat_user_indexes.idx_scan = 0` over an observed window (F18) |

### 5.7 V23__contract_drop_leftovers.sql (contract)

```sql
-- Phase 1C Batch 4 (BE-1C-4), contract. Nothing in the application reads these objects (F16, F17).
DROP TABLE IF EXISTS schema_version_test;
DROP INDEX IF EXISTS idx_wallets_user_id;        -- superseded by uq_wallets_user_id (V20)
DROP INDEX IF EXISTS idx_notifications_user_id;  -- left prefix of idx_notifications_user_read (V11)
-- Conditional on pg_stat_user_indexes evidence (F18); not in the default migration:
-- DROP INDEX IF EXISTS idx_transactions_payout_channel;
-- DROP INDEX IF EXISTS idx_transactions_external_status;
```

Rollback: Flyway is forward-only. A bad migration is fixed with a new migration. A dropped table can be restored from a backup, so take one before V23 on any shared environment.

### 5.8 Numbering check

- V1 to V16 are not edited. V12 is not created.
- New names are V18 to V23. If BE-1C-3 takes V17, nothing changes. If it does not, the numbers drop by one.
- The Java V22 uses the `db.migration` package, which Flyway scans by default, as V12 does.

## 6. API and error mapping (T5 item 4)

No endpoint, request, or response shape changes. There is no OpenAPI change and no `reports/API_ENDPOINT_INVENTORY.md` change.

New behaviour in `GlobalExceptionHandler` (F26): handle `DataIntegrityViolationException` by reading the root `SQLException.getSQLState()`.

| SQLState | Meaning | HTTP | `error` code | Body |
|---|---|---|---|---|
| 23505 | unique violation | 409 | `DUPLICATE_RESOURCE` (same as existing `DuplicateResourceException`, `GlobalExceptionHandler.java:37-42`) | generic message |
| 23514 | check violation | 422 | `DATA_RULE_VIOLATION` | generic message: "The request violates a data rule and was not applied" |
| 23503 | foreign key violation | 422 | `DATA_RULE_VIOLATION` | same |
| 23502 | not null violation | 422 | `DATA_RULE_VIOLATION` | same |
| other | — | unchanged (500, `GlobalExceptionHandler.java:158-170`) | — | — |

Rules: never return SQL text, constraint names, table names, or stack traces. Log the constraint name at WARN, not the statement or values. A correct application path should never reach these branches. They are a safety net.

## 7. Risks and how each is tested (T5 item 5)

| # | Risk | Mitigation | Test (PostgreSQL 16 unless noted) |
|---|---|---|---|
| R1 | Existing data violates a constraint, and the migration fails halfway | precheck; transactional DDL; migration stops | `MigrationUpgradePostgresTest`: dirty seed → migrate throws, `flyway_schema_history` has no V18 row, schema unchanged |
| R2 | DDL breaks the H2 test suite, failing every Spring context | portable SQL only; V22 skips non-PG | `MigrationPortabilityH2Test`; full `./mvnw -B clean test` |
| R3 | `uq_wallets_user_id` fails on existing duplicates | precheck; V20 is gated on BE-1C-3 | `MigrationUpgradePostgresTest` dirty seed with a duplicate wallet |
| R4 | `transactions` self-FK rejects a valid transfer or payout flush | none needed beyond test | existing transfer and payout integration tests re-run against PG16 with V19 applied |
| R5 | `kyc_tier` default change is wrong for the business | DEC-35 gate; no change without owner answer | `UserDefaultsMigrationTest` (after DEC-35): native insert without `kyc_tier` yields the agreed value; entity default agrees |
| R6 | NOT NULL on `deleted` hides users | backfill, then NOT NULL | `ConstraintIntegrityPostgresTest`: NULL `deleted` rejected; upgrade seed with NULL flags backfilled and visible |
| R7 | CHECK rejects a value the app writes | enum lists taken from the Java sources (F27) | `ConstraintIntegrityPostgresTest` inserts every enum constant and expects success; every violating value expects 23514 |
| R8 | Audit REVOKE is ineffective under a superuser app role | DEC-06a; trigger always applied | `AuditImmutabilityPostgresTest`: as non-owner role, UPDATE/DELETE/TRUNCATE → 42501; as owner, trigger → 42501 |
| R9 | Trigger can be disabled by the owner | documented as defence in depth; role separation is the real control | documented test: owner `ALTER TABLE … DISABLE TRIGGER` is possible (records residual risk) |
| R10 | Query index not needed, adds write cost | conditional on EXPLAIN | §10 step 6 |
| R11 | Dropping an index that a query needs | V23 drops only FK-superseded and prefix-redundant indexes; the rest is conditional | `EXPLAIN` before and after; pg_stat evidence |
| R12 | DB error leaks SQL to the client | handler maps SQLState to fixed text | `DataIntegrityExceptionMappingTest`: 23505→409, 23514/23503/23502→422, body has no SQL, no constraint name, no stack |
| R13 | Entity and schema drift again | Hibernate `validate` on PG16 | `ddl-auto: validate` context test under `pgtest` |
| R14 | Upgrade path not exercised | fixture seeded at V16 (or BE-1C-3 head) → head | `MigrationUpgradePostgresTest` |
| R15 | Fresh path not exercised | empty PG16 → head | `MigrationFreshPostgresTest` (includes V1 to V12 Java ordering) |
| R16 | `flyway validate` drift | validate after every run | `flyway.validate()` in both migration tests |
| R17 | Regression of Phase 1A and 1B safeguards | full suite, targeted payout and idempotency tests | `./mvnw -B clean test` (count recorded); payout and idempotency tests unchanged |

## 8. Decisions needed (T5 item 6)

| ID | Question | Options | Recommendation | Blocks |
|---|---|---|---|---|
| **DEC-06a** (new, extends DEC-06) | Role separation for `audit_logs` immutability. Today the app, Flyway, and the superuser are one role (F22). | A: separate migration-owner and app roles, with a bootstrap SQL in `docker-compose.yml` and prod DB setup (**touches `infra/` and `docker-compose.yml`, outside this batch**) · B: trigger only (works for superusers; owner can disable it) · C: REVOKE on the same role (ineffective, reject) | **A plus the trigger.** Owner and infra must agree the bootstrap. Until then V22 stays blocked | V22, `AuditImmutabilityPostgresTest`, `docs/architecture/db-roles.md` |
| **DEC-35** (new) | KYC tier default and nullability: DB default 1 (`V5…sql:3`) vs Java 0 (`User.java:88-90`); domain says 0 = unverified (`KycStatusResponse.java:15`, `KycExpiryJob.java:41`) | 0 (align DB to Java and domain) · 1 (change the domain and KycExpiryJob) | **0.** Does not rewrite existing rows; affects only new native inserts | `kyc_tier` lines in V19 |
| **DEC-36** (new) | Fee percentage unit. Formula divides by 100 (`FeeEngineService.java:54-55`), but seeds are `0.0100` (`V7…sql:16`). Read as 0.01% today. | Confirm the intended unit and seeded values before any upper-bound CHECK | Owner confirms with business. No upper bound until then | `ck_fee_config_percentage_*` upper bound; the Batch 4 fee CHECK stays `>= 0` |
| **DEC-37** (new, scope) | Which type and enum CHECKs wait for a later decision. `kyc_documents.document_type` waits for DEC-33 (NRC_FRONT/NRC_BACK). `notifications.type` waits for BE-1D (types will grow). | Add now and migrate on each change · defer | **Defer both** (§3.1) | §3.1 |
| **DEC-38** (new, confirm) | Audit foreign keys. `audit_logs.entity_id` and `performed_by` have no FK (polymorphic, history-preserving). | Keep no FK · add FK | Keep no FK, record the reason | none |
| **DEC-11** (existing, OPEN) | `pgtest` profile is referenced in docs but missing in code (F23, F29). | `pgtest` profile (no new dependency) · Testcontainers | `pgtest`. Create `application-pgtest.yml` in this batch | all PG16 tests |
| **DEC-05 ack** (existing, DECIDED) | DB-29 closure: uniqueness on scoped keys already exists (F25). No composite unique. | Owner acknowledges closure | Acknowledge and mark DB-29 closed in the register at the gate | DB-29 status |
| **Numbering** (confirm) | Does BE-1C-3 take V17? | yes (Batch 4 starts V18) · no (starts V17) | yes, as written here | file names |
| **Scope** (confirm) | `TODO.md` at repo root is named by `_COMMON_BATCH_RULES.md` step 3 but outside the `novawallet-api/` scope named by the BE-1C-4 prompt | Update it · skip | Owner decides | TODO only |
| **Baseline** (confirm) | Test count baseline: 316 (`novawallet-api/AGENTS.md:26`) vs 243 (`PHASE_1C_IMPLEMENTATION_PLAN.md` §2) | Use the live count from a clean run at the start | Live count, recorded | regression evidence |

## 9. Steps in order (T5 item 7)

Each step is small and verified before the next.

1. Owner answers DEC-06a, DEC-35, DEC-36, DEC-37, DEC-38, DEC-11, numbering, and scope. Stop until answered.
2. Confirm BE-1C-3 state and whether V17 exists. Take the live test baseline on a clean tree. Record the count.
3. Add `application-pgtest.yml` (DEC-11). Verify it boots against a disposable PG16 with Flyway on an empty schema, before any new migration.
4. Create `scripts/db/precheck_v18.sql`. Run it on the disposable PG16 seed (clean and dirty). Clean returns all zeros. Dirty returns the expected non-zero rows.
5. Write V18. Run H2 suite. Run PG16 fresh and upgrade tests. Record counts.
6. Run EXPLAIN (§10 step 6) for the reconciliation and audit queries on a seeded PG16. Decide the conditional V21 indexes.
7. Write V19 and the tests for NULL backfill, NOT NULL, and FKs. Re-run H2 and PG16.
8. Write V20 only after BE-1C-3 is merged. Run the duplicate-seed test. Re-run.
9. Write V21 (confirmed indexes). Re-run.
10. Write `GlobalExceptionHandler` mapping and `DataIntegrityExceptionMappingTest`. Run.
11. Write V22 only after DEC-06a. Write `AuditImmutabilityPostgresTest` with separate roles. Run on PG16.
12. Write `docs/architecture/db-roles.md` (role model, bootstrap SQL outline, what the REVOKE does and does not stop). Infra changes are a separate ask.
13. Write V23 after code has been checked for references. Run fresh and upgrade again.
14. Run the gate: `git status --short`, `git diff --check`, `git diff --stat`, full `./mvnw -B clean test` (H2), PG16 tagged suite, `flyway validate` on both paths, secret scan of the diff. Read-only git commands only.
15. Update the register and matrix with executable evidence only. Write `novawallet-api/docs/phase-1c/PHASE_1C_BATCH_4_REPORT.md`. Stop.

## 10. PostgreSQL 16 verification steps

All PG16 runs use a disposable container. The user's own instance on 5432 is never used (BLK-05). Passwords come from environment variables and are never written into the repo.

```bash
# 0. Disposable PostgreSQL 16 (password from env, never committed)
docker run -d --rm --name nw-pg16-b4 -e POSTGRES_PASSWORD="$NW_PG_PW" \
  -e POSTGRES_USER=nw_owner -e POSTGRES_DB=nw_test -p 55432:5432 postgres:16-alpine
docker exec nw-pg16-b4 psql -U nw_owner -d nw_test -tAc 'SHOW server_version;'   # expect 16.x
```

1. **Precheck.** `psql -v ON_ERROR_STOP=1 -f scripts/db/precheck_v18.sql` on the clean seed (all zeros) and on the dirty seed (named non-zero rows). Record both outputs.
2. **Fresh V1 to head.** Run `MigrationFreshPostgresTest` against an empty database through the `pgtest` profile. Assert `flyway.info()` shows every version success, V12 included, and `validate()` passes.
3. **Upgrade V16 (or BE-1C-3 head) to head.** Create the schema at the earlier target, load `batch4_seed.sql`, record row counts for `users`, `wallets`, `transactions`, `audit_logs`, `idempotency_keys`, then migrate to head. Assert counts unchanged, NULL flags backfilled, and `validate()` passes.
4. **Dirty upgrade.** The same path with the dirty seed must fail with a clear message. Assert no V18 row, no partial constraint, and unchanged counts.
5. **Constraint probes.** For each new CHECK, UNIQUE, FK, and NOT NULL, run one violating insert and assert SQLSTATE: 23514 (check), 23505 (unique), 23503 (FK), 23502 (not null). Run every enum constant and assert success.
6. **EXPLAIN evidence.** On the seeded database with realistic row counts (the test seed is small, so note this as a limit):
   ```sql
   EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM transactions
    WHERE type = 'WITHDRAWAL' AND provider_reference IS NOT NULL
      AND status IN ('PENDING','PENDING_RECONCILIATION')
      AND (next_status_check_at IS NULL OR next_status_check_at <= now())
    ORDER BY next_status_check_at ASC LIMIT 50;
   EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM audit_logs
    WHERE action = 'PAYOUT_INITIATED' AND created_at BETWEEN now() - interval '30 days' AND now();
   ```
   Keep the conditional V21 indexes only if the plan changes to use them.
7. **Index presence.** `SELECT indexname FROM pg_indexes WHERE tablename IN ('kyc_documents','transactions','idempotency_keys','users','audit_logs');`. Assert the FK indexes from §5.4.
8. **Constraint inventory.** `SELECT conrelid::regclass, conname, pg_get_constraintdef(oid) FROM pg_constraint WHERE connamespace = 'public'::regnamespace ORDER BY 1, 2;`. Save as evidence.
9. **Index usage for conditional drops.** `SELECT relname, indexrelname, idx_scan FROM pg_stat_user_indexes WHERE indexrelname IN ('idx_transactions_payout_channel','idx_transactions_external_status');`. Drop only on a staging window with zero scans. Note the stats reset on restart.
10. **Audit immutability (after DEC-06a).**
    ```sql
    CREATE ROLE nw_app LOGIN PASSWORD :'app_pw';
    GRANT SELECT, INSERT ON audit_logs TO nw_app;
    -- as nw_app:
    SET ROLE nw_app;
    UPDATE audit_logs SET action = 'X';   -- expect 42501
    DELETE FROM audit_logs;               -- expect 42501
    TRUNCATE audit_logs;                  -- expect 42501
    RESET ROLE;
    -- as owner (trigger still applies):
    UPDATE audit_logs SET action = 'X';   -- expect 42501 from trigger
    ```
    Also record that the superuser bootstrap role can still bypass REVOKE but not the trigger, which confirms DEC-06a is needed.
11. **Flyway history.** `SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;`. Save as evidence.
12. **Regression.** `./mvnw -B clean test` on H2 (count and exit code recorded). Targeted PG16 run: `./mvnw -B test -Dgroups=postgres -Dspring.profiles.active=pgtest` with `SPRING_DATASOURCE_URL` pointed at the container. Re-run the payout, webhook, idempotency, and transfer tests.
13. **Diff hygiene.** `git diff --check`, `git diff --stat`, `git diff --name-status` (read-only). Secret scan of the diff for password, secret, and token patterns.

**Evidence to record in the Batch 4 report:** command, exit code, test count, failures, errors, skipped, database version, log path, precheck output (clean and dirty), constraint inventory, index list, Flyway history, and the audit probe results.

## Known limits of this plan

- Nothing here has been run. The SQL is written from reading, not executed. H2 compatibility of `ALTER COLUMN … SET NOT NULL` and of `DROP INDEX IF EXISTS` is expected but unverified; the first H2 run will show it.
- No Flyway version was verified from `pom.xml` beyond the artifact names. The Java migration API for reading placeholders must be checked against the pinned version.
- Java 17 runtime is not available locally (BLK-04). Source compatibility only.
- The PostgreSQL 16 container and the `pgtest` profile do not exist yet (F23).
