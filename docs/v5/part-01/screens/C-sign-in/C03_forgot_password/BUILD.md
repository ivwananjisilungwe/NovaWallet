# C03 · Forgot password

**Flow:** Sign in  
**Route:** `/password/forgot`  
**Design files:** `light.png`, `dark.png` (3×, 1170 × 2532) and `light.svg`, `dark.svg` in this folder.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Start a password reset.

## Layout (top to bottom, 390 × 844 canvas, 20 side padding)
1. TopBar back. 64 primarySoft circle with key icon.
2. H1 28 “Reset your password” + sub.
3. NwField “Mobile number or email” focused.
4. NwBottomCta “Send reset code”.

## Components used (see ../../../docs/COMPONENTS.md)
- NwField
- NwBottomCta

## States and behaviour
- Always show the same success message whether the account exists or not.

## Navigation
| When | Go to |
|---|---|
| Send | OTP screen (reuse B02 layout, purpose RESET) → C04 |

## Data and backend
- `POST /api/v1/password/forgot`.

## Done when
- Matches `light.png` and `dark.png` side by side at 390 × 844 (spacing within 2 px, same colours from theme tokens).
- Works at text scale 1.3× without clipping, and on a 360-wide Android phone.
- Every state listed above can be reached (use a debug menu or fake repository).
- TalkBack reads every button and field with a clear label.
- No hard-coded colours, strings (all in ARB files) or limits.

## Prompt to give opencode (paste as is)
```text
Build screen C03 "Forgot password" for the NovaWallet Flutter app (route /password/forgot).
Read docs/COMPONENTS.md and this file (screens/.../C03_forgot_password/BUILD.md) first, and open light.png and dark.png to match the design exactly.
Use only the shared components from lib/core/ui and colours from the theme extension; if a component is missing, build it in lib/core/ui first.
Put the screen in the feature folder for "Sign in" following the existing architecture (presentation / application / data layers, Riverpod for state, go_router for navigation).
Implement every state listed under "States and behaviour", the navigation table, and the data calls listed under "Data and backend" through a repository interface with a fake implementation for now if the endpoint is marked NEW.
Add all text to the ARB localisation files. Add a widget test that pumps the screen in light and dark and a golden test for each.
Do not change other screens. When finished, list the files you created or changed and anything you could not complete.
```
