# NovaWallet v5 — ChatGPT build guide (Batches 1–4)

This guide tells **ChatGPT** how to write **opencode** prompts that build the new NovaWallet Flutter app and connect it to your Spring Boot backend (`novawallet-api`), one small step at a time.

How you use it:

1. Start a new ChatGPT conversation.
2. Paste **Section 1 (Master instruction)** as the first message.
3. Upload or paste the files listed in **Section 2**.
4. Then say: *"Give me prompt 0.1"*. ChatGPT writes one opencode prompt. You paste it into opencode, let it finish, then paste opencode's summary back into ChatGPT and say *"next"*.
5. Work through the steps in **Section 4** in order. Never skip a step; each one builds on the last.

> No Dart or Java code appears in this guide. ChatGPT writes instructions; opencode writes the code.

---

## 1. Master instruction (paste this into ChatGPT first)

```text
You are my senior Flutter + Spring Boot tech lead. I am building NovaWallet, a student-first mobile wallet for Zambia.
Your job: write prompts for opencode (an AI coding agent working inside my repo). You never write the code yourself.

Rules for every prompt you write:
1. One prompt = one small, testable step (one screen, one component group, or one backend endpoint). Max ~30 minutes of agent work.
2. Start each prompt with: the goal, the files/docs opencode must read first, and "do not change anything outside this scope".
3. Always point opencode at the design files: screens/<flow>/<ID>_<name>/BUILD.md, light.png, dark.png, and docs/COMPONENTS.md.
4. Always say which backend endpoint to call. If the BUILD.md marks it NEW, first write a separate backend prompt for the Spring Boot repo, then the Flutter prompt.
5. Flutter stack (fixed, do not change): Flutter stable, Dart 3, Riverpod (riverpod_generator), go_router, dio, freezed + json_serializable, flutter_secure_storage, local_auth, flutter_svg, lottie, intl + gen-l10n, drift (offline cache), firebase_messaging, camera, mobile_scanner, connectivity_plus.
6. Architecture: feature-first folders lib/features/<feature>/{presentation,application,domain,data}; shared UI in lib/core/ui; theme in lib/core/theme; networking in lib/core/network.
7. Money: never use double. Use integer ngwee (minor units) in Dart, convert to/from the API's decimal string at the data layer.
8. Every money-moving request sends an Idempotency-Key header (UUID v4 created once per review screen and reused on retry).
9. Never show "success" until the backend says SUCCESS. Never ask for a mobile money PIN.
10. Each prompt must end with: "Run flutter analyze and flutter test. Fix all errors. Then list the files you changed, what you tested, and anything not finished."
11. Backend prompts must end with: "Add/extend integration tests, run ./mvnw test, and list changed files and the Flyway migration you added (if any)."
12. After I paste opencode's summary, check it against the step's "Done when" list before giving the next prompt. If something is missing, write a fix-up prompt first.

Start by asking me to upload the docs. Then give me prompt 0.1 only.
```

---

## 2. Files to give ChatGPT

| File | Where it is |
|---|---|
| `docs/COMPONENTS.md` | Any of the v5 Part zips |
| `README.md` of each Part (screen list and order) | Part 01–04 zips |
| The `BUILD.md` of each screen when you reach it | `screens/<flow>/<ID>_<name>/BUILD.md` |
| Backend docs from your repo | `docs/architecture/payout-lifecycle.md`, `backend-risk-register.md`, `implementation-roadmap.md` |
| This guide | `docs/CHATGPT_BUILD_GUIDE_BATCHES_1-4.md` |

Put the design folders inside the new Flutter repo as `design/` so opencode can open the PNGs.

---

## 3. Backend facts ChatGPT must know (from your current repo)

All routes are under `/api/v1`. Responses use `ApiResponse<T> {success, message, data}`; errors use `ErrorResponse {code, message, timestamp, path}`. JWT bearer auth. An `IdempotencyFilter` already enforces `Idempotency-Key` on POST/PUT/PATCH where configured.

**Already exists (use as is)**

