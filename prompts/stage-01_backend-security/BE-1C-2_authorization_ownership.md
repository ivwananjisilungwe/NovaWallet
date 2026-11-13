# BE-1C-2 — Phase 1C Batch 2: Authorization & ownership hardening 🔒

**Area:** novawallet-api · **Needs:** S0-03 · **Decisions:** DEC-03 (admin roles)

---
Follow `prompts/stage-01_backend-security/_COMMON_BATCH_RULES.md` for Batch 2.

Scope (from plan §7 Batch 2): SEC-10 input bounds, ownership/IDOR verification, role-separation assessment.

Do:
1. **Endpoint authorization map:** generate `reports/PHASE_1C_AUTHZ_MAP.md` from `SecurityConfig` + every controller: method, path, required auth/role, how ownership is enforced (file:line), test that proves it. Mark gaps.
2. **IDOR tests** for every endpoint that takes a resource id (`walletId`, `reference`, `documentId`, `userId`, notification ids): user B cannot read/act on user A's resources; admins only via admin routes. Fix any gap by enforcing ownership in the service layer (not only the controller).
3. **Input bounds (SEC-10):** every request DTO has explicit constraints (size, pattern, min/max, scale for amounts, enum validation, pagination caps such as max page size 100). Reject unknown/oversized payloads; cap multipart sizes consistently with the KYC limit.
4. **Admin role model:** if DEC-03 is "keep single ADMIN", document the risk and add tests that every `/api/v1/admin/**` route requires ADMIN. If DEC-03 chooses roles, STOP after the plan — roles are implemented in `BE-1D-11`.
5. Ensure webhooks and public routes are the only permit-all paths; add a test that enumerates all mappings and fails if a new unauthenticated route appears without being allow-listed.

Tests: IDOR matrix tests, validation tests (boundary values), route-security enumeration test.
