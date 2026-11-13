# NovaWallet — Decisions register

**The one place agents check for owner decisions.** If a prompt needs a decision that has no "Owner decision" here, the agent stops and asks.
Phase 1C detail stays canonical in `novawallet-api/docs/phase-1c/PHASE_1C_DECISION_LOG.md` ("the 1C log"); this file mirrors it and must not contradict it.

Status words: **DECIDED** (owner answered) · **IMPLEMENTED** (decided and in code) · **OPEN** (needs owner) · **BLOCKED** (needs an external party).

## 1. Phase 1C decisions and blockers
| ID | Question | Options | Recommendation | Owner decision | Date | Blocks |
|---|---|---|---|---|---|---|
| DEC-01 | Email-verification enforcement | A global in JWT filter · B financial-only | B | **IMPLEMENTED: B, financial-only** (`EmailVerificationPolicy`); 1C log §3 | 2026-11-13 | — |
| DEC-02 | Access-token revocation after logout | in-memory deny-list · accept 15-min window · DB-backed store | plan said in-memory | **IMPLEMENTED: DB-backed revocation store**, fail-closed 503 (V16); 1C log §4 | 2026-11-13 | — |
| DEC-03 | Admin role model | single ADMIN · SUPPORT / KYC_REVIEWER / FINANCE / SUPER_ADMIN | single role through BE-1C-2; roles in BE-1D-07 | **DECIDED: single ADMIN now; split roles in BE-1D-07** | | BE-1C-2, BE-1D-07, ADM-* |
| DEC-04 | Confirm R-01/R-03 identifier meanings | confirm proposed mapping (1C log §7) · redefine | confirm | **DECIDED: confirm the proposed mapping** (1C log §7: R-01 admin wallet-status mutation, R-02 snapshot rewrite, R-03 account-number race) | 2026-11-14 | BE-1C-3 sign-off |
| DEC-05 | Duplicate financial effect (DB-29) | naive composite unique · idempotency-scoped design | idempotency-scoped | **DECIDED: idempotency-key scope (user + key per operation); no composite unique on amount/reference** | | BE-1C-3/4 |
| DEC-06 | Audit immutability | DB revoke UPDATE/DELETE · trigger · app-only | DB-level revoke | **DECIDED: DB-level revoke of UPDATE/DELETE** (trigger fallback only if a separate migration user is impossible; report it) | | BE-1C-4, X14 |
| DEC-07 / BLK-01 | Lipila collections signing contract | — (external document) | request from Lipila now; keep 503 guard | **BLOCKED** on Lipila docs | | BE-1C-5, PAY-03, APP-08 |
| DEC-08 | KYC/PII retention period | — (legal) | ask Zambian counsel; no deletion until answered | OPEN / BLOCKED on counsel | | BE-1C-6 |
| DEC-09 | Rate-limit store | in-memory per operation · shared store | in-memory, single instance documented | OPEN | | BE-1C-7 |
| DEC-10 | Forced re-login on iss/aud rollout | accept · opt-in per env | accept | **IMPLEMENTED: accepted**; 1C log §5 | 2026-11-13 | — |
| DEC-11 | Postgres test infra | `pgtest` profile · Testcontainers | `pgtest` | **DECIDED: `pgtest` profile** (disposable postgres:16 container; default suite stays on H2; no Testcontainers) | 2026-11-14 | BE-1C-3/4/9 |
| DEC-12 | Restoring a soft-deleted user clears a fraud freeze | keep · preserve freeze, explicit unfreeze | preserve freeze | **DECIDED: keep freeze + reason on restore; explicit unfreeze required** | | BE-1C-3 |
| SEC-07 | Email-verification token expiry | close in 1C · later | close in BE-1D-02 | OPEN (excluded from 1C, 1C log §9) | | BE-1D-02 |
| BLK-02 | R-01/R-03 undefined in register | — | see DEC-04 | **RESOLVED by DEC-04** | | BE-1C-3, BE-1C-9 |
| BLK-03 | Canonical risk register | — | `novawallet-api/docs/architecture/backend-risk-register.md` | **RESOLVED**; 1C log §6 | 2026-11-13 | — |
| BLK-04 | No Java 17 runtime locally | — | never claim Java 17 runtime verification | standing limitation (local JDK is 21; CI uses Temurin 17) | | reporting |
| BLK-05 | Local PG on 5432 unusable | — | disposable PG16 container | RESOLVED | | — |
| BLK-06 | No Testcontainers | — | `pgtest` profile | RESOLVED | | — |