| Area | Endpoint | Notes |
|---|---|---|
| Auth | `POST /auth/register` | RegisterRequest {firstName, lastName, email, phone, password} |
| Auth | `POST /auth/login` | LoginRequest {email, password} → AuthResponse {accessToken, refreshToken, user{id, firstName, lastName, email, phone, role, emailVerified, pinSet}} |
| Auth | `POST /auth/refresh` | refresh token rotation |
| Password | `POST /password/forgot`, `POST /password/reset` | |
| Email | `POST /email/verify` | |
| PIN | `POST /pin/change` | |
| User | `GET /users/me`, `PUT /users/me`, `POST /users/me/change-password` | |
| KYC | `POST /kyc/documents/upload` (multipart), `GET /kyc/status`, `POST /kyc/submit` | |
| Wallet | `GET /wallets/me`, `GET /wallets/{id}/balance`, `GET /wallets/{id}/statements` | WalletResponse {id, accountNumber, balance, currency, status, freezeReason…} |
| Transactions | `GET /wallets/{id}/transactions`, `GET /transactions/{reference}` | TransactionResponse {id, reference, type, amount, balanceBefore, balanceAfter, status, description, senderWalletId, receiverWalletId…} |
| Transfer | `POST /transfers` | TransferRequest {receiverWalletId, amount, pin, description} |
| Payout (withdraw to mobile money) | `POST /wallets/{id}/payouts` | PayoutRequest {amount, pin, accountNumber, description}; three-stage flow, statuses include PENDING_RECONCILIATION |
| Fees | `GET /fees/estimate` | |
| Notifications | `GET /notifications/unread-count`, `PUT /notifications/read-all` | |
| Webhook | `POST /webhooks/lipila/collections` | |

**Important gaps (must be built before the matching screens)**

| Gap | Why | Needed by |
|---|---|---|
| Login by phone | LoginRequest takes `email` only | C01 Phone tab |
| OTP send/verify | No SMS OTP endpoints | B01, B02, C03, C05 |
| PIN set + verify | Only `/pin/change` exists | B04/B05, C02, PIN sheets |
| Mobile-money **collection** (top up) | `/deposit` is deliberately disabled: "Direct wallet deposits are unavailable until verified payment collection is implemented" | F04–F08 |
| Reconciliation + disbursement webhook | Phase 1B Batch 3 not started | F08, I03, P03 |
| App config / notices | Maintenance, min version, provider kill switch | A01, E05, P05, P06 |
| Lookups | user by phone, payout name-check | G01, G07 |
| Device trust | `X-Device-Id`, trusted devices, new-device OTP | C05, O04 |
| Notification list | only count + read-all exist | E06 |

---

## 4. Step-by-step plan (ChatGPT turns each line into one opencode prompt)

Legend: **[F]** Flutter repo prompt · **[B]** backend repo prompt · *Done when* = what ChatGPT checks before moving on.

### Phase 0 — New Flutter project foundation

| Step | Type | What to build | Done when |
|---|---|---|---|
| 0.1 | F | Create the new Flutter app `novawallet_app` (Android + iOS), add the fixed packages, folder structure, `analysis_options` (strict lints), flavors `dev`/`prod` with `API_BASE_URL` via `--dart-define`. Copy design assets (fonts, icons SVG, illustrations SVG, Lottie) into `assets/`. | App runs; `flutter analyze` clean |
| 0.2 | F | Theme: `AppColors` light/dark from COMPONENTS.md §1 as a ThemeExtension, text styles §2, spacing/radius constants §3. Theme mode follows system, overridable. | A debug "Design tokens" page shows every colour and text style in both themes |
| 0.3 | F | Core components part 1: NwButton (all kinds + loading/disabled), NwBottomCta, NwTopBar, NwField (focus, error), NwCard, NwPill (dot + text), NwBanner, NwToggle, NwChips. | Widget gallery page + golden tests light/dark |
| 0.4 | F | Core components part 2: NwKeypad, NwPinDots (shake), NwOtpBoxes, NwStepProgress, NwListGroup/NwRow, NwKeyValueCard, NwTxRow, NwCategoryTile, NwAvatar, NwProviderBadge, NwBottomSheet, NwSkeleton, NwHeroHeader, NwTabBar. | Gallery + goldens |
| 0.5 | F | Templates: Result screen, System screen (centred block), Illustration widget that picks light/dark SVG. | Goldens for success/failed/pending/offline |
| 0.6 | F | Networking: dio client, base URL per flavor, auth interceptor (bearer), refresh-on-401 with a single in-flight refresh, `X-Device-Id` header (UUID stored securely), `Idempotency-Key` helper, error mapper from ErrorResponse → typed `AppFailure`, connectivity check. | Unit tests for refresh queue and error mapping |
| 0.7 | F | Session: secure token storage, `SessionController` (signedOut / locked / signedIn), go_router with redirect guards, app lifecycle lock after 5 min in background. | Tests for redirects |
| 0.8 | F | Localisation setup (ARB en + empty bem/nya), money formatter `K 1,250.00`, date formatter, `FLAG_SECURE` helper. | Tests for formatter |

