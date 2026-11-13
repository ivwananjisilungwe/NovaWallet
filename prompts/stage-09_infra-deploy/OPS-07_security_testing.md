# OPS-07 — Security testing: SAST, DAST, dependency, pen-test prep 🔒

**Area:** infra, all code (read-only findings) · **Needs:** OPS-06

---
Read root `AGENTS.md`, `novawallet-api/docs/phase-1c/PHASE_1C_SECURITY_BASELINE.md`, threat model in the Phase 1C plan §3.

Do:
1. **Automated in CI:** dependency scan (OWASP dependency-check / Dependabot for Maven, pub, npm), SAST (CodeQL or Semgrep rules for Java/Dart/TS), secret scan, container scan, Flutter `--obfuscate` build check, Android manifest lint (no cleartext, no debuggable, backups off, exported components).
2. **DAST against staging/sandbox only:** OWASP ZAP baseline + authenticated scan using the seeded users; script in `infra/security/zap/`. Never against production.
3. **Targeted abuse tests** written as automated tests or scripts: auth brute force/lockout, OTP abuse, token replay after logout, IDOR sweep over every ID-bearing endpoint with two users, webhook forgery/replay, idempotency-key reuse across users, race conditions on limits and goals, file upload attacks, enumeration (lookup/OTP/login timing), mass assignment, CORS, header checks.
4. **Mobile checks:** static review of the app for insecure storage, logging, WebView use, deep-link hijack (verify link targets are validated and need auth), screenshot protection, root/emulator policy, certificate-pinning decision recorded in `docs/DECISIONS.md` with the rotation plan.
5. Findings in `reports/SECURITY_TEST_REPORT.md`: ID, severity (CVSS-like), evidence, fix prompt. Fix critical/high via T2/T3 prompts before launch.
6. **Pen-test brief** for an external tester: scope, endpoints, test accounts, rules of engagement, what is out of scope. Owner commissions the real test (strongly recommended before holding real customer money).
Do not claim the system is "secure" or "certified"; report what was tested and what was not.
