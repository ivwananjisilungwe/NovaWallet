# N02 · Nova tips

**Flow:** Nova AI  
**Route:** `/nova/tips`  
**Design files:** `light.png`, `dark.png` (3×, 1170 × 2532) and `light.svg`, `dark.svg` in this folder.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Feed of personal money tips with one action each.

## Layout (top to bottom, 390 × 844 canvas, 20 side padding)
1. TopBar “Nova tips”.
2. Tip cards (176): tinted icon circle, title 16/800, body 14.5, tonal action button + “Not now”.

## Components used (see ../../../docs/COMPONENTS.md)
- Tip card

## States and behaviour
- Not now hides the tip and teaches the ranking.
- Empty: “No tips right now.”

## Navigation
| When | Go to |
|---|---|
| Action | Related screen (M03, K02, M05…) |

## Data and backend
- NEW `GET /api/v1/ai/tips`.

## Rules and notes
- Tips must use real numbers from the user’s data, never invented amounts.

## Done when
- Matches `light.png` and `dark.png` side by side at 390 × 844 (spacing within 2 px, same colours from theme tokens).
- Works at text scale 1.3× without clipping, and on a 360-wide Android phone.
- Every state listed above can be reached (use a debug menu or fake repository).
- TalkBack reads every button and field with a clear label.
- No hard-coded colours, strings (all in ARB files) or limits.

## Prompt to give opencode (paste as is)
```text
Build screen N02 "Nova tips" for the NovaWallet Flutter app (route /nova/tips).
Read docs/COMPONENTS.md and this file (screens/.../N02_nova_tips/BUILD.md) first, and open light.png and dark.png to match the design exactly.
Use only the shared components from lib/core/ui and colours from the theme extension; if a component is missing, build it in lib/core/ui first.
Put the screen in the feature folder for "Nova AI" following the existing architecture (presentation / application / data layers, Riverpod for state, go_router for navigation).
Implement every state listed under "States and behaviour", the navigation table, and the data calls listed under "Data and backend" through a repository interface with a fake implementation for now if the endpoint is marked NEW.
Add all text to the ARB localisation files. Add a widget test that pumps the screen in light and dark and a golden test for each.
Do not change other screens. When finished, list the files you created or changed and anything you could not complete.
```
