# M08 · Add cash

**Flow:** Insights, budgets and goals  
**Route:** `/cash/new`  
**Design files:** `light.png`, `dark.png` (3×, 1170 × 2532) and `light.svg`, `dark.svg` in this folder.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Log cash spending or income so insights include cash.

## Layout (top to bottom, 390 × 844 canvas, 20 side padding)
1. TopBar “Add cash”.
2. Segmented I spent | I received.
3. Amount 48/800 with caret.
4. Category chips with icons.
5. Note field + When field side by side.
6. Keypad, NwBottomCta “Save”.

## Components used (see ../../../docs/COMPONENTS.md)
- Segmented control
- Chips
- NwField
- NwKeypad

## States and behaviour
- Works offline; syncs later (queue in local DB).

## Navigation
| When | Go to |
|---|---|
| Save | Back with snackbar “Cash entry added” |

## Data and backend
- NEW `POST /api/v1/cash-entries` (local first, then sync).

## Rules and notes
- Cash entries never change the wallet balance; they only feed insights.

## Done when
- Matches `light.png` and `dark.png` side by side at 390 × 844 (spacing within 2 px, same colours from theme tokens).
- Works at text scale 1.3× without clipping, and on a 360-wide Android phone.
- Every state listed above can be reached (use a debug menu or fake repository).
- TalkBack reads every button and field with a clear label.
- No hard-coded colours, strings (all in ARB files) or limits.

## Prompt to give opencode (paste as is)
```text
Build screen M08 "Add cash" for the NovaWallet Flutter app (route /cash/new).
Read docs/COMPONENTS.md and this file (screens/.../M08_add_cash_entry/BUILD.md) first, and open light.png and dark.png to match the design exactly.
Use only the shared components from lib/core/ui and colours from the theme extension; if a component is missing, build it in lib/core/ui first.
Put the screen in the feature folder for "Insights, budgets and goals" following the existing architecture (presentation / application / data layers, Riverpod for state, go_router for navigation).
Implement every state listed under "States and behaviour", the navigation table, and the data calls listed under "Data and backend" through a repository interface with a fake implementation for now if the endpoint is marked NEW.
Add all text to the ARB localisation files. Add a widget test that pumps the screen in light and dark and a golden test for each.
Do not change other screens. When finished, list the files you created or changed and anything you could not complete.
```
