# NovaWallet App — Screen Coverage Report

**Date:** 2026-08-06
**Source of truth (designs):** `.planning/prompts/` (39 numbered Stitch prompts) + `stitch_duplicate_of_novawallet/` (exported HTML/PNG)
**Source of truth (impl):** `novawallet-app/lib/features/**/screens/`

Summary: **27 Flutter screen files implemented**, covering **29 of 39** design prompts. The user-facing wallet core is effectively complete; the Admin console and several edge-state screens are still missing.

---

## ✅ Fully implemented (matches design)

| Design prompt | Flutter screen |
|---|---|
| 01-onboarding (+ 01-register) | `auth/screens/onboarding_screen.dart`, `register_screen.dart` |
| 02-login | `auth/screens/login_screen.dart` |
| 03-forgot-password | `auth/screens/forgot_password_screen.dart` |
| 04-reset-password | `auth/screens/reset_password_screen.dart` |
| 05-email-verify | `auth/screens/verify_email_screen.dart` |
| 06-set-pin | `auth/screens/set_pin_screen.dart` |
| 07-home-dashboard | `wallet/screens/dashboard_screen.dart` |
| 08-transaction-history | `transaction/screens/transaction_history_screen.dart` |
| 09-transaction-detail | `transaction/screens/transaction_detail_screen.dart` |
| 10-deposit | `wallet/screens/deposit_screen.dart` |
| 11-withdraw | `wallet/screens/withdraw_screen.dart` |
| 12-send-transfer | `wallet/screens/send_screen.dart` |
| 14-fee-estimate | `transaction/screens/fee_estimate_screen.dart` |
| 15-success | `transaction/screens/success_screen.dart` |
| 16-kyc-status | `kyc/screens/kyc_status_screen.dart` |
| 17-kyc-upload | `kyc/screens/kyc_upload_screen.dart` |
| 18-profile-settings | `profile/screens/profile_screen.dart` |
| 19-edit-profile | `profile/screens/edit_profile_screen.dart` |
| 21-admin-dashboard | `admin/screens/admin_dashboard_screen.dart` (KYC queue + approve/reject) |
| 26-notifications | `extras/screens/notifications_screen.dart` |
| 28-statements | `extras/screens/statements_screen.dart` |
| 33-security-center | `profile/screens/security_screen.dart` |
| 30-empty-state | shared `EmptyStateView` (in `core/widgets/widgets.dart`) |
| 31-error-state | shared `ErrorStateView` (in `core/widgets/widgets.dart`) |
| 32-loading-state | shared `LoadingView` (in `core/widgets/widgets.dart`) |
| 37-insufficient-balance | `transaction/screens/insufficient_balance_screen.dart` |

> Plus an extra screen not in the numbered set: `auth/screens/change_password_screen.dart` (reached from security center).

---

## ⚠️ Implemented but partial / folded in

| Design prompt | How handled |
|---|---|
| 13-transfer-confirm-PIN | Folded into `send_screen.dart` as a **PIN modal** (works, but not a standalone route) |
| 20-virtual-cards | `wallet/screens/cards_screen.dart` = **"coming soon"** placeholder (backend cards are V2 / post-MVP by design) |
| 34-fee-disclosure | Merged into `fee_estimate_screen.dart` |
| 35-wallet-frozen | Shown as FROZEN **badge** on dashboard `BalanceCard` (`frozen: w.isFrozen`), no dedicated screen |
| 36-transaction-detail-sent | Handled as a variant inside `transaction_detail_screen.dart` |

---

## ❌ NOT implemented (gaps vs. designs)

| Design prompt | What is missing | Priority |
|---|---|---|
| 22-admin-kyc-review | Full KYC **review** screen (queue is shown inline on dashboard) — **design exists** at `stitch_duplicate_of_novawallet/kyc_review_admin_console/` (HTML + PNG ready to port) | HIGH |
| 23-admin-users | Admin **users management** screen | MED |
| 24-admin-transactions | Admin **transactions / search** screen | MED |
| 25-admin-fees | Admin **fee configuration** screen | MED |
| 27-transaction-filters | Transaction **filter** UI (also the one Stitch generation that failed) | LOW |
| 29-buy-airtime | **Buy airtime** screen — only a placeholder quick-action button | MED |
| 38-session-expired | **Session expired** screen | LOW |
| 39-pin-lockout | **PIN lockout** screen (only commented in `pin_pad.dart`) | LOW |

---

## Navigation / wiring status
- All implemented screens are registered in feature `routes.dart` files and reachable.
- 4-tab `MainShell` (Home / Send / Cards / Profile) wires the primary navigation.
- Router gate (`app/router.dart`) handles splash → onboarding → login → PIN-set redirects.
- Bug fixed this session: send/withdraw pushed `/states/insufficient` (nonexistent) → corrected to `/insufficient`.
- `flutter analyze` → **No issues found.**

## Recommended next steps
1. Build **22-admin-kyc-review** (design already exported — port HTML → Flutter).
2. Build admin **users / transactions / fees** (23–25) to complete the admin console.
3. Build **buy-airtime (29)** and edge-state screens **session-expired (38) / pin-lockout (39)**.
4. Re-generate + build **transaction-filters (27)**.
