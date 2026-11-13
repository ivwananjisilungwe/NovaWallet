# Phase 1C — Batch 6 Plan: KYC, PII and Document Security

Date: 2026-11-14 · Prompt: BE-1C-6 · Mode: **PLAN ONLY** (no source, test, migration, or git changes)
Decision dependencies: **DEC-08 is BLOCKED on legal counsel** (retention). New decisions DEC-35 to DEC-40 are proposed below; they are **not yet recorded** in `docs/DECISIONS.md`, and the IDs must be checked against the 1C log before recording.

## 1. What was read and the facts found

Read: root `AGENTS.md`, `novawallet-api/AGENTS.md`, `docs/DECISIONS.md`, `prompts/templates/T5_plan_first.md`, `prompts/stage-01_backend-security/_COMMON_BATCH_RULES.md`, `BE-1C-6_kyc_pii_documents.md`, `PHASE_1C_BATCH_2_PLAN.md` (style), `PHASE_1C_IMPLEMENTATION_PLAN.md` (Batch 6 at lines 373–401), `PHASE_1C_SECURITY_BASELINE.md` (§5.5 KYC rows), `backend-risk-register.md` (SEC-12 at line 426, SEC-13 at line 667), `docs/api/CONTRACT_TARGET.md` (row 35, 36, A10 at line 130, C7 at line 155), and all KYC, storage, redactor, logging, admin, and migration code cited below.

### Verified facts (file:line)

