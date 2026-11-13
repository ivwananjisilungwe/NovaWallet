# BE-1C-4 — Phase 1C Batch 4: Database integrity & migration hardening 🔒

**Area:** novawallet-api · **Needs:** BE-1C-3 · **Decisions:** DEC-05, DEC-06

---
Follow `_COMMON_BATCH_RULES.md` for Batch 4.

Scope (plan §7 Batch 4): NEW-DB-01..04, DB-29 (design per DEC-05), duplicate-wallet constraint. Planned migration names are in plan §9.2; rejected migrations in §9.3 must not be added.

Do:
1. Pre-migration data check queries (duplicates, nulls, orphans) written as a script in `scripts/db/precheck_v17.sql` (or the next version); the migration must fail safely with a clear message if data violates the new constraint.
2. Migrations (next numbers after V16): `UNIQUE(user_id)` on wallets (depends on Batch 3's idempotent creation), NOT NULL / CHECK constraints (e.g. balance ≥ 0, amount > 0, valid status values), foreign keys and indexes listed in the plan, DB-29 per DEC-05 (idempotency-scoped, **no** naive composite unique on amount/reference).
3. DEC-06 audit immutability: implement the chosen mechanism (e.g. revoke UPDATE/DELETE on `audit_logs` from the app role) with a separate migration-owner vs app-role setup documented in `docs/architecture/db-roles.md`. If Flyway runs as the app role today, document the change needed and STOP for owner decision if it affects deployment.
4. Verification matrix (plan §9.4): fresh V1→head, upgrade V16→head, `flyway validate`, and a test that a violating write is rejected by the DB.
5. Map each DB error to a controlled API response (409/422), never a 500 with SQL text.

Tests: constraint violation tests on PostgreSQL 16, migration tests, mapping tests.
