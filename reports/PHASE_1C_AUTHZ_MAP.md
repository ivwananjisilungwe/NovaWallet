# Phase 1C — Endpoint Authorization Map (BE-1C-2)

Generated 2026-11-14 from source. Role model: DEC-03 = single `ADMIN` role (admins and customers share one role model; split roles deferred to BE-1D-07).

## Method and conventions

- **Routes:** 48 controller routes across 15 `*Controller.java` classes under `novawallet-api/src/main/java`. Full paths include the `/api/v1` prefix.
- **Required auth** has three values: `permitAll`, `authenticated` (SecurityConfig `anyRequest().authenticated()`), or `ROLE_ADMIN` (SecurityConfig `/api/v1/admin/**` rule plus `@PreAuthorize` where present).
- **File paths** in the Ownership column are relative to `novawallet-api/src/main/java/com/novawallet/novawallet_api/`. `SecurityConfig` is at `config/SecurityConfig.java`.
- **Proving tests** are named `TestClass#method`, found under `novawallet-api/src/test/java/com/novawallet/novawallet_api/`. `AuthorizationMatrixIntegrationTest#intruder_cannotRead` and similar nested-class methods are listed by method name only.
- **Test status (updated 2026-11-14):** the cited tests were run after generation. Full suite: 361 run, 0 failures (before the gap test was added). `AuthorizationGapIntegrationTest`: 5 run, 0 failures. See `novawallet-api/docs/phase-1c/PHASE_1C_BATCH_2_REPORT.md`.
- **Generic 401 coverage:** `endpoint/RouteSecurityEnumerationTest#everyNonPublicRouteRequiresAuthentication` covers every non-allow-listed route anonymously. Boundary validation is covered by `endpoint/InputBoundsIntegrationTest`.
- **Non-controller permitAll paths** (outside the 48 counted routes): `/actuator/health`, `/actuator/info`, `/api/swagger-ui/**`, `/api/swagger-ui.html`, `/api/api-docs/**`, `/webjars/**` (`config/SecurityConfig.java:111-120`).

## 1. AuthController (`/api/v1/auth`)

| # | Method | Path | Required auth (set where) | Ownership (file:line) | Proving test |
|---|---|---|---|---|---|
| 1 | POST | /api/v1/auth/register | permitAll (`config/SecurityConfig.java:99`) | n/a (public registration) | AuthControllerIntegrationTest#register_shouldReturn201AndTokens |
| 2 | POST | /api/v1/auth/login | permitAll (`config/SecurityConfig.java:100`) | n/a (credentials checked in AuthService) | AuthControllerIntegrationTest#login_shouldReturn200AndTokens |
| 3 | POST | /api/v1/auth/refresh | permitAll (`config/SecurityConfig.java:101`) | n/a (the refresh token is the credential; read at `auth/controller/AuthController.java:124-128`) | RefreshTokenRotationIntegrationTest#refreshTokenRotation_shouldRotateAndDetectReuse |
| 4 | POST | /api/v1/auth/logout | authenticated (`config/SecurityConfig.java:125`) | n/a (acts on current user: `auth/controller/AuthController.java:153,162`) | LogoutIntegrationTest#logoutRevokesPresentedAccessToken; LogoutIntegrationTest#logoutWithoutTokenIsRejected; LogoutIntegrationTest#logoutIsScopedToTheCaller |

## 2. PasswordController (`/api/v1/password`)

| # | Method | Path | Required auth (set where) | Ownership (file:line) | Proving test |
|---|---|---|---|---|---|
| 5 | POST | /api/v1/password/forgot | permitAll (`config/SecurityConfig.java:102`) | n/a (email-keyed, anti-enumeration response) | PasswordResetIntegrationTest#shouldResetPassword (forgot is called from the setupUser helper) |
| 6 | POST | /api/v1/password/reset | permitAll (`config/SecurityConfig.java:103`) | n/a (single-use reset token) | PasswordResetIntegrationTest#shouldResetPassword; PasswordResetIntegrationTest#shouldRejectInvalidResetToken |

## 3. VerificationController (`/api/v1/email`)

