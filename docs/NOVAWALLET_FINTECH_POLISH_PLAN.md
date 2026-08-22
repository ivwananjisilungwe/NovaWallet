# NovaWallet Flutter App - Fintech Polish Plan

**Generated:** 2026-08-20
**Last Updated:** 2026-08-21
**Status:** ✅ Phase 1 COMPLETE (0 analyzer issues) — Phase 2 ready to start
**Target:** Production-ready modern fintech app with clean navigation & animations

---

## 📋 Executive Summary

NovaWallet has a **professional-grade architecture** (Riverpod, GoRouter, Material 3, Design Tokens) but has **44 build issues** blocking compilation and several **fintech UX gaps** for a modern clean app.

This plan addresses:
1. **Phase 1:** Fix all build errors (Blocker)
2. **Phase 2:** Core Fintech Polish (Dark mode, Haptics, Pull-to-refresh, Bottom sheets)
3. **Phase 3:** Animation & Micro-interactions (Staggered lists, Count-up, Success states)
4. **Phase 4:** Testing Infrastructure (E2E, Widget, Golden tests)
5. **Phase 5:** Accessibility & Performance Audit

---

## 🎯 Phase 1: Fix Build Errors (✅ COMPLETE — 2026-08-21)

> **Result:** `flutter analyze` → **No issues found!** All 44 issues resolved.
> ⚠️ **Outstanding:** `flutter test` not yet verified (timed out) — run first in next session.

### Errors Fixed (44 total from `flutter analyze`)

| # | File | Line | Error | Fix |
|---|------|------|-------|-----|
| 1 | `lib/app/router.dart` | 5 | Unused import: `page_transitions.dart` | Remove import |
| 2 | `lib/core/theme/design_tokens.dart` | 20-77 | 28 unused fields (indigo800, indigo700, etc.) | Remove or mark `@unused` |
| 3 | `lib/core/transitions/page_transitions.dart` | 1 | Dangling library doc comment | Add `library` directive |
| 4 | `lib/core/transitions/page_transitions.dart` | 32 | Undefined class `TransitionsBuilder` | Fix to `PageTransitionsBuilder` |
| 5 | `lib/core/transitions/page_transitions.dart` | 189-190 | `pathParameters`/`queryParameters` not in `go()` | Remove or use `GoRouter.go()` correctly |
| 6 | `lib/core/widgets/skeleton.dart` | 1 | Dangling library doc comment | Add `library` directive |
| 7 | `lib/features/admin/screens/admin_dashboard_screen.dart` | 103-104 | Deprecated `groupValue`/`onChanged` on Radio | Migrate to `RadioGroup` |
| 8 | `lib/features/wallet/screens/dashboard_screen.dart` | 214 | Undefined class `Wallet` | Import `Wallet` model |
| 9 | `lib/features/wallet/screens/dashboard_screen.dart` | 320 | Undefined method `formatCurrency` | Import `formatCurrency` from formatters |
| 10 | `lib/features/wallet/screens/dashboard_screen.dart` | 346 | Undefined `onDeposit` | Add callback or remove |
| 11 | `lib/features/wallet/screens/dashboard_screen.dart` | 356 | Undefined `onWithdraw` | Add callback or remove |
| 12 | `lib/features/wallet/screens/dashboard_screen.dart` | 486 | Undefined `HapticFeedback` | Import `package:flutter/services.dart` |
| 13 | `lib/features/wallet/screens/dashboard_screen.dart` | 610 | Unnecessary multiple underscores | Use single `_` |
| 14 | `lib/features/wallet/screens/dashboard_screen.dart` | 671-672 | `TransactionTile` constructor mismatch | Fix constructor call |

### Files to Patch

1. **`lib/app/router.dart`** - Remove unused import
2. **`lib/core/theme/design_tokens.dart`** - Clean unused primitive tokens
3. **`lib/core/transitions/page_transitions.dart`** - Fix `TransitionsBuilder` → `PageTransitionsBuilder`, fix GoRouter extension
4. **`lib/core/widgets/skeleton.dart`** - Add library directive
5. **`lib/features/admin/screens/admin_dashboard_screen.dart`** - Migrate Radio to RadioGroup
6. **`lib/features/wallet/screens/dashboard_screen.dart`** - Fix all 8 issues (imports, callbacks, constructor)

---

