# G08 · Request money

**Flow:** Send money  
**Route:** `/request`  
**Design files:** `light.png`, `dark.png` (3×, 1170 × 2532) and `light.svg`, `dark.svg` in this folder.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Ask someone to pay you.

## Layout (top to bottom, 390 × 844 canvas, 20 side padding)
1. TopBar “Request money”.
2. “Ask for” + amount 52/800.
3. Overline “From”: removable person chip (primarySoft) + 48 outline add button.
4. NwField “What’s it for?”.
5. Overline “Or share a payment link”; list group: Copy payment link (link shown), Share to WhatsApp.
6. NwBottomCta “Send request”.

## Components used (see ../../../docs/COMPONENTS.md)
- Person chip
- NwField
- NwListGroup

## States and behaviour
- Request appears for the payer as a notification with Pay / Decline.

## Navigation
| When | Go to |
|---|---|
| Send request | Success snackbar → Home |

## Data and backend
- NEW `POST /api/v1/payment-requests`.

## Rules and notes
- Links expire after 7 days.

## Done when
- Matches `light.png` and `dark.png` side by side at 390 × 844 (spacing within 2 px, same colours from theme tokens).
- Works at text scale 1.3× without clipping, and on a 360-wide Android phone.
- Every state listed above can be reached (use a debug menu or fake repository).
- TalkBack reads every button and field with a clear label.
- No hard-coded colours, strings (all in ARB files) or limits.

## Prompt to give opencode (paste as is)
```text
Build screen G08 "Request money" for the NovaWallet Flutter app (route /request).
Read docs/COMPONENTS.md and this file (screens/.../G08_request_money/BUILD.md) first, and open light.png and dark.png to match the design exactly.
Use only the shared components from lib/core/ui and colours from the theme extension; if a component is missing, build it in lib/core/ui first.
Put the screen in the feature folder for "Send money" following the existing architecture (presentation / application / data layers, Riverpod for state, go_router for navigation).
Implement every state listed under "States and behaviour", the navigation table, and the data calls listed under "Data and backend" through a repository interface with a fake implementation for now if the endpoint is marked NEW.
Add all text to the ARB localisation files. Add a widget test that pumps the screen in light and dark and a golden test for each.
Do not change other screens. When finished, list the files you created or changed and anything you could not complete.
```