| # | Topic | Fact | Evidence |
|---|---|---|---|
| F1 | MIME | Type check uses the **client-supplied** `Content-Type` only. No signature or byte inspection. | `kyc/service/KycService.java:33-35` (allowlist), `:181-186` (`file.getContentType()`), `:80` (stored as `mimeType`) |
| F2 | MIME | Size cap is enforced in two places (servlet and service). | `application.yml:19-24` (10MB file / 11MB request), `KycService.java:32`, `:177-178`; `GlobalExceptionHandler.java:149-155` maps oversize to 413 |
| F3 | MIME | Zero-byte and empty files are rejected. | `KycService.java:173-175` |
| F4 | MIME | Stored bytes are written unchanged. No EXIF/XMP/IPTC stripping and no re-encode. No Tika, imaging, or metadata library on the classpath. | `FileStorageService.java:47` (`Files.copy` of raw stream); `novawallet-api/pom.xml` (no such dependency) |
| F5 | Path | On-disk name is `<docId>_<sanitised original name>` under `<uploadDir>/<userId>/`. The user ID is a UUID, so the directory is safe. The sanitiser keeps `.`, but the UUID prefix prevents traversal. | `FileStorageService.java:40-43`, `:76-79` |
| F6 | Path | The **absolute** path is persisted in the DB (`file_path VARCHAR(500)`). The original name is persisted unsanitised. | `FileStorageService.java:49` (`targetPath.toString()`), `KycService.java:78` and `:87-88`; `V5__add_kyc_tables.sql:15-16` |
| F7 | Path | `loadFile` guards with `Paths.get(filePath).normalize().startsWith(uploadDir)`. It does not resolve symlinks (`toRealPath`) and trusts the DB path. The guard exists but is string-prefix on a normalised path. | `FileStorageService.java:55-60`; Security baseline line 125 ("Forge") |
| F8 | Path | `storeFile` has **no** containment check (the master plan asks for one, item 2). Directories are created with default umask, not 0700. | `FileStorageService.java:29-37`, `:43-47` |
| F9 | Path | Upload is written **inside** the class-level `@Transactional` before commit. A later rollback leaves an orphan file. | `KycService.java:28`, `:84-89` |
| F10 | Encryption | Documents are plaintext on disk. No encryption code or key config exists for KYC. The only crypto is HMAC-SHA256 in webhook verification and JWT signing. | `FileStorageService.java:47`; `payment/service/WebhookService.java:496-497`; `security/JwtUtil.java:15` |
| F11 | Encryption | Prod volume is a named Docker volume mounted at `/app/uploads`, owned by the `novawallet` user. No encryption is declared. | `novawallet-api/Dockerfile:47-48`; `novawallet-api/docker-compose.yml:75`, `:118-119`; `application-prod.yml:118` (`upload-dir: /app/uploads/kyc`) |
| F12 | Access | Admin download streams `byte[]` with `application/octet-stream`. It sets **no** `Content-Disposition` and **no** explicit `Cache-Control`. It writes **no** audit record. | `admin/controller/AdminKycController.java:94-103` |
| F13 | Access | Admin routes are gated twice: class-level `@PreAuthorize("hasRole('ADMIN')")` and URL rule `/api/v1/admin/**`. | `AdminKycController.java:35-36`; `config/SecurityConfig.java:124` |
| F14 | Access | Ownership check: a missing document returns 404, and a document owned by another user returns 403. The 403 response leaks that the document ID exists. | `kyc/service/AdminKycService.java:93-98` |
| F15 | Access | Customer routes return only document **metadata** (no path, no bytes). There is no customer download endpoint. | `kyc/controller/KycController.java:57-70`, `:85-92`; `kyc/dto/KycDocumentResponse.java:13-36` (no `filePath`) |
| F16 | Access | `nosniff` is enabled globally. `cacheControl` is not overridden in `SecurityConfig` (grep for `cache` returns nothing), so Spring Security defaults apply. | `config/SecurityConfig.java:60`; grep result (no override) |
| F17 | Audit | **No KYC state change writes an audit record.** `AuditService` is not referenced from the `kyc` package or `AdminKycController`. Its write is `@Async` + `REQUIRES_NEW`, so it is not atomic with the state change. | `KycService.java:84-89` (upload), `:161-163` (submit); `AdminKycService.java:120-135` (approve), `:166-180` (reject); `KycExpiryJob.java:46`; `audit/service/AuditService.java:34-35`; Security baseline `KYC-AUDIT` (line 218) |
| F18 | Audit | Reject stores the reason on the user and on each document, but also **sets `kycApprovedBy` to the admin on rejection**, which is a mislabel. | `AdminKycService.java:168-169` |
| F19 | PII (logs) | `AdminKycService` logs the free-text rejection reason at INFO. | `AdminKycService.java:182` |
| F20 | PII (logs) | `FileStorageService` logs the full absolute server path at INFO. | `FileStorageService.java:48` |
| F21 | PII (logs) | Other PII logs exist **outside** the `kyc` package: full email (`auth/service/AuthService.java:99`, `:118`, `:124`, `:217`, `:237`, `:259`), full phone (`notification/service/SmsService.java:57`, `:68`, `:71`), admin email (`bootstrap/AdminDataInitializer.java:86`). | Grep of `src/main/java` |
| F22 | PII (logs) | `SensitiveDataRedactor` masks URL token parameters, labelled secrets, and 32+ char token-like runs. It has **no** pattern for email, phone, or national-ID numbers. Its callers are `MailService.java:112`, `:127` and `NotificationResponse.java:31-32`. It is not used for logs in the KYC or auth paths. | `security/SensitiveDataRedactor.java:67-70`, `:88-89`, `:138-140` |
| F23 | PII (logs) | `logback-spring.xml:86` has a comment "Mask sensitive data patterns in logs", but the prod config defines **no** masking filter or converter. | `novawallet-api/src/main/resources/logback-spring.xml:86-88` |
| F24 | PII (responses) | KYC responses expose no phone, no email, no ID number, and no path. Admin list `GET /admin/kyc/pending` returns full email and phone through `UserSummaryResponse`. | `KycDocumentResponse.java:13-36`; `admin/dto/UserSummaryResponse.java` (email, phone); `AdminKycController.java:58-63` |
| F25 | PII (content) | The ID number is **inside the document image**, not in any DB column. Metadata stripping and access control are therefore the main controls. No ID-number column exists. | `V5__add_kyc_tables.sql:11-28` |
| F26 | Retention | No deletion path exists. `FileStorageService.deleteFile` has no callers. `KycExpiryJob` (cron `0 0 0 * * ?` at line 31) changes status only: after one year it sets user to `NOT_SUBMITTED`, tier 0, and documents to `REJECTED` (lines 34, 46, 50). Files and rows are kept. | `FileStorageService.java:67-74`; `KycExpiryJob.java:31-50`; Security baseline `KYC-RET` (line 221) |
| F27 | Retention | Only `V5` touches KYC tables. No retention, encryption, or audit column exists. | `V5__add_kyc_tables.sql`; grep for `kyc` across `db/migration` |
| F28 | Contract | Row 35 (upload) and row 36 (submit) are "Conflict C7" (document type enum). Admin row A10 expects **"signed, expiring URLs"** for `/admin/kyc/{userId}/documents/{documentId}`, but the code **streams bytes** (F12). | `docs/api/CONTRACT_TARGET.md:50-51`, `:130`, `:155` |
| F29 | Migrations | Head is V16 in `db/migration`. A Java migration class `src/main/java/db/migration/V12__CreateTransactionReferenceSequence.java` exists, which conflicts with AGENTS.md ("V12 does not exist"). The next free version is **V17** in either case. | `novawallet-api/AGENTS.md:7`; file list |

