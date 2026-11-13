# J05 · Statements

**Flow:** Activity  
**Route:** `/statements`  
**Design files:** `light.png`, `dark.png` (3×, 1170 × 2532) and `light.svg`, `dark.svg` in this folder.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Create and download account statements (e.g. for bursary or sponsor).

## Layout (top to bottom, 390 × 844 canvas, 20 side padding)
1. TopBar “Statements”.
2. Card “Create a statement”: Period chips, Format segmented control (PDF | Excel CSV).
3. Primary button “Create statement” with statement icon.
4. Section “Past statements”: rows with file bubble, period, “PDF · 84 KB”, download icon.
5. Caption “Statements are free and include every transaction.”

## Components used (see ../../../docs/COMPONENTS.md)
- Chips
- Segmented control
- NwListGroup

## States and behaviour
- Generating → row appears with spinner, push when ready.

## Navigation
| When | Go to |
|---|---|
| Download | Open file |

## Data and backend
- Existing `GET /api/v1/wallets/{walletId}/statements` (extend with format + period).

## Done when
- Matches `light.png` and `dark.png` side by side at 390 × 844 (spacing within 2 px, same colours from theme tokens).
- Works at text scale 1.3× without clipping, and on a 360-wide Android phone.
- Every state listed above can be reached (use a debug menu or fake repository).
- TalkBack reads every button and field with a clear label.
- No hard-coded colours, strings (all in ARB files) or limits.

## Prompt to give opencode (paste as is)
```text
Build screen J05 "Statements" for the NovaWallet Flutter app (route /statements).
Read docs/COMPONENTS.md and this file (screens/.../J05_statements/BUILD.md) first, and open light.png and dark.png to match the design exactly.
Use only the shared components from lib/core/ui and colours from the theme extension; if a component is missing, build it in lib/core/ui first.
Put the screen in the feature folder for "Activity" following the existing architecture (presentation / application / data layers, Riverpod for state, go_router for navigation).
Implement every state listed under "States and behaviour", the navigation table, and the data calls listed under "Data and backend" through a repository interface with a fake implementation for now if the endpoint is marked NEW.
Add all text to the ARB localisation files. Add a widget test that pumps the screen in light and dark and a golden test for each.
Do not change other screens. When finished, list the files you created or changed and anything you could not complete.
```
