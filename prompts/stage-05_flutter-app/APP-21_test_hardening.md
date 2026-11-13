# APP-21 — Test hardening: coverage, goldens, integration, contract

**Area:** novawallet-app · **Needs:** APP-20

---
1. Measure coverage; raise to floors in AGENTS.md §7 (80 % domain/core, 60 % overall, 100 % money formatting + status mapping) with meaningful tests.
2. **Fidelity suite** runs for **all 127 app screens** (every code in `SCREEN_INDEX.md` except X*) × both themes; `tool/fidelity_compare.dart --all` produces `build/fidelity/REPORT.md` (table of %, state variants, ignored regions). CI fails above the threshold.
3. **Integration journeys** (`integration_test/`) against the sandbox backend in CI (emulator job): sign-up → KYC → top-up → send → request/split → withdraw → bills → goal → support case → logout; negative paths: wrong PIN lockout, insufficient funds, over limit, unknown outcome, maintenance, update required, session expiry, offline recovery.
4. **Contract tests:** the app's generated client is regenerated in CI and fails on drift against `docs/api/openapi.json`; error-code table test ensures every code in `ERROR_CODES.md` has a mapped message.
5. **Security tests:** no `print`, no token/PIN in logs, no `badCertificateCallback`, manifest flags (cleartext off, debuggable off, allowBackup off), FLAG_SECURE list test.
6. Mutation spot-check on `Money`, idempotency-key reuse, state machines.
7. Flaky tests fixed, not skipped. Document how to update fidelity baselines and why they need owner review.
8. CI: format, analyze, unit/widget, fidelity, integration (emulator), size budget check.

Acceptance: reports with numbers; all journeys green; CI file; list of any unreproducible behaviours.