## 2. Files to change (T5 item 2)

Matches the master plan's "Files likely to change" table (`PHASE_1C_IMPLEMENTATION_PLAN.md:388-400`), with deviations noted.

| File | Change | Topic |
|---|---|---|
| `kyc/service/KycService.java` | Detect type from magic bytes; reject mismatch with client MIME and extension; store detected MIME; add KYC audit calls | MIME, audit |
| `kyc/service/FileStorageService.java` | Store under a server-generated key; containment check on write and read (`toRealPath`); reject symlinks; 0700/0600 perms where POSIX; log doc ID only; keep legacy absolute-path reads working | Path, PII (logs) |
| `kyc/service/AdminKycService.java` | Audit approve/reject; stop logging rejection reason (log reason code and length); fix `kycApprovedBy` on reject (F18) | Audit, PII |
| `kyc/service/FileStorageService.java` or new `kyc/service/ImageSanitiser.java` | Strip JPEG APP1/APP13 and PNG `eXIf`/`tEXt`/`iTXt`/`zTXt`; WebP metadata chunks if DEC-37 allows | MIME, PII |
| `admin/controller/AdminKycController.java` | Download headers (`Content-Disposition: attachment`, server-generated filename, explicit `Cache-Control: no-store`, `nosniff`); audit each download; optional reason parameter (DEC-35) | Access |
| `kyc/dto/KycDocumentResponse.java` | No change needed (F15). Add a test that it never serialises a path | Access, PII |
| `admin/dto/UserSummaryResponse.java` | Only if DEC-39 approves masking (admin package, see scope note) | PII |
| `audit/service/AuditService.java` | Add a synchronous same-transaction write method if DEC-38 approves | Audit |
| New `security/PiiMasker.java` | Mask email, phone, and ID-like values for logs and admin lists; unit tests | PII |
| `db/migration/V17__kyc_document_hardening.sql` | Additive columns (§3) | MIME, encryption |
| `resources/logback-spring.xml` | Only if a masking converter is approved; otherwise correct the misleading comment at line 86 | PII |

**Not changed in Batch 6:** `RequestTracingFilter.java:33` (SEC-12 request-ID half, owned by BE-1C-8, `PHASE_1C_IMPLEMENTATION_PLAN.md:438-445`); `KycExpiryJob.java` (no retention change, DEC-08); `deleteFile` (no callers, must stay unused until DEC-08). Packages `auth/`, `notification/` and `bootstrap/` are out of scope under the prompt's folder rule (see DEC-40).

## 3. Data and migration (T5 item 3)

**Proposed V17 (additive, expand only). Migration name `V17__kyc_document_hardening.sql`:**