| # | Method | Path | Required auth (set where) | Ownership (file:line) | Proving test |
|---|---|---|---|---|---|
| 7 | POST | /api/v1/email/verify | permitAll (`config/SecurityConfig.java:104`) | n/a (token-bound) | EmailVerificationIntegrationTest#shouldVerifyEmail; EmailVerificationIntegrationTest#shouldRejectInvalidToken |

## 4. PinController (`/api/v1/pin`)

| # | Method | Path | Required auth (set where) | Ownership (file:line) | Proving test |
|---|---|---|---|---|---|
| 8 | POST | /api/v1/pin | authenticated (`config/SecurityConfig.java:125`) | n/a (acts on current user: `auth/controller/PinController.java:52`) | PinValidationIntegrationTest#shouldRejectPinWithoutAuth; PinValidationIntegrationTest#shouldSetPinSuccessfully |
| 9 | POST | /api/v1/pin/change | authenticated (`config/SecurityConfig.java:125`) | n/a (acts on current user: `auth/controller/PinController.java:77`) | AuthorizationGapIntegrationTest#pinChange_anonymous_isUnauthorized; AuthorizationGapIntegrationTest#pinChange_invalidBody_isBadRequest |

## 5. WalletController (`/api/v1/wallets`)

| # | Method | Path | Required auth (set where) | Ownership (file:line) | Proving test |
|---|---|---|---|---|---|
| 10 | GET | /api/v1/wallets/me | authenticated (`config/SecurityConfig.java:125`) | n/a (acts on current user: `wallet/controller/WalletController.java:52-53`) | ProfileFeeEndpointIntegrationTest#profileAndWalletEndpoints_shouldReturnCurrentUserData |
| 11 | GET | /api/v1/wallets/{walletId}/statements | authenticated (`config/SecurityConfig.java:125`) | `transaction/service/TransactionHistoryService.java:146-148` (owner check, called at `:97`) | AuthorizationMatrixIntegrationTest#intruder_cannotRead; AuthorizationMatrixIntegrationTest#owner_canRead |

## 6. TransactionController (`/api/v1`)

