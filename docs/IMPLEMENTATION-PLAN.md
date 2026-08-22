# NovaWallet UI/UX Implementation Plan

## Overview

This plan transforms NovaWallet from a strong foundational design system (8/10 fidelity) into a production-grade fintech app that meets banking/financial industry standards. The plan is designed for a solo developer or small team and assumes Phase 1 of the Fintech Polish Plan (44→0 analyzer issues) is already complete.

**Prerequisites:**
- ✅ Phase 1 Fintech Polish Plan complete (44→0 analyzer issues)
- ⚠️ Phases 2-5 of Fintech Polish Plan still pending (typography, spacing, component, accessibility audits)
- ✅ Design token system established (`design_tokens.dart`, `nova_colors.dart`)
- ✅ Flutter SDK ^3.12.2 environment ready

**Timeline:** 6 weeks (solo developer) or 3 weeks (2-person team)

**Success Criteria:**
- 100% design token adoption (zero hardcoded colors)
- WCAG 2.1 AA compliance
- 80%+ widget test coverage
- Skeleton screens for all data-loading screens
- Transaction confirmation flow implemented

---

## Phase 1: Foundation Fixes (Week 1-2)

**Goal:** Restore design system integrity and fix critical accessibility gaps.

### Week 1: Dashboard Token Compliance

| Task | File | Effort | Verification |
|------|------|--------|--------------|
| Audit all hardcoded colors in dashboard | `dashboard_screen.dart` | 1 hour | grep for `Color(0x` in file |
| Replace hardcoded primary colors with `NovaColors.primary` | `dashboard_screen.dart` | 2 hours | Visual diff, no color changes |
| Replace hardcoded secondary colors with `NovaColors.secondary` | `dashboard_screen.dart` | 2 hours | Visual diff |
| Replace hardcoded background colors with `DesignTokens.background` | `dashboard_screen.dart` | 1 hour | Visual diff |
| Replace hardcoded text colors with `NovaColors.textPrimary/Secondary` | `dashboard_screen.dart` | 2 hours | Visual diff |
| Verify contrast ratios for all text elements | `dashboard_screen.dart` | 1 hour | Use contrast checker tool |

**Deliverable:** Dashboard uses 100% design tokens, zero hardcoded colors.

### Week 2: Accessibility Foundation

| Task | File | Effort | Verification |
|------|------|--------|--------------|
| Add `Semantics` labels to all buttons | All screen files | 3 hours | Screen reader test |
| Add `Semantics` labels to all input fields | All screen files | 2 hours | Screen reader test |
| Add `Semantics` labels to all interactive icons | All screen files | 2 hours | Screen reader test |
| Verify 48dp minimum hit targets on all buttons | All screen files | 2 hours | Manual measurement |
| Add `Semantics` properties to transaction list items | Transaction screens | 2 hours | Screen reader test |

**Deliverable:** All interactive elements have accessibility labels.

**Phase 1 Exit Criteria:**
- `grep -r "Color(0x" lib/` returns zero results
- All buttons have `Semantics` labels
- Screen reader can navigate dashboard

---

## Phase 2: Fintech UX Patterns (Week 3-4)

**Goal:** Add industry-standard fintech patterns for trust, security, and usability.

### Week 3: Trust Signals & Confirmation

| Task | File | Effort | Verification |
|------|------|--------|--------------|
| Add "Last login" indicator to dashboard | `dashboard_screen.dart` | 3 hours | Visual verification |
| Add security badge to dashboard header | `dashboard_screen.dart` | 2 hours | Visual verification |
| Create transaction confirmation screen | New: `confirmation_screen.dart` | 4 hours | Manual test with send flow |
| Add biometric authentication prompt for transfers | Auth flow files | 3 hours | Test with fingerprint |

**Deliverable:** Dashboard shows trust indicators, transfers require confirmation.

### Week 4: Balance & Transaction UX

| Task | File | Effort | Verification |
|------|------|--------|--------------|
| Add hide/reveal balance toggle (eye icon) | `dashboard_screen.dart` | 3 hours | Toggle test |
| Add haptic feedback on balance toggle | `dashboard_screen.dart` | 1 hour | Device test |
| Add "Last updated" timestamp to balance | `dashboard_screen.dart` | 2 hours | Visual verification |
| Add transaction receipt screen on tap | New: `receipt_screen.dart` | 4 hours | Tap transaction, view receipt |
| Add transaction search bar | `transaction_list.dart` | 3 hours | Search test |

**Deliverable:** Balance is hideable, transactions are searchable and tappable.

**Phase 2 Exit Criteria:**
- Balance toggle works with haptic feedback
- Transaction confirmation shows before send
- Transaction receipt is viewable on tap

---

## Phase 3: Loading & Error States (Week 5)

**Goal:** Standardize all loading and error states for perceived performance and reliability.

### Skeleton Screens

| Task | File | Effort | Verification |
|------|------|--------|--------------|
| Create skeleton screen base component | New: `skeleton_loader.dart` | 3 hours | Component renders correctly |
| Add skeleton to dashboard while loading | `dashboard_screen.dart` | 2 hours | Slow network test |
| Add skeleton to transaction list | `transaction_list.dart` | 2 hours | Slow network test |
| Add skeleton to profile screen | `profile_screen.dart` | 2 hours | Slow network test |

