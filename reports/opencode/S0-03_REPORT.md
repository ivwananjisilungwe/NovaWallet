# S0-03 — Decisions register and API contract target

Date: 2026-11-13. Docs only; no source code changed.

## Outputs
- `docs/DECISIONS.md`:
  - Every Phase 1C DEC/BLK with its real status, citing the 1C log. DEC-01, 02 and 10 are implemented; BLK-03 is resolved.
  - DEC-13…29 from `prompts/02_DECISIONS_AND_BLOCKERS.md`.
  - DEC-24 recorded as decided (A, monorepo).
  - New DEC-30…34 from the conflict review.
  - An external-dependencies table.
- `docs/api/CONTRACT_TARGET.md`: 99 app rows and 24 admin rows (endpoint · method · screens · exists, citing the controller · delivering prompt), 11 conflicts with options, and counts.

Note: the 1C log shows DEC-02 was implemented as a **DB-backed** store, not the in-memory deny-list the plan §12 recommended. DECISIONS.md follows the log, which is canonical.

## Counts
Exists 27 · needs extension 10 · conflicting 5 · missing 81 (123 rows).

## Open decisions, ordered by the first prompt they block
| First blocked prompt | Decisions |
|---|---|
| BE-1C-2 (#4) | DEC-03 admin roles (recommended: keep single ADMIN in 1C-2; roles in BE-1D-07) |
| BE-1C-3 (#5) | DEC-04 R-01/R-03 mapping · DEC-05 duplicate-effect design · DEC-11 `pgtest` · DEC-12 fraud freeze on restore |
| BE-1C-4 (#6) | DEC-06 audit immutability |
| BE-1C-6 (#7) | DEC-08 PII retention (counsel) |
| BE-1C-7 (#8) | DEC-09 rate-limit store |
| BE-1D-02 (#12) | DEC-13 SMS provider · DEC-25 phone-first sign-up · SEC-07 |
| BE-1D-03 (#13) | DEC-31 liveness vendor (PIN reset by selfie) |
| BE-1D-04 (#14) | DEC-15 fees/limits values · DEC-33 NRC doc types |
| BE-1D-05 (#15) | DEC-27 discoverability · DEC-32 handle/QR |
| BE-1D-06 (#16) | DEC-14 FCM |
| BE-1D-07 (#17) | DEC-30 admin login/TOTP |
| BE-1D-08 (#18) | DEC-28 referrals |
| BE-1D-10 (#29) | DEC-29 Chilimba |
| BE-1D-12 (#31) | DEC-23 support channels · DEC-34 support chat |
| BE-1C-5 / PAY-03 (#25–26) | DEC-07/BLK-01 Lipila signing spec · DEC-26 collections-only |
| PAY-05 (#32) | DEC-16 bills aggregator |
| PAY-06 (#33) | DEC-18 card partner |
| AI-01 | DEC-17 LLM vendor |
| OPS-02 | DEC-19 hosting |
| APP-04 | DEC-21 minimum age |
| LAUNCH-01/02 | DEC-20 licence route · DEC-22 terms/privacy |

## Report — S0-03
Done: decisions register; contract target; conflict list; counts.
Files changed: 3 (`docs/DECISIONS.md`, `docs/api/CONTRACT_TARGET.md`, this report).
Evidence: endpoints taken from a grep of `@*Mapping` in all `*Controller.java` (61 mappings, 15 controllers); design needs from the "Data and backend" sections of all 143 `BUILD.md` files. No build needed (docs only).
Blocked / not done: none for this prompt.
Decisions needed: see the table above. DEC-03 blocks the next prompt.
Risks noticed (not fixed): some X-screen BUILD.md files describe existing endpoints as NEW (X07). Several design calls have no path (QR validate, push token, bill status); the delivering prompts must define them and add them here.
Next: BE-1C-2 (`prompts/stage-01_backend-security/BE-1C-2_authorization_ownership.md`) once DEC-03 is answered.
