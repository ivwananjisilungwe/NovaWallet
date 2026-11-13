# API contract target: what "backend complete" means

Created by S0-03 on 2026-11-13. Sources: `docs/v5/**/BUILD.md` (the "Data and backend" sections), controllers in `novawallet-api/src/main/java/com/novawallet/novawallet_api/**/controller/`, and `prompts/01_MASTER_ORDER.md`. Base path `/api/v1` (omitted below). `BE-99` freezes this contract; the app and admin build only against it.

**Exists** values:
- **Yes**: the endpoint exists and fits the design.
- **Ext**: it exists but needs additive fields or filters (no decision needed).
- **Conflict**: the design and the API disagree, so an owner decision is needed (section 3).
- **No**: it is missing.

Controller abbreviations: Auth=`auth/controller/AuthController`, Pwd=`auth/controller/PasswordController`, Ver=`auth/controller/VerificationController`, Pin=`auth/controller/PinController`, User=`user/controller/UserController`, Kyc=`kyc/controller/KycController`, Wal=`wallet/controller/WalletController`, Tx=`transaction/controller/TransactionController`, Fee=`fee/controller/FeeController`, Pay=`payment/controller/PayoutController`, Hook=`payment/controller/WebhookController`, Notif=`notification/controller/NotificationController`, AdmU=`admin/controller/AdminUserController`, AdmK=`admin/controller/AdminKycController`, AdmW=`admin/controller/AdminWalletController`.

