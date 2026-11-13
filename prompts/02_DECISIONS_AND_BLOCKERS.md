# Decisions & Blockers — only you (or partners) can resolve these

Record every answer in `docs/DECISIONS.md` (created by `S0-03`). The agent reads that file and must stop when a needed decision is missing.

## A. Already in your Phase 1C plan (from `novawallet-api/docs/phase-1c/PHASE_1C_IMPLEMENTATION_PLAN.md` §12)
| ID | Decision | Blocks | Plan's recommendation |
|---|---|---|---|
| DEC-03 | Admin roles: single ADMIN vs SUPPORT / KYC_REVIEWER / FINANCE / SUPER_ADMIN | BE-1C-2, BE-1D-07, ADM-* | For the admin console you need separate roles. Recommend: introduce roles in BE-1D-07, keep single role during BE-1C-2. |
| DEC-04 | Confirm R-01/R-03 identifier meanings | BE-1C-3 sign-off | Confirm the matrix mapping |
| DEC-05 | Duplicate financial effect (DB-29): idempotency scope, not a naive composite unique | BE-1C-3/4 | Idempotency-scoped design |
| DEC-06 | Audit immutability: DB revoke vs trigger vs app-only | BE-1C-4 | DB-level revoke UPDATE/DELETE |
| DEC-07 / BLK-01 | Lipila collections signing contract (header, algorithm, canonical string, timestamp, tolerance, event id) | BE-1C-5, PAY-03, APP-09 | **Request docs from Lipila now** |
| DEC-08 | KYC/PII retention period | BE-1C-6 | Ask qualified Zambian counsel; don't implement deletion until answered |
| DEC-09 | Rate limiting store (in-memory now; shared store later) | BE-1C-7 | In-memory per operation, documented single-instance |
| DEC-11 | Test infra: `pgtest` profile vs Testcontainers | BE-1C-3/4/9 | `pgtest` (already used) |
| DEC-12 | Restoring soft-deleted user must not clear a fraud freeze | BE-1C-3 | Preserve freeze reason; explicit unfreeze |
| SEC-07 | Email-verification token expiry | — | Stays OPEN in 1C; decide when to close (suggest BE-1D-02) |

## B. New decisions this pack needs
| ID | Decision | Blocks | Suggested default |
|---|---|---|---|
| DEC-13 | SMS/OTP provider for Zambia (coverage on MTN/Airtel/Zamtel, price, sender ID) | BE-1D-02 | Pick one with verified Zambian delivery; keep an interface so it can be swapped. VERIFY coverage yourself. |
| DEC-14 | Push notifications: Firebase Cloud Messaging | BE-1D-06, APP-18 | FCM (Android standard). Payload must carry no financial details. |
| DEC-15 | Real fees and limits per KYC tier and per operation | BE-1D-04, PAY-04, ADM-06 | Placeholder values in a config table; owner sets real ones |
| DEC-16 | Bills/airtime/data aggregator (Lipila if it offers it, or another) | PAY-05 | VERIFY with providers; no build without a contract |
| DEC-17 | LLM vendor + data policy for Nova AI (no PII to model, retention, region) | AI-01/03 | Gateway with redaction; vendor chosen by owner |
| DEC-18 | Card issuing partner (see `docs/CARD-ISSUING-RESEARCH.md`) | PAY-06 | Post-launch |
| DEC-19 | Hosting: VPS provider/region, domain, backups location | OPS-02 | One VPS for staging, separate for prod; off-site backups |
| DEC-20 | Licence route: own BoZ authorisation vs operating under a licensed partner | LAUNCH-01/02 | See `docs/v5/part-08/06_COMPLIANCE_ZAMBIA.md`. Partner route is faster. Counsel must confirm. |
| DEC-21 | Minimum age / students under 18 | APP-04, LAUNCH-01 | 18+ at launch unless counsel and partner approve otherwise |
| DEC-22 | Terms of Service and Privacy Policy text | APP-08, APP-23, LAUNCH-01 | Drafted by counsel |
| DEC-23 | Customer support channels and hours | BE-1D-12, LAUNCH-03 | In-app cases + WhatsApp/email; define SLA |

## C. External items to start NOW (they take weeks)
1. Lipila: collections signing spec, sandbox credentials for collections + disbursements, live onboarding requirements (KYB documents), settlement account process.
2. Legal: Zambian counsel on licence route, Data Protection Act 2021 registration/obligations, AML/CFT obligations (Financial Intelligence Centre), consumer terms. Ask counsel which current laws and BoZ directives apply; do not rely on the agent for legal conclusions.
3. Bank / settlement account for the business; company registration (PACRA) and tax (ZRA) if not done.
4. Google Play developer account (organisation account for a finance app), D-U-N-S if required.
5. Pilot partner at Mulungushi University (student union / ICT department) for the closed beta.

## D. Decisions added by the cleanup/design review (raise in S0-03)
| ID | Decision | Blocks | Suggested default |
|---|---|---|---|
| DEC-24 | Repository layout: one monorepo at the root vs separate repos (today only `novawallet-api` has git) | S0-02 | One monorepo |
| DEC-25 | Sign-up model: the designs use **phone + OTP** (B01–B07); the API uses email + password | BE-1D-02, APP-04 | Phone-first with email kept as verified secondary + login |
| DEC-26 | Top-up: designs use mobile-money **collections**; `/deposit` is disabled | PAY-03, APP-08 | Collections only; keep `/deposit` disabled |
| DEC-27 | Discoverability default: can other users find me by phone number? | BE-1D-05 | On, with an opt-out in settings (O-screens) |
| DEC-28 | Referral rewards: amounts and who funds them | BE-1D-08 | Rewards off until the owner sets them |
| DEC-29 | Chilimba: legal treatment of group savings and pool custody | BE-1D-10 | Flag off until counsel answers |
