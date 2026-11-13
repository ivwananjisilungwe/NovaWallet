# D07 · ID in review

**Flow:** Verify ID (KYC)  
**Route:** `/kyc/status`  
**Design files:** `light.png`, `dark.png` (3×, 1170 × 2532) and `light.svg`, `dark.svg` in this folder.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Tracks review progress.

## Layout (top to bottom, 390 × 844 canvas, 20 side padding)
1. TopBar close “ID verification”.
2. Illustration `payment_pending` 270 wide.
3. Title “We’re checking your ID”, sub.
4. Timeline card: four steps with connector lines — done (success circle + check), current (warning soft circle + dot), future (outline).
5. Tonal CTA “Back to Home”.

## Components used (see ../../../docs/COMPONENTS.md)
- Timeline card
- NwIllustration

## States and behaviour
- Pull to refresh. Push notification when decided.

## Navigation
| When | Go to |
|---|---|
| Approved push | D08 |
| Rejected | D09 |
| Back to Home | E01 |

## Data and backend
- `GET /api/v1/kyc/status`.

## Done when
- Matches `light.png` and `dark.png` side by side at 390 × 844 (spacing within 2 px, same colours from theme tokens).
- Works at text scale 1.3× without clipping, and on a 360-wide Android phone.
- Every state listed above can be reached (use a debug menu or fake repository).
- TalkBack reads every button and field with a clear label.
- No hard-coded colours, strings (all in ARB files) or limits.

## Prompt to give opencode (paste as is)
```text
Build screen D07 "ID in review" for the NovaWallet Flutter app (route /kyc/status).
Read docs/COMPONENTS.md and this file (screens/.../D07_kyc_in_review/BUILD.md) first, and open light.png and dark.png to match the design exactly.
Use only the shared components from lib/core/ui and colours from the theme extension; if a component is missing, build it in lib/core/ui first.
Put the screen in the feature folder for "Verify ID (KYC)" following the existing architecture (presentation / application / data layers, Riverpod for state, go_router for navigation).
Implement every state listed under "States and behaviour", the navigation table, and the data calls listed under "Data and backend" through a repository interface with a fake implementation for now if the endpoint is marked NEW.
Add all text to the ARB localisation files. Add a widget test that pumps the screen in light and dark and a golden test for each.
Do not change other screens. When finished, list the files you created or changed and anything you could not complete.
```
