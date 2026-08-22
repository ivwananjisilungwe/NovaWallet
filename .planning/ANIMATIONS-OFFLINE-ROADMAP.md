# NovaWallet — Animations, Offline & Integration Roadmap

**Date:** 2026-08-21
**Stack:** Flutter (Riverpod + GoRouter) -> Spring Boot 3.5 (JWT + PostgreSQL)
**Integration status:** ALREADY CONNECTED. ApiClient (Dio) handles JWT refresh, idempotency keys, error normalization. 8 repositories call live backend endpoints. Remaining work is UX polish, animations, and offline resilience.

---

## Phase A — Missing Screens (P0, no Stitch needed)

| # | Task | Files | Done when |
|---|------|-------|-----------|
| A1 | TransactionListScreen: search, type chips, date filter, infinite scroll, pull-to-refresh | transaction/screens/transaction_history_screen.dart (verify/fill) | list renders from transactionPaginationProvider |
| A2 | TransactionDetailScreen: amount, fee, status badge, reference, balances before/after, share receipt | transaction/screens/transaction_detail_screen.dart (verify/fill) | route /transactions/:ref shows full detail |
| A3 | ProfileScreen: user info, KYC badge, change PIN/password, notifications prefs, logout | profile/screens/profile_screen.dart | /profile tab renders real data |
| A4 | SuccessScreen: animated checkmark, amount, ref, Share receipt + Send another actions | extras/screens/success_screen.dart | all flows land on it |
| A5 | Recipient picker for Send: recent recipients, search by phone/name | wallet/widgets/recent_recipients.dart + send_screen.dart edit | user never types raw wallet ID |
| A6 | PaymentMethodsScreen: add/list mobile money numbers | wallet/screens/payment_methods_screen.dart | deposit offers method choice |

## Phase B — Animations (use MotionTokens)

| # | Task | How | Done when |
|---|------|-----|-----------|
| B1 | Hero transitions: balance card dashboard->detail, tile->detail | Hero widget + sharedAxis page routes | tap tile = card morphs into detail header |
| B2 | Skeleton-to-content crossfade | AnimatedSwitcher(300ms) wrapping AsyncValue.when | no hard layout swap anywhere |
| B3 | Haptics everywhere interactive | HapticFeedback.selectionClick in PillButton onTapDown, chips, tiles, PIN pad keys | every tap gives tactile response |
| B4 | Success checkmark draw animation | CustomPainter + easeOutBack controller on success screen | check draws itself in ~600ms |
| B5 | Number count-up on balance card | Tween<double> + formatZmw, 800ms easeOutCubic | balance animates on load/refresh |
| B6 | Pull-to-refresh custom indicator | RefreshIndicator with brand-colored spinner + displacement tokens | consistent across list screens |

## Phase C — Offline Interactivity

| # | Task | Approach | Done when |
|---|------|----------|-----------|
| C1 | Local cache layer: Hive boxes for wallet, transactions, user profile | hive_flutter, typed adapters, box.open at bootstrap | cold start renders cached data instantly |
| C2 | Stale-while-revalidate providers | wrap FutureProviders: emit cache first, then network refresh | offline open shows last-known balance/txs |
| C3 | Connectivity banner | connectivity_plus stream -> top pill "Offline - showing cached data" | banner appears/disappears live |
| C4 | Optimistic send/deposit UI | queue op locally, show pending state, reconcile on reconnect | action feels instant even offline |
| C5 | Pending ops outbox | Hive-backed queue of idempotent ops; flush on connectivity restore | queued transfer completes after reconnect |
| C6 | Offline read-only gating | disable money-out buttons when offline; allow history browsing | clear affordance, no dead taps |

## Phase D — Backend Hardening (release blockers)

| # | Task | Notes |
|---|------|-------|
| D1 | Persist IdempotencyKey rows (entity exists: idempotency/entity/IdempotencyKey.java — verify table usage in TransactionService) | unique(user_id, key), replay returns original result |
| D2 | Gate financial endpoints on verified email | 403 pre-verification in TransactionService |
| D3 | Verify change-password endpoint exists end-to-end | PasswordController + /v1/users/me/change-password contract match |
| D4 | Remove committed dev DB password; env-based config | application.yml audit |
| D5 | Balance cache invalidation on every mutation | CacheConfig + WalletService review |

## Phase E — Ship Gate

| # | Task |
|---|------|
| E1 | flutter analyze clean + full test suite green |
| E2 | ./mvnw test green |
| E3 | Manual E2E: register->verify->PIN->deposit->send->withdraw->history->detail |
| E4 | Update HANDOFF doc with outcomes |

---

## Suggested execution order
1. A1+A2+A3+A4 (screens) — parallelizable, biggest visible win
2. B2+B3 (crossfade + haptics) — cheap, high polish
3. C1+C2+C3 (offline foundation)
4. B1+B4+B5 (hero + delight animations)
5. D1-D5 (backend hardening, can run parallel with frontend work)
6. C4+C5 (optimistic + outbox — needs D1 done first)
7. E (ship gate)

## Key facts for implementers
- MotionTokens: durationFast=150ms, normal=250ms, slow=400ms, springScale=0.96
- Colors: NovaColors.primary=#3525CD, primaryContainer=#4F46E5, success=#0F766E
- All money formatting via formatZmw() from core/utils/formatters.dart
- PIN sheet pattern: sealed _PinOutcome classes (Cancelled/Insufficient/Success) — reuse for new flows
- Idempotency: client generates key via Random.secure(), sends Idempotency-Key header
- Backend envelope: {success, message, data} — ApiClient unwraps automatically
