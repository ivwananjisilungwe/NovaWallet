# OPS-09 — Runbooks, on-call & incident response

**Area:** docs/infra · **Needs:** OPS-08

---
Write in `infra/runbooks/` (plain language, copy-paste commands, verified against the staging stack):
1. `INDEX.md` and a template (symptom → impact → first 5 minutes → diagnosis → fix → verify → follow-up).
2. Runbooks: API down; DB down/slow; provider down (use kill switch X10, customer comms); payments stuck in UNKNOWN/PENDING_RECONCILIATION (how to query, how to resolve through X09 with maker-checker); daily proof failed / ledger drift (stop-the-line procedure, who can authorise freezing money movement); webhook flood/forgery; suspected account takeover wave; secret leak (link OPS-06); bad deploy → rollback; migration failure; restore from backup; disk full; certificate expiry; SMS/OTP outage; push outage; AI vendor outage (disable flag).
3. **Incident process:** severity levels, roles (incident lead, communicator, scribe — one person can hold all in a pilot), status page/message templates (in-app notice via X12, SMS/WhatsApp), post-incident review template with owner actions.
4. **Regulatory/customer notification checklist** as headings only with `OWNER + COUNSEL TO COMPLETE` (breach notification, provider notification, regulator contact).
5. **Operational calendar:** daily (proof, queues), weekly (backup verification, dependency updates), monthly (restore drill, access review, secret rotation check), quarterly (pen-test follow-up, DR game day).
6. Support playbooks for X13: common cases (money debited but not received, wrong number sent, locked PIN, KYC rejected, lost phone) with exact steps and what staff may/may not do.
Gates: every command in the runbooks executed against the local/staging stack at least once; list any that could not be.
