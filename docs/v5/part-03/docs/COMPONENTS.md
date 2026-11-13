# NovaWallet v5 "Horizon": Shared Components (minimal revision)

Every screen's `BUILD.md` names components from this file. Build these first, in `lib/core/ui/`, before any screen. Each component reads colours from the theme only. No colour hex values inside screens.

Canvas used in the designs: **390 × 844 logical px** (iPhone 14 / mid-size Android). Side padding is **20**. All sizes below are logical pixels (Flutter `double`), so they map 1:1.

---

## 1. Theme tokens (put in `AppColors` light + dark, exposed through a `ThemeExtension`)

| Token | Light | Dark | Use |
|---|---|---|---|
| bg | #F3F5FB | #060A18 | Scaffold background |
| surface | #FFFFFF | #0E1430 | Cards, sheets, tab bar |
| surface2 | #ECF0F8 | #151D3D | Filled inputs, keypad keys, OTP boxes |
| surface3 | #DFE5F2 | #1E2850 | Inactive progress, toggles off |
| border | #E3E8F3 | #1F2A52 | Dividers |
| borderStrong | #C5CEE2 | #33427A | Outline buttons, empty radio |
| text1 | #0A1230 | #F2F5FF | Headings, values |
| text2 | #4A5573 | #AEB8D8 | Body |
| text3 | #5F6A88 | #8592BA | Captions, labels |
| primary | #2F5BF5 | #7896FF | Buttons, focus, active |
| textOnPrimary | #FFFFFF | #050A1C | Text on primary buttons |
| primarySoft / primarySoftText | #E6ECFF / #2146D1 | #1A2659 / #B3C3FF | Tonal buttons, active tab pill |
| gold / goldText / goldSoft | #F2B544 / #8A5800 / #FFF2D8 | #F5C062 / #F5C062 / #33280F | Rewards, coins, cards |
| success / successSoft | #0B7A55 / #DEF5EC | #4BD69C / #0E2E24 | Money in, done |
| warning / warningSoft | #8F5300 / #FFF0D6 | #F6B64A / #33250B | Pending, limits |
| danger / dangerSoft | #C0263B / #FDE6E9 | #FF7D8F / #3A1622 | Errors, failed |
| info / infoSoft | #1F5FA8 / #E3EEFB | #7CB8F7 / #102A47 | Tips, notices |
| hero gradient | #4C7BFF → #2F5BF5 → #1A2F9E → #0B1435 | #4C7BFF → #2C4FE0 → #16258A → #070C24 | Diagonal top-left → bottom-right |

All text/background pairs above pass WCAG AA (4.5:1) for body text.

## 2. Type

- **Headings and numbers:** Plus Jakarta Sans (Google Fonts), weights 700/800, letter spacing −2.5% of size.
- **Body:** Inter, 400/500/600/700.
- Bundle both fonts as assets (no runtime download; students may be offline).
- Scale: Display 34/800 · H1 30/800 · H2 25/800 · H3 18/800 · Body L 16/600 · Body 15/400 (line height 22–23) · Caption 13 · Label 12.5/600 · Overline 11.5/700 uppercase, spacing 0.8.
- Money: Plus Jakarta Sans 800, tabular figures (`FontFeature.tabularFigures()`), always written `K 1,250.00` with a space after K.
- Respect system text scaling up to 1.3×. Layouts must not clip at 1.3×.

## 3. Shape, elevation, spacing

- Radius: buttons and pills fully rounded; inputs 18; cards 24; hero header bottom corners 36–40; sheets 34 top corners.
- Shadows (light only): `sh1` soft card shadow (y 4, blur 16, primary-navy 6%); `sh2` lifted (y 8, blur 24, 12%); `sh3` sheet (y −8, blur 32, 18%). In dark mode, replace shadows with a 1px white border at 5% opacity.
- Spacing scale: 4, 8, 12, 16, 20, 24, 32, 40.

## 4. Components

