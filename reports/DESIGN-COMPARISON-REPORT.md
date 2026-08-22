# NovaWallet Design Comparison Report

**Date:** 2026-08-20  
**Purpose:** Compare Stitch reference designs against Flutter implementation for a fintech mobile app focused on online payments and banking.

---

## Executive Summary

| Metric | Status |
|--------|--------|
| **Overall Match** | ⚠️ **PARTIAL MATCH** — Core structure is correct, but several visual details diverge |
| **Color System** | ✅ Matches Stitch tokens exactly |
| **Typography** | ⚠️ Font family matches (Inter), but sizing/spacing has minor deviations |
| **Component Structure** | ✅ Core components (BalanceCard, TransactionTile, PillButton) match design intent |
| **Spacing & Layout** | ⚠️ Some padding/margin differences detected |
| **Missing Elements** | 🔴 Bottom navigation bar not in dashboard scaffold |
| **Accessibility** | ✅ PillButton has semantics; overall good foundation |

---

## Screen-by-Screen Analysis

### 1. Dashboard / Home Screen

**Stitch Reference:** `stitch_duplicate_of_novawallet/novawallet_home_dashboard/code.html`  
**Flutter Implementation:** `novawallet-app/lib/features/wallet/screens/dashboard_screen.dart`

#### ✅ What Matches

| Element | Stitch | Flutter | Status |
|---------|--------|---------|--------|
| Top bar (greeting + notifications + avatar) | "Hi, {name}" + bell icon + avatar | `_TopBar` with same layout | ✅ Match |
| Balance card gradient | Indigo gradient (`#3525CD` → `#4F46E5`) | `NovaColors.primary` → `NovaColors.primaryContainer` | ✅ Match |
| Balance amount display | Large bold amount | `fontSize: 28, fontWeight: w700` | ✅ Match |
| Masked account number | Pill badge with copy icon | `accountNumber` with copy icon | ✅ Match |
| Deposit/Withdraw buttons | Two CTAs on card | `PillButton` filled + outlined | ✅ Match |
| Quick actions section | 4 circular icons | `_QuickActions` with 4 items | ✅ Match |
| Transaction list | List with colored icons | `TransactionTile` with leading icon | ✅ Match |

#### ⚠️ What Differs

| Element | Stitch | Flutter | Gap |
|---------|--------|---------|-----|
| **Bottom navigation bar** | Fixed bottom nav with Home/Send/Cards/Profile | **Not present in DashboardScreen** | 🔴 **Missing** — needs `Scaffold.bottomNavigationBar` |
| Quick action icons | Material Symbols (home, send, credit_card, person) | Custom Icons (Icons.send, Icons.qr_code_scanner, etc.) | ⚠️ Icon mismatch — Stitch uses `phone_android` for Airtime, `receipt_long` for Bills |
| Quick action labels | "Send", "Request", "Airtime", "Bills" | Same labels | ✅ Match |
| Notification badge | Red dot (no count) | Count badge with "99+" overflow | ⚠️ Stitch shows dot only; Flutter shows count |
| Transaction amount color | Error red for outgoing | `NovaColors.error` for outgoing | ✅ Match |
| Section header | "Recent transactions" + "View all" | Same | ✅ Match |

#### 🔧 Recommended Fixes

1. **Add bottom navigation bar** to `DashboardScreen` or parent `Scaffold`
2. **Align quick action icons** with Stitch (use `Icons.phone_android` for Airtime)
3. **Simplify notification badge** to dot-only mode (or keep count as improvement)

---

### 2. Send Money Screen

**Stitch Reference:** `stitch_duplicate_of_novawallet/send_money_novawallet/code.html`  
**Flutter Implementation:** `novawallet-app/lib/features/wallet/screens/send_screen.dart`

#### ✅ What Matches

| Element | Stitch | Flutter | Status |
|---------|--------|---------|--------|
| Page title | "Send Money" | AppBar `title: 'Send money'` | ✅ Match |
| Recipient input | Labeled text field with person icon | `TextField` with `Icons.person_outline` | ✅ Match |
| Amount input | Large `ZMW` prefix + input | `AmountInput` widget with `prefixText: 'ZMW '` | ✅ Match |
| Quick amount chips | `ZMW 20`, `ZMW 50`, etc. | `quickAmounts: ['20', '50', '100', '200']` | ✅ Match |
| Description field | Optional textarea | `TextField` with `labelText: 'Description (optional)'` | ✅ Match |
| Continue button | Primary CTA | `PillButton(label: 'Continue')` | ✅ Match |