### Phase B1 — Batch 1 screens (Launch, Sign up, Sign in, KYC)

| Step | Type | What to build | Done when |
|---|---|---|---|
| 1.1 | B | `GET /api/v1/app/config` (minVersion, latestVersion, maintenance{until,message}, features{cards,chilimba,store}, biometricThreshold). Public endpoint, cached 60 s. | Integration test |
| 1.2 | F | A01 Splash (native splash + routing logic), P05 Maintenance, P06 Update required. | All routes from A01 nav table work with a fake config |
| 1.3 | F | A02–A04 Onboarding (one PageView), A05 Welcome, A06 Language, A07 Notifications permission. | Goldens match light.png/dark.png |
| 1.4 | B | OTP service: `POST /auth/otp/send` {phone, purpose: SIGNUP\|RESET\|DEVICE}, `POST /auth/otp/verify` → short-lived `otpToken`. Hash codes, 5-minute expiry, 3 attempts, rate limit per phone/IP, SMS provider behind an interface (log-only in dev). | Tests: expiry, attempts, rate limit |
| 1.5 | B | Register accepts `otpToken` (phone must be verified), add `dateOfBirth`, and new `POST /pin/set` + `POST /pin/verify` (server-side attempt counter, lockout 30 min after 5 wrong, returns remainingAttempts). | Tests |
| 1.6 | F | B01 Phone, B02 Code (SMS Retriever), B03 Details, B04/B05 PIN create/confirm, B06 Biometrics, B07 Account ready — one `SignUpFlow` controller holding state across steps. | Full sign-up against dev backend creates a user and stores tokens |
| 1.7 | B | Login by phone or email: LoginRequest `{identifier, password}` (keep `email` working for old clients), generic error message, lockout after repeated failures. Device trust: store devices, flag new device in AuthResponse (`newDevice: true`), `POST /auth/device/verify`. | Tests |
| 1.8 | F | C01 Sign in, C02 Unlock with PIN, C05 New phone, C06 Session expired, C07 PIN locked. | Lock/unlock + refresh paths tested |
| 1.9 | F | C03 Forgot password (+ OTP screen reuse), C04 New password. | Reset works end to end |
| 1.10 | B | KYC: tiers endpoint `GET /kyc/tiers`, document type enum (NRC_FRONT, NRC_BACK, SELFIE, PASSPORT), rejection reason codes returned by `/kyc/status`. | Tests |
| 1.11 | F | D01 Intro, D02 Levels, D03 Choose ID. | Goldens |
| 1.12 | F | D04 Capture NRC, D05 Selfie (camera, overlays, compression, upload with progress). | Upload works on a real Android phone |
| 1.13 | F | D06 Check and submit, D07 In review (polling + push), D08 Verified, D09 Action needed. | All KYC states reachable with fake repo and real backend |

### Phase B2 — Batch 2 screens (Home, Top up, Send, Receive, Withdraw)