### Error States

| Task | File | Effort | Verification |
|------|------|--------|--------------|
| Create error state component with retry | New: `error_state.dart` | 3 hours | Component renders correctly |
| Add error state to dashboard | `dashboard_screen.dart` | 2 hours | Kill backend, show error |
| Add error state to transaction list | `transaction_list.dart` | 2 hours | Kill backend, show error |
| Create offline indicator widget | New: `offline_indicator.dart` | 2 hours | Airplane mode test |

**Deliverable:** All screens show skeletons while loading, specific errors with retry, and offline indicator.

**Phase 3 Exit Criteria:**
- Skeleton screens appear during data fetch
- Error states show specific messages with retry
- Offline indicator appears when no network

---

## Phase 4: Polish & Testing (Week 6)

**Goal:** Final polish, animation, and test coverage.

### Animations & Polish

| Task | File | Effort | Verification |
|------|------|--------|--------------|
| Add Hero animation to balance on screen transition | `dashboard_screen.dart` | 3 hours | Visual test |
| Add custom page transitions (slide up) | Navigation files | 4 hours | Visual test |
| Add micro-interactions (button press scale) | Component files | 3 hours | Visual test |

### Test Coverage

| Task | File | Effort | Verification |
|------|------|--------|--------------|
| Write widget test for dashboard screen | New: `dashboard_test.dart` | 4 hours | `flutter test` passes |
| Write widget test for transaction list | New: `transaction_list_test.dart` | 3 hours | `flutter test` passes |
| Write widget test for confirmation screen | New: `confirmation_test.dart` | 3 hours | `flutter test` passes |
| Write widget test for balance toggle | New: `balance_toggle_test.dart` | 2 hours | `flutter test` passes |
| Verify 80%+ coverage | — | 1 hour | `flutter test --coverage` |

**Deliverable:** Animations added, 80%+ test coverage achieved.

**Phase 4 Exit Criteria:**
- `flutter test --coverage` shows 80%+
- All widget tests pass
- Animations are smooth (60fps)

---

## Dependencies

```
Phase 1 (Foundation)
    ↓
Phase 2 (Fintech UX)
    ↓
Phase 3 (Loading/Error)
    ↓
Phase 4 (Polish/Testing)
```

**External Dependencies:**
- Fintech Polish Phases 2-5 should run in parallel for typography/spacing/component audits
- Backend API must support transaction confirmation endpoint
- Biometric auth API must be configured in Flutter

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Dashboard refactor breaks layout | Medium | High | Take screenshots before/after, visual diff |
| Accessibility changes affect screen readers | Medium | Medium | Test with TalkBack (Android) and VoiceOver (iOS) |
| Skeleton screens add complexity | Low | Low | Start with one screen, extract pattern |
| Test coverage target not met | Medium | Medium | Focus on critical paths first |
| Animation affects performance | Low | Low | Profile on low-end devices |

**Rollback Plan:**
- Each phase creates a git branch
- Merge only after visual verification
- Keep Phase 1 changes minimal and isolated
- Use feature flags for new UX patterns

---

## Success Metrics

| Metric | Current | Target | How to Measure |
|--------|---------|--------|----------------|
| `flutter analyze` issues | 0 | 0 | `flutter analyze` |
| Hardcoded colors | ~12 | 0 | `grep -r "Color(0x" lib/` |
| Widget test coverage | 0% | 80%+ | `flutter test --coverage` |
| Accessibility labels | Partial | 100% | Manual screen reader test |
| Skeleton screens | 0 | 4 screens | Visual verification |
| Transaction confirmation | No | Yes | Manual test |
| Balance toggle | No | Yes | Manual test |

---

## Appendix: File Changes Summary

### New Files to Create
- `lib/features/confirmation/screens/confirmation_screen.dart`
- `lib/features/receipt/screens/receipt_screen.dart`
- `lib/core/widgets/skeleton_loader.dart`
- `lib/core/widgets/error_state.dart`
- `lib/core/widgets/offline_indicator.dart`
- `test/features/wallet/screens/dashboard_test.dart`
- `test/features/transaction/screens/transaction_list_test.dart`
- `test/features/confirmation/screens/confirmation_test.dart`
- `test/core/widgets/balance_toggle_test.dart`

### Existing Files to Modify
- `lib/features/wallet/screens/dashboard_screen.dart` (tokens, accessibility, skeletons)
- `lib/features/transaction/screens/transaction_list.dart` (skeleton, error, search)
- `lib/features/profile/screens/profile_screen.dart` (skeleton, error)
- `lib/core/theme/design_tokens.dart` (possibly extend spacing)
- `lib/core/theme/nova_colors.dart` (possibly extend palette)

### Files NOT to Modify
- `lib/core/theme/app_theme.dart` — already well-structured
- `lib/core/theme/typography.dart` — typography scale is correct
- `pubspec.yaml` — no new dependencies needed for Phase 1-3
