# NovaWallet Visual Polish & Animation Plan

## Goal
Transform NovaWallet from a functional prototype into a polished, beautiful fintech app with smooth animations and visual delight.

## Priority Order
1. **Page transitions** (highest impact, lowest effort)
2. **Button/CTA feedback** (tap animations, loading states)
3. **Card animations** (balance card, transaction tiles)
4. **Navigation polish** (tab transitions, bottom nav indicators)
5. **Loading/skeleton states** (perceived performance)
6. **Dark mode foundation** (theme tokens ready, need implementation)

---

## Phase 1: Page Transitions (Day 1)

### 1.1 Custom Page Route Builder
**File:** `novawallet-app/lib/core/transitions/page_transitions.dart`

Create a reusable `AppPageRoute` class with:
- `slideFromRight` — default for forward navigation
- `slideUp` — for modals and bottom sheets
- `fadeIn` — for subtle transitions
- `sharedAxis` — for tab-level transitions (Material 3 motion)

### 1.2 Router Integration
**File:** `novawallet-app/lib/app/router.dart`

Wrap all routes with custom transitions:
- Auth screens: `slideUp` (modals)
- Feature screens: `slideFromRight`
- Tab switches: `sharedAxis` (horizontal)

---

## Phase 2: Button & CTA Animations (Day 1)

### 2.1 PillButton Enhancement
**File:** `novawallet-app/lib/core/widgets/pill_button.dart`

Add:
- `AnimatedScale` on tap (0.95 → 1.0)
- `AnimatedContainer` for loading state
- Haptic feedback on press (`HapticFeedback.lightImpact()`)
- Smooth loading spinner transition

### 2.2 Balance Card CTAs
**File:** `novawallet-app/lib/core/widgets/widgets.dart`

- Deposit/Withdraw buttons scale on tap
- Ripple effect on tap

---

## Phase 3: Card & List Animations (Day 2)

### 3.1 Balance Card Hero Animation
**File:** `novawallet-app/lib/core/widgets/widgets.dart`

- `AnimatedBuilder` for gradient shimmer on load
- Smooth balance counter animation (count up effect)
- Subtle parallax on scroll

### 3.2 Transaction List Items
**File:** `novawallet-app/lib/core/widgets/widgets.dart`

- `AnimatedList` for staggered entry
- Slide-in from bottom with fade
- Dismissible with slide-to-delete

### 3.3 Quick Actions
**File:** `novawallet-app/lib/features/wallet/screens/dashboard_screen.dart`

- Staggered scale-in animation on load
- Bounce effect on tap

---

## Phase 4: Navigation Polish (Day 2)

### 4.1 Bottom Navigation
**File:** `novawallet-app/lib/app/main_shell.dart`

- Animated indicator (sliding dot/bar)
- Icon morphing on selection
- Badge pulse animation

### 4.2 App Bar Transitions
- Title fade between screens
- Back button scale animation

---

## Phase 5: Loading & Skeleton States (Day 3)

### 5.1 Skeleton Loaders
**File:** `novawallet-app/lib/core/widgets/skeleton.dart`

Create `SkeletonLoader` widget:
- Shimmer effect (gradient animation)
- Placeholder shapes matching content
- Smooth fade to real content

### 5.2 Dashboard Skeleton
**File:** `novawallet-app/lib/features/wallet/screens/dashboard_screen.dart`

- Balance card skeleton
- Transaction list skeleton
- Quick actions skeleton

### 5.3 Pull-to-Refresh
- Custom animated refresh indicator
- Smooth content shift

---

## Phase 6: Dark Mode Foundation (Day 3)

### 6.1 Theme Toggle
**File:** `novawallet-app/lib/core/theme/app_theme.dart`

- Add `AppTheme.dark` with proper dark tokens
- `ThemeMode` state management

### 6.2 Dark Mode Colors
**File:** `novawallet-app/lib/core/theme/nova_colors.dart`

- Dark surface colors
- Dark primary/secondary variants
- Proper contrast ratios

---

## Implementation Rules

1. **No new packages** — use Flutter's built-in `AnimatedWidget`, `Tween`, `AnimationController`
2. **Performance first** — use `RepaintBoundary` where needed
3. **Accessibility** — respect `MediaQuery.disableAnimations`
4. **Consistency** — all animations use same duration curves (200-300ms)
5. **Tests** — add widget tests for animated components

---

## Files to Create/Modify

### New Files
- `lib/core/transitions/page_transitions.dart`
- `lib/core/widgets/skeleton.dart`

### Modified Files
- `lib/app/router.dart` — page transitions
- `lib/app/main_shell.dart` — nav animations
- `lib/core/widgets/pill_button.dart` — tap animations
- `lib/core/widgets/widgets.dart` — card/list animations
- `lib/features/wallet/screens/dashboard_screen.dart` — skeleton states
- `lib/core/theme/app_theme.dart` — dark mode
- `lib/core/theme/nova_colors.dart` — dark tokens