```
ALTER TABLE kyc_documents ADD COLUMN IF NOT EXISTS detected_mime_type VARCHAR(50);
ALTER TABLE kyc_documents ADD COLUMN IF NOT EXISTS storage_key VARCHAR(120);
ALTER TABLE kyc_documents ADD COLUMN IF NOT EXISTS content_sha256 CHAR(64);
-- encryption_key_id VARCHAR(64) only if DEC-36 = application-level encryption
```

- `detected_mime_type`: server-detected type, not client input (fixes F1).
- `storage_key`: relative key `<userId>/<docId>.<ext>`. Nullable. Legacy rows keep `file_path`.
- `content_sha256`: integrity check and duplicate detection. Optional, owner to confirm.
- **Not added:** `retention_review_at`. DEC-08 is BLOCKED, so the column is a design note only (§5.6).

**Expand → migrate → contract:**
1. V17 adds nullable columns. Old code keeps working.
2. Code writes `storage_key` and `detected_mime_type` for new uploads. `loadFile` reads `storage_key` when present, else `file_path` (F7 guard kept).
3. Backfill of legacy rows is **not** planned in Batch 6. Their `file_path` is absolute and is still served through the containment check. Contract (drop `file_path`) is a later batch.

**Verification (AGENTS rule 4):** fresh V1→V17 and upgrade V16→V17 on PostgreSQL 16 (pgtest profile), `flyway validate`, and the V12 Java-migration question (F29) recorded in the report.

**Audit storage:** no new table. KYC events go into the existing `audit_logs` (V8). `entity_type` `KYC_DOCUMENT` / `KYC_USER` fits `VARCHAR(50)`. `old_value`/`new_value` are `TEXT`, so store state names and reason codes only, never document content.

## 4. API changes (T5 item 4): document access

**Chosen direction (recommended):** keep **admin streaming** through the authenticated endpoint. Do not introduce public or bearer-style URLs in Batch 6.

| Endpoint | Batch 6 change | Contract impact |
|---|---|---|
| `GET /api/v1/admin/kyc/{userId}/documents/{documentId}` | Headers: `Content-Disposition: attachment; filename="<docId>.<ext>"` (server-generated, no user input); `Cache-Control: no-store`; `Content-Type` from `detected_mime_type`; `nosniff` asserted by test. Writes an audit row with actor, user ID, doc ID, and time. Optional `reason` (DEC-35). | Row A10 says "signed, expiring URLs". **Contract update needed** (DEC-35). |
| `GET /api/v1/admin/kyc/{userId}/documents/{documentId}` (not-found vs other-user) | Return 404 for both cases, so existence is not leaked (F14). | Same status set as Batch 2 note on 403 vs 404; treat as a contract decision, not a silent change. |
| `GET /api/v1/kyc/status`, `POST /api/v1/kyc/documents/upload`, `POST /api/v1/kyc/submit` | No new fields. Upload errors still 400 (and 413 for oversize). Response unchanged; test asserts no path or storage key. | None. |

**Why streaming and not signed URLs now:**
- A signed URL is a bearer credential. Anyone holding it can fetch the ID image until it expires, and the only control is the TTL.
- Signing needs a new secret (an HMAC key, following the `WebhookService` pattern at line 496). That is a new secret to manage, and it is not a dependency Batch 6 can justify alone.
- Streaming keeps access checked on every request, and it is covered by existing `/api/v1/admin/**` gating (F13).
- If the owner keeps "signed, expiring URLs" in the contract, that is DEC-35 option B. It would be a later batch with its own key-management decision.

## 5. Risks and tests (T5 item 5), by topic

