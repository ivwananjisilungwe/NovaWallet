# OPS-06 — Secrets & key management 🔒

**Area:** infra, novawallet-api (config only) · **Needs:** OPS-05

---
Read `infra/AGENTS.md`.

Do:
1. **Inventory** `infra/SECRETS_INVENTORY.md`: every secret (JWT signing key, DB passwords, Lipila keys, webhook secrets, QR signing key, SMS/FCM/LLM keys, backup encryption keys, TOTP encryption key, deploy keys, Play signing) → owner, where stored, rotation period, blast radius, rotation procedure. No values.
2. **Storage:** choose and document one approach fit for a small team: SOPS + age encrypted files in the repo, or Docker secrets + server-side env files (600, root-owned), or a hosted secret manager. Implement templates and scripts; compose reads secrets as files, not plain env where possible.
3. **Rotation without downtime:** JWT signing supports key ids with overlapping validity (verify old + new, sign with new); webhook secrets support two active values; DB password rotation steps; document each as a runbook and test JWT/webhook rotation with automated tests.
4. **Startup validation:** the API refuses to start in staging/prod with missing, default, short or example secrets (extend BE-1C-8).
5. **Least privilege DB roles:** migration owner vs application role (BE-1C-4) vs read-only reporting role vs backup role — SQL scripts + docs.
6. CI: gitleaks on every push; pre-commit hook config offered to the owner; GitHub secret scanning/push protection instructions.
7. **Exposure runbook:** what to do in the first hour if a secret leaks (rotate, revoke sessions, review logs, notify).

Gates: rotation tests green; secret scan clean; startup-validation tests.
