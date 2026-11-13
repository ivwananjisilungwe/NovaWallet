# Q08 · Goal – add money

**Flow:** Extra flows  
**Route:** `/goals/:id (sheet)`  
**Design files:** `light.png`, `dark.png` (3×, 1170 × 2532) and `light.svg`, `dark.svg` in this folder.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Move money from wallet into a savings goal.

## Layout (top to bottom, 390 × 844 canvas, 20 side padding)
1. M05 goal hero behind a scrim.
2. Sheet: title, “From wallet · … available”, amount 46/800 with caret, quick amount chips, keypad, NwBottomCta “Add K 200” with lock.

## Components used (see ../../../docs/COMPONENTS.md)
- NwBottomSheet
- Amount display
- Chips
- NwKeypad

## States and behaviour
- Amount above wallet balance → amount danger + Top up link.

## Navigation
| When | Go to |
|---|---|
| Add | PIN → sheet closes, ring animates to new % |

## Data and backend
- NEW `POST /goals/{id}/deposit` (+Idempotency-Key) → ledger move customer_wallet → goal.

## Done when
- Matches `light.png` and `dark.png` side by side at 390 × 844 (spacing within 2 px, same colours from theme tokens).
- Works at text scale 1.3× without clipping, and on a 360-wide Android phone.
- Every state listed above can be reached (use a debug menu or fake repository).
- TalkBack reads every button and field with a clear label.
- No hard-coded colours, strings (all in ARB files) or limits.

## Prompt to give opencode (paste as is)
```text
Build screen Q08 "Goal – add money" for the NovaWallet Flutter app (route /goals/:id (sheet)).
Read docs/COMPONENTS.md and this file (screens/.../Q08_goal_add_money/BUILD.md) first, and open light.png and dark.png to match the design exactly.
Use only the shared components from lib/core/ui and colours from the theme extension; if a component is missing, build it in lib/core/ui first.
Put the screen in the feature folder for "Extra flows" following the existing architecture (presentation / application / data layers, Riverpod for state, go_router for navigation).
Implement every state listed under "States and behaviour", the navigation table, and the data calls listed under "Data and backend" through a repository interface with a fake implementation for now if the endpoint is marked NEW.
Add all text to the ARB localisation files. Add a widget test that pumps the screen in light and dark and a golden test for each.
Do not change other screens. When finished, list the files you created or changed and anything you could not complete.
```