| Step | Type | What to build | Done when |
|---|---|---|---|
| 2.1 | B | Notifications list `GET /notifications?category=&page=` and `PUT /notifications/{id}/read`; FCM device token registration `POST /devices/push-token`. | Tests |
| 2.2 | F | Shell with NwTabBar (5 tabs, placeholders for tabs not built yet). E02 Home, E03 hidden balance, E04 new user, E01 ID in review, P07 skeleton. | Home loads real wallet + last 5 transactions |
| 2.3 | B | `GET /app/notices` driven by an admin provider kill switch table (provider, direction COLLECTION/PAYOUT, enabled, message). | Tests |
| 2.4 | F | E05 Service notice, E06 Notifications, E07 Empty notifications, push handling (foreground banner, tap → route). | Push opens the right screen |
| 2.5 | B | **Mobile-money collection (top up)**: `POST /wallets/{id}/collections` {amount, provider MTN\|AIRTEL\|ZAMTEL, msisdn}. Create PENDING transaction, call Lipila collection API, return {reference, status}. Webhook `/webhooks/lipila/collections` finalises (verify signature, idempotent). Status query job for stuck PENDING (every 1 min for 10 min, then every 10 min up to 24 h). Never credit without provider confirmation. | Integration tests with a fake Lipila: success, failed, timeout, duplicate webhook |
| 2.6 | B | `GET /payment-methods` (providers + enabled flag + saved numbers), fee estimate for COLLECTION. | Tests |
| 2.7 | F | F01 Method, F02 Amount, F03 Over limit, F04 Review. | Fee updates live |
| 2.8 | F | F05 Approve on phone (polling + push + countdown), F06 Success, F07 Failed, F08 Pending. P03 unknown result. | Every status path tested with fake repo |
| 2.9 | B | Lookups: `GET /users/lookup?phone=` (returns name + walletId only for verified users), `POST /contacts/match` (hashed phones). | Privacy tests |
| 2.10 | F | G01 Recipient, G02 Amount, G03 Review, G04/G05 PIN sheet, G06 Success. | Real transfer between two dev users |
| 2.11 | B | Payout name check `GET /payouts/name-check?msisdn=&network=` (if Lipila supports it; otherwise return `unavailable`). Finish **Phase 1B Batch 3**: reconciliation scheduler for PENDING_RECONCILIATION, disbursement webhook, cleanup-job fix (FIN-05). | Tests per risk register IDs |
| 2.12 | F | G07 Send to mobile money, I01 Withdraw, I02 Review, I03 Processing (maps PENDING_RECONCILIATION → "Checking with Airtel"). | Payout states tested |
| 2.13 | B | Payment requests + splits: `POST /payment-requests`, `GET /payment-requests`, `POST /payment-requests/{id}/pay\|decline`, `POST /splits`. | Tests |
| 2.14 | F | G08 Request money, G09 Split bill. | |
| 2.15 | B | QR: `GET /qr/me` (signed payload), `POST /qr/resolve`; merchant payments endpoint stub behind a feature flag. | Tests |
| 2.16 | F | H01 My QR, H02 Scan, H03 Pay a shop. | Scan a printed code on a real phone |

### Phase B3 — Batch 3 screens (Activity, Bills, Store, Cards)

| Step | Type | What to build | Done when |
|---|---|---|---|
| 3.1 | B | Extend transactions list with `q`, `type`, `category`, `from`, `to`; `PATCH /transactions/{ref}/category`; `GET /insights/summary?month=`. | Tests |
| 3.2 | F | J01 Activity, J02 Filters, J07 No results, P08 skeleton. | Infinite scroll + filters |
| 3.3 | F | J03 Detail, J04 Receipt (share image), J05 Statements (download). | |
| 3.4 | B | Support cases `POST /support/cases`, receipt PDF `GET /transactions/{ref}/receipt.pdf`. | |
| 3.5 | F | J06 Report a problem. | |
| 3.6 | F | K00 Payments hub (tab). | |
| 3.7 | B | Bills via aggregator (Lenco or similar) behind a `BillProvider` interface: `GET /billers`, `POST /bills/validate`, `POST /bills/pay` (idempotent), saved billers. | Fake provider tests |
| 3.8 | F | K01 Bills, K02 ZESCO, K03 Token ready, K04 Airtime and data. | |
| 3.9 | B | Store: `GET /store/products`, `POST /store/orders` behind feature flag. | |
| 3.10 | F | K05 Store, K06 Product. | |
| 3.11 | B | Cards behind feature flag `cards`: waitlist endpoint now; issuer integration later (see virtual cards doc). | |
| 3.12 | F | L01 Waitlist (live); L02–L07 built against a fake card repository and hidden by the flag. | Flag off in prod |

### Phase B4 — Batch 4 screens (Insights, Nova AI, Me, Offline)

