# M03 · New budget

**Flow:** Insights, budgets and goals  
**Route:** `/budgets/new`  
**Design files:** `light.png`, `dark.png` (3×, 1170 × 2532) and `light.svg`, `dark.svg` in this folder.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Create a category budget.

## Layout (top to bottom, 390 × 844 canvas, 20 side padding)
1. TopBar “New budget”.
2. Overline Category + 4 × 2 bubble grid (selected has a 2.5 primary ring and primary label).
3. Overline Limit + amount 46/800 with caret (tap opens keypad sheet).
4. Segmented Weekly | Monthly.
5. List group toggles: Warn me at 80%, Repeat every month.
6. Info banner with last month’s spend for the category.
7. NwBottomCta “Create budget”.

## Components used (see ../../../docs/COMPONENTS.md)
- Category grid
- Segmented control
- NwListGroup
- NwBanner

## States and behaviour
- Category that already has a budget shows “Edit existing” instead.

## Navigation
| When | Go to |
|---|---|
| Create | M02 with new row highlighted |

## Data and backend
- NEW `POST /api/v1/budgets`; last-month spend from insights summary.

## Done when
- Matches `light.png` and `dark.png` side by side at 390 × 844 (spacing within 2 px, same colours from theme tokens).
- Works at text scale 1.3× without clipping, and on a 360-wide Android phone.
- Every state listed above can be reached (use a debug menu or fake repository).
- TalkBack reads every button and field with a clear label.
- No hard-coded colours, strings (all in ARB files) or limits.

## Prompt to give opencode (paste as is)
```text
Build screen M03 "New budget" for the NovaWallet Flutter app (route /budgets/new).
Read docs/COMPONENTS.md and this file (screens/.../M03_create_budget/BUILD.md) first, and open light.png and dark.png to match the design exactly.
Use only the shared components from lib/core/ui and colours from the theme extension; if a component is missing, build it in lib/core/ui first.
Put the screen in the feature folder for "Insights, budgets and goals" following the existing architecture (presentation / application / data layers, Riverpod for state, go_router for navigation).
Implement every state listed under "States and behaviour", the navigation table, and the data calls listed under "Data and backend" through a repository interface with a fake implementation for now if the endpoint is marked NEW.
Add all text to the ARB localisation files. Add a widget test that pumps the screen in light and dark and a golden test for each.
Do not change other screens. When finished, list the files you created or changed and anything you could not complete.
```