## 1. App endpoints
| # | Endpoint | Method | Screens | Exists | Delivered by |
|---|---|---|---|---|---|
| 1 | `/app/config` (min/latest version, maintenance, feature flags) | GET | A01, P05, P06, L01 | No | BE-1D-01 |
| 2 | `/app/notices` | GET | E05 (driven by X10/X12) | No | BE-1D-01 |
| 3 | `/legal/{terms\|privacy}` | GET | Q12 | No | BE-1D-08 |
| 4 | `/legal/accept` {version} | POST | Q12 | No | BE-1D-08 |
| 5 | `/auth/otp/send` {phone, purpose} | POST | B01, Q01, Q10 | No | BE-1D-02 |
| 6 | `/auth/otp/verify` | POST | B02, Q01 | No | BE-1D-02 |
| 7 | `/auth/register` (signupToken, details, password, pin) | POST | B05 | **Conflict C1** (Auth: email+password) | BE-1D-02 |
| 8 | `/auth/login` {identifier, password} + `X-Device-Id` | POST | C01 | **Conflict C2** (Auth: email only) | BE-1D-02 |
| 9 | `/auth/refresh` | POST | C02, C06 | Yes (Auth) | — |
| 10 | `/auth/logout` | POST | O01 (sign out) | Yes (Auth) | — |
| 11 | `/auth/device/verify` | POST | C05 | No | BE-1D-02 |
| 12 | `/password/forgot` | POST | C03 | Yes (Pwd) | — |
| 13 | `/password/reset` | POST | C04 | Yes (Pwd) | — |
| 14 | `/email/verify` | POST | O02, Q11 | Yes (Ver) | (SEC-07 expiry: BE-1D-02) |
| 15 | `/pin` (set) | POST | B05 | Yes (Pin) | — |
| 16 | `/pin/change` | POST | O05 | Yes (Pin) | — |
| 17 | `/pin/verify` (returns lock state / remaining attempts) | POST | C02, C07, G05 | No | BE-1D-03 |
| 18 | `/pin/reset/selfie` (multipart) | POST | Q02 | No (needs DEC-31) | BE-1D-03 |
| 19 | `/pin/reset` {resetToken, newPin} | POST | Q03 | No | BE-1D-03 |
| 20 | `/users/me` | GET | B07, E01, O01, O02 | Yes (User) | — |
| 21 | `/users/me` (+`language`) | PUT | A06, O02 | Ext (User) | BE-1D-08 |
| 22 | `/users/me/change-password` | POST | O03 (implied) | Yes (User) | — |
| 23 | `/users/me/close` | POST | O13 | No | BE-1D-08 |
| 24 | `/users/me/phone/change` | POST | Q10 | No | BE-1D-08 |
| 25 | `/users/me/phone/confirm` | POST | Q10 | No | BE-1D-08 |
| 26 | `/referrals/me` | GET | O09 | No (rewards: DEC-28) | BE-1D-08 |
| 27 | `/privacy/consents` | GET/PUT | O12 | No | BE-1D-08 |
| 28 | `/privacy/export` | POST | O12 | No | BE-1D-08 |
| 29 | `/security/overview` | GET | O03 | No | BE-1D-02 |
| 30 | `/security/lock` | POST | O03 | No | BE-1D-02 |
| 31 | `/devices` | GET | O04 | No | BE-1D-02 |
| 32 | `/devices/{id}` | DELETE | O04 | No | BE-1D-02 |
| 33 | `/kyc/status` (incl. rejection reason) | GET | D01, D07–D09, E01 | Yes (Kyc) | — |
| 34 | `/kyc/tiers` (limits per tier) | GET | D02 | No | BE-1D-04 |
| 35 | `/kyc/documents/upload` (type NRC_FRONT/NRC_BACK/SELFIE) | POST | D04, D05 | **Conflict C7** (enum NATIONAL_ID/PASSPORT/SELFIE/PROOF_OF_ADDRESS) | BE-1D-04 |
| 36 | `/kyc/submit` | POST | D06 | Yes (Kyc) | — |
| 37 | `/wallets/me` (+`limits`) | GET | E02, D08, F03 | Ext (Wal) | BE-1D-04 |
| 38 | `/wallets/{id}/transactions` (+`q`, `category`, `size=0` count) | GET | E02, J01, J02, J07 | Ext (Tx) | BE-1D-11 |
| 39 | `/transactions/{reference}` | GET | F05–F08, G06, I03, J03, P03 | Yes (Tx) | — |
| 40 | `/transactions/{reference}/category` | PATCH | J03 | No | BE-1D-11 |
| 41 | `/transactions/{reference}/receipt.pdf` (+ public verify page) | GET | J04 | No | BE-1D-12 |
| 42 | `/wallets/{id}/statements` (+format, period) | GET | J05 | Ext (Wal) | BE-1D-12 |
| 43 | `/wallets/{id}/balance` | GET | — (no screen) | Yes (Tx) | — |
| 44 | `/wallets/{id}/deposit` | POST | — (designs use #49) | **Conflict C3** (exists, deliberately disabled) | PAY-03 |
| 45 | `/wallets/{id}/withdraw` | POST | — (designs use #52) | Yes (Tx), legacy | (C4) |
| 46 | `/transfers` + `Idempotency-Key` | POST | G04 | Yes (Tx) (design names `toWalletId`/`note`; API `receiverWalletId`/`description`: client maps) | — |
| 47 | `/fees/estimate?type=&amount=` | GET | F02, G02, I01 | Yes (Fee) | (real values: PAY-04, DEC-15) |
| 48 | `/payment-methods` (providers + status) | GET | F01 | No | BE-1D-05 |
| 49 | `/wallets/{id}/collections` + `Idempotency-Key` | POST | F04 | No (**BLOCKED** BLK-01) | PAY-03 |
| 50 | `/webhooks/lipila/collections` | POST | (F05 server side) | Yes (Hook), guarded 503 | BE-1C-5 / PAY-03 |
| 51 | `/webhooks/lipila/disbursements` | POST | (I03 server side) | Yes (Hook) | — |
| 52 | `/wallets/{id}/payouts` + `Idempotency-Key` | POST | G07, I02 | Yes (Pay) | PAY-02 moves it onto orchestration |
| 53 | `/payouts/name-check?msisdn=&network=` | GET | G07 | No (only where the provider supports it) | BE-1D-05 / PAY-02 |
| 54 | `/users/lookup?phone=` | GET | G01 | No (DEC-27) | BE-1D-05 |
| 55 | `/contacts/match` (hashed numbers) | POST | G01, Q13 | No | BE-1D-05 |
| 56 | `/qr/me` (server-signed payload) | GET | H01 | No (DEC-32) | BE-1D-05 |
| 57 | QR payload validation (path not given in design) | POST | H02 | No | BE-1D-05 |
| 58 | `/payment-accounts` | GET/POST/DELETE | O06 | No | BE-1D-05 |
| 59 | `/payment-requests` | POST | G08 | No | BE-1D-09 |
| 60 | `/payment-requests/{id}` | GET | Q05 | No | BE-1D-09 |
| 61 | `/payment-requests/{id}/pay` + `Idempotency-Key` | POST | Q05 | No | BE-1D-09 |
| 62 | `/payment-requests/{id}/decline` | POST | Q05 | No | BE-1D-09 |
| 63 | `/splits` | POST | G09 | No | BE-1D-09 |
| 64 | `/merchant-payments` | POST | H03 | No | BE-1D-09 |
| 65 | `/notifications?category=` | GET | E06, E07 | Ext (Notif) | BE-1D-06 |
| 66 | `/notifications/unread-count` | GET | E01, E02 | Yes (Notif) | — |
| 67 | `/notifications/read-all` | PUT | E06 | Yes (Notif) | — |
| 68 | `/notification-preferences` | GET/PUT | O07 | No | BE-1D-06 |
| 69 | Push-token registration (path not given) | POST | A07 | No (DEC-14) | BE-1D-06 |
| 70 | `/budgets?month=` | GET | M02 | No | BE-1D-10 |
| 71 | `/budgets` | POST | M03 | No | BE-1D-10 |
| 72 | `/goals` | GET | M04 | No | BE-1D-10 |
| 73 | `/goals` | POST | M06 | No | BE-1D-10 |
| 74 | `/goals/{id}/deposit` + `Idempotency-Key` | POST | M05, Q08 | No | BE-1D-10 |
| 75 | `/goals/{id}/withdraw` + `Idempotency-Key` | POST | M05, Q09 | No | BE-1D-10 |
| 76 | `/round-ups` | GET/PUT | M07 | No | BE-1D-10 |
| 77 | `/cash-entries` | POST | M08 | No | BE-1D-10 |
| 78 | `/chilimba/groups` (+ members, contributions, payouts) | POST/GET | M09, Q06, Q07 | No (flag off: DEC-29) | BE-1D-10 |
| 79 | `/insights/summary?month=` | GET | J01, M01, M03 | No | BE-1D-11 |
| 80 | `/support/cases` {reference, reason, text, image} | POST | J06 | No | BE-1D-12 |
| 81 | `/help/articles` | GET | O10 | No | BE-1D-12 |
| 82 | Support chat | WS/3rd party | O11 | No (DEC-34) | BE-1D-12 |
| 83 | `/billers` | GET | K01 | No (**BLOCKED** DEC-16) | PAY-05 |
| 84 | `/billers/saved` | GET | K00 | No | PAY-05 |
| 85 | `/bills/validate` | POST | K02 | No | PAY-05 |
| 86 | `/bills/pay` (ZESCO, airtime, DATA) | POST | K02, K04, Q04 | No | PAY-05 |
| 87 | Bill payment status (path not given) | GET | K03 | No | PAY-05 |
| 88 | `/bills/bundles?network=` | GET | Q04 | No | PAY-05 |
| 89 | `/store/products` | GET | K05 | No | PAY-05 |
| 90 | `/store/orders` | POST | K06 | No | PAY-05 |
| 91 | `/cards/waitlist` | POST | L01 | No | PAY-06 (Part A) |
| 92 | `/cards` | POST | L02 | No (**BLOCKED** DEC-18) | PAY-06 |
| 93 | `/cards` | GET | L03 | No | PAY-06 |
| 94 | `/cards/{id}/transactions` | GET | L03 | No | PAY-06 |
| 95 | `/cards/{id}/controls` | PATCH | L05 | No | PAY-06 |
| 96 | `/cards/{id}/freeze`, `/unfreeze` | POST | L06 | No | PAY-06 |
| 97 | `/cards/{id}/fund` + `Idempotency-Key` | POST | L07 | No | PAY-06 |
| 98 | `/ai/chat` | POST | N01 | No (DEC-17) | AI-03 |
| 99 | `/ai/tips` | GET | N02 | No | AI-01 |

Screens with no backend call: A02–A05, A07 (permission only), B03, B04, B06, D03, E03, G03, J07, O08, P01, P02 (local cache), P04 (needs the correlation-ID header on all errors), P07–P09, Q06.

## 2. Admin endpoints (X01–X16)
| # | Endpoint | Method | Screens | Exists | Delivered by |
|---|---|---|---|---|---|
| A1 | `/admin/auth/login` + TOTP, separate audience | POST | X01 | **Conflict C6** (admins use `/auth/login` today) | BE-1D-07 |
| A2 | `/admin/metrics/overview` | GET | X02 | No | BE-1D-07 |
| A3 | `/admin/alerts` | GET | X02 | No | BE-1D-07 |
| A4 | `/admin/users` (+filters, search) | GET | X03 | Ext (AdmU) | BE-1D-07 |
| A5 | `/admin/users/{userId}` | GET | X04 | Yes (AdmU) | — |
| A6 | `/admin/users/{userId}/deactivate` | PATCH | X04 | Yes (AdmU) | (R-01/DEC-12 fix: BE-1C-3) |
| A7 | `/admin/wallets/{walletId}/freeze`, `/unfreeze` | POST | X04 | Yes (AdmW) | — |
| A8 | `/admin/wallets` | GET | — (no screen) | Yes (AdmW) | — |
| A9 | `/admin/kyc/pending` | GET | X05 | Yes (AdmK) | — |
| A10 | `/admin/kyc/{userId}` and `…/documents/{documentId}` (signed, expiring URLs) | GET | X06 | Ext (AdmK) | BE-1C-6 |
| A11 | `/admin/kyc/{userId}/approve`, `/reject` (+reason codes) | POST | X06 | Ext (AdmK) | BE-1D-04 |
| A12 | `/admin/transactions` (+filters) | GET | X07 | Ext (AdmW; design wrongly says NEW) | BE-1D-07 |
| A13 | `/admin/transactions/{ref}` (ledger + events) | GET | X08 | No | LED-03 |
| A14 | `/admin/transactions/{ref}/requery` | POST | X08 | No | PAY-01 |
| A15 | `/admin/transactions/{ref}/resolve` (maker-checker) | POST | X08 | No | LED-04 (+ BE-1D-07 maker-checker) |
| A16 | Reconciliation exceptions: list, assign, resolve | GET/POST | X09 | No | LED-04 |
| A17 | `/admin/providers` (status, kill switch, maker-checker) | GET/PUT | X10 | No | PAY-01 |
| A18 | `/admin/fees` CRUD | GET/POST/PUT/DELETE | X11 | Yes (Fee) | — |
| A19 | Limits config + change requests | GET/POST | X11 | No | PAY-04 |
| A20 | Notices CRUD | GET/POST/PUT | X12 | No | BE-1D-01 |
| A21 | Support cases + messages | GET/POST | X13 | No | BE-1D-12 |
| A22 | `/admin/audit-logs` (append-only at DB level) | GET | X14 | Ext (AdmW; immutability DEC-06) | BE-1C-4 / BE-1C-8 |
| A23 | Staff and roles (invite, role change, deactivate, reset TOTP) | GET/POST/PATCH | X15 | No (DEC-03) | BE-1D-07 |
| A24 | Card waitlist admin view | GET | X16 | No | PAY-06 |

## 3. Conflicts between the designs and the current API (owner decisions)
| ID | Conflict | Options | Recommendation | Decision |
|---|---|---|---|---|
| C1 | Sign-up: designs B01–B05 are phone + OTP → signupToken → register with PIN. API registers with email + password, no OTP. | (a) phone-first, email optional secondary; (b) keep email-first, add phone OTP as verification; (c) both paths | (a) | DEC-25 |
| C2 | Login: designs C01 have Phone/Email tabs (`identifier`) and an `X-Device-Id` header; API takes `email` only. | (a) `identifier` accepting phone or email + device binding; (b) email only | (a) | DEC-25 |
| C3 | Top-up: designs F04 use mobile-money collections; API `/deposit` exists but is deliberately disabled. | (a) collections only, keep `/deposit` disabled (later remove); (b) re-enable deposit for testing | (a) | DEC-26 |
| C4 | Withdraw: designs use `/payouts`; legacy `/withdraw` still exists. | (a) deprecate `/withdraw` at BE-99; (b) keep both | (a). No owner decision needed unless you object; recorded for BE-99. | — |
| C5 | App-side PIN: designs verify PIN on unlock (C02) and expect a server lock state (C07); API checks PIN only inside money calls. | add `/pin/verify` with lockout | add it (already in BE-1D-03 scope) | — |
| C6 | Admin auth: designs X01 require a separate admin login with TOTP and its own JWT audience; admins share `/auth/login` today. | (a) separate endpoint + TOTP + audience; (b) shared login + TOTP step | (a) | DEC-30 |
| C7 | KYC document types: designs upload NRC_FRONT/NRC_BACK; API enum has NATIONAL_ID/PASSPORT/SELFIE/PROOF_OF_ADDRESS. | (a) add NRC_FRONT/NRC_BACK; (b) map both sides to NATIONAL_ID with a `side` field | (a) | DEC-33 |
| C8 | Liveness / face match (D05, Q02) assumes a vendor (Smile ID mentioned); none is contracted. | choose vendor · manual review only | manual review until a contract exists; Q02 PIN reset via selfie stays BLOCKED | DEC-31 |
| C9 | QR/lookup: the design QR uses a handle (`u=amara`); users have no username/handle. | (a) optional unique handle, QR carries a signed opaque token; (b) phone number only | (a) | DEC-32 |
| C10 | Bills: designs name Lenco as the aggregator; no provider is chosen. | — | no build without a contract | DEC-16 |
| C11 | Support chat O11 assumes live chat (WebSocket or Intercom/Freshchat). | in-house · SaaS · cases only | cases only at launch | DEC-34 |

## 4. Counts
| | App | Admin | Total |
|---|---|---|---|
| Exists (Yes) | 21 | 6 | **27** |
| Exists, needs extension (Ext) | 5 | 5 | **10** |
| Conflicting | 4 | 1 | **5** |
| Missing | 69 | 12 | **81** |
| **Rows** | 99 | 24 | **123** |

API endpoints with no design screen: `/wallets/{id}/balance`, `/wallets/{id}/deposit` (disabled), `/wallets/{id}/withdraw` (legacy), `/admin/wallets`. Keep, deprecate or remove them at BE-99.
Rows are endpoint × method as named in the designs. Rows that group several operations (#27, #58, #78, A16–A23) are counted once.
