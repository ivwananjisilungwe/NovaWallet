# L02 · Design your card

**Flow:** Cards  
**Route:** `/cards/new`  
**Design files:** `light.png`, `dark.png` (3×, 1170 × 2532) and `light.svg`, `dark.svg` in this folder.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Choose a card colour and confirm the name.

## Layout (top to bottom, 390 × 844 canvas, 20 side padding)
1. TopBar “Design your card”.
2. Live NwVirtualCard preview.
3. Four colour swatches (40) with a 2.5 primary ring on the selected; colour name under them.
4. Name on card field (read-only from KYC).
5. NwKeyValueCard: Card type, Currency, Card fee.
6. NwBottomCta “Create my card”.

## Components used (see ../../../docs/COMPONENTS.md)
- NwVirtualCard
- Swatches
- NwKeyValueCard

## States and behaviour
- Swatch change animates the card gradient 300 ms.
- Requires Verified level; otherwise sheet to D02.

## Navigation
| When | Go to |
|---|---|
| Create | PIN → L03 with success toast |

## Data and backend
- NEW `POST /api/v1/cards` (issuer integration — see virtual cards doc).

## Done when
- Matches `light.png` and `dark.png` side by side at 390 × 844 (spacing within 2 px, same colours from theme tokens).
- Works at text scale 1.3× without clipping, and on a 360-wide Android phone.
- Every state listed above can be reached (use a debug menu or fake repository).
- TalkBack reads every button and field with a clear label.
- No hard-coded colours, strings (all in ARB files) or limits.

## Prompt to give opencode (paste as is)
```text
Build screen L02 "Design your card" for the NovaWallet Flutter app (route /cards/new).
Read docs/COMPONENTS.md and this file (screens/.../L02_card_choose_design/BUILD.md) first, and open light.png and dark.png to match the design exactly.
Use only the shared components from lib/core/ui and colours from the theme extension; if a component is missing, build it in lib/core/ui first.
Put the screen in the feature folder for "Cards" following the existing architecture (presentation / application / data layers, Riverpod for state, go_router for navigation).
Implement every state listed under "States and behaviour", the navigation table, and the data calls listed under "Data and backend" through a repository interface with a fake implementation for now if the endpoint is marked NEW.
Add all text to the ARB localisation files. Add a widget test that pumps the screen in light and dark and a golden test for each.
Do not change other screens. When finished, list the files you created or changed and anything you could not complete.
```
