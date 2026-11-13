# G02 · Send – amount

**Flow:** Send money  
**Route:** `/send/amount`  
**Design files:** `light.png`, `dark.png` (3×, 1170 × 2532) and `light.svg`, `dark.svg` in this folder.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Enter amount and an optional note.

## Layout (top to bottom, 390 × 844 canvas, 20 side padding)
1. TopBar “Send”.
2. Recipient header: 56 avatar, name 16/700, “NovaWallet · 097 ••• 2210”.
3. Amount 52/800 with caret; caption “Available K 2,450.75 · Free”.
4. NwField 58 high “Note (optional)”.
5. Keypad with “.”; NwBottomCta “Continue”.

## Components used (see ../../../docs/COMPONENTS.md)
- NwAvatar
- Amount display
- NwField
- NwKeypad

## States and behaviour
- Insufficient balance → amount danger + “Top up” link.

## Navigation
| When | Go to |
|---|---|
| Continue | G03 |

## Data and backend
- `GET /api/v1/fees/estimate?type=TRANSFER`.

## Done when
- Matches `light.png` and `dark.png` side by side at 390 × 844 (spacing within 2 px, same colours from theme tokens).
- Works at text scale 1.3× without clipping, and on a 360-wide Android phone.
- Every state listed above can be reached (use a debug menu or fake repository).
- TalkBack reads every button and field with a clear label.
- No hard-coded colours, strings (all in ARB files) or limits.

## Prompt to give opencode (paste as is)
```text
Build screen G02 "Send – amount" for the NovaWallet Flutter app (route /send/amount).
Read docs/COMPONENTS.md and this file (screens/.../G02_send_amount/BUILD.md) first, and open light.png and dark.png to match the design exactly.
Use only the shared components from lib/core/ui and colours from the theme extension; if a component is missing, build it in lib/core/ui first.
Put the screen in the feature folder for "Send money" following the existing architecture (presentation / application / data layers, Riverpod for state, go_router for navigation).
Implement every state listed under "States and behaviour", the navigation table, and the data calls listed under "Data and backend" through a repository interface with a fake implementation for now if the endpoint is marked NEW.
Add all text to the ARB localisation files. Add a widget test that pumps the screen in light and dark and a golden test for each.
Do not change other screens. When finished, list the files you created or changed and anything you could not complete.
```
