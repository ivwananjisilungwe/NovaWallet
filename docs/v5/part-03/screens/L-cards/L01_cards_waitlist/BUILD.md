# L01 · Cards – waitlist

**Flow:** Cards  
**Route:** `/cards (tab)`  
**Design files:** `light.png`, `dark.png` (3×, 1170 × 2532) and `light.svg`, `dark.svg` in this folder.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Cards tab before the card programme launches.

## Layout (top to bottom, 390 × 844 canvas, 20 side padding)
1. Large title “Cards”.
2. Illustration `cards_waitlist`.
3. Title “Virtual cards are coming”, body.
4. List group of three benefits (54 rows, no chevrons).
5. Primary “Join the waitlist” above the tab bar.
6. NwTabBar Cards active.

## Components used (see ../../../docs/COMPONENTS.md)
- NwIllustration
- NwListGroup
- NwTabBar

## States and behaviour
- After joining: button becomes tonal “You’re on the list” + position pill.

## Navigation
| When | Go to |
|---|---|
| Programme live | L02 |

## Data and backend
- NEW `POST /api/v1/cards/waitlist`; feature flag `cards_enabled` from app config.

## Rules and notes
- Do not promise fees or dates until the issuing partner signs.

## Done when
- Matches `light.png` and `dark.png` side by side at 390 × 844 (spacing within 2 px, same colours from theme tokens).
- Works at text scale 1.3× without clipping, and on a 360-wide Android phone.
- Every state listed above can be reached (use a debug menu or fake repository).
- TalkBack reads every button and field with a clear label.
- No hard-coded colours, strings (all in ARB files) or limits.

## Prompt to give opencode (paste as is)
```text
Build screen L01 "Cards – waitlist" for the NovaWallet Flutter app (route /cards (tab)).
Read docs/COMPONENTS.md and this file (screens/.../L01_cards_waitlist/BUILD.md) first, and open light.png and dark.png to match the design exactly.
Use only the shared components from lib/core/ui and colours from the theme extension; if a component is missing, build it in lib/core/ui first.
Put the screen in the feature folder for "Cards" following the existing architecture (presentation / application / data layers, Riverpod for state, go_router for navigation).
Implement every state listed under "States and behaviour", the navigation table, and the data calls listed under "Data and backend" through a repository interface with a fake implementation for now if the endpoint is marked NEW.
Add all text to the ARB localisation files. Add a widget test that pumps the screen in light and dark and a golden test for each.
Do not change other screens. When finished, list the files you created or changed and anything you could not complete.
```