### 5.1 MIME and magic-byte checks
- **Design:** detect type from the first bytes (format signatures): JPEG starts `FF D8 FF`; PNG starts `89 50 4E 47 0D 0A 1A 0A`; WebP has `RIFF` at 0 and `WEBP` at 8; PDF starts `%PDF-`. Require the **trailer** too: JPEG ends `FF D9`, PNG contains an `IEND` chunk, PDF contains `%%EOF`. Reject where the detected type does not match the client `Content-Type` or the filename extension. Reject zero-byte files (already done).
- **Risk R1 (polyglot):** a file can carry two valid signatures. Magic bytes alone do not stop this. **Mitigation:** serve only as `attachment` with `application/octet-stream` (already true, F12), plus the trailer check. Polyglot detection is heuristic and is **not** claimed as complete.
- **Risk R2 (decode):** full decode via `javax.imageio` covers JPEG and PNG. The JDK has **no** WebP decoder, so WebP is validated by signature only (DEC-37).
- **Tests:** malicious matrix: extension `.jpg` with PDF bytes; `.pdf` with PNG bytes; `.exe` renamed `.pdf`; truncated JPEG (no `FF D9`); zero-byte; 10 MB + 1 byte (413); 11 MB multipart request (413); valid JPEG/PNG/WebP/PDF accepted; detected MIME stored, not client MIME.

### 5.2 Storage path safety
- **Design:** server-generated key `<userId>/<docId>.<ext>`, with `<ext>` taken from the detected type. The original name is kept in DB only (`file_name`) and never used in a path. Resolve as `uploadDir.resolve(key).normalize()`; assert it starts with `uploadDir`. On read, use `toRealPath()` and reject symlinks. Create directories 0700 and files 0600 where POSIX is supported. Write to a temp file and `ATOMIC_MOVE` to the final name.
- **Risk R3 (orphan on rollback, F9):** the file is written before commit. **Mitigation:** write after commit (`TransactionSynchronization.afterCommit`) or delete on rollback. Decision DEC-38 covers ordering with audit; the mechanism is a design choice for the owner to confirm.
- **Risk R4 (legacy absolute paths):** rows written before V17 keep absolute `file_path`. They must still read after the change.
- **Tests:** traversal in `originalName` (`../../etc/passwd`, `..\\x`, NUL byte) → stored key contains none of these; `loadFile` on a DB path outside `uploadDir` → denied; symlink inside `uploadDir` pointing outside → denied; file perms on POSIX (0600); legacy absolute path still readable; `storeFile` cannot write outside `uploadDir` for a crafted `documentId`.

### 5.3 Encryption at rest (design only, DEC-36)
- **Current state:** plaintext files on a named Docker volume (F10, F11). No KYC-specific key exists.
- **Options:**
  - **A. Host or volume encryption** (disk-level, managed by infra and hosting, DEC-19). No code change. Protects against disk theft; does not protect against an attacker who has app access.
  - **B. Application envelope encryption:** per-file data key (AES-256-GCM) wrapped by a key-encryption key from env or KMS. Gives per-file control. Costs: key lifecycle, rotation, and backup of keys separately from data. Losing the KEK means losing every document.
  - **C. Both**, staged.
- **Recommendation:** **A now** (documented in the infra plan), **B designed** and approved by the owner before implementation. Do not reuse `JWT_SECRET` as a KEK. No key goes into code, tests, or `.env.example` values (AGENTS rule 5).
- **Implementation:** none in Batch 6 unless DEC-36 = B. If B, add `encryption_key_id` (V17) and a crypto round-trip test that fails when the key is missing.
- **Counsel question (not answered here):** whether any data-residency or encryption requirement applies is a legal question. It is not asserted in this plan.

### 5.4 Document access (admin streaming vs signed URLs)
- See §4. **Tests:** customer `GET` on another user's document → 403/404 (and the same response for a non-existent doc); unauthenticated → 401; customer on any `/api/v1/admin/kyc/**` → 403 (URL rule `SecurityConfig.java:124`); admin download → headers asserted (`Content-Disposition`, `Cache-Control: no-store`, `X-Content-Type-Options: nosniff`, `Content-Type` = detected type); each download writes one audit row (assert actor and doc ID; no bytes in audit); response does not contain the absolute path; 404 for an unknown doc and for a doc owned by another user return the same status.
- **Risk R5 (download audit is async, F17):** `AuditService` is `@Async REQUIRES_NEW`. Under DEC-38 option sync, the audit row is written in the same transaction. Test: force an audit failure and assert the download does not succeed silently.