| Widget | Look | Behaviour |
|---|---|---|
| **NwStatusBarStyle** | Dark icons on light screens; white icons on hero/blue screens. | Set per screen with `AnnotatedRegion<SystemUiOverlayStyle>`. |
| **NwTopBar** | 44 circle icon button left (back or close, on surface colour), centered title 17/700, optional 44 circle action right. | Back pops; close returns to the flow's start. |
| **NwButton** | Height 56, pill. Kinds: primary (with sh2 shadow), tonal, outline (1.5 borderStrong), danger, dangerTonal, ghost (text only), white (on hero), glass (white 16% on hero). Optional 22 icon before the label. Label 16/700 heading font. | Pressed: scale 0.98 + primaryPressed. Loading: label swaps for a 20 spinner, button stays same width. Disabled: 40% opacity. Haptic light tap. |
| **NwBottomCta** | Full-width NwButton pinned 98 from bottom (above the home indicator); stacked pair puts primary at 168 from bottom, secondary below. | Lifts above the keyboard. |
| **NwField** (filled) | Height 64, radius 18, surface2. Label 12.5/600 inside top-left, value 16/600 below. Optional 22 leading icon, optional trailing icon (eye, chevron). | Focus: 2px primary border, label and icon turn primary, caret primary. Error: 2px danger border + 16 warning icon + 12.5 message below. |
| **NwOtpBoxes** | 6 boxes, gap 10, height 64, radius 18, surface2. Digit 26/800. | Active box gets 2px primary border and blinking caret. Auto-fill from SMS (Android SMS Retriever / iOS one-time-code). Paste fills all. |
| **NwKeypad** | 3 × 4 grid, row height 66–84. Keys are 64–70 circles in surface2 with 27/600 digits. Bottom-left optional (fingerprint, “.”, blank); bottom-right backspace. On blue screens: white 12% circles, white digits. | Haptic per press. Long-press backspace clears. Custom keypad (not system keyboard) for PIN, amounts, phone. |
| **NwPinDots** | 4 dots, 32 apart. Filled 9 radius primary; empty 8 radius 2px outline. Error turns all dots danger. | Error: horizontal shake 300 ms + heavy haptic, then clear. |
| **NwStepProgress** | Segmented bar, 5px high, gap 6. Below: “Step X of N” 12.5 text3 left, step name 12.5/700 primary right. | Animates fill 250 ms. |
| **NwCard** | Surface, radius 24, sh1 (light) or 5% border (dark). | — |
| **NwListGroup / NwRow** | Card holding rows of height 52–66: plain 24 icon in primary at 18 from the left (**no circle behind it**), title 15.5/600 at 56, optional subtitle 13 text3, trailing: chevron, toggle, status as coloured text 13.5/700 (success, warning, primary or text3), or value + chevron. Divider starts at the text. | Whole row tappable, ripple clipped to card. |
| **NwBubble** | Used sparingly (transaction rows, status cards). Circle in surface2 with a primary icon at 50% size; only success, warning, danger and gold use their soft tint. Where a BUILD.md says “bubble” inside a list group, use the plain NwRow icon instead. |
| **NwCategoryTile** | Circle filled with the category colour at 12% (light) / 18% (dark) and a solid icon in the same colour. One colour per category, no gradient, no gloss. Colours: food orange #F2683C/#FF9F45, transport blue #2F86F5/#5CC8FF, airtime/data/TV violet #6B4FE8/#A992FF, school indigo #2F49D6/#5B87FF, shopping/fun pink #E04A7A/#FF8DB0, bills/electricity/housing amber #E9962A/#FFC963, health/savings green #14A37C/#5FE0B5, other grey #7D89A8/#B7C1DA (light/dark). | — |
| **NwPill** (status) | **No background.** 7px dot + 12.5/700 text in the status colour (success, warning, danger, primary, info, gold; neutral uses text3). On hero/gradient surfaces use white text with a white 16% background (“glass” pill). | Not tappable unless stated. |
| **NwChips** | Height 38 pills. Selected: text1 fill, bg-coloured text. Unselected: surface fill, text2. | Single select. |
| **NwBanner** | Radius 20, soft tint background, 22 icon, 14 text, wraps to multiple lines. Kinds: info, warning, danger, success. | — |
| **NwToggle** | 50 × 30 track, 24 thumb. | 200 ms slide. |
| **NwHeroHeader** | Hero gradient, bottom corners 36–40, one soft glow at the top-right. No rings or other decoration. | Status bar icons white. |
| **NwKeyValueCard** | Card with rows of 46: key 14.5 text3 left, value 15/600 right. Optional dashed divider + bold total row (value 19/800). | Long values ellipsize in the middle. |
| **NwTxRow** | 46 bubble or category tile, title 15.5/600, sub 13 text3, amount right 15.5/700 (green with “+” for money in), optional status text under amount (Pending warning, Failed danger). | Tap opens transaction detail. |
| **NwTabBar** | Surface, 86 high including home indicator, 1px top border, no shadow. Five equal tabs: Home, Payments, Cards, Insights, Me. Every tab shows a 26 icon with an 11.5 label under it. Active: icon and label in primary, label 700. Inactive: icon text3, label text2. | Colour cross-fades 150 ms. Re-tap scrolls to top. |
| **NwBottomSheet** | Surface, top radius 34, 44 × 5 handle, scrim (navy 45% light / black 65% dark). | Drag to dismiss. |
| **NwAvatar** | Circle, initials 80% of radius, tones primary/mint/gold/info. White variant on hero. | — |
| **NwProviderBadge** | Circle with **text only**: “MTN”, “AIR”, “ZAM” on surface2 with outline. NovaWallet = primary circle with white logo. Bank = info bubble. Card = gold bubble. | **Never use MTN/Airtel/Zamtel logos** without written permission. |
| **NwIllustration** | Calm 3D object on a faint round backdrop with one soft floor shadow (no podium, no sparkles). Width 230–300, centred. In result and system screens the illustration + text block is centred vertically between the top bar and the buttons. | Fade + 8px rise on entry 300 ms. |
| **NwVirtualCard** | Ratio 1 : 0.63, radius 22, gradient (blue, midnight, gold, sky), gloss top half, logo + “NovaWallet”, chip, contactless, masked number, name. Frozen: navy 62% overlay with snowflake and “Card frozen”. | Number reveal requires PIN/biometric; auto-hide after 30 s. |
| **NwSkeleton** | surface2 blocks with shimmer (surface3 sweep, 1.2 s). | Shown while loading; never a full-screen spinner. |

