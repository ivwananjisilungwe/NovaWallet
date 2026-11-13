# OPS-04 — Backups, point-in-time recovery, restore drills 🔒

**Area:** infra · **Needs:** all app/admin tracks done (or run earlier before any pilot data exists) · **Decisions:** DEC-19 (off-site location)

---
Read `infra/AGENTS.md`.

Do:
1. Targets written in `infra/backup/POLICY.md`: RPO ≤ 15 minutes, RTO ≤ 2 hours (owner may adjust); retention (daily 14, weekly 8, monthly 12) subject to DEC-08.
2. PostgreSQL: continuous WAL archiving + periodic base backups (pgBackRest or WAL-G — pick one, justify), encrypted client-side, shipped off-site to object storage in a different provider/region; backup user with least privilege.
3. Uploaded KYC files volume: encrypted incremental backups (restic) to the same off-site store.
4. Scripts: `backup-now.sh`, `restore-pitr.sh <timestamp>` (restores into a **separate** instance, never over production), `verify-backup.sh` (restores the latest backup into a scratch container, runs Flyway validate, runs the ledger trial-balance + daily proof queries, reports).
5. Scheduled weekly automatic restore verification with alert on failure; monthly manual drill checklist with a log file of past drills.
6. Encryption keys: where they live, who holds them, recovery procedure if the server is lost (owner keeps an offline copy). No keys in the repo.
7. Staging refresh only from **anonymised** data: `anonymise.sql` (names, phones, IDs, emails, documents removed) — never copy production data raw.

Gates: full cycle proven locally with compose: write data → backup → destroy → PITR restore to a timestamp → proof queries pass. Output in the report.