### 5.5 PII masking in logs and responses
- **Design:** new `security/PiiMasker` (email: first character plus domain; phone: keep country code and last 3 digits, mask the rest; ID-like: last 2 characters). Use it in KYC-scoped logs. **Do not** widen `SensitiveDataRedactor`, which is for tokens (F22).
- **KYC-scope log changes:**
  - `AdminKycService.java:182`: log user ID and a reason **code** (or length), not the free text (F19).
  - `FileStorageService.java:48`: log document ID only, not the absolute path (F20).
- **Out-of-scope PII logs (F21):** `AuthService` (lines 99, 118, 124, 217, 237, 259), `SmsService` (57, 68, 71), `AdminDataInitializer:86`. These need a separate prompt or owner approval to widen scope (DEC-40).
- **Logback:** the comment at `logback-spring.xml:86` claims masking that does not exist (F23). Either add a masking converter (only if DEC-40 covers it) or correct the comment. Do not claim log masking is in place.
- **Responses:** KYC responses already carry no PII (F24). Admin pending list returns full email and phone (`UserSummaryResponse`). Masking it is a **contract-visible change** for the admin UI (DEC-39).
- **Notification bodies:** `MailService.java:78-86` (KYC approved) includes first name and tier only, no document data. Test asserts no document field appears in any KYC notification body.
- **Risk R6 (ID number in image, F25):** cannot be masked in the image; only access control and metadata stripping apply.
- **Tests:** `PiiMaskerTest` (email, phone, short values, null); log capture test asserting no full email, phone, reason text, or absolute path in KYC flow logs; response test asserting `KycDocumentResponse` and `KycStatusResponse` JSON has no `filePath`, `storage_key`, phone, or email.

### 5.6 Retention (DEC-08 — BLOCKED)

**BLOCKED: DEC-08.** Retention periods and deletion rules are a legal question that requires Zambian counsel or a regulated partner (`docs/DECISIONS.md:18`, `:62`). This plan does **not** assert any legal fact, period, or obligation.

- **Current state (facts):** nothing deletes files or rows (F26). `KycExpiryJob` changes status only.
- **Proposed in this batch:** **no deletion, no retention job, no column.** Design note only: a future nullable `retention_review_at` (date) on `kyc_documents`, to be added only after DEC-08 is answered.
- **Questions for counsel (no answer assumed):**
  1. What retention period applies to identity documents, selfies, and proof-of-address documents, separately?
  2. Must documents of **rejected** applicants be kept, and for how long?
  3. Does any legal hold or regulator access requirement apply to these records?
  4. Should `KycExpiryJob` delete files, or only change status, once a period is set?
  5. Does the answer change the audit-log retention (DEC-06, append-only)?
- **Tests:** none for retention in Batch 6. A test asserts that no code path calls `deleteFile` (guards against an accidental deletion before DEC-08).

## 6. Decisions needed (T5 item 6)

| ID | Question | Options | Recommendation |
|---|---|---|---|
| **DEC-08** | KYC/PII retention period | — (legal, BLOCKED) | Ask counsel using the five questions in §5.6. No deletion until answered. **BLOCKED: DEC-08** |
| **DEC-35** | Document access model | A: admin streaming with headers, audit, and optional reason · B: signed expiring URLs (new HMAC key, bearer risk, contract A10 as written) | **A now.** Owner also decides whether `reason` is required (API change) and whether contract A10 is rewritten |
| **DEC-36** | Encryption at rest | A: host or volume encryption (DEC-19) · B: application envelope encryption with KEK in env or KMS · C: both, staged | **A now, B designed** for owner approval before any implementation. No key in code or `JWT_SECRET` reuse |
| **DEC-37** | Metadata stripping and WebP/PDF validation | A: JDK `ImageIO` re-encode for JPEG/PNG (WebP signature-only) · B: add a third-party metadata or imaging library (new dependency) | **A.** Owner acknowledges that WebP is not decoded and PDF metadata is not stripped |
| **DEC-38** | Audit write ordering for KYC events | A: synchronous same-transaction write (new sync method in `AuditService`) · B: existing async `recordAction` | **A.** It also requires owner approval to touch the `audit/` package |
| **DEC-39** | Admin pending list PII | A: mask email and phone in `UserSummaryResponse` list · B: keep full values (intended admin function) | **A**, with full values on the detail view only. This is a contract-visible change to the admin UI |
| **DEC-40** | Scope of PII log fixes | A: separate follow-up prompt for `auth/`, `notification/`, `bootstrap/` · B: include in Batch 6 | **A.** Keeps Batch 6 within the KYC folder rule |
| DEC-33 (existing, OPEN) | KYC document types (NRC_FRONT/NRC_BACK) | see C7 | Needed before the MIME/type test matrix is final. Not a blocker for the mechanism |
| DEC-31 (existing, OPEN) | Liveness and face-match vendor (affects SELFIE handling) | see C8 | Not a blocker. Selfie biometrics stay out of Batch 6 |

