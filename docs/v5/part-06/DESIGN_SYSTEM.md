# NovaWallet v5 "Horizon": Brand and design system

This pack holds every brand asset for the app, the admin console and the store listing. Screen-level component rules live in `docs/COMPONENTS.md` (in each screen Part); this file covers the brand itself.

## 1. Logo

The symbol is a **wallet with two cards peeking out and an N on the front pocket**. Nothing else is added to it (no star, snap or tab).

| File | Use |
|---|---|
| `logo/svg/mark_app.svg` | Gradient tile: app icon, social avatar, favicon |
| `logo/svg/mark_blue.svg` | Symbol on white or light backgrounds |
| `logo/svg/mark_white.svg` | Symbol on the blue hero or photos (N is cut out) |
| `logo/svg/mark_dark.svg`, `mark_light.svg` | Navy or white tile versions |
| `logo/svg/mark_mono.svg` | One-colour printing (receipts, stamps) |
| `logo/svg/logo_horizontal_on_light.svg` | Default lockup on light screens and documents |
| `logo/svg/logo_horizontal_on_dark.svg` | Dark mode and dark slides |
| `logo/svg/logo_horizontal_white.svg` | On the blue gradient |
| `logo/svg/logo_stacked_*.svg` | Splash, store screenshots, posters |

The wordmark is set in **Plus Jakarta Sans**, with "Nova" in ExtraBold (800) and "Wallet" in Medium (500) and no space between the words. Always use the supplied files rather than retyping it.

**Clear space:** keep at least the height of the N clear on every side of the logo.

**Minimum size:** symbol 20 px tall on screen (16 px for the favicon); horizontal lockup 96 px wide.

**Don'ts:** don't recolour the cards, rotate or stretch the logo, add shadows or outlines, put the blue symbol on the blue hero (use white), or add the star or snap back.

## 2. App icons

| Platform | Files | Notes |
|---|---|---|
| iOS | `app-icon/ios/AppIcon_1024.png` | Full-bleed square with no transparency; iOS rounds the corners. |
| Google Play | `app-icon/play-store/play_store_512.png` | 512 × 512, 32-bit PNG. |
| Android adaptive | `app-icon/android/ic_launcher_background*`, `ic_launcher_foreground*`, `ic_launcher_monochrome*` | 108 dp canvas with the symbol inside the 66 dp safe zone. Monochrome is used for Android 13+ themed icons. |
| Android legacy | `app-icon/android/legacy/mipmap-*_ic_launcher.png` | For Android 7 and older. |
| Notification | `app-icon/android/notification/drawable-*_ic_stat_novawallet.png` | White silhouette on transparent; Android tints it. Set the accent colour to #2F5BF5. |
| Web / admin | `app-icon/web/favicon.svg`, `icon_16…512.png` | Favicon, PWA manifest and Apple touch icon (180). |
| Native splash | `splash/splash_logo_white_1152.png` + background #2F5BF5 (or `splash_background_*.png`) | For `flutter_native_splash`. On Android 12+ the icon sits in a 288 dp circle; it is already padded to fit. |

## 3. Colour

The full token list (light and dark) is in `tokens.json` and in COMPONENTS.md §1.

- **One accent:** Nova blue #2F5BF5 in light mode, #7896FF in dark mode.
- **Status:** success green, warning amber, danger red and info blue appear only to carry meaning.
- **Gold** #F2B544 is for rewards, coins and the second card in the logo.
- **Hero gradient:** #4C7BFF → #2F5BF5 → #1A2F9E → #0B1435, diagonal. Use it only on the hero header, the splash, the virtual card and promo cards.
- **Categories:** each category has one colour (see `category-tiles/` and the `category` block in tokens.json), shown as a soft tint circle with a solid icon.

## 4. Typography

- **Headings and numbers:** Plus Jakarta Sans 700/800, letter spacing −2.5%.
- **Body:** Inter 400/500/600/700.
- The font files are in `fonts/`. Bundle them with the app; don't download them at runtime.
- **Money:** Plus Jakarta Sans 800 with tabular figures, written `K 1,250.00`.

## 5. Icons

There are 125 two-tone icons on a 24 × 24 grid.

- **Layers:** each icon has a back shape at 30% opacity, a solid front shape, and 2 px strokes with round caps.
- **`icons/svg/`:** uses `currentColor`. Tint it in Flutter with `ColorFilter`.
- **`icons/png_primary_72/`:** quick PNGs in Nova blue, for documents and slides.
- **Overview:** `icons/ICON_SHEET_light.png` and `_dark.png` show every icon with its name.
- **Sizes:** 24 for list rows and the tab bar (26), 20 for chevrons, 18 inside small pills.

## 6. Illustrations

26 calm 3D scenes: one object on a faint round backdrop with a soft shadow. Each comes in light and dark versions, as SVG and as PNG @4x.

- **Display size:** 230–300 px wide, centred.
- **Pairing:** never place two illustrations on one screen.
- **Overview:** `illustrations/ILLUSTRATION_SHEET_light.png` and `_dark.png`.

| Scene | Used on |
|---|---|
| welcome | A05 |
| onboarding_track, onboarding_budget, onboarding_save | A02–A04 |
| kyc_verify, kyc_approved | D01, D08 |
| payment_success, payment_pending, payment_failed | Result screens |
| approve_on_phone | F05 |
| no_network, server_error, maintenance, update_required | P01–P06 |
| empty_activity, empty_cards, empty_goals, empty_notifications, empty_search | Empty states |
| security | C05, C07 |
| referral | O09 |
| chilimba | M09 empty state |
| nova_ai | N01 empty state |
| digital_store | K05 |
| cards_waitlist | L01 |
| bills | K03 |

## 7. Voice and tone

- **Plain words, short sentences.** "Money added", not "Your transaction has been processed successfully."
- **Say what happens next,** especially when money is pending: "We'll tell you as soon as Airtel confirms."
- **Never blame the user.** Say what still works: "Your balance is safe."
- **Second person, warm, not cute.** No jokes on errors or money screens.
- **Kwacha format:** always `K 1,250.00`. Phone numbers are shown masked as `097 ••• 4812`.

## 8. Store listing (Google Play)

- **Icon:** `play_store_512.png`.
- **Feature graphic (1024 × 500):** the hero gradient with `logo_horizontal_white` on the left and a phone showing E02 Home on the right. Build it from the screen PNGs.
- **Screenshots:** use the light-mode PNGs of E02, F02, G03, M01, L03 and N01 with a short caption above each (for example "See where every kwacha goes").