#### ⚠️ What Differs

| Element | Stitch | Flutter | Gap |
|---------|--------|---------|-----|
| **Balance display** | Shows current balance at top of form | **Not shown** | ⚠️ Missing context — user can't see if they have enough |
| **Fee disclosure** | Inline fee estimate before submit | Fee shown in PIN sheet only | ⚠️ UX gap — fee should be visible earlier |
| **Recipient clear button** | "X" button to clear recipient | Not implemented | ⚠️ Minor UX gap |
| **Form validation** | Real-time validation with error states | Only on-submit validation | ⚠️ Stitch shows inline errors |

#### 🔧 Recommended Fixes

1. **Add balance display** at top of send form
2. **Show fee estimate** inline after amount entry (fetch on amount change)
3. **Add recipient clear button** for better UX
4. **Add inline validation** for real-time error feedback

---

### 3. Cards Screen

**Stitch Reference:** `stitch_duplicate_of_novawallet/my_cards_novawallet/code.html`  
**Flutter Implementation:** `novawallet-app/lib/features/wallet/screens/cards_screen.dart`

#### ✅ What Matches

| Element | Stitch | Flutter | Status |
|---------|--------|---------|--------|
| Page title | "My Cards" | AppBar `title: 'My Cards'` | ✅ Match |
| Card carousel | Horizontal scrollable cards | `PageView.builder` with `viewportFraction: 0.88` | ✅ Match |
| Card gradient | Indigo/purple gradients | `_MockCard` with `startColor`/`endColor` | ✅ Match |
| Card elements | Chip, masked number, holder, expiry, brand | All present in `_GradientCard` | ✅ Match |
| Page dots | Animated indicator dots | `AnimatedContainer` dots | ✅ Match |
| Card actions | Freeze/View PIN/Settings | `_CardActions` with same 3 buttons | ✅ Match |
| Recent transactions | List below carousel | `_recentTransactions` with `TransactionTile` | ✅ Match |
| Add card CTA | "Add a new card" button | `PillButton(label: 'Add a new card')` | ✅ Match |

#### ⚠️ What Differs

| Element | Stitch | Flutter | Gap |
|---------|--------|---------|-----|
| **Card height** | ~200px visual height | `SizedBox(height: 210)` | ⚠️ Minor (10px) |
| **Frozen overlay** | Semi-transparent dark overlay with "FROZEN" badge | Same implementation | ✅ Match |
| **Card chip design** | Metallic chip with contact lines | `_CardChip` with `_ChipPainter` | ✅ Match |

#### 🔧 Status: **GOOD MATCH** — No critical fixes needed

---

### 4. Auth Screens (Login/Onboarding)

**Stitch References:** `novawallet_login/`, `novawallet_welcome_1/`, `novawallet_welcome_2/`  
**Flutter Implementation:** `novawallet-app/lib/features/auth/screens/`

#### ✅ What Matches (from report)

| Element | Status |
|---------|--------|
| Onboarding hero with trust indicators | ✅ Implemented |
| Page indicators | ✅ Implemented |
| Primary Get Started CTA | ✅ Implemented |
| Login form with pill inputs | ✅ Implemented |
| Password visibility toggle | ✅ Implemented |
| Remember Me checkbox | ✅ Implemented |
| Registration CTA | ✅ Implemented |

#### ⚠️ Known Issues (from report)

- **Remember-me row overflow** at narrow widths (fixed with `Flexible` + ellipsis)
- **Test viewport issues** (fixed with `ensureVisible` before tap)

---

## Design Token Compliance

### Color Tokens

| Token | Stitch Value | Flutter Value | Status |
|-------|--------------|---------------|--------|
| `primary` | `#3525CD` | `Color(0xFF3525CD)` | ✅ Exact match |
| `primaryContainer` | `#4F46E5` | `Color(0xFF4F46E5)` | ✅ Exact match |
| `error` | `#BA1A1A` | `Color(0xFFBA1A1A)` | ✅ Exact match |
| `background` | `#F8F9FF` | `Color(0xFFF8F9FF)` | ✅ Exact match |
| `onSurface` | `#0B1C30` | `Color(0xFF0B1C30)` | ✅ Exact match |