## 2. Decisions this build needs
| ID | Question | Options | Recommendation | Owner decision | Date | Blocks |
|---|---|---|---|---|---|---|
| DEC-13 | SMS/OTP provider for Zambia | owner shortlist | one with verified MTN/Airtel/Zamtel delivery, behind an interface; **verify coverage yourself** | OPEN | | BE-1D-02 |
| DEC-14 | Push notifications | FCM · other | FCM; no financial details in payloads | OPEN | | BE-1D-06, APP-18 |
| DEC-15 | Real fees and limits per KYC tier / operation | — | placeholder config rows; owner sets values | OPEN | | BE-1D-04, PAY-04, ADM-06 |
| DEC-16 | Bills/airtime/data aggregator | Lipila (if offered) · other (designs mention Lenco) | verify with providers; no build without a contract | OPEN | | PAY-05 |
| DEC-17 | LLM vendor and data policy for Nova AI | owner choice | gateway with PII redaction | OPEN | | AI-01, AI-03 |
| DEC-18 | Card-issuing partner | see `docs/CARD-ISSUING-RESEARCH.md` | post-launch; waitlist only | OPEN | | PAY-06 |
| DEC-19 | Hosting: VPS provider/region, domain, backups | owner choice | separate staging and prod VPS; off-site backups | OPEN | | OPS-02 |
| DEC-20 | Licence route | own BoZ authorisation · licensed partner | partner route; **counsel must confirm** | OPEN | | LAUNCH-01/02 |
| DEC-21 | Minimum age | 18+ · under-18 with consent | 18+ at launch unless counsel and partner approve | OPEN | | APP-04, LAUNCH-01 |
| DEC-22 | Terms of Service and Privacy Policy text | — | drafted by counsel | OPEN | | APP-08, APP-23, LAUNCH-01 |
| DEC-23 | Support channels and hours | in-app cases · WhatsApp · email · helpdesk SaaS | in-app cases + WhatsApp/email with an SLA | OPEN | | BE-1D-12, LAUNCH-03 |
| DEC-24 | Repository layout | A monorepo · B separate repos | A | **DECIDED: A, one monorepo at `NovaWallet/`** (move commands in `reports/opencode/S0-02_REPORT.md`, owner runs) | 2026-11-13 | S0-02 |
| DEC-25 | Sign-up/login model (designs: phone + OTP; API: email + password) | see CONTRACT_TARGET C1/C2 | phone-first; email as verified secondary; login by phone or email | OPEN | | BE-1D-02, APP-04, APP-05 |
| DEC-26 | Top-up path (designs: collections; `/deposit` disabled) | see C3 | collections only; keep `/deposit` disabled | OPEN | | PAY-03, APP-08 |
| DEC-27 | Discoverable by phone number by default? | on with opt-out · off with opt-in | on with opt-out | OPEN | | BE-1D-05 |
| DEC-28 | Referral rewards: amounts and funding | — | rewards off until owner sets them | OPEN | | BE-1D-08 |
| DEC-29 | Chilimba legal treatment and pool custody | — | feature flag off until counsel answers | OPEN | | BE-1D-10 |
| DEC-30 | Admin authentication (designs: separate admin login + TOTP + separate JWT audience) | see C6 | separate `/admin/auth/login`, TOTP mandatory, own audience | OPEN | | BE-1D-07, ADM-01 |
| DEC-31 | KYC liveness / face-match vendor (designs mention Smile ID) | see C8 | choose a vendor with a contract; manual review until then | OPEN | | BE-1D-04, BE-1D-03 (PIN reset by selfie), AI-04 |
| DEC-32 | Public identifier for QR / lookup (design QR shows a handle `u=amara`; API has no username) | see C9 | add an optional unique handle; QR carries a server-signed opaque token, not the handle | OPEN | | BE-1D-05 |
| DEC-33 | KYC document types (designs: NRC_FRONT/NRC_BACK; API: NATIONAL_ID/PASSPORT/SELFIE/PROOF_OF_ADDRESS) | see C7 | add NRC_FRONT and NRC_BACK; keep NATIONAL_ID readable for old rows | OPEN | | BE-1D-04, APP-06 |
| DEC-34 | Support chat (designs: live chat O11) | in-house WebSocket · helpdesk SaaS · cases only | cases only at launch (ties to DEC-23) | OPEN | | BE-1D-12, APP-16 |
| DEC-35 | Fee `percentage_fee` unit: fraction (0.0100 = 1%) or percent (0.0100 = 0.01%)? `FeeEngineService` divides by 100, so the V7 seeds charge 0.01% / 0.005% today (the min fee masks it) | fraction (drop /100) · percent (keep; reseed later) | — | **OPEN: owner undecided; change nothing until PAY-04** | 2026-11-14 | PAY-04, BE-1C-4 fee CHECK bound |