## 5. Icons and illustrations

- Icons: the v5 two-tone set (SVG). Use `flutter_svg`, size 22–24 by default, coloured via `ColorFilter` with the token colour. Icon names in BUILD.md files match the SVG file names.
- Illustrations: SVG files from the v5 brand pack, light and dark versions. Choose by theme brightness.

## 6. Motion

- Page transitions: shared-axis horizontal (300 ms, emphasized easing) inside flows; fade-through between tabs.
- Content entry: fade + 8px rise, 50 ms stagger per item, max 6 items.
- Success: Lottie `success.json` once, then haptic success.
- Respect “Reduce motion”: replace all of the above with 150 ms fades.

## 7. Rules that apply to every screen

1. Money is never shown as “sent” or “successful” until the backend confirms.
2. The app never asks for a mobile money PIN. The customer approves on their own phone.
3. Demo or sandbox money is labelled “Demo” on every balance.
4. Every tappable target is at least 44 × 44, with a semantic label for TalkBack/VoiceOver.
5. Every list has: loading (skeleton), empty, error (with retry) and offline states.
6. Screens with balances or card numbers set `FLAG_SECURE` (no screenshots, blurred in app switcher).

## 8. Minimal rules (revision 2)

1. One accent colour (blue). Other colours appear only for status (green, amber, red), rewards (gold) and spending categories.
2. No circle behind list icons. No gradients except the hero header, the virtual card and primary promo cards.
3. At most one banner per screen. Prefer coloured text over a pill for status inside lists.
4. Home shows only: balance, four actions, recent activity. Bill shortcuts live in the Payments tab.
5. Every tab has a label. One primary button per screen.
