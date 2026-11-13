# BE-1C-6 — Phase 1C Batch 6: KYC, PII & document security 🔒

**Area:** novawallet-api · **Needs:** BE-1C-4 · **Decisions:** DEC-08 (retention — legal)

---
Follow `_COMMON_BATCH_RULES.md` for Batch 6.

Scope (plan §7 Batch 6): SEC-12 (upload half), KYC audit trail, document ownership.

Do:
1. **Upload hardening:** verify content by magic bytes (not only extension/MIME header), allow-list jpeg/png/webp/pdf, 10 MB cap enforced at servlet + service level, reject polyglots/zero-byte files, strip image metadata (EXIF/GPS) before storing, random server-side file names (no user input in paths), path traversal tests, storage directory outside the web root with restrictive permissions.
2. **Ownership & access:** a user can only see their own KYC status/documents; documents are never served by a public URL; admin download endpoint requires the admin role, logs who/when/why (audit), sets `Content-Disposition`, `X-Content-Type-Options: nosniff`, no caching.
3. **Audit trail:** every KYC state change (upload, submit, approve, reject, tier change) writes an audit record with actor, previous → new state, reason; rejection reason stored (needed later by `GET /kyc/status`).
4. **PII minimisation:** mask ID numbers/phone in logs and API responses where the full value is not needed; confirm notification bodies carry no document data.
5. **Storage abstraction:** `FileStorageService` behind an interface so object storage (S3/MinIO) can replace local disk later; do not add the object store now.
6. DEC-08: do **not** implement deletion/retention. Record the question; add a `retention_review_at` design note only.
7. Optional encryption at rest for stored files: write the design + key-management implications; implement only if the owner approves in `docs/DECISIONS.md`.

Tests: malicious upload matrix, IDOR on documents, audit records asserted, EXIF removal, traversal attempts.