## 🎨 Phase 2: Core Fintech Polish (2-3 Days)

### 2.1 Dark Mode Support (High Priority)
- [ ] Add `ThemeMode.system` support in `AppTheme`
- [ ] Create `AppTheme.dark` using dark tokens from `design_tokens.dart`
- [ ] Add theme toggle in Profile screen
- [ ] Persist theme preference via `SharedPreferences`
- [ ] Update `NovaColors` semantic tokens for dark (already defined in tokens)

### 2.2 Haptic Feedback (High Priority)
- [ ] Add `HapticFeedback.lightImpact()` to `PinPad` key presses
- [ ] Add `HapticFeedback.mediumImpact()` to `PillButton` on tap
- [ ] Add `HapticFeedback.heavyImpact()` on transaction confirm
- [ ] Add `HapticFeedback.vibrate()` on error (PIN wrong, validation fail)
- [ ] Respect system haptic settings

### 2.3 Pull-to-Refresh (Medium Priority)
- [ ] Wrap `DashboardScreen` body in `RefreshIndicator`
- [ ] Add `refresh()` to `WalletNotifier` and `TransactionNotifier`
- [ ] Add pull-to-refresh to `TransactionsList`, `CardsScreen`
- [ ] Show loading indicator with skeleton shimmer

### 2.4 Bottom Sheet Confirmations (Medium Priority)
- [ ] Replace `/pin/confirm` full-screen route with `showModalBottomSheet`
- [ ] Add drag handle, rounded top corners (28px radius)
- [ ] Add `PinPad` inside bottom sheet
- [ ] Auto-dismiss on success, shake on error
- [ ] Apply to: Send confirm, Withdraw confirm, Card freeze

### 2.5 Empty States (Medium Priority)
- [ ] Create `EmptyStateView` widget (illustration + title + action button)
- [ ] Apply to: No transactions, No cards, No beneficiaries, No notifications
- [ ] Use consistent illustration style (undraw or custom SVG)

---

## ✨ Phase 3: Animation & Micro-interactions (3-4 Days)

### 3.1 Staggered List Animations
- [ ] Add `flutter_animate` dependency
- [ ] Animate `TransactionTile` list: `fadeIn` + `slideX` with 100ms stagger
- [ ] Animate dashboard cards: `fadeIn` + `scale` entrance
- [ ] Animate bottom nav tab switch: cross-fade (no slide)

### 3.2 Balance Count-up Animation
- [ ] Add `animated_text_kit` or custom `TweenAnimationBuilder`
- [ ] Animate balance on dashboard load: 0 → actual (800ms, easeOutCubic)
- [ ] Animate balance change after transaction: old → new (600ms)

### 3.3 Success/Error Micro-interactions
- [ ] Success: Lottie/Rive checkmark animation (400ms) on transfer complete
- [ ] Error: Horizontal shake (80ms × 3) on PIN field / validation error
- [ ] Loading: Skeleton shimmer on all async screens (already have `Skeleton` widget)
- [ ] Button states: Pressed scale (0.98), loading spinner, success checkmark

### 3.4 Page Transitions Polish
- [ ] Tab switches: 200ms cross-fade (Material 3 spec)
- [ ] Deep nav: 280ms shared-axis horizontal
- [ ] Modals/Sheets: 200ms scale + fade from center
- [ ] Full-screen auth: 280ms fade + slide up

---

## 🧪 Phase 4: Testing Infrastructure (2-3 Days)

### 4.1 Unit/Widget Tests
- [ ] `BalanceCard` - golden test (light/dark, frozen, actions)
- [ ] `TransactionTile` - golden test (incoming/outgoing/pending/failed)
- [ ] `PinPad` - widget test (input, clear, biometric, error shake)
- [ ] `PillButton` - states: default, loading, disabled, pressed
- [ ] `InputField` - validation, obscure toggle, error states
- [ ] `WalletNotifier` - provider test (load, refresh, error)
- [ ] `AuthNotifier` - provider test (login, logout, PIN, restore)

### 4.2 Integration Tests (Patrol/Flutter Driver)
- [ ] **Critical Path 1:** Cold start → Splash → Login → PIN → Dashboard
- [ ] **Critical Path 2:** Dashboard → Send → Amount → Recipient → PIN → Success
- [ ] **Critical Path 3:** Dashboard → Cards → Freeze/Unfreeze card
- [ ] **Critical Path 4:** Profile → Settings → Dark mode toggle → Persist
- [ ] **Critical Path 5:** KYC flow (if implemented)

