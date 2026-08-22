# NovaWallet – Session Handoff

## 📚 Overview
- **Project type:** Full‑stack monorepo
  - `novawallet-app/` – Flutter/Dart mobile client
  - `novawallet-api/` – Spring Boot Java backend
- **Goal:** Align the mobile UI with the Stitch design system, harden the backend, and achieve 80 %+ test coverage.

## ✅ What’s Completed
### Flutter (Mobile)
- Ran `flutter pub get` – all dependencies (including `mocktail` and `vector_math`) are now installed.
- `flutter analyze` now reports **0 errors** (just warnings about unused design tokens).
- **174/177** widget tests pass. The three failures are due to `google_fonts` needing `TestWidgetsFlutterBinding.ensureInitialized()` – a low‑priority fix.
- Design‑parity audit performed vs. Stitch reference screens (see `reports/full-audit-report.md`).
- Saved a combined audit report: `reports/full-audit-report.md`.

### Backend (Spring Boot)
- Ran the full test suite – **125/125** tests pass.
- Identified **7 CRITICAL** security issues:
  1. Hard‑coded JWT secret fallback.
  2. Hard‑coded admin password fallback and plaintext logging.
  3. User‑scoped idempotency not enforced (no `user_id` in unique constraint, filter does not extract `userId`).
  4. Missing email‑verification gate on login.
  5. No authenticated *change‑password* endpoint.
  6. Refresh‑token not rotated.
  7. Cache‑consistency (no eviction on wallet/transaction writes).
- Produced a prioritized fix list (see `reports/full-audit-report.md`).

## ⏭️ Immediate Next Steps
### Backend – Critical Fixes (do these together)
1. **Remove hard‑coded JWT secret & admin password defaults**
   - Edit `src/main/resources/application.yml`:
     ```yaml
-    jwt:
-      secret: ${JWT_SECRET:ZGV2LXNlY3JldC1rZXktdGhhdC1pcy1hdC1sZWFzdC0yNTYtYml0cy1sb25nLWZvci1oczI1Ng==}
+    jwt:
+      secret: ${JWT_SECRET}
-    app:
-      admin:
-        password: ${APP_ADMIN_PASSWORD:Admin@123}
+    app:
+      admin:
+        password: ${APP_ADMIN_PASSWORD}
     ```
   - Ensure the application fails fast if these env vars are missing (Spring will throw an exception on startup).
   - Update `AdminDataInitializer.java` to log the admin credentials **only at WARN** and only when the password is supplied via env var (no default).
2. **Enforce user‑scoped idempotency**
   - **Database migration** (new Flyway script, e.g. `V12__add_user_scoped_idempotency_constraint.sql`):
     ```sql
     ALTER TABLE idempotency_keys ADD COLUMN user_id UUID;
     CREATE UNIQUE INDEX uk_user_idempotency_key ON idempotency_keys (user_id, idempotency_key);
     ```
   - Update `IdempotencyFilter` to correctly extract `userId` from the JWT token. Example (assuming a `JwtAuthenticationToken`):
     ```java
     if (request.getUserPrincipal() instanceof JwtAuthenticationToken jwt) {
         UUID uid = UUID.fromString(jwt.getTokenAttributes().get("sub").toString());
         userId = uid;
     }
     ```
   - Adjust any integration tests that expect the old unique constraint.
3. **Add missing security endpoints** (optional for now, but part of the security hardening):
   - Implement `POST /api/auth/change-password` (authenticated, validates current password, updates hash).
   - Ensure login flow checks `User.emailVerified` before issuing JWT.
   - Rotate refresh tokens in the refresh‑token flow (`TokenService.refresh` should invalidate the old token).
   - Add cache eviction on wallet/transaction mutations (`@CacheEvict` annotations).

### Verification & Quality Gates
- **Run tests after each change**:
  ```bash
  cd novawallet-api && ./mvnw test
  ```
- **Check build**:
  ```bash
  ./mvnw verify
  ```
- **Front‑end test run** (after any UI changes):
  ```bash
  cd novawallet-app && flutter test
  ```
- **Coverage** – ensure overall coverage ≥ 80 %:
  ```bash
  ./mvnw test -Dcoverage
  cd novawallet-app && flutter test --coverage
  ```
- **Verification Loop** – run the verification‑loop skill (or manually):
  ```bash
  verification-loop
  ```

## 📋 Handoff Checklist for the Next Engineer
- [ ] Pull the latest `main` (or current working branch).
- [ ] Ensure an active **Java 21** (or the version defined in `pom.xml`) and **Flutter 3.44.4** environment.
- [ ] Verify `./mvnw clean test` passes – **all 125 tests** should be green.
- [ ] Verify `flutter analyze` passes with **0 errors**.
- [ ] Implement the **backend critical fixes** listed above.
- [ ] Add a **new migration** for the idempotency `user_id` column and unique index.
- [ ] Update `AdminDataInitializer` to avoid logging passwords and to require `APP_ADMIN_PASSWORD`.
- [ ] Run the full test suite again; commit each logical change with conventional commits:
  - `fix: enforce JWT secret via env var`
  - `fix: enforce admin password via env var`
  - `feat(idempotency): add user‑scoped unique constraint`
  - `refactor(idempotency): extract userId from JWT`
- [ ] After all fixes, run the **verification‑loop** and confirm:
  - Build SUCCESS
  - Test coverage ≥ 80 %
  - No remaining CRITICAL findings.
- [ ] Update `reports/full-audit-report.md` with the new status (add a **Post‑Fix** section).
- [ ] Create a PR targeting `main`, include the audit report link, and request a review from the security team.

## 📦 Quick Commands Recap
```bash
# Backend – start from repo root
cd novawallet-api
./mvnw clean verify        # full build + tests
./mvnw test                # run unit/integration tests
# Add migration (example)
mkdir -p src/main/resources/db/migration && cat > src/main/resources/db/migration/V12__add_user_scoped_idempotency_constraint.sql <<'SQL'
ALTER TABLE idempotency_keys ADD COLUMN user_id UUID;
CREATE UNIQUE INDEX uk_user_idempotency_key ON idempotency_keys (user_id, idempotency_key);
SQL
# Front‑end – after any change
cd ../novawallet-app
flutter pub get
flutter analyze
flutter test
```

---
*Generated by the OpenCode orchestrator.*
