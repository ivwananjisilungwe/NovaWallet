# O03 · Security centre

**Flow:** Me and settings  
**Route:** `/me/security`  
**Design files:** `light.png`, `dark.png` (3×, 1170 × 2532) and `light.svg`, `dark.svg` in this folder.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
One place to see and improve account safety.

## Layout (top to bottom, 390 × 844 canvas, 20 side padding)
1. TopBar “Security centre”.
2. Hero card: white ring 4/5 + “Well protected” + sub.
3. Checklist card: success or warning icons; missing item has a small primary “Turn on” button.
4. List group: Change PIN, Change password, Devices (2).
5. Danger soft row “Lock my account now”.

## Components used (see ../../../docs/COMPONENTS.md)
- Ring progress
- Checklist
- NwListGroup

## States and behaviour
- Lock account → confirm sheet → all sessions revoked, card frozen; unlock via support + selfie.

## Navigation
| When | Go to |
|---|---|
| Change PIN | O05 |
| Devices | O04 |

## Data and backend
- NEW `GET /api/v1/security/overview`, `POST /api/v1/security/lock`.

## Done when
- Matches `light.png` and `dark.png` side by side at 390 × 844 (spacing within 2 px, same colours from theme tokens).
- Works at text scale 1.3× without clipping, and on a 360-wide Android phone.
- Every state listed above can be reached (use a debug menu or fake repository).
- TalkBack reads every button and field with a clear label.
- No hard-coded colours, strings (all in ARB files) or limits.

## Prompt to give opencode (paste as is)
```text
Build screen O03 "Security centre" for the NovaWallet Flutter app (route /me/security).
Read docs/COMPONENTS.md and this file (screens/.../O03_security_centre/BUILD.md) first, and open light.png and dark.png to match the design exactly.
Use only the shared components from lib/core/ui and colours from the theme extension; if a component is missing, build it in lib/core/ui first.
Put the screen in the feature folder for "Me and settings" following the existing architecture (presentation / application / data layers, Riverpod for state, go_router for navigation).
Implement every state listed under "States and behaviour", the navigation table, and the data calls listed under "Data and backend" through a repository interface with a fake implementation for now if the endpoint is marked NEW.
Add all text to the ARB localisation files. Add a widget test that pumps the screen in light and dark and a golden test for each.
Do not change other screens. When finished, list the files you created or changed and anything you could not complete.
```
