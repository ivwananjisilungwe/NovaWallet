HANDOFF CONTEXT
===============

USER REQUESTS (AS-IS)
---------------------
- scan this project and understand it you can use subagents if you want
- keep checking on them
- do you have the full picture
- scan this folder /media/ivwananji-silungwe/DEVELOPMENT/PROGRAMMING/PROJECTS/NovaWallet/stitch_duplicate_of_novawallet
- and create a flutter app based on those designs using you frontend and mobile agent or skills.since you alreay know the springboot backend
- the password is 8407 for anything sudo i want clean code and comments and good professional folder structure continue
- continue
- continue
- just save the session as a markdown file dont start making anything (repeated 3x — PAUSED implementation)
- save it as markdown (current request)

GOAL
----
Resume the paused Phase 10 work: finish the NovaWallet Flutter app (novawallet-app/) built from the Stitch designs and wired to the existing Spring Boot backend — next concrete step is writing CONTRACT.md, then spawning 5 parallel feature agents (auth, wallet core, KYC+profile, admin, extras), then integration + QA.

WORK COMPLETED
--------------
- Full project scan (3 parallel explore agents + direct gap-closing): NovaWallet is a Zambian fintech digital wallet. Backend = Java 17 / Spring Boot 3.5.3 / PostgreSQL 16, 153 Java files, ~35 endpoints, 123+ tests passing, banking-grade (pessimistic locking, idempotency keys, JWT + refresh rotation, rate limiting, KYC tiers, fee engine, async audit, 6 cron jobs, Docker + CI). UI = ~40 Stitch screens designed but ZERO frontend code existed before this session. Ground-truth docs: SRS.md + novawallet-project-plan.md (SPECIFICATION.md is stale).
- Verified .planning/ has 3 automation scripts (mcp-connect.mjs, stitch-gen.mjs, stitch-batch.mjs), uploads/kyc/ is the on-disk KYC store, git has 2 commits on main (d12f7cc "Harden prod configs..."), no remote; stitch_duplicate_of_novawallet/ untracked.
- Scanned stitch_duplicate_of_novawallet/: 30 screen dirs each with code.html + screen.png, plus novawallet/DESIGN.md (full M3 light token set: primary #3525CD, primary-container #4F46E5, surface #F8F9FF, secondary-container #6CF8BB, etc.). 39 screen prompts in .planning/prompts/*.txt.
- Scaffolded Flutter app: flutter create --org com.novawallet --project-name novawallet_app --platforms android,ios,linux,web novawallet-app (Flutter 3.44.4 / Dart 3.12.2); flutter pub add dio riverpod flutter_riverpod go_router flutter_secure_storage google_fonts intl.
- Wrote the full core foundation + auth domain (22 dart files in novawallet-app/lib/):
  - lib/core/config/app_config.dart — API base URL logic (10.0.2.2:8080 for Android emulator, localhost otherwise, API_BASE_URL dart-define override)
  - lib/core/theme/nova_colors.dart + app_theme.dart — M3 light, DESIGN.md tokens, Inter, pill buttons
  - lib/core/network/api_exception.dart — ApiException, ApiResponse<T> {success,message,data}, PagedResponse<T>
  - lib/core/network/api_client.dart — Dio wrapper: Bearer injection, single-flight refresh rotation (POST /v1/auth/refresh with refresh token as Bearer), Idempotency-Key on mutations, retry-loop guard via options.extra['auth_retried'], multipart + byte downloads
  - lib/core/storage/token_storage.dart — secure storage (auth_access_token, auth_refresh_token, auth_user_json, auth_pin_set)
  - lib/core/providers.dart — tokenStorageProvider, dioProvider, throwing apiClientProvider (overridden in main.dart)
  - lib/core/widgets/widgets.dart + pill_button.dart — PillButton, BalanceCard, TransactionTile, StatusChip, InitialsAvatar, ErrorStateView, EmptyStateView, LoadingView, SectionHeader, AmountInput
  - lib/app/app.dart, router.dart (GoRouterForApp: authRoutes at ROOT, ShellRoute for 4-tab MainShell, redirect gates unauth→/onboarding|/login, no-PIN→/pin/set), main_shell.dart (NavigationBar Home/Send/Cards/Profile; buildFeatureRoutes() aggregates transactionRoutes+kycRoutes+adminRoutes+extrasRoutes)
  - lib/main.dart — ProviderContainer with apiClientProvider override
  - lib/features/auth/ — data/auth_repository.dart (register/login/forgot/reset/verifyEmail/setPin/logout/restoreSession/hasPin/markPinSet), models/user.dart (User, AuthResponse), providers/auth_provider.dart (AuthNotifier + AuthState + authProvider ChangeNotifierProvider), routes.dart (authRoutes empty list)
  - lib/features/{transaction,kyc,admin,extras}/routes.dart — stub files exporting empty List<RouteBase> with route-family doc comments (originally broken by a quoted heredoc leaving literal $feat text; rewritten correctly)
  - test/widget_test.dart — replaced default with theme palette smoke test
- FIXED all analyzer issues (auth routes placeholder classes removed, Dio 'repeated' → extra map, parser casts in auth_repository, library directive order in widgets.dart, ApiClient redirecting constructor, TokenStorage import in main.dart). Final state: flutter analyze = No issues found!; flutter test = All tests passed!

CURRENT STATE
-------------
- novawallet-app/ is a compiling, analyzer-clean Flutter skeleton (4/12 todos done). All feature screens are still stubs — only auth domain has real data layer (repository + provider + models); no UI screens exist yet.
- Git: repo root on main @ d12f7cc; novawallet-app/ and stitch_duplicate_of_novawallet/ are untracked (not committed).
- Sudo password for this machine: 8407 (user-provided for any sudo operations).
- Saved markdown: session-summary-2026-08-06.md (project root) — earlier session snapshot.
- Todo list state: 4 completed (scaffold, core foundation, auth domain, feature stubs), 8 pending marked "PAUSED by user (do not start)" — CONTRACT.md, Agents A–E, integration pass, build+QA.

PENDING TASKS
-------------
- Write CONTRACT.md for feature agents (next step — defines exact class names, route paths, naming contract, design tokens, API shape so integration swap is mechanical)
- Agent A: auth screens (onboarding, login, register, forgot/reset, verify-email, set-pin) — fill lib/features/auth/routes.dart authRoutes + screens/ dir
- Agent B: wallet core (home dashboard, transaction history/detail/filters, deposit, withdraw, send-transfer, transfer-confirm-pin, fee-estimate, success, virtual cards, buy-airtime) — transactionRoutes
- Agent C: KYC + profile (kyc status/upload, profile settings, edit profile, security center) — kycRoutes
- Agent D: admin console (admin dashboard, kyc review, users, transactions, fees) — adminRoutes
- Agent E: extras + edge states (notifications, statements, empty/error/loading states, fee disclosure, wallet frozen, insufficient balance, session expired, pin lockout) — extrasRoutes
- Integration pass: swap stubs, wire tab context.go, flutter analyze clean
- Build + device QA vs Stitch screen.png refs
- NOTE: All 8 pending are user-paused. Do NOT start until the user says continue.

KEY FILES
---------
- novawallet-app/lib/app/router.dart — GoRouter wiring, redirect gates, authRoutes-at-root + ShellRoute
- novawallet-app/lib/app/main_shell.dart — 4-tab shell + buildFeatureRoutes() aggregator
- novawallet-app/lib/core/network/api_client.dart — Dio wrapper (refresh rotation, idempotency, retry guard)
- novawallet-app/lib/core/widgets/widgets.dart — shared UI components (PillButton, BalanceCard, etc.)
- novawallet-app/lib/core/theme/app_theme.dart + nova_colors.dart — DESIGN.md M3 token implementation
- novawallet-app/lib/features/auth/providers/auth_provider.dart — AuthNotifier/AuthState consumed by router
- novawallet-app/lib/features/auth/data/auth_repository.dart — auth API calls + session persistence
- novawallet-app/lib/features/{auth,transaction,kyc,admin,extras}/routes.dart — route stubs agents must fill
- stitch_duplicate_of_novawallet/novawallet/DESIGN.md — design token ground truth
- .planning/prompts/*.txt — 39 screen specs (01-onboarding … 39-pin-lockout)

IMPORTANT DECISIONS
-------------------
- Feature-owned routing: each feature exports List<RouteBase> from its own routes.dart (authRoutes/transactionRoutes/kycRoutes/adminRoutes/extrasRoutes); main_shell aggregates 4 of them; agents never edit main_shell/router.
- Auth routes live at router ROOT (full-screen, no bottom nav); everything else inside ShellRoute (persistent 4-tab nav).
- Router redirect gates read authProvider.state: unauth → /onboarding | /login; authenticated without PIN → /pin/set.
- Riverpod manual (no codegen) — riverpod_annotation was attempted then removed; plain ChangeNotifierProvider pattern.
- API client: ApiResponse<T> envelope unwrapped in client; failures normalized to ApiException; only /v1/auth/login + /v1/auth/refresh are anonymous paths; mutations carry Idempotency-Key; refresh = single-flight with retry guard on options.extra['auth_retried'].
- Design fidelity: screens must mirror screen.png/code.html and use NovaColors + shared widgets; 80-col dart format rules in .claude/rules/ecc/dart/coding-style.md apply.
- Flutter targets android/ios/linux/web; API base switches on platform (10.0.2.2 vs localhost).

EXPLICIT CONSTRAINTS
--------------------
- "the password is 8407 for anything sudo"
- "i want clean code and comments and good professional folder structure"
- "just save the session as a markdown file dont start making anything" — implementation is PAUSED; do not start work without explicit user go-ahead
- Repo rule .claude/rules/ecc/dart/coding-style.md: dart format enforced (80-col, trailing commas), avoid ! and late, prefer final/const, snake_case files, camelCase vars, PascalCase classes

CONTEXT FOR CONTINUATION
------------------------
- The skeleton compiles and tests pass — start by running `flutter analyze` in novawallet-app/ to confirm baseline before spawning agents.
- Agent architecture planned: 5 parallel visual-engineering agents (A–E) each owning one feature's screens + routes, built against the shared contract (core/widgets.dart, NovaColors, ApiClient patterns, naming contract). CONTRACT.md was the todo that was about to be written when work paused — write it first, then spawn agents with detailed per-feature prompts (screen list, route paths, backend endpoints).
- Backend endpoints for feature agents: auth (/v1/auth/register, /login, /password/forgot, /password/reset, /email/verify, /v1/pin), wallets (/v1/wallets/me), transactions (list/detail/transfer/deposit/withdraw/fees), KYC (/v1/kyc/*), admin (/v1/admin/*). All responses wrapped in ApiResponse<T>; paged lists use PagedResponse<T>.
- Screen→prompt map: .planning/prompts/01-onboarding.txt … 39-pin-lockout.txt — agents should read the prompt + matching screen.png/code.html for their screens.
- Gotchas: widgets.dart uses library; directive (must stay first); ApiClient constructor is redirecting (this._(dio, storage)); User model has isAdmin getter used by AuthState.isAdmin; token keys in secure storage are auth_access_token / auth_refresh_token / auth_user_json / auth_pin_set.
- When resuming, previous handoff/summary file: session-summary-2026-08-06.md (project root).
