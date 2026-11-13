# NovaWallet v5 — Motion guide

All Lottie files are in `lottie/`, with a GIF preview for each in `preview/`. Each file comes in a `_light` and a `_dark` version except the splash, which is drawn for the blue splash background. Pick the file that matches `Theme.of(context).brightness`.

Use the `lottie` Flutter package. Put the JSON files in `assets/lottie/` and list them in `pubspec.yaml`.

## 1. Lottie files

| File | Size | Length | Loop | Where it is used | What it does |
|---|---|---|---|---|---|
| `nw_splash_logo.json` | 300×300 | 1.6 s (96 f @60) | No | A01 Splash | Wallet body pops in, the two cards slide up out of it, then the N draws itself. Play once while the session restores; if loading finishes first, still let it reach the end (max 1.6 s). |
| `nw_success_{light,dark}.json` | 240×240 | 1.4 s | No | F06, G06, D08, K03, result screens | Soft circle grows, green disc pops, check draws, a ring ripples out. Fire `HapticFeedback.mediumImpact` at 0.45 s (when the check finishes). |
| `nw_failed_{light,dark}.json` | 240×240 | 1.2 s | No | F07, failed results | Red disc pops, the X draws in two strokes, the symbol shakes once. Heavy haptic at 0.75 s. |
| `nw_pending_loop_{light,dark}.json` | 240×240 | 2.0 s | Yes | F05, F08, I03, D07, P03 | Clock hand turns, an amber arc travels around the disc, the soft halo breathes. |
| `nw_loader_{light,dark}.json` | 160×160 | 1.5 s | Yes | Full-screen waits over 1 s (KYC upload, statement generation) | The two cards bob up and down behind the wallet. Show at 64–96 px. |
| `nw_pull_refresh_{light,dark}.json` | 96×96 | 0.8 s | Yes | Pull to refresh on Home, Activity, Notifications | Arc spinner (72% circle) turning. Show at 28–32 px. |
| `nw_money_in_{light,dark}.json` | 240×240 | 1.5 s | No | Top-up success header (optional), money received push opened | A gold coin drops into the wallet, the wallet squashes, a green + appears. |
| `nw_confetti_{light,dark}.json` | 320×320 | 1.7 s | No | Goal reached (M05), verified (D08) | Confetti bursts up and falls. Overlay full-width, `IgnorePointer`, play once per event, never on every open. |
| `nw_card_freeze_{light,dark}.json` | 200×200 | 1.2 s | No | L03 → L06 freeze | Blue disc pops, snowflake spins in, an ice ring ripples. |

Rules:

- Play once and then hold the last frame unless the table says loop.
- Never block a button while a one-shot animation plays.
- With **Reduce motion** on (system setting or O08 toggle): don't play Lottie; show the last frame (static) instead. For loops, show a static frame plus the text.
- Keep Lottie files under 40 KB each (they are much smaller now).

## 2. Motion done in code (no Lottie)

| Element | Spec |
|---|---|
| Page transitions inside a flow | Shared axis horizontal, 300 ms, `Curves.easeInOutCubicEmphasized` (Material 3 emphasized). |
| Switching tabs | Fade through, 200 ms. Tab colour cross-fade 150 ms. |
| Bottom sheets | Slide up 280 ms `easeOutCubic`; scrim fades to its opacity in 200 ms. Drag to dismiss. |
| Buttons | Press scales to 0.98 in 80 ms, back in 120 ms. Light haptic on tap for primary buttons. |
| Content entry | Fade + 8 px rise, 220 ms, 40 ms stagger per item, max 6 staggered items. |
| Balance change | Count up/down over 400 ms with tabular figures, only when the value changes. |
| Hide/show balance | Cross-fade 150 ms between amount and •••••• . |
| PIN error | Horizontal shake: 0 → −10 → 10 → −6 → 0 px over 300 ms, heavy haptic, dots turn danger, clear after 600 ms. |
| OTP box focus | Border colour 120 ms; caret blinks 1 s. |
| Toggle | Thumb slides 200 ms `easeOut`. |
| Skeleton shimmer | Diagonal highlight (surface → white 30% light / #2A3566 30% dark → surface) sweeping left to right every 1.2 s. Show only if loading takes over 150 ms. |
| Progress bars (budgets, goals, step bar) | Animate width 600 ms `easeOutCubic` on first show. |
| Donut chart | Segments sweep in clockwise over 700 ms on first show. |
| Snackbar / toast | Slide up 200 ms, stay 3 s, fade out 150 ms. |
| Card details reveal (L04) | Numbers cross-fade from •••• in 200 ms after PIN/biometric; countdown text updates each second. |
| Freeze (L06) | Play `nw_card_freeze`, then the frozen overlay fades in over 250 ms on the card. |

## 3. Haptics

| Moment | Haptic |
|---|---|
| Keypad key | `selectionClick` |
| Primary button | `lightImpact` |
| Success result | `mediumImpact` (at the end of the check) |
| Error / wrong PIN | `heavyImpact` |
| Toggle | `selectionClick` |

## 4. Prompt for opencode

```text
Add the NovaWallet motion system.
Read design/animations/MOTION.md and open the GIFs in design/animations/preview/.
1. Copy all JSON files from design/animations/lottie into assets/lottie and register them in pubspec.yaml.
2. Create lib/core/ui/motion/ with: NwLottie (picks _light or _dark by theme brightness, plays once or loops, respects reduce motion by showing the last frame), motion constants (durations and curves from section 2), NwShake, NwShimmer, NwCountUp, haptics helper (section 3).
3. Use NwLottie in the Result template (success / failed / pending), the Splash, the KYC upload wait, pull to refresh and the card freeze.
4. Add a debug "Motion gallery" page that plays every animation in light and dark.
Run flutter analyze and flutter test, fix errors, list changed files.
```
