# M05 · Goal detail

**Flow:** Insights, budgets and goals  
**Route:** `/goals/:id`  
**Design files:** `light.png`, `dark.png` (3×, 1170 × 2532) and `light.svg`, `dark.svg` in this folder.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Progress and actions for one goal.

## Layout (top to bottom, 390 × 844 canvas, 20 side padding)
1. Hero header 360 with white TopBar (goal name, more menu).
2. Big white ring (radius 72, stroke 12) with “40% saved”; “K 3,200 of K 8,000”.
3. Two stat cards: Auto-save, Target date.
4. Success banner “On track…” (warning if behind).
5. Buttons: primary Add money, tonal Take out.
6. History list.

## Components used (see ../../../docs/COMPONENTS.md)
- Ring progress
- Stat card
- NwBanner
- NwTxRow

## States and behaviour
- Locked goal: “Take out” disabled with lock icon and date.

## Navigation
| When | Go to |
|---|---|
| Add money | Amount sheet + PIN |
| Take out | Amount sheet + PIN |
| More | Edit / pause auto-save / delete |

## Data and backend
- NEW `POST /api/v1/goals/{id}/deposit`, `/withdraw`.

## Done when
- Matches `light.png` and `dark.png` side by side at 390 × 844 (spacing within 2 px, same colours from theme tokens).
- Works at text scale 1.3× without clipping, and on a 360-wide Android phone.
- Every state listed above can be reached (use a debug menu or fake repository).
- TalkBack reads every button and field with a clear label.
- No hard-coded colours, strings (all in ARB files) or limits.

## Prompt to give opencode (paste as is)
```text
Build screen M05 "Goal detail" for the NovaWallet Flutter app (route /goals/:id).
Read docs/COMPONENTS.md and this file (screens/.../M05_goal_detail/BUILD.md) first, and open light.png and dark.png to match the design exactly.
Use only the shared components from lib/core/ui and colours from the theme extension; if a component is missing, build it in lib/core/ui first.
Put the screen in the feature folder for "Insights, budgets and goals" following the existing architecture (presentation / application / data layers, Riverpod for state, go_router for navigation).
Implement every state listed under "States and behaviour", the navigation table, and the data calls listed under "Data and backend" through a repository interface with a fake implementation for now if the endpoint is marked NEW.
Add all text to the ARB localisation files. Add a widget test that pumps the screen in light and dark and a golden test for each.
Do not change other screens. When finished, list the files you created or changed and anything you could not complete.
```