### 4.3 E2E Tests (Playwright - Optional)
- [ ] Set up Playwright with Flutter web build
- [ ] Run critical paths in CI

### 4.4 CI/CD Integration
- [ ] Add `flutter analyze` + `flutter test` to GitHub Actions
- [ ] Add golden test comparison in CI
- [ ] Block merge on test failures

---

## ♿ Phase 5: Accessibility & Performance (1-2 Days)

### 5.1 Accessibility Audit
- [ ] Add `Semantics` labels to all icon-only buttons
- [ ] Ensure `excludeSemantics` on decorative icons
- [ ] Verify 4.5:1 contrast ratios (light & dark)
- [ ] Test with TalkBack/VoiceOver
- [ ] Support dynamic text sizes (no layout breakage)
- [ ] Add `prefersReducedMotion` media query handling
- [ ] Focus order logical on all screens

### 5.2 Performance
- [ ] Profile with `flutter run --profile` + DevTools
- [ ] Ensure 60fps on all transitions
- [ ] Optimize image assets (WebP, correct sizing)
- [ ] Lazy-load heavy screens (admin, charts)
- [ ] Cache network images with `cached_network_image`

### 5.3 Code Quality
- [ ] Enable stricter lints: `avoid_print`, `prefer_single_quotes`, `always_declare_return_types`
- [ ] Add `dart_code_metrics` for complexity analysis
- [ ] Document public APIs with `///` comments

---

## 📦 Dependencies to Add

```yaml
# pubspec.yaml additions
dependencies:
  flutter_animate: ^4.5.0        # Phase 3: Staggered animations
  animated_text_kit: ^4.2.2      # Phase 3: Balance count-up
  lottie: ^3.1.2                 # Phase 3: Success animations
  shared_preferences: ^2.3.2     # Phase 2: Theme persistence
  cached_network_image: ^3.4.0   # Phase 5: Image caching
  flutter_svg: ^2.0.10           # Phase 2: Empty state illustrations

dev_dependencies:
  patrol: ^2.0.0                 # Phase 4: Integration tests
  golden_toolkit: ^0.15.0        # Phase 4: Golden tests
  dart_code_metrics: ^5.7.0      # Phase 5: Code quality
```

---

## 📊 Success Metrics

| Metric | Target |
|--------|--------|
| **Build** | `flutter analyze` = 0 issues |
| **Tests** | 80%+ coverage, all critical paths covered |
| **Performance** | 60fps sustained, <16ms frame time |
| **Accessibility** | WCAG 2.1 AA compliant |
| **Dark Mode** | Full support, system preference respected |
| **Animations** | All transitions <300ms, respect reduced motion |
| **Haptics** | Feedback on all primary actions |

---

## 🗓️ Timeline

| Phase | Duration | Start | End |
|-------|----------|-------|-----|
| 1: Build Fixes | 30 min | Day 0 | Day 0 |
| 2: Core Polish | 2-3 days | Day 0 | Day 3 |
| 3: Animations | 3-4 days | Day 3 | Day 7 |
| 4: Testing | 2-3 days | Day 7 | Day 10 |
| 5: A11y/Perf | 1-2 days | Day 10 | Day 12 |
| **Total** | **~12 days** | | |

---

## 📝 Notes

- **Design System:** Already excellent - three-layer tokens, Material 3, semantic colors
- **Navigation:** GoRouter + ShellRoute is production-ready pattern
- **State:** Riverpod AsyncNotifier handles loading/error well
- **Backend:** Spring Boot API with JWT auth, proper endpoints
- **Risk:** Phase 1 must complete before any other work

---

## 🚀 Next Steps

1. ✅ **Complete Phase 1** - Fix all 44 build issues *(DONE 2026-08-21)*
2. ✅ Run `flutter analyze` to verify clean build *(DONE — 0 issues)*
3. ⬜ Run `flutter test` to ensure no regressions *(⚠️ NOT VERIFIED — timed out, retry first)*
4. ⬜ Begin Phase 2 with Dark Mode implementation

---

**Session handoff:** See `HANDOFF_2026-08-21.md` for full context, key technical knowledge, and next-session instructions.

*Plan created using ui-ux-pro-max, design-system, e2e-testing skills analysis*