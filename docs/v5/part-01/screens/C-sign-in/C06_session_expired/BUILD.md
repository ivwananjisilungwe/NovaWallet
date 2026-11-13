# C06 · Session expired

**Flow:** Sign in  
**Route:** `/session-expired`  
**Design files:** `light.png`, `dark.png` (3×, 1170 × 2532) and `light.svg`, `dark.svg` in this folder.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Explain an automatic sign-out after inactivity.

## Layout (top to bottom, 390 × 844 canvas, 20 side padding)
1. System template: illustration `payment_pending` (clock), title “You were signed out”, body, primary “Unlock with PIN”, outline “Use fingerprint”. No close button.

## Components used (see ../../../docs/COMPONENTS.md)
- System screen template

## States and behaviour
- Fingerprint button hidden if not enabled.

## Navigation
| When | Go to |
|---|---|
| Unlock | C02 |
| Fingerprint success | Return to previous screen |

## Data and backend
- Triggered when refresh fails with 401.

## Rules and notes
- Keep unsent form data (e.g. a send amount) so the user can continue.

## Done when
- Matches `light.png` and `dark.png` side by side at 390 × 844 (spacing within 2 px, same colours from theme tokens).
- Works at text scale 1.3× without clipping, and on a 360-wide Android phone.
- Every state listed above can be reached (use a debug menu or fake repository).
- TalkBack reads every button and field with a clear label.
- No hard-coded colours, strings (all in ARB files) or limits.

## Prompt to give opencode (paste as is)
```text
Build screen C06 "Session expired" for the NovaWallet Flutter app (route /session-expired).
Read docs/COMPONENTS.md and this file (screens/.../C06_session_expired/BUILD.md) first, and open light.png and dark.png to match the design exactly.
Use only the shared components from lib/core/ui and colours from the theme extension; if a component is missing, build it in lib/core/ui first.
Put the screen in the feature folder for "Sign in" following the existing architecture (presentation / application / data layers, Riverpod for state, go_router for navigation).
Implement every state listed under "States and behaviour", the navigation table, and the data calls listed under "Data and backend" through a repository interface with a fake implementation for now if the endpoint is marked NEW.
Add all text to the ARB localisation files. Add a widget test that pumps the screen in light and dark and a golden test for each.
Do not change other screens. When finished, list the files you created or changed and anything you could not complete.
```
