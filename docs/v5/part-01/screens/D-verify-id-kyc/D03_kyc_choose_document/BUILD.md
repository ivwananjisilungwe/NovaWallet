# D03 · Choose your ID

**Flow:** Verify ID (KYC)  
**Route:** `/kyc/document`  
**Design files:** `light.png`, `dark.png` (3×, 1170 × 2532) and `light.svg`, `dark.svg` in this folder.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Pick which document to verify with.

## Layout (top to bottom, 390 × 844 canvas, 20 side padding)
1. TopBar “Choose your ID”, NwStepProgress 4 steps (Document, Photo, Selfie, Check).
2. H1 26 “Which ID do you have?”.
3. Selectable cards 80 high: NRC (Recommended · fastest, selected), Passport, Driver’s licence (Coming soon, greyed).
4. NwBottomCta “Continue”.

## Components used (see ../../../docs/COMPONENTS.md)
- NwStepProgress
- Selectable NwCard
- NwBubble

## States and behaviour
- Disabled options not selectable.

## Navigation
| When | Go to |
|---|---|
| Continue | D04 Capture |

## Data and backend
- None.

## Done when
- Matches `light.png` and `dark.png` side by side at 390 × 844 (spacing within 2 px, same colours from theme tokens).
- Works at text scale 1.3× without clipping, and on a 360-wide Android phone.
- Every state listed above can be reached (use a debug menu or fake repository).
- TalkBack reads every button and field with a clear label.
- No hard-coded colours, strings (all in ARB files) or limits.

## Prompt to give opencode (paste as is)
```text
Build screen D03 "Choose your ID" for the NovaWallet Flutter app (route /kyc/document).
Read docs/COMPONENTS.md and this file (screens/.../D03_kyc_choose_document/BUILD.md) first, and open light.png and dark.png to match the design exactly.
Use only the shared components from lib/core/ui and colours from the theme extension; if a component is missing, build it in lib/core/ui first.
Put the screen in the feature folder for "Verify ID (KYC)" following the existing architecture (presentation / application / data layers, Riverpod for state, go_router for navigation).
Implement every state listed under "States and behaviour", the navigation table, and the data calls listed under "Data and backend" through a repository interface with a fake implementation for now if the endpoint is marked NEW.
Add all text to the ARB localisation files. Add a widget test that pumps the screen in light and dark and a golden test for each.
Do not change other screens. When finished, list the files you created or changed and anything you could not complete.
```
