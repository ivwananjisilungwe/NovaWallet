# BE-1C-8 — Phase 1C Batch 8: Audit, logging & error handling 🔒

**Area:** novawallet-api · **Needs:** BE-1C-7

---
Follow `_COMMON_BATCH_RULES.md` for Batch 8.

Scope (plan §7 Batch 8): SEC-04, SEC-11, SEC-12 (request-id half), log hygiene.

Do:
1. **Request id:** accept/generate `X-Request-Id`, put in MDC, return in every response and error body, propagate to provider calls and async jobs.
2. **Structured logging:** JSON logs in non-dev profiles with timestamp, level, request id, user id (not email/phone), route, status, duration. Apply `SensitiveDataRedactor` everywhere; add a test that scans captured logs of the main flows for tokens, PINs, passwords, full phone numbers, ID numbers, Authorization headers.
3. **Error handling:** one error envelope (`code`, `message`, `requestId`, `fieldErrors[]`) for every failure path including filters, 404/405, multipart errors, DB constraint errors, provider errors. No stack traces, SQL, class names or internal hosts in bodies. Stable machine-readable `code` values listed in `docs/api/ERROR_CODES.md` (the app maps these to user messages).
4. **Security headers & config hardening:** HSTS (behind TLS), `X-Content-Type-Options`, `Referrer-Policy`, CORS allow-list per environment (no `*` with credentials), actuator endpoints restricted (health only public), Swagger UI off or protected in production, no default credentials, fail-fast when required secrets are missing or weak (JWT secret length).
5. **Audit completeness:** table of every admin and money action → audit record fields; add missing records; audit writes cannot be silently dropped (failure policy documented).
6. Security event log: failed logins, lockouts, token reuse detection, revoked-token use, authorization failures — at WARN with request id.

Tests: error-envelope contract tests for each failure class, header tests, log-leak scan test, startup fails with weak/missing secret.
