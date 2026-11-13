# APP-18 — Push notifications & deep links 🔒

**Area:** novawallet-app · **Needs:** APP-17, BE-1D-06 · **Decisions:** DEC-14

---
Follow `novawallet-app/AGENTS.md`.

Do:
1. Firebase Messaging (only if DEC-14 approved): flavor-specific Firebase config files supplied by the owner outside git (`google-services.json` git-ignored; document placement). Register/refresh/delete the token against the device endpoint; unregister on logout.
2. Handling: foreground (in-app toast as the designs show), background/terminated tap → deep link; **payloads carry no amounts/names** (BE-1D-06) so the app fetches details after unlock; if the app is locked the link waits until C02 succeeds, then navigates.
3. Android notification channels per category (money, security, KYC, promos) with importance matching BE preferences; small icon from the brand assets.
4. **Deep links:** App Links (`https://<domain>/…`) + a custom scheme for QR/payment requests, declared routes mapping to screens (request pay Q05, receipt, KYC status, card, notification target). Every link is validated against an allow-list; links needing auth wait for session + unlock; links never carry secrets. `assetlinks.json` template for the owner to host.
5. Permission re-prompt logic honouring A07 and OS rules; settings deep link when denied.
6. Tests: deep-link parser/guard table tests, locked-app link queue, payload without PII renders safely, token lifecycle.

Acceptance: on a device with the staging backend, each notification type opens the right screen after unlock; hijack test with a malformed link is rejected.
