# L06 · Card frozen

**Flow:** Cards  
**Route:** `/cards (tab)`  
**Design files:** `light.png`, `dark.png` (3×, 1170 × 2532) and `light.svg`, `dark.svg` in this folder.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
State after freezing.

## Layout (top to bottom, 390 × 844 canvas, 20 side padding)
1. Same as L03 with frozen overlay on the card (navy 62%, snowflake, “Card frozen”).
2. The Freeze action becomes a filled primary “Unfreeze”.
3. Info banner with snowflake: declines and subscriptions warning.

## Components used (see ../../../docs/COMPONENTS.md)
- NwVirtualCard (frozen)
- NwBanner

## States and behaviour
- Unfreeze requires PIN/biometric.

## Navigation
| When | Go to |
|---|---|
| Unfreeze | L03 |

## Data and backend
- NEW `POST /api/v1/cards/{id}/freeze` / `unfreeze`.

## Done when
- Matches `light.png` and `dark.png` side by side at 390 × 844 (spacing within 2 px, same colours from theme tokens).
- Works at text scale 1.3× without clipping, and on a 360-wide Android phone.
- Every state listed above can be reached (use a debug menu or fake repository).
- TalkBack reads every button and field with a clear label.
- No hard-coded colours, strings (all in ARB files) or limits.

## Prompt to give opencode (paste as is)
```text
Build screen L06 "Card frozen" for the NovaWallet Flutter app (route /cards (tab)).
Read docs/COMPONENTS.md and this file (screens/.../L06_card_frozen/BUILD.md) first, and open light.png and dark.png to match the design exactly.
Use only the shared components from lib/core/ui and colours from the theme extension; if a component is missing, build it in lib/core/ui first.
Put the screen in the feature folder for "Cards" following the existing architecture (presentation / application / data layers, Riverpod for state, go_router for navigation).
Implement every state listed under "States and behaviour", the navigation table, and the data calls listed under "Data and backend" through a repository interface with a fake implementation for now if the endpoint is marked NEW.
Add all text to the ARB localisation files. Add a widget test that pumps the screen in light and dark and a golden test for each.
Do not change other screens. When finished, list the files you created or changed and anything you could not complete.
```
