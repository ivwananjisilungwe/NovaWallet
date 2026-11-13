# J02 · Activity filters

**Flow:** Activity  
**Route:** `/activity (sheet)`  
**Design files:** `light.png`, `dark.png` (3×, 1170 × 2532) and `light.svg`, `dark.svg` in this folder.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Filter by type, date and category.

## Layout (top to bottom, 390 × 844 canvas, 20 side padding)
1. J01 behind a scrim.
2. Sheet from y 236: “Filters” 22/800 + “Reset” link.
3. Overline Type + multi-select chips (selected: primarySoft fill + primary border).
4. Overline Date + chips (This month, Last month, Last 3 months, Pick dates).
5. Overline Category + chips with small category icons.
6. NwBottomCta “Show 24 results” (live count).

## Components used (see ../../../docs/COMPONENTS.md)
- NwBottomSheet
- Multi-select chips

## States and behaviour
- Count updates as filters change (debounced).
- “Pick dates” opens a date range picker.

## Navigation
| When | Go to |
|---|---|
| Show results | J01 filtered |

## Data and backend
- Count: same list endpoint with `size=0` returning total.

## Done when
- Matches `light.png` and `dark.png` side by side at 390 × 844 (spacing within 2 px, same colours from theme tokens).
- Works at text scale 1.3× without clipping, and on a 360-wide Android phone.
- Every state listed above can be reached (use a debug menu or fake repository).
- TalkBack reads every button and field with a clear label.
- No hard-coded colours, strings (all in ARB files) or limits.

## Prompt to give opencode (paste as is)
```text
Build screen J02 "Activity filters" for the NovaWallet Flutter app (route /activity (sheet)).
Read docs/COMPONENTS.md and this file (screens/.../J02_activity_filters/BUILD.md) first, and open light.png and dark.png to match the design exactly.
Use only the shared components from lib/core/ui and colours from the theme extension; if a component is missing, build it in lib/core/ui first.
Put the screen in the feature folder for "Activity" following the existing architecture (presentation / application / data layers, Riverpod for state, go_router for navigation).
Implement every state listed under "States and behaviour", the navigation table, and the data calls listed under "Data and backend" through a repository interface with a fake implementation for now if the endpoint is marked NEW.
Add all text to the ARB localisation files. Add a widget test that pumps the screen in light and dark and a golden test for each.
Do not change other screens. When finished, list the files you created or changed and anything you could not complete.
```