**Verdict:** ✅ All color tokens match Stitch exactly.

### Typography

| Element | Stitch | Flutter | Status |
|---------|--------|---------|--------|
| Font family | Inter | `GoogleFonts.interTextTheme()` | ✅ Match |
| Headline weight | 700 | `fontWeight: FontWeight.w700` | ✅ Match |
| Body size | 14px | `fontSize: 14` | ✅ Match |

**Verdict:** ✅ Typography matches design spec.

---

## Gaps Summary for Fintech App

### Critical Gaps (Must Fix)

| # | Gap | Impact | Effort |
|---|-----|--------|--------|
| 1 | **Bottom navigation bar missing** | Users can't navigate between Home/Send/Cards/Profile | Low — add to parent Scaffold |
| 2 | **Send screen lacks balance context** | Users can't see available balance before sending | Low — add `BalanceCard` or summary |
| 3 | **Fee not shown before PIN entry** | Surprise fees at confirmation step | Medium — add fee estimate on amount change |

### Important Gaps (Should Fix)

| # | Gap | Impact | Effort |
|---|-----|--------|--------|
| 4 | **Quick action icons don't match Stitch** | Visual inconsistency | Low — swap icon constants |
| 5 | **Notification badge shows count vs dot** | Minor visual difference | Low — add `dotMode` parameter |
| 6 | **Recipient clear button missing** | Minor UX friction | Low — add clear icon button |
| 7 | **Inline form validation missing** | Users only see errors on submit | Medium — add real-time validation |

### Nice-to-Have Gaps

| # | Gap | Impact | Effort |
|---|-----|--------|--------|
| 8 | **No error/empty states for cards** | Mock data always present | Low — already has `EmptyStateView` |
| 9 | **No dark mode** | Stitch has dark mode tokens ready | High — requires full theme system |
| 10 | **No haptic feedback** | Missing tactile confirmation | Low — add `HapticFeedback.lightImpact()` |

---

## Recommendations for Fintech UX

### 1. Add Bottom Navigation (Priority: HIGH)

The Stitch design includes a fixed bottom nav with 4 tabs:
- **Home** (active state with filled icon)
- **Send**
- **Cards**
- **Profile**

Implement this in the parent `Scaffold` that wraps the tab screens.

### 2. Enhance Send Money Flow (Priority: HIGH)

For a fintech app, the send money screen should:
- Show **available balance** at the top
- Display **fee estimate** in real-time as user types amount
- Show **total deduction** (amount + fee) before PIN entry
- Add **recipient recent contacts** for quick selection

### 3. Add Security Indicators (Priority: MEDIUM)

For user trust in a banking app:
- Show **SSL/security badge** in sensitive screens
- Add **transaction limits** display on send screen
- Show **2FA/PIN required** indicator before CTA

### 4. Improve Empty States (Priority: LOW)

The current empty states are good but could be more engaging:
- Add **illustrations** instead of just icons
- Include **onboarding tooltips** for first-time users
- Add **progressive disclosure** for complex features

---

## Next Steps

1. **Immediate:** Add bottom navigation bar to dashboard
2. **This week:** Enhance send money flow with balance + fee display
3. **Next week:** Add inline validation to all forms
4. **Future:** Implement dark mode using existing token foundation

---

## Appendix: Stitch Directory Structure

```
stitch_duplicate_of_novawallet/
├── novawallet_home_dashboard/     # Dashboard design
├── send_money_novawallet/         # Send money flow
├── my_cards_novawallet/           # Cards screen
├── novawallet_login/              # Login screen
├── novawallet_welcome_1/          # Onboarding step 1
├── novawallet_welcome_2/          # Onboarding step 2
├── novawallet_registration_1/     # Registration step 1
├── novawallet_registration_2/     # Registration step 2
├── novawallet_forgot_password/    # Forgot password
├── novawallet_reset_password/     # Reset password
├── transaction_history/           # Transaction list
├── transaction_details_sent/      # Sent transaction detail
├── transaction_details_received/  # Received transaction detail
├── profile_settings/              # Profile screen
├── security_center/               # Security settings
├── notifications/                 # Notifications screen
└── ... (51 total screens)
```

---

*Report generated by OpenCode orchestrator using `ui-ux-pro-max` and `design-system` skills.*
