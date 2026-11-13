# NovaWallet — Phase 1C Batch 0 Report

> **Location:** version-controlled copy — `novawallet-api/docs/phase-1c/`. Do not edit findings in this copy; supersede with a later batch report.`

**Document:** PHASE_1C_BATCH_0_REPORT.md
**Phase:** 1C — Security and Database Hardening
**Batch:** 0 — Read-Only Security Baseline and Threat Model
**Branch:** `feature/phase-1c-security-database-hardening`
**HEAD:** `797571914b98d3b3206c5529e8eaf356ba79d1c8` (Phase 1B final commit)
**Date:** 2026-11-12
**Author:** Forge (primary engineering orchestrator)
**Decision required:** Human approval to proceed to Batch 1

---

## 1. Status

**BATCH 0 COMPLETE.**

Batch 0 was executed strictly read-only. No source file, test, migration, configuration file, or pre-existing report was modified. Nothing was committed, pushed, merged, or deployed.

**No Batch 1 work has begun. Work is stopped pending human approval.**

---

## 2. Entry Gate

| Check | Result | Evidence |
|---|---|---|
| Phase 1B entry gate | **PASSED** | Phase 1B committed as `7975719` — 43 files, +5,561/−173, after a clean secret scan |
| Branch created from committed Phase 1B HEAD | **PASSED** | `feature/phase-1c-security-database-hardening` at `7975719` |
| Working tree clean at branch creation | **PASSED** | 0 modified, 0 staged, 0 untracked |
| Commits on Phase 1C branch | **0** | Nothing committed, per constraint |

Gate failure text (`PHASE 1C BLOCKED — PHASE 1B COMMIT NOT VERIFIED`) was **not** emitted because the gate passed.

---

## 3. Deliverables

| # | Report | Lines | Purpose |
|---|---|---|---|
| 1 | `docs/phase-1c/PHASE_1C_SECURITY_BASELINE.md` | 302 | Architecture map, STRIDE model T1–T24, verified weaknesses, every correction applied to specialist claims |
| 2 | `docs/phase-1c/PHASE_1C_RISK_TRACEABILITY_MATRIX.md` | 336 | All 16 in-scope identifiers with verbatim register text, independent re-verification log, status summary, blockers |
| 3 | `docs/phase-1c/PHASE_1C_IMPLEMENTATION_PLAN.md` | 672 | 9 batches, exact per-batch file manifests, PostgreSQL 16 test strategy, 12 human decisions, 6 blockers |

This document is the fourth: the consolidated batch gate report.

---

## 4. Execution Method

Eight specialist audits were dispatched in parallel:

| Audit | Scope |
|---|---|
| A1 | Repository structure and conventions |
| A2 | Authentication (JWT, tokens, login, PIN, password reset) |
| A3 | Authorization and ownership (IDOR, roles) |
| A4 | Database schema and constraints |
| A5 | Risk register extraction and verbatim traceability |
| A6 | Webhooks, provider integration, operational hardening |
| A7 | KYC, PII, document handling |
| A8 | Financial correctness and concurrency |

**Every load-bearing claim was independently re-verified against source by Forge.** Subagent output was treated as leads, not findings. Six claims did not survive re-verification and are recorded as corrections below. This re-verification step is the primary value of Batch 0.

---

## 5. Corrections Applied to Specialist Claims

| # | Claim | Verdict | Evidence |
|---|---|---|---|
| C1 | "CRITICAL admin IDOR" (4 findings) | **False positive** | `@EnableMethodSecurity` active (`SecurityConfig.java:31`); class-level `@PreAuthorize("hasRole('ADMIN')")`. Admin acting across users is the endpoint's designed purpose |
| C2 | "Rate limiting configured but not enforced" | **False** | `RateLimitFilter` is a `@Component` extending `OncePerRequestFilter`, `@Profile("!test")`, returns 429 — auto-registered by Spring Boot |
| C3 | "`LipilaGateway` has no HTTP timeouts" | **False** | `CONNECT_TIMEOUT = 5s`, `READ_TIMEOUT = 10s` (`LipilaGateway.java:43-47,77-83`) |
| C4 | "`WalletService.freeze/unfreezeWallet` are unlocked" | **False** | Both call `walletRepository.findByIdWithLock(...)` — PESSIMISTIC_WRITE at `WalletService.java:74,89` |
| C5 | "`BalanceRecalculationJob` ACTIVE in dev/test" | **False** | `@ConditionalOnProperty(matchIfMissing = false)`; property set **only** in `application-prod.yml:145-146` as `false`. Bean never registers in any profile |
| C6 | "175 tests in the suite" | **False** | Verified run: **243 tests**. The subagent count is not reproducible and is not used |
| C7 | "KYC path traversal = CRITICAL RCE" | **Re-rated Medium** | `loadFile` guard present; filename sanitisation removes separators; UUID prefix; uploads never statically served or executed |
| C8 | "`V14` unique index is a defect" | **Partly false** | `V14` already creates `idx_transactions_idempotency_key` as UNIQUE. Multiple NULLs are **required** for non-payout rows — intended, not a defect |
| C9 | "Access token valid after logout = HIGH" | **Re-rated Medium** | Bounded 15-minute window is standard for stateless JWT. Still requires a decision under Batch 1 requirement 8 |
| C10 | "PII in admin user listing = HIGH" | **Re-rated Low** | Intended admin function; should be minimised but is not an access-control defect |

### 5.1 Correction to the risk register itself — SEC-01

The register describes SEC-01 as "valid JWT bypasses account-state checks (disabled/locked/soft-deleted)." **This does not survive contact with the code:**

| Register claim | Actual |
|---|---|
| Soft-deleted user retains access | **Already mitigated.** `@SQLRestriction("deleted = false")` on `User` (`User.java:26`) hides deleted rows from `findById`; `loadUserById` throws; the filter catches and sets no authentication |
| "Locked" account state unchecked | **No such column exists.** `User` has no `locked`, `disabled`, or `suspended` field |
| `accountNonLocked` maps to a lock flag | **False.** It maps to `!user.isDeleted()` — redundant with the above |
| `isEnabled()` maps to account enablement | **Partly true.** It maps to `emailVerified`, which is never checked |

**Net:** the only real gap is that email verification gates nothing, despite the entity Javadoc stating it is "required before financial operations." Naively adding a global `isEnabled()` check would lock **every unverified user out of the entire API** — a product-breaking change, not a security fix. Re-rated **High → Medium**. Resolution requires DEC-01.

---

## 6. New Defects Discovered in Batch 0

These are **not** register identifiers. They are new findings and must be registered in the owning batch.

| ID | Finding | Sev | Batch |
|---|---|---|---|
| **NEW-FIN-01** | **KYC approval TOCTOU creates duplicate wallets.** `AdminKycService.approveKyc` checks `PENDING`, saves the user, then creates a wallet with no uniqueness guard. `wallets.user_id` has **no unique constraint** (`V3`) while `WalletRepository` declares `Optional<Wallet> findByUserId`. Two concurrent approvals → two wallets → `IncorrectResultSizeDataAccessException` → **the user's funds become permanently unreachable** | **High** | 3 (+4) |
| **NEW-DB-01** | No `CHECK (balance >= 0)` on `wallets`. Non-negativity rests entirely on one conditional `UPDATE` in `WalletRepository` | High | 4 |
| **NEW-AUTH-01** | No `jti` claim → access-token replay undetectable; token valid up to 15 min after logout | Medium | 1 |
| **NEW-DB-02** | No `CHECK` on `wallets.status` or `transactions.status` enum values | Low | 4 |
| **NEW-DB-03** | `audit_logs` has no DB-level immutability | Medium | 4 |
| **NEW-DB-04** | Missing FK/query indexes for reconciliation and pending-payout scans | Low | 4 |
| **NEW-OPS-01** | Collections 503 containment guard has no regression test | Medium | 5 |

NEW-FIN-01 is absent from the register and is the most severe finding in this batch.

---

## 7. Confirmed Defects (identifier unconfirmed)

| ID | Defect | Sev | Evidence |
|---|---|---|---|
| R-01 | `AdminService.toggleUserDeletedStatus` mutates wallet status with **no lock and no status precondition** — `findByUserId` + `save` | High | `AdminService.java:84-88` (restore → ACTIVE), `:96-100` (deactivate → FROZEN) |
| R-02 | `BalanceRecalculationJob` overwrites historical `balance_before`/`balance_after` snapshots with the current balance | Medium | `BalanceRecalculationJob.java`; **contained in all profiles** — containment holds, job logic is unsafe |
| R-03 | `AccountNumberGenerator` TOCTOU — `existsByAccountNumber()` is advisory; INSERT happens later; collision surfaces as unhandled 500 | High | `AccountNumberGenerator.java:31-40` |
| FIN-09 | Fee percentage rounded to 2dp **before** flat-fee addition and **before** min/max clamping — double rounding | Medium | `FeeEngineService.java:51-53,56-68` |
| FIN-11 | Unverified users exempt from daily-send and wallet limits — early `return` for `kycStatus != APPROVED \|\| tier < 1` | Low | `TransactionLimitService.java:117-120` |

**Identifier integrity:** R-01 and R-03 do not appear in either register copy. The defects are confirmed; the identifiers are not. They will not be marked CLOSED on an inferred definition. See BLK-02.

---

## 8. Risk Coverage

All 16 in-scope identifiers accounted for:

| Category | Count | IDs |
|---|---|---|
| Security | 11 | SEC-01, SEC-02, SEC-03, SEC-04, SEC-05, SEC-06, SEC-08, SEC-09, SEC-10, SEC-11, SEC-12 |
| Financial | 2 | FIN-09, FIN-11 |
| Existing backend | 3 | R-01, R-02, R-03 |

| Status | Count | IDs |
|---|---|---|
| OPEN (confirmed in code) | 14 | SEC-01..SEC-04, SEC-06, SEC-08..SEC-12, FIN-09, FIN-11, R-01, R-03 |
| MITIGATED (verified, must not regress) | 1 | R-02 (containment) |
| MITIGATED (disbursement) / BLOCKED (collections) | 1 | SEC-05 |
| **CLOSED** | **0** | — |

`SEC-07` exists in the register (email-verification token has no expiry) and is **deliberately out of Phase 1C scope**. It must not be "fixed" incidentally.

**No risk is marked CLOSED. Phase 1C has not implemented a single correction.**

---

## 9. Verified Controls — Regression-Locked

The following work today and must not regress:

BCrypt password and PIN hashing · SHA-256-hashed refresh tokens with rotation · revocation on password reset and logout · uniform login errors · anti-enumeration on forgot-password · login and PIN lockout · **enforced** in-memory rate limiting (100 req/min global, 10 auth, 429) · stateless session · security headers · `@EnableMethodSecurity` with admin role gates · disbursement webhook HMAC + 300 s timestamp window + 120 s future bound + durable `(provider, event_id)` dedup + amount match + provider re-query outside transaction + quarantine · collections credit path contained by a 503 guard · KYC `loadFile` traversal guard · uploads not statically served · no committed secrets · non-root Docker · safe exception handler · fail-closed schedulers · Phase 1B three-stage payout split · compensating credit on definite failure · `PENDING_RECONCILIATION` replay guard · transfer pessimistic locking with conditional non-negative `UPDATE`.

---

## 10. Threat Model

STRIDE model T1–T24 documented in `PHASE_1C_SECURITY_BASELINE.md` §4. The four scenarios that drive batch sequencing:

| Priority | Scenario | Rationale |
|---|---|---|
| 1 | Concurrency or replay creates/destroys money (T9, T10, T22, T24, T25) | Irreversible financial harm; hardest to detect after the fact |
| 2 | Credential exposure via product surfaces (T15) and unrevoked sessions (T4) | Direct account takeover |
| 3 | Financial invariants rest on application code, not the DB (T9, T10, T24) | One bypass path silently corrupts balances |
| 4 | Collections credit without HMAC (T11) | Contained today; catastrophic if uncontained without the provider contract |

---

## 11. Test Infrastructure

| Item | Result |
|---|---|
| Baseline suite | **243 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS**, 1:28 |
| Command | `./mvnw -B clean test` |
| JDK | 21.0.12.1 Temurin (`JAVA_HOME` explicitly exported — system `java` is JRE-only) |
| Maven | 3.9.16 |
| Log | `/tmp/opencode/phase1c-baseline-test.log` |
| PostgreSQL | **16.15** verified in a disposable Docker container (Docker 29.9.3, `postgres:16` cached); probe container removed after use |
| Local 5432 instance | **Not used.** Not the project database; credentials unavailable |
| Testcontainers | **Not added** — no new dependency. A `pgtest` Spring profile reads `SPRING_DATASOURCE_*` from the environment |

**H2 is explicitly rejected as proof** of PostgreSQL locking, constraint, or isolation behaviour. Batches 3, 4, and 9 require PostgreSQL 16.

**Standing limitation:** no Java 17 runtime is installed on this machine. Java 17 compatibility is claimed only at source/`--release 17` level. No Java 17 runtime verification will be claimed.

---

## 12. Blockers

| ID | Blocker | Affects | What is needed |
|---|---|---|---|
| BLK-01 | **Lipila collections signing contract not evidenced anywhere in the repository** | Batch 5 | Written provider documentation specifying: signature header name, algorithm, encoding, canonical string, timestamp header, tolerance window, event-id field. **A signature scheme will not be invented** |
| BLK-02 | `R-01`/`R-03` identifiers undefined in both register copies | Batch 3 sign-off, Batch 9 | Human confirmation of the proposed meanings in matrix §2. The defects are confirmed regardless; only the identifier mapping is unresolved |
| BLK-03 | `DOC-SOT-01` — two divergent register copies: root unversioned `70b06e2c…` vs in-repo versioned `34a5c3a7…`. The Phase 1B edit dropped the `(R-02)` label from the in-repo copy | All batches (documentation) | Human decision on the canonical path |
| BLK-04 | No Java 17 runtime installed | Reporting only | Standing limitation |
| BLK-05 | Local PostgreSQL on 5432 inaccessible and not the project DB | Batches 3/4/9 | **Resolved** — disposable PG16 container, 16.15 verified |
| BLK-06 | Testcontainers absent from `pom.xml` | Test infrastructure | **Resolved** — `pgtest` profile, no dependency change (see DEC-11) |

---

## 13. Decisions Required

Full detail in `PHASE_1C_IMPLEMENTATION_PLAN.md` §12. No batch may proceed past a decision it depends on.

### 13.1 Blocks Batch 1 immediately

| ID | Decision | Recommended |
|---|---|---|
| **DEC-01** | **Email-verification enforcement policy.** `isEnabled()` = `emailVerified` and is never checked. Global enforcement locks out every unverified user. Options: (A) global enforcement, (B) financial-only enforcement, (C) no change / remap `enabled`, (D) introduce a real `locked` column | **B — financial-only.** Matches the entity Javadoc; preserves onboarding; gates money movement. Needs a guard at the money-movement service boundary, not a global filter check |
| **DEC-02** | **Access-token revocation after logout.** `jti` + in-memory deny-list (single-instance), accept the 15-minute window as accepted risk, or a DB-backed revocation table. Redis is not permitted | `jti` + in-memory deny-list; retain 15-minute TTL; document the single-instance limitation |
| **DEC-10** | **Forced re-login on `iss`/`aud` rollout.** Adding and validating these invalidates all existing tokens | Accept forced re-login (pre-production) |

### 13.2 Blocks later batches

| ID | Decision | Blocks | Recommended |
|---|---|---|---|
| DEC-03 | Admin role model — keep single `ADMIN` or introduce SUPPORT / KYC_REVIEWER / FINANCE / SUPER_ADMIN (needs migration + seeding) | Batch 2 | Defer role separation; document the single-role risk |
| DEC-04 | `R-01`/`R-03` identifier confirmation | Batch 3 | Confirm the proposed mapping |
| DEC-05 | DB-29 duplicate-financial-effect design. A naive composite unique would reject legitimate identical transfers | Batch 4 | Define an idempotency scope; no composite unique on amount/reference |
| DEC-06 | Audit immutability mechanism — DB permissions vs trigger vs application-only | Batch 4 | DB-level revoke of `UPDATE`/`DELETE`, after confirming Flyway/owner permissions |
| DEC-07 | Collections provider contract (external) | Batch 5 | Keep the 503 guard; record BLOCKED |
| DEC-08 | KYC/PII retention period — requires qualified Zambian counsel | Batch 6 | Do not implement deletion; record the question |
| DEC-09 | Rate-limiting scope and deployment assumption — in-memory only; multi-instance needs a shared store (not Redis this phase) | Batch 7 | Per-operation in-memory limits; document single-instance |
| DEC-11 | Test infrastructure — `pgtest` profile vs Testcontainers | Batches 3/4/9 | `pgtest` profile; no dependency change |
| DEC-12 | Restoring a soft-deleted user currently unfreezes the wallet unconditionally, which can clear a fraud freeze | Batch 3 | Preserve the freeze reason; require an explicit unfreeze action |

---

## 14. Planned Batch Sequence

Risk-ordered, not register-ordered.

| Batch | Scope | Addresses | Blocked by |
|---|---|---|---|
| 1 | Authentication and session hardening | SEC-01, SEC-02, SEC-03, SEC-06, SEC-08, NEW-AUTH-01 | DEC-01, DEC-02 |
| 2 | Authorization and ownership | SEC-10, ownership matrix, mass assignment | DEC-03 |
| 3 | Wallet and financial concurrency | R-01, R-02, R-03, FIN-09, FIN-11, NEW-FIN-01 | DEC-04, DEC-05 |
| 4 | Database integrity and migrations | NEW-DB-01..04, DB-29 design, duplicate-wallet constraint | DEC-05, DEC-06 |
| 5 | Webhook and provider security | SEC-05, NEW-OPS-01 | DEC-07 (BLK-01) |
| 6 | KYC, PII and document security | SEC-12 (upload half), KYC audit trail | DEC-08 |
| 7 | Rate limiting and abuse protection | SEC-09 | DEC-09 |
| 8 | Audit, logging and error handling | SEC-04, SEC-11, SEC-12 (request-id half) | DEC-10 |
| 9 | Final security regression | Fixes from regression only | — |

**Batch 1 before Batch 3:** an attacker who can take over a session never needs to exploit a concurrency race. Authentication is the cheapest path to financial loss.

**Batch 3 before Batch 4:** the application fix lands first so the constraint cannot be bypassed by an unfixed code path. Batch 4 then makes the invariant durable. Hard dependency: Batch 4's `UNIQUE (user_id)` requires Batch 3's idempotent creation path, otherwise concurrent approval surfaces as a 500 instead of a controlled retry.

---

## 15. Read-Only Integrity Verification

| Check | Result |
|---|---|
| `git status --short` | Empty — 0 modified, 0 staged, 0 untracked |
| `git status -- src/ pom.xml .mvn/` | Empty — source, tests, migrations, config untouched |
| Files modified outside `docs/phase-1c/` | **None** |
| Files created | The four `docs/phase-1c/PHASE_1C_*.md` documents only |
| Existing reports modified | **None** — all Phase 1B reports intact |
| Commits on Phase 1C branch | **0** |

**Read-only mandate violations by subagents:** two specialist audits wrote artifacts into the workspace (`db-schema-audit.md`, `risk-extraction.md`). Both files were **relocated, not deleted**, to `/tmp/opencode/phase1c-batch0-artifacts/`. The repository was then re-confirmed clean. No content was lost.

**Note on report version control:** `reports/` is at the workspace root, **outside** the git repository (`novawallet-api` is the repo root). These deliverables are therefore not version-controlled — consistent with every Phase 1B report, but it weakens the audit trail for a security phase. Recommend deciding whether reports belong inside the repo.

---

## 16. Findings Requiring Register Updates

Batch 0 identified register gaps that the owning batches must close:

1. **NEW-FIN-01** (duplicate-wallet TOCTOU) must be added as a new register entry — it is absent and is High severity.
2. **NEW-DB-01..04** must be added.
3. **NEW-AUTH-01** must be added.
4. **NEW-OPS-01** must be added.
5. **SEC-01's description must be corrected** — it currently claims soft-delete bypass, which is mitigated, and references lock/disable states that do not exist.
6. **`DOC-SOT-01`** — the two divergent register copies must be reconciled (BLK-03).
7. **`R-01`/`R-03`** — either define these identifiers or remove them (BLK-02).

Per constraint, no register file was modified in Batch 0.

---

## 17. Stop State

**Batch 0 is complete. Work is stopped.**

- No Batch 1 work has begun.
- Nothing has been committed, pushed, merged, or deployed.
- No source, test, migration, config, or pre-existing report was modified.
- No live Lipila credentials were used.
- No production database was contacted.
- The local PostgreSQL instance was not used.

**To proceed, three inputs are required:**

1. **Approval of Batch 0.**
2. **DEC-01 and DEC-02** — these block the first line of Batch 1 code.
3. **BLK-03** — canonical register path, so risk statuses update in one place.

BLK-01 (Lipila collections contract) blocks only Batch 5 and may remain open. BLK-02 affects only identifier labelling, not whether the defects are fixed.

---

## 18. Change Log

| Date | Change |
|---|---|
| 2026-11-12 | Batch 0 consolidated report. Entry gate passed; branch created at `7975719`; 8 specialist audits executed and independently re-verified; 10 corrections applied; 7 new defects identified; 3 deliverables plus this report produced; 12 human decisions and 6 blockers recorded. Nothing committed. Awaiting approval. |
