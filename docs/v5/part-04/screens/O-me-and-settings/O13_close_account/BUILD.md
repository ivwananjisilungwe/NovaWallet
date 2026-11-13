# O13 · Close account

**Flow:** Me and settings  
**Route:** `/me/close`  
**Design files:** `light.png`, `dark.png` (3×, 1170 × 2532) and `light.svg`, `dark.svg` in this folder.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Close the account safely.

## Layout (top to bottom, 390 × 844 canvas, 20 side padding)
1. TopBar “Close account”.
2. Danger soft circle with warning icon.
3. Title “Before you go”, sub.
4. Checklist card: each item success or danger icon + status line + “Fix” link.
5. Legal note on record keeping.
6. Danger CTA “Close my account” disabled (40%) until all items pass.

## Components used (see ../../../docs/COMPONENTS.md)
- Checklist
- NwButton danger

## States and behaviour
- All pass → enabled → reason sheet → PIN → done screen.

## Navigation
| When | Go to |
|---|---|
| Fix | I01 / M05 / L05 |

## Data and backend
- NEW `POST /api/v1/users/me/close`.

## Rules and notes
- Confirm the record-keeping period with the licensed partner (AML rules).

## Done when
- Matches `light.png` and `dark.png` side by side at 390 × 844 (spacing within 2 px, same colours from theme tokens).
- Works at text scale 1.3× without clipping, and on a 360-wide Android phone.
- Every state listed above can be reached (use a debug menu or fake repository).
- TalkBack reads every button and field with a clear label.
- No hard-coded colours, strings (all in ARB files) or limits.

## Prompt to give opencode (paste as is)
```text
Build screen O13 "Close account" for the NovaWallet Flutter app (route /me/close).
Read docs/COMPONENTS.md and this file (screens/.../O13_close_account/BUILD.md) first, and open light.png and dark.png to match the design exactly.
Use only the shared components from lib/core/ui and colours from the theme extension; if a component is missing, build it in lib/core/ui first.
Put the screen in the feature folder for "Me and settings" following the existing architecture (presentation / application / data layers, Riverpod for state, go_router for navigation).
Implement every state listed under "States and behaviour", the navigation table, and the data calls listed under "Data and backend" through a repository interface with a fake implementation for now if the endpoint is marked NEW.
Add all text to the ARB localisation files. Add a widget test that pumps the screen in light and dark and a golden test for each.
Do not change other screens. When finished, list the files you created or changed and anything you could not complete.
```