## 3. External dependencies
| Item | Owner of the action | Status | Blocks |
|---|---|---|---|
| Lipila collections signing spec (header, algorithm, canonical string, timestamp, tolerance, event id) | owner → Lipila | **missing** | BE-1C-5, PAY-03 |
| Lipila sandbox credentials (collections + disbursements) | owner → Lipila | unknown — owner confirm | PAY-02, PAY-03 |
| Lipila live onboarding / KYB documents, settlement account | owner → Lipila, bank | not started (per StartHere) | LAUNCH-02 |
| Zambian counsel: licence route, Data Protection Act 2021, AML/CFT (FIC), consumer terms | owner | not started | DEC-08, DEC-20, DEC-21, DEC-22, DEC-29 |
| Company registration (PACRA), tax (ZRA), business bank account | owner | unknown — owner confirm | LAUNCH-* |
| Google Play organisation developer account (D-U-N-S if required) | owner | not started | APP-23 |
| Pilot partner at Mulungushi University | owner | not started | LAUNCH-03 |
| Fonts: Plus Jakarta Sans + Inter files in `docs/v5/part-06/fonts/` | owner | **missing** | APP-01 fidelity |
| Provider docs in `docs/providers/` (SMS, bills, cards) | owner | missing | BE-1D-02, PAY-05, PAY-06 |

## Change log
| Date | Change |
|---|---|
| 2026-11-13 | Created (S0-03). DEC-24 recorded as decided (A). DEC-30…34 added from the design/API conflict review. |
| 2026-11-13 | Owner decided DEC-03, DEC-05, DEC-06, DEC-12 (recommended options). |
| 2026-11-14 | Owner decided DEC-04 (confirm), DEC-11 (`pgtest`); approved the BE-1C-3 plan. DEC-35 (fee unit) opened, undecided. Plan-proposed IDs in the Batch 4/6/7/8 plans (DEC-35..40, D2..D12) are NOT registered; renumber from DEC-36 when recorded. |
| 2026-11-14 | BE-1C-3 plan decisions: D-4 → transfer/withdraw `Idempotency-Key` REQUIRED (missing → 400) with request fingerprint (mismatch → 422; migration V17); D-2 → new `FreezeReason.USER_DEACTIVATED`; D-6 → `novawallet-api/docs/architecture/financial-invariants.md`. D-3 (limit for KYC-unapproved users) stays OPEN — FIN-11 BLOCKED. |