**Also to confirm (owner, not a DEC):** the V12 conflict (F29) and whether the `kyc-uploads` volume is backed up (this affects whether DEC-36 option B is safe to adopt).

## 7. Steps in order (T5 item 7)

Each step is small and verifiable. Do not start until the owner approves this plan and answers DEC-35 to DEC-40.

1. **Owner decisions:** answer DEC-35 to DEC-40. Record them in `docs/DECISIONS.md` (only with owner consent). DEC-08 stays BLOCKED.
2. **Red tests first:** malicious upload matrix (§5.1), traversal and symlink tests (§5.2), IDOR and headers (§5.4), PII log and response tests (§5.5). Expect failures against current code; quote them in the report.
3. **MIME/magic-byte detection** in `KycService` (F1). Store `detected_mime_type`. Green on §5.1 tests.
4. **Storage containment and perms** in `FileStorageService` (F5–F9). Keep legacy absolute-path reads (R4).
5. **V17 migration** (§3). Verify fresh V1→V17 and upgrade V16→V17 on PostgreSQL 16; `flyway validate`.
6. **Metadata stripping** for JPEG and PNG (DEC-37).
7. **Download hardening** in `AdminKycController` (headers, no-store, not-found parity). Contract row A10 updated per DEC-35.
8. **KYC audit events** for upload, submit, approve, reject, tier change, and download (DEC-38). Assert audit rows in tests.
9. **PII:** `PiiMasker`; fix `AdminKycService:182` and `FileStorageService:48`; `UserSummaryResponse` per DEC-39.
10. **Retention:** design note in this plan only. No code. Confirm no `deleteFile` caller (test).
11. **Gate:** `./mvnw -B clean test` (quote counts), PostgreSQL 16 migration check, `git diff --check`, secret scan of diff. Write `novawallet-api/docs/phase-1c/PHASE_1C_BATCH_6_REPORT.md`. Update the traceability matrix only with executable evidence.

## 8. Risks noticed, not fixed in Batch 6

- **F29:** `src/main/java/db/migration/V12__CreateTransactionReferenceSequence.java` exists, but AGENTS.md says V12 does not exist. Flag for owner.
- **SEC-12 request-ID half** (`RequestTracingFilter.java:33`): belongs to BE-1C-8.
- **F18:** `AdminKycService.java:169` sets `kycApprovedBy` on rejection (mislabel). Recommend fixing in the audit step.
- **F14:** 403 vs 404 leaks document existence (`AdminKycService.java:93-98`). Same family as the Batch 2 note.
- **F9:** file written before commit (`KycService.java:28`, `:87`): orphan risk on rollback.
- **F23:** misleading masking comment in `logback-spring.xml:86`.
- **F21:** PII in auth, SMS, and bootstrap logs (see DEC-40).
- **Security baseline KYC-AUDIT** is rated H (`PHASE_1C_SECURITY_BASELINE.md:218`). This plan is the fix path.
- `getDocumentFile` loads the whole file into memory (`AdminKycService.java:100`, `FileStorageService.java:61`). Bounded at 10 MB, acceptable; note only.

**Next:** the next prompt after BE-1C-6 per `prompts/01_MASTER_ORDER.md` (not read in this session; check it before starting). Do not start it from this plan.
