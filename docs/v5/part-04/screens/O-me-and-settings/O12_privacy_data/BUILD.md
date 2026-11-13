# O12 · Privacy and data

**Flow:** Me and settings  
**Route:** `/me/privacy`  
**Design files:** `light.png`, `dark.png` (3×, 1170 × 2532) and `light.svg`, `dark.svg` in this folder.

| Light | Dark |
|---|---|
| ![light](light.png) | ![dark](dark.png) |

## Purpose
Data rights and consent choices.

## Layout (top to bottom, 390 × 844 canvas, 20 side padding)
1. TopBar “Privacy and data”.
2. Group Your data: Download my data, What we keep and why.
3. Group Choices: toggles for contacts, usage stats, offers, Nova AI access.
4. Info banner about the Data Protection Act.
5. Link “Read the full Privacy Notice”.

## Components used (see ../../../docs/COMPONENTS.md)
- NwListGroup
- NwToggle
- NwBanner

## States and behaviour
- Turning off contacts deletes uploaded hashes.

## Navigation
| When | Go to |
|---|---|
| Download | Request → email when ready |

## Data and backend
- NEW `GET/PUT /api/v1/privacy/consents`, `POST /api/v1/privacy/export`.

## Rules and notes
- Consent changes are logged with time for audit.

## Done when
- Matches `light.png` and `dark.png` side by side at 390 × 844 (spacing within 2 px, same colours from theme tokens).
- Works at text scale 1.3× without clipping, and on a 360-wide Android phone.
- Every state listed above can be reached (use a debug menu or fake repository).
- TalkBack reads every button and field with a clear label.
- No hard-coded colours, strings (all in ARB files) or limits.

## Prompt to give opencode (paste as is)
```text
Build screen O12 "Privacy and data" for the NovaWallet Flutter app (route /me/privacy).
Read docs/COMPONENTS.md and this file (screens/.../O12_privacy_data/BUILD.md) first, and open light.png and dark.png to match the design exactly.
Use only the shared components from lib/core/ui and colours from the theme extension; if a component is missing, build it in lib/core/ui first.
Put the screen in the feature folder for "Me and settings" following the existing architecture (presentation / application / data layers, Riverpod for state, go_router for navigation).
Implement every state listed under "States and behaviour", the navigation table, and the data calls listed under "Data and backend" through a repository interface with a fake implementation for now if the endpoint is marked NEW.
Add all text to the ARB localisation files. Add a widget test that pumps the screen in light and dark and a golden test for each.
Do not change other screens. When finished, list the files you created or changed and anything you could not complete.
```