| Step | Type | What to build | Done when |
|---|---|---|---|
| 4.1 | B | Budgets CRUD, goals CRUD + goal deposit/withdraw (separate ledger sub-accounts), round-up settings, cash entries. | Tests |
| 4.2 | F | M01 Insights (tab), M02 Budgets, M03 New budget. | |
| 4.3 | F | M04 Goals, M05 Goal detail, M06 New goal, M07 Round-ups, M08 Add cash (offline-first with drift). | Cash entries sync after reconnect |
| 4.4 | B | Chilimba as tracker only (groups, members, schedule, reminders). | |
| 4.5 | F | M09 Chilimba behind feature flag. | |
| 4.6 | B | AI gateway: `POST /ai/chat` (server-side tool that reads only the user's summaries), `GET /ai/tips`; consent check. | No PII sent to the model in tests |
| 4.7 | F | N01 Nova chat (streaming), N02 Tips. | |
| 4.8 | B | Security overview, devices list/delete, privacy consents + export, notification preferences, payment accounts (link with OTP + name check), referrals, help articles, account close checks. | Tests |
| 4.9 | F | O01 Me (tab), O02 Details, O03 Security centre, O04 Devices, O05 Change PIN. | |
| 4.10 | F | O06 Linked accounts, O07 Notification settings, O08 Appearance, O09 Invite. | |
| 4.11 | F | O10 Help, O11 Support chat, O12 Privacy, O13 Close account. | |
| 4.12 | F | Offline everywhere: P01, P02 offline Home (cached balance with time), P04 Server error, P09 Back-online toast; global connectivity banner; retry policy. | Airplane-mode test on every tab |
| 4.13 | F | Final pass: accessibility labels, text scale 1.3×, 360-wide phone, dark mode goldens for every screen, performance (no jank on low-end Android). | Checklist signed |

---

## 5. Template ChatGPT should follow for each opencode prompt

```text
GOAL: <one sentence>
READ FIRST: docs/COMPONENTS.md; design/<path>/BUILD.md; design/<path>/light.png and dark.png; <backend doc if any>
SCOPE: only these files/folders: <list>. Do not change anything else.
BUILD:
1. ...
2. ...
BACKEND: call <METHOD /api/v1/...> through <Repository>. Request: {...}. Response: {...}. Errors: <codes → UI state>.
STATES TO SUPPORT: <copy from BUILD.md>
TESTS: <widget/golden/unit/integration>
DONE WHEN: <copy from the step table>
FINISH: Run flutter analyze and flutter test (or ./mvnw test). Fix all errors. List changed files, tests added, and anything unfinished.
```

## 6. Example (ChatGPT should produce prompts like this)

```text
GOAL: Build screen F04 "Top up – review" and connect it to the new collection endpoint.
READ FIRST: docs/COMPONENTS.md; design/screens/F-top-up/F04_topup_review/BUILD.md; light.png; dark.png.
SCOPE: lib/features/topup/** and test/features/topup/**. Do not edit other features.
BUILD:
1. Create TopUpReviewScreen matching the design: amount 44/800, NwKeyValueCard (From, Number, To, Fee, You get), info NwBanner, NwBottomCta "Top up K 250".
2. Create an Idempotency-Key when the screen opens; keep it in the TopUpController state so a retry reuses it.
3. On tap, call TopUpRepository.startCollection(amountNgwee, provider, msisdn, idempotencyKey) → POST /api/v1/wallets/{walletId}/collections.
4. On 201/200 with status PENDING navigate to F05 with the reference. On LIMIT_EXCEEDED go back to F03. On network timeout go to P03 with the reference.
STATES: idle, submitting (button loading, back disabled), error banner for 4xx with the server message.
TESTS: golden light/dark; controller test that a retry sends the same Idempotency-Key.
DONE WHEN: matches the PNGs; a double tap creates only one request.
FINISH: run flutter analyze and flutter test, fix errors, list changed files and anything unfinished.
```

## 7. Rules ChatGPT must enforce

- Build in the order above. Backend prompt before the Flutter prompt that needs it.
- Keep prompts small. If opencode reports something unfinished, the next prompt fixes it before moving on.
- Real money: production must use a licensed partner (Lipila/Lenco merchant account and BoZ requirements). Until then the dev flavor uses Lipila sandbox and the app shows a "Demo" pill on balances.
- Never log tokens, PINs, OTPs, NRC numbers or card data, in either repo.
