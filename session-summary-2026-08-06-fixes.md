# NovaWallet — Session Summary (2026-08-06, Part 2: Full-Stack Scan + P0/P1 Fixes)

Companion to `session-summary-2026-08-06.md` (Part 1: Flutter app build). This file covers the
full-stack contract scan and the execution of all P0/P1 fixes approved by the user.

## 1. User Requests
- "scan the project full-stack and fix all P0s and P1s" — DONE
  (`FULLSTACK-SCAN-REPORT.md` delivered; user approved fixing all P0 + P1 findings)
- "check on the remaining 4" — DONE (all 4 background agents found STALLED → cancelled, fixes
  applied directly by main agent)
- "continue" — DONE (all remaining fixes implemented + verified)
- "save a summary first of everything" — DONE (this file)

## 2. Goal
Fix every P0 and P1 contract/UX defect found in the full-stack scan across
`novawallet-api/` (Spring Boot 3.5.3 / Java 17 / PostgreSQL) and `novawallet-app/`
(Flutter, Riverpod, go_router, Dio), then verify: `flutter analyze` clean + `./mvnw test` green.

## 3. Backend Contracts Verified (source of truth, read from Java code)
- `GET /api/v1/admin/kyc/pending` → `ApiResponse<List<UserSummaryResponse>>` — **bare JSON array**
  in `data` (NOT a paged envelope). Fields: `id, firstName, lastName, email, phone, role,
  emailVerified, deleted, createdAt`. **No `tier`/`kycTier` field.**
- `POST /api/v1/admin/kyc/{userId}/approve` → requires body `{"tier": <int>}` (`@NotNull @Min(1)`).
- `POST /api/v1/admin/kyc/{userId}/reject` → requires body `{"reason": "<string>"}` (`@NotBlank`).
- `POST /api/v1/kyc/documents/upload` → multipart; form fields exactly `documentType`
  (enum: `NATIONAL_ID, PASSPORT, SELFIE, PROOF_OF_ADDRESS` — **no `DRIVERS_LICENSE`**) + `file`
  (≤10 MB). **No `path` query param.**
- `POST /api/v1/wallets/{id}/deposit`, `.../withdraw`, `POST /api/v1/transfers` → all return
  `ApiResponse<TransactionResponse>` (id, reference, type, amount, balanceBefore, balanceAfter,
  status, description, senderWalletId, receiverWalletId, createdAt) — **NOT a Wallet**.
- `GET /api/v1/wallets/me` → **404 until KYC approved** (wallet created on approval).
- Change-password: **new endpoint added** `POST /api/v1/users/me/change-password` with
  `ChangePasswordRequest{currentPassword, newPassword}` (BCrypt verify + encode).

## 4. Work Completed (all 8 fix batches)

### Batch A — Android manifests (P0, done by main agent)
- `novawallet-app/android/app/src/main/AndroidManifest.xml`: added
  `<uses-permission android:name="android.permission.INTERNET"/>` (release builds had no network).
- `novawallet-app/android/app/src/debug/AndroidManifest.xml`: added
  `<application android:usesCleartextTraffic="true"/>` for plain-HTTP dev backend (10.0.2.2).
  Release stays HTTPS-only (cleartext NOT added to main manifest).

### Batch B — Admin repository contract (P0, done by main agent)
`novawallet-app/lib/features/admin/data/admin_repository.dart`:
- `pendingKyc()` now parses `data` as a **bare `List`** via
  `_api.get<List<KycQueueItem>>` (was `PagedResponse<KycQueueItem>` → crash/empty against real
  backend).
- `approveKyc(String userId, {required int tier})` now sends body `{'tier': tier}`.
- `rejectKyc(String userId, {required String reason})` now sends body `{'reason': reason}`.
- **Note:** signatures use NAMED params to match Batch C's screen calls
  (`approveKyc(item.userId, tier: tier)`, `rejectKyc(item.userId, reason: ...)`).
- Removed unused `api_exception.dart` import.

### Batch C — Admin dashboard screen (done by subagent bg_ddae91ab, verified by main agent)
`novawallet-app/lib/features/admin/screens/admin_dashboard_screen.dart`:
- `_approve(item)` / `_reject(item)` split with per-item `_loadingIds` loading state.
- Tier picker: `_showTierDialog` — `RadioListTile<int>` for tiers [1,2,3], default from
  `item.tier` if valid. **Main-agent bugfix**: added missing Confirm `FilledButton` →
  `Navigator.pop(context, selectedTier)` (dialog previously could never resolve).
- Reason dialog: `_showRejectDialog` — `TextField` (max 200 chars), non-blank guard.
- `PillButton` wired with `loading:` state.

### Batch D — KYC upload flow (done by subagent bg_1f775dc5, verified by main agent)
- `novawallet-app/lib/features/kyc/data/kyc_repository.dart`: `uploadDocument` uses
  `_api.postMultipart<dynamic>` with `fileFieldName: 'file'` and `fields: {'documentType': ...}`;
  no `path` param. `submitKyc` after uploads.
- `novawallet-app/lib/features/kyc/screens/kyc_upload_screen.dart`: `image_picker` wired
  (`ImagePicker().pickImage`, source choice sheet); document types SELFIE + PROOF_OF_ADDRESS
  (no DRIVERS_LICENSE).
- `pubspec.yaml`: `image_picker: ^1.2.1` added; `ios/Runner/Info.plist`: camera +
  photo-library usage descriptions added.

### Batch E — Wallet money-move return types (P0, done by main agent)
`novawallet-app/lib/features/wallet/data/wallet_repository.dart`:
- `deposit`, `withdraw`, `transfer`, `_moneyMove` return `Future<WalletTransaction>` (was
  `Future<Wallet>`); parser switched to `WalletTransaction.fromJson`.
