# Q04 · Data bundles

**Flow:** Extra flows  
**Route:** `/airtime?tab=data`  
**Design files:** `light.png`, `dark.png` (3×, 1170 × 2532) and `light.svg`, `dark.svg` in this folder.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Data bundles tab of K04.

## Layout (top to bottom, 390 × 844 canvas, 20 side padding)
1. Segmented control with “Data bundles” selected.
2. Phone field + network status text.
3. Overline “Choose a bundle”; card with radio rows: name, validity, price.
4. NwBottomCta “Buy <bundle> · K 35” with lock.

## Components used (see ../../../docs/COMPONENTS.md)
- Segmented control
- Radio list

## States and behaviour
- Bundles depend on the detected network; loading skeleton rows.

## Navigation
| When | Go to |
|---|---|
| Buy | PIN sheet → result |

## Data and backend
- NEW `GET /bills/bundles?network=`, `POST /bills/pay` (type DATA).

## Done when
- Matches `light.png` and `dark.png` side by side at 390 × 844 (spacing within 2 px, same colours from theme tokens).
- Works at text scale 1.3× without clipping, and on a 360-wide Android phone.
- Every state listed above can be reached (use a debug menu or fake repository).
- TalkBack reads every button and field with a clear label.
- No hard-coded colours, strings (all in ARB files) or limits.

## Prompt to give opencode (paste as is)
```text
Build screen Q04 "Data bundles" for the NovaWallet Flutter app (route /airtime?tab=data).
Read docs/COMPONENTS.md and this file (screens/.../Q04_data_bundles/BUILD.md) first, and open light.png and dark.png to match the design exactly.
Use only the shared components from lib/core/ui and colours from the theme extension; if a component is missing, build it in lib/core/ui first.
Put the screen in the feature folder for "Extra flows" following the existing architecture (presentation / application / data layers, Riverpod for state, go_router for navigation).
Implement every state listed under "States and behaviour", the navigation table, and the data calls listed under "Data and backend" through a repository interface with a fake implementation for now if the endpoint is marked NEW.
Add all text to the ARB localisation files. Add a widget test that pumps the screen in light and dark and a golden test for each.
Do not change other screens. When finished, list the files you created or changed and anything you could not complete.
```
