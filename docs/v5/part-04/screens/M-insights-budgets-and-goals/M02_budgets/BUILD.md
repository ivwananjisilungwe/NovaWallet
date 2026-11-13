# M02 · Budgets

**Flow:** Insights, budgets and goals  
**Route:** `/budgets`  
**Design files:** `light.png`, `dark.png` (3×, 1170 × 2532) and `light.svg`, `dark.svg` in this folder.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
All budgets for the month.

## Layout (top to bottom, 390 × 844 canvas, 20 side padding)
1. TopBar “Budgets” with plus.
2. Hero card (gradient, radius 26): “November budget”, “K 1,240 of K 2,000”, white progress bar, “K 760 left · 19 days to go”.
3. Budget rows (80 high, gap 10). Over-budget row shows red bar and “K 15 over”.
4. Outline “Add a budget” row.

## Components used (see ../../../docs/COMPONENTS.md)
- NwHeroHeader (card)
- Budget row

## States and behaviour
- Swipe a row to edit/delete.
- Alerts at 80% and 100% come as push.

## Navigation
| When | Go to |
|---|---|
| Row | Budget detail (edit = M03 prefilled) |
| Add | M03 |

## Data and backend
- NEW `GET /api/v1/budgets?month=`.

## Done when
- Matches `light.png` and `dark.png` side by side at 390 × 844 (spacing within 2 px, same colours from theme tokens).
- Works at text scale 1.3× without clipping, and on a 360-wide Android phone.
- Every state listed above can be reached (use a debug menu or fake repository).
- TalkBack reads every button and field with a clear label.
- No hard-coded colours, strings (all in ARB files) or limits.

## Prompt to give opencode (paste as is)
```text
Build screen M02 "Budgets" for the NovaWallet Flutter app (route /budgets).
Read docs/COMPONENTS.md and this file (screens/.../M02_budgets/BUILD.md) first, and open light.png and dark.png to match the design exactly.
Use only the shared components from lib/core/ui and colours from the theme extension; if a component is missing, build it in lib/core/ui first.
Put the screen in the feature folder for "Insights, budgets and goals" following the existing architecture (presentation / application / data layers, Riverpod for state, go_router for navigation).
Implement every state listed under "States and behaviour", the navigation table, and the data calls listed under "Data and backend" through a repository interface with a fake implementation for now if the endpoint is marked NEW.
Add all text to the ARB localisation files. Add a widget test that pumps the screen in light and dark and a golden test for each.
Do not change other screens. When finished, list the files you created or changed and anything you could not complete.
```