- Import added: `../../transaction/models/transaction.dart`.
- Verified all 3 callers (deposit/withdraw/send screens) discard the result — no screen changes
  needed.

### Batch F — Dashboard no-wallet UX (P1, done by subagent bg_0991107e, verified by main agent)
`novawallet-app/lib/features/wallet/screens/dashboard_screen.dart`:
- `walletAsync.when(error:)` → `if (e is ApiException && e.statusCode == 404)
  return const _KycPrompt();`
- `_KycPrompt` widget (line ~192): explains wallet is created after KYC approval; CTAs to
  `/kyc/upload` and `/kyc/status`.
- `wallet_provider.dart` surfaces error via `AsyncValue` (screen handles the 404 branch).

### Batch G — Change-password endpoint (done by subagent bg_9fa67d9e, verified by main agent)
Backend (`novawallet-api/`):
- `user/dto/request/ChangePasswordRequest.java` (currentPassword, newPassword — both @NotBlank).
- `user/service/UserService.java`: `changePassword(UUID userId, ChangePasswordRequest)` — verifies
  current password via injected `PasswordEncoder.matches`, encodes new password, saves.
- `user/controller/UserController.java`: `@PostMapping("/me/change-password")` (authenticated).
- `UserServiceTest$ChangePassword`: 2 tests (wrong current password → UnauthorizedException;
  success → hash updated).
Flutter:
- `auth_repository.dart`: `changePassword({currentPassword, newPassword})` →
  `POST /v1/users/me/change-password`.
- `auth_provider.dart`: `changePassword` delegated to repo.
- `change_password_screen.dart`: current/new password fields (obscured), ≥8-char validation,
  `PillButton(loading: _submitting)`, success → toast + `context.pop()`.

### Batch H — Token clear + admin route guard (P0/P1, done by main agent)
- `novawallet-app/lib/core/storage/token_storage.dart`: `clear()` now also deletes
  `_kPinSet` (`auth_pin_set`) — logout properly resets the PIN-set gate (next account can't skip).
- `novawallet-app/lib/app/router.dart`: added admin guard in `_redirect`, after PIN gate:
  `if (location.startsWith('/admin') && !auth.isAdmin) return '/wallet';`
  (`AuthState.isAdmin` verified to exist: `user?.isAdmin ?? false`).

### Bonus P0 found & fixed (by main agent)
- `go_router` was **missing from `pubspec.yaml`** entirely → 131 analyzer errors; the app could
  never compile. Added `go_router: ^14.8.1` + `flutter pub get` → analyzer clean.

## 5. Verification Results
- `flutter analyze` (novawallet-app): **0 errors, 0 warnings** (3 info-level:
  `groupValue`/`onChanged` Radio deprecation in admin tier dialog — Flutter 3.32 API;
  `curly_braces_in_flow_control_structures` in kyc_repository.dart:22).
- `./mvnw compile` (novawallet-api): clean.
- `./mvnw test` (novawallet-api): **125 tests, 0 failures, 0 errors — BUILD SUCCESS**
  (includes new UserServiceTest$ChangePassword; pre-existing async notification
  `RECIPIENT NULL` log noise in integration tests does not fail the build).

## 6. Agent Status / Infra Notes
- 8 fix batches were delegated as background subagents (Sisyphus-Junior / backend-expert).
  Batches C, D, F, G completed successfully (results retrieved & verified on disk).
  Batches A, B, E, H were **stalled** (18+ min, zero assistant messages, zero file changes) →
  cancelled via `background_cancel all=true`, then completed directly by the main agent.
- Cancelled session IDs (resumable if ever needed): `ses_027548276ffeVlhStAmvGpRgnC` (A),
  `ses_0275471aaffeGMrIqmwacsi0IP` (B), `ses_027543b84ffex3X7J77J5wkZ0A` (E),
  `ses_027540e1dffefMJsGBj2UbR2xz` (H).
- `novawallet-app/` and docs are **untracked in git** — Flutter diffs must be reviewed by reading
  files directly, not `git diff`.

## 7. Remaining / Known Non-Blocking Items
- (info) Radio `groupValue`/`onChanged` deprecation — migrate to `RadioGroup` ancestor
  (admin_dashboard_screen.dart:99-100).
- (info) `curly_braces_in_flow_control_structures` — kyc_repository.dart:22.
- Manual runtime QA on emulator/device not yet performed (analyzer + unit tests only);
  recommend smoke-testing: admin approve/reject with tier/reason, KYC upload → submit,
  deposit/withdraw/transfer, change password, logout → PIN gate reset, /admin guard.

## 8. Key Files Touched This Session
**Backend:** `novawallet-api/src/main/java/com/novawallet/novawallet_api/user/dto/request/
ChangePasswordRequest.java`, `.../user/service/UserService.java`,
`.../user/controller/UserController.java`, `.../test/java/.../user/service/UserServiceTest.java`

**Flutter:** `lib/core/storage/token_storage.dart`, `lib/app/router.dart`,
`lib/features/admin/data/admin_repository.dart`,
`lib/features/admin/screens/admin_dashboard_screen.dart`,
`lib/features/kyc/data/kyc_repository.dart`,
`lib/features/kyc/screens/kyc_upload_screen.dart`,
`lib/features/wallet/data/wallet_repository.dart`,
`lib/features/wallet/screens/dashboard_screen.dart`,
`lib/features/auth/data/auth_repository.dart`, `lib/features/auth/providers/auth_provider.dart`,
`lib/features/auth/screens/change_password_screen.dart`,
`android/app/src/main/AndroidManifest.xml`, `android/app/src/debug/AndroidManifest.xml`,
`ios/Runner/Info.plist`, `pubspec.yaml` (+ `pubspec.lock`)