| # | Method | Path | Required auth (set where) | Ownership (file:line) | Proving test |
|---|---|---|---|---|---|
| 12 | POST | /api/v1/wallets/{walletId}/deposit | authenticated (`config/SecurityConfig.java:125`) | `transaction/service/TransactionService.java:383-385` (via `findActiveWalletWithLock`, called at `:104`) | AuthorizationMatrixIntegrationTest#intruder_cannotMoveMoney; TransactionEdgeCaseEndpointIntegrationTest#directDeposit_nonOwner_isForbidden |
| 13 | POST | /api/v1/wallets/{walletId}/withdraw | authenticated (`config/SecurityConfig.java:125`) | `transaction/service/TransactionService.java:383-385` (via `findActiveWalletWithLock`, called at `:111`) | AuthorizationMatrixIntegrationTest#intruder_cannotMoveMoney |
| 14 | POST | /api/v1/transfers | authenticated (`config/SecurityConfig.java:125`) | n/a (sender is the caller's own wallet, looked up by principal at `transaction/service/TransactionService.java:195-196`; the receiver is credit-only, so no ownership check applies) | TransactionFlowIntegrationTest#shouldCompleteDepositWithdrawAndTransfer (positive path only; no negative test) |
| 15 | GET | /api/v1/wallets/{walletId}/transactions | authenticated (`config/SecurityConfig.java:125`) | `transaction/service/TransactionHistoryService.java:146-148` (via `:63`) | AuthorizationMatrixIntegrationTest#intruder_cannotRead; AuthorizationMatrixIntegrationTest#owner_canRead |
| 16 | GET | /api/v1/transactions/{reference} | authenticated (`config/SecurityConfig.java:125`) | `transaction/service/TransactionHistoryService.java:82-85` (caller must own sender or receiver wallet) | AuthorizationMatrixIntegrationTest#referenceOwnership |
| 17 | GET | /api/v1/wallets/{walletId}/balance | authenticated (`config/SecurityConfig.java:125`) | `transaction/service/TransactionHistoryService.java:146-148` (via `:92`) | AuthorizationMatrixIntegrationTest#intruder_cannotRead; AuthorizationMatrixIntegrationTest#unknownAndMalformedIds |

## 7. PayoutController (`/api/v1`)

| # | Method | Path | Required auth (set where) | Ownership (file:line) | Proving test |
|---|---|---|---|---|---|
| 18 | POST | /api/v1/wallets/{walletId}/payouts | authenticated (`config/SecurityConfig.java:125`) | `payment/service/PayoutService.java:559-561` (via `findOwnedActiveWalletWithLock`, called at `:233`) | PayoutEnabledIntegrationTest#payout_fromAnotherUsersWallet_isForbidden_andGatewayNotCalled; AuthorizationMatrixIntegrationTest#unauthenticated; PayoutEndpointIntegrationTest#payout_unauthenticated_returns401 |

## 8. WebhookController (`/api/v1/webhooks`)

| # | Method | Path | Required auth (set where) | Ownership (file:line) | Proving test |
|---|---|---|---|---|---|
| 19 | POST | /api/v1/webhooks/lipila/collections | permitAll (`config/SecurityConfig.java:110`); shared-secret auth | n/a (no user context); secret checked at `payment/controller/WebhookController.java:74` and `:165-169` (constant-time) | PaymentWebhookEndpointIntegrationTest#invalidSecret_returns401; PaymentWebhookEndpointIntegrationTest#missingSecret_returns401 |
| 20 | POST | /api/v1/webhooks/lipila/disbursements | permitAll (`config/SecurityConfig.java:110`); optional shared secret plus HMAC | n/a (no user context); optional secret at `payment/controller/WebhookController.java:103-105`; HMAC required by default and enforced at `payment/service/WebhookService.java:300-303` | DisbursementWebhookControllerTest#validJson_invalidSignature_returns401 |

## 9. FeeController (`/api/v1`)

| # | Method | Path | Required auth (set where) | Ownership (file:line) | Proving test |
|---|---|---|---|---|---|
| 21 | GET | /api/v1/fees/estimate | authenticated (`config/SecurityConfig.java:125`) | n/a (no user-scoped data; fee configuration is global) | ProfileFeeEndpointIntegrationTest#feeEndpoints_shouldEstimateAndProtectAdminConfiguration |
| 22 | GET | /api/v1/admin/fees | ROLE_ADMIN (`fee/controller/FeeController.java:98`; `config/SecurityConfig.java:124`) | n/a (admin) | AuthorizationMatrixIntegrationTest#customerGets403_anonymousGets401 |
| 23 | GET | /api/v1/admin/fees/{id} | ROLE_ADMIN (`fee/controller/FeeController.java:109`; `config/SecurityConfig.java:124`) | n/a (admin) | AuthorizationMatrixIntegrationTest#customerGets403_anonymousGets401; ProfileFeeEndpointIntegrationTest#adminFeeUpdate_shouldPersistChanges |
| 24 | PUT | /api/v1/admin/fees/{id} | ROLE_ADMIN (`fee/controller/FeeController.java:122`; `config/SecurityConfig.java:124`) | n/a (admin) | AuthorizationMatrixIntegrationTest#customerGets403_anonymousGets401; ProfileFeeEndpointIntegrationTest#adminFeeUpdate_shouldPersistChanges |
| 25 | POST | /api/v1/admin/fees | ROLE_ADMIN (`fee/controller/FeeController.java:136`; `config/SecurityConfig.java:124`) | n/a (admin) | AuthorizationMatrixIntegrationTest#customerGets403_anonymousGets401 (denial only; no admin success test) |
| 26 | DELETE | /api/v1/admin/fees/{id} | ROLE_ADMIN (`fee/controller/FeeController.java:149`; `config/SecurityConfig.java:124`) | n/a (admin) | AuthorizationMatrixIntegrationTest#customerGets403_anonymousGets401 (denial only; no admin success test) |

## 10. NotificationController (`/api/v1/notifications`)

| # | Method | Path | Required auth (set where) | Ownership (file:line) | Proving test |
|---|---|---|---|---|---|
| 27 | GET | /api/v1/notifications | authenticated (`config/SecurityConfig.java:125`) | Scoped by caller id: `notification/controller/NotificationController.java:61`; repository filter `notification/service/NotificationService.java:105` (`findByUserIdOrderByCreatedAtDesc`) | AuthorizationGapIntegrationTest#notifications_areScopedPerUser |
| 28 | GET | /api/v1/notifications/unread-count | authenticated (`config/SecurityConfig.java:125`) | Scoped by caller id: `notification/controller/NotificationController.java:94`; `notification/service/NotificationService.java:113` | AuthorizationGapIntegrationTest#notifications_areScopedPerUser |
| 29 | PUT | /api/v1/notifications/read-all | authenticated (`config/SecurityConfig.java:125`) | Scoped by caller id: `notification/controller/NotificationController.java:115`; query `notification/repository/NotificationRepository.java:28` (`WHERE n.userId = :userId`) | AuthorizationGapIntegrationTest#notifications_areScopedPerUser (B read-all leaves A rows unread) |

## 11. UserController (`/api/v1/users`)

| # | Method | Path | Required auth (set where) | Ownership (file:line) | Proving test |
|---|---|---|---|---|---|
| 30 | GET | /api/v1/users/me | authenticated (`config/SecurityConfig.java:125`) | n/a (acts on current user: `user/controller/UserController.java:54`) | ProfileFeeEndpointIntegrationTest#profileAndWalletEndpoints_shouldReturnCurrentUserData |
| 31 | PUT | /api/v1/users/me | authenticated (`config/SecurityConfig.java:125`) | n/a (acts on current user: `user/controller/UserController.java:80`) | ProfileFeeEndpointIntegrationTest#profileAndWalletEndpoints_shouldReturnCurrentUserData |
| 32 | POST | /api/v1/users/me/change-password | authenticated (`config/SecurityConfig.java:125`) | n/a (acts on current user: `user/controller/UserController.java:106`) | SessionRevocationIntegrationTest#passwordChangeRevokesRefreshToken |

## 12. KycController (`/api/v1/kyc`)

| # | Method | Path | Required auth (set where) | Ownership (file:line) | Proving test |
|---|---|---|---|---|---|
| 33 | POST | /api/v1/kyc/documents/upload | authenticated (`config/SecurityConfig.java:125`) | n/a (acts on current user: `kyc/controller/KycController.java:67`) | AuthorizationGapIntegrationTest#kycUpload_anonymous_isUnauthorized; KycNegativeEndpointIntegrationTest#uploadDocument_emptyFile_shouldReturnBadRequest and #uploadDocument_invalidMimeType_shouldReturnBadRequest assert validation only |
| 34 | GET | /api/v1/kyc/status | authenticated (`config/SecurityConfig.java:125`) | n/a (acts on current user: `kyc/controller/KycController.java:89`) | KycAdminEndpointIntegrationTest#kycEndpoints_shouldSupportSubmitAndAdminApprovalFlow |
| 35 | POST | /api/v1/kyc/submit | authenticated (`config/SecurityConfig.java:125`) | n/a (acts on current user: `kyc/controller/KycController.java:114`) | KycNegativeEndpointIntegrationTest#uploadDocument_pendingReview_shouldReturnBadRequest; KycAdminEndpointIntegrationTest#kycEndpoints_shouldSupportSubmitAndAdminApprovalFlow |

## 13. AdminUserController (`/api/v1/admin`)

Class-level `@PreAuthorize("hasRole('ADMIN')")` at `admin/controller/AdminUserController.java:35`; URL rule at `config/SecurityConfig.java:124`.

| # | Method | Path | Required auth (set where) | Ownership (file:line) | Proving test |
|---|---|---|---|---|---|
| 36 | GET | /api/v1/admin/users | ROLE_ADMIN (`admin/controller/AdminUserController.java:35`; `config/SecurityConfig.java:124`) | n/a (admin) | AuthorizationMatrixIntegrationTest#customerGets403_anonymousGets401; AuthorizationMatrixIntegrationTest#adminAllowed |
| 37 | GET | /api/v1/admin/users/{userId} | ROLE_ADMIN (`admin/controller/AdminUserController.java:35`; `config/SecurityConfig.java:124`) | n/a (admin) | AuthorizationMatrixIntegrationTest#customerGets403_anonymousGets401; KycAdminEndpointIntegrationTest#adminEndpoints_shouldCoverReviewToggleAndWalletFreezeFlow |
| 38 | PATCH | /api/v1/admin/users/{userId}/deactivate | ROLE_ADMIN (`admin/controller/AdminUserController.java:35`; `config/SecurityConfig.java:124`) | n/a (admin; actor id taken from principal at `:112` for audit) | AuthorizationMatrixIntegrationTest#customerGets403_anonymousGets401; SessionRevocationIntegrationTest#adminDeactivationRevokesRefreshTokens |

## 14. AdminKycController (`/api/v1/admin`)

Class-level `@PreAuthorize("hasRole('ADMIN')")` at `admin/controller/AdminKycController.java:36`; URL rule at `config/SecurityConfig.java:124`.

| # | Method | Path | Required auth (set where) | Ownership (file:line) | Proving test |
|---|---|---|---|---|---|
| 39 | GET | /api/v1/admin/kyc/pending | ROLE_ADMIN (`admin/controller/AdminKycController.java:36`; `config/SecurityConfig.java:124`) | n/a (admin) | AuthorizationMatrixIntegrationTest#customerGets403_anonymousGets401; KycAdminEndpointIntegrationTest#kycEndpoints_shouldSupportSubmitAndAdminApprovalFlow |
| 40 | GET | /api/v1/admin/kyc/{userId} | ROLE_ADMIN (`admin/controller/AdminKycController.java:36`; `config/SecurityConfig.java:124`) | n/a (admin) | AuthorizationMatrixIntegrationTest#customerGets403_anonymousGets401; KycAdminEndpointIntegrationTest#adminEndpoints_shouldCoverReviewToggleAndWalletFreezeFlow |
| 41 | GET | /api/v1/admin/kyc/{userId}/documents/{documentId} | ROLE_ADMIN (`admin/controller/AdminKycController.java:36`; `config/SecurityConfig.java:124`) | `kyc/service/AdminKycService.java:96-98` (document must belong to the path userId) | AuthorizationMatrixIntegrationTest#customerGets403_anonymousGets401; KycAdminEndpointIntegrationTest#kycEndpoints_shouldSupportSubmitAndAdminApprovalFlow (success path). AuthorizationGapIntegrationTest#adminDocumentDownload_crossUserDocument_isForbidden (user-to-document mismatch). |
| 42 | POST | /api/v1/admin/kyc/{userId}/approve | ROLE_ADMIN (`admin/controller/AdminKycController.java:36`; `config/SecurityConfig.java:124`) | n/a (admin) | AuthorizationMatrixIntegrationTest#customerGets403_anonymousGets401; KycAdminEndpointIntegrationTest#kycEndpoints_shouldSupportSubmitAndAdminApprovalFlow |
| 43 | POST | /api/v1/admin/kyc/{userId}/reject | ROLE_ADMIN (`admin/controller/AdminKycController.java:36`; `config/SecurityConfig.java:124`) | n/a (admin) | AuthorizationMatrixIntegrationTest#customerGets403_anonymousGets401; KycAdminEndpointIntegrationTest#adminEndpoints_shouldCoverReviewToggleAndWalletFreezeFlow |

## 15. AdminWalletController (`/api/v1/admin`)

Class-level `@PreAuthorize("hasRole('ADMIN')")` at `admin/controller/AdminWalletController.java:44`; URL rule at `config/SecurityConfig.java:124`.

| # | Method | Path | Required auth (set where) | Ownership (file:line) | Proving test |
|---|---|---|---|---|---|
| 44 | POST | /api/v1/admin/wallets/{walletId}/freeze | ROLE_ADMIN (`admin/controller/AdminWalletController.java:44`; `config/SecurityConfig.java:124`) | n/a (admin) | AuthorizationMatrixIntegrationTest#customerGets403_anonymousGets401; KycAdminEndpointIntegrationTest#adminEndpoints_shouldCoverReviewToggleAndWalletFreezeFlow |
| 45 | POST | /api/v1/admin/wallets/{walletId}/unfreeze | ROLE_ADMIN (`admin/controller/AdminWalletController.java:44`; `config/SecurityConfig.java:124`) | n/a (admin) | AuthorizationMatrixIntegrationTest#customerGets403_anonymousGets401; KycAdminEndpointIntegrationTest#adminEndpoints_shouldCoverReviewToggleAndWalletFreezeFlow |
| 46 | GET | /api/v1/admin/wallets | ROLE_ADMIN (`admin/controller/AdminWalletController.java:44`; `config/SecurityConfig.java:124`) | n/a (admin) | AuthorizationMatrixIntegrationTest#customerGets403_anonymousGets401 (denial only) |
| 47 | GET | /api/v1/admin/transactions | ROLE_ADMIN (`admin/controller/AdminWalletController.java:44`; `config/SecurityConfig.java:124`) | n/a (admin) | AuthorizationMatrixIntegrationTest#customerGets403_anonymousGets401 (denial only) |
| 48 | GET | /api/v1/admin/audit-logs | ROLE_ADMIN (`admin/controller/AdminWalletController.java:44`; `config/SecurityConfig.java:124`) | n/a (admin) | AuthorizationMatrixIntegrationTest#customerGets403_anonymousGets401 (denial only) |

## Gaps

No row lacks an ownership check where one is required. Every walletId, reference and document route resolves ownership in a service (rows 11-18, 41).

The six gaps from the first pass of BE-1C-2 are closed by `endpoint/AuthorizationGapIntegrationTest` (5 tests). Targeted run: **Tests run: 5, Failures: 0** (`./mvnw -B test -Dtest=AuthorizationGapIntegrationTest`). This class was not part of the 361-test full run, so it is not yet in a combined full-suite result.

| # | Route | Closed by |
|---|---|---|
| 9 | POST /api/v1/pin/change | AuthorizationGapIntegrationTest#pinChange_anonymous_isUnauthorized; AuthorizationGapIntegrationTest#pinChange_invalidBody_isBadRequest |
| 27 | GET /api/v1/notifications | AuthorizationGapIntegrationTest#notifications_areScopedPerUser |
| 28 | GET /api/v1/notifications/unread-count | AuthorizationGapIntegrationTest#notifications_areScopedPerUser |
| 29 | PUT /api/v1/notifications/read-all | AuthorizationGapIntegrationTest#notifications_areScopedPerUser |
| 33 | POST /api/v1/kyc/documents/upload | AuthorizationGapIntegrationTest#kycUpload_anonymous_isUnauthorized |
| 41 | GET /api/v1/admin/kyc/{userId}/documents/{documentId} | AuthorizationGapIntegrationTest#adminDocumentDownload_crossUserDocument_isForbidden |

Gap count: **0 open** (6 closed). Batch report: `novawallet-api/docs/phase-1c/PHASE_1C_BATCH_2_REPORT.md`.

## Notes (not gaps)

- Admin-only rows 25, 26, 46, 47 and 48 are proved by the denial matrix only. They have no admin-success test. Their authorization is still covered.
- Row 14 (`POST /api/v1/transfers`) has a positive test only. The sender is taken from the principal, so no client-supplied sender exists to test.
- `GET /api/v1/transactions/{reference}` returns 403 for another customer's reference and 404 for an unknown one (proved by AuthorizationMatrixIntegrationTest#referenceOwnership). The Batch 2 plan records this as an API-contract question, not changed here.
- `fee/controller/FeeController.java:29-30` says the class needs "No authentication required". That contradicts `config/SecurityConfig.java:125` and the operation description at `fee/controller/FeeController.java:45`. The route is authenticated. The Javadoc is wrong.
- `docs/api/CONTRACT_TARGET.md` A10 specifies signed, expiring URLs for KYC documents. `admin/controller/AdminKycController.java:99-102` streams the bytes directly. This is a contract mismatch, not an authorization gap. It is not fixed here.
- `config/SecurityConfig.java:110` opens `/api/v1/webhooks/lipila/**` with `permitAll`. Any future route under that prefix will be public by default. The shared secret or HMAC must protect each handler.
