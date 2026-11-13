# K05 · Digital store

**Flow:** Bills, airtime and store  
**Route:** `/store`  
**Design files:** `light.png`, `dark.png` (3×, 1170 × 2532) and `light.svg`, `dark.svg` in this folder.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Buy digital codes (gaming, streaming, learning).

## Layout (top to bottom, 390 × 844 canvas, 20 side padding)
1. TopBar “Store” with search.
2. Promo card (hero gradient, radius 26): title, sub, white “Shop now” pill, illustration `digital_store` right.
3. Chips: All, Gaming, Streaming, Learning.
4. 2-column product cards (176): surface2 image area with category tile (NwCategoryTile), name 15/700, “From K …” primary.
5. Footnote about refunds.

## Components used (see ../../../docs/COMPONENTS.md)
- Promo card
- Chips
- Product card

## States and behaviour
- Out of stock → greyed card with “Sold out”.

## Navigation
| When | Go to |
|---|---|
| Product | K06 |

## Data and backend
- NEW `GET /api/v1/store/products` (digital goods partner).

## Rules and notes
- Use generic product art unless the brand owner gives permission for logos.

## Done when
- Matches `light.png` and `dark.png` side by side at 390 × 844 (spacing within 2 px, same colours from theme tokens).
- Works at text scale 1.3× without clipping, and on a 360-wide Android phone.
- Every state listed above can be reached (use a debug menu or fake repository).
- TalkBack reads every button and field with a clear label.
- No hard-coded colours, strings (all in ARB files) or limits.

## Prompt to give opencode (paste as is)
```text
Build screen K05 "Digital store" for the NovaWallet Flutter app (route /store).
Read docs/COMPONENTS.md and this file (screens/.../K05_digital_store/BUILD.md) first, and open light.png and dark.png to match the design exactly.
Use only the shared components from lib/core/ui and colours from the theme extension; if a component is missing, build it in lib/core/ui first.
Put the screen in the feature folder for "Bills, airtime and store" following the existing architecture (presentation / application / data layers, Riverpod for state, go_router for navigation).
Implement every state listed under "States and behaviour", the navigation table, and the data calls listed under "Data and backend" through a repository interface with a fake implementation for now if the endpoint is marked NEW.
Add all text to the ARB localisation files. Add a widget test that pumps the screen in light and dark and a golden test for each.
Do not change other screens. When finished, list the files you created or changed and anything you could not complete.
```
