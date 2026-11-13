# BE-1D-05 — User lookup, contacts, QR, name check, linked mobile money 🔒

**Area:** novawallet-api · **Needs:** BE-1D-04 · **Screens:** G01, G07, H01, H02, O06, Q13

---
Follow `_COMMON_FEATURE_RULES.md`. Privacy-critical: write a short privacy review in the plan (what is exposed, to whom, abuse cases) before coding.

Endpoints:
- `GET /api/v1/users/lookup?phone=` — find a NovaWallet user to send to (G01). Returns display name (first name + initial), masked phone, avatar colour, a stable recipient token — never the internal user id, email, balance, or KYC data.
- `POST /api/v1/contacts/match` — match the user's phone contacts (Q13): accept only hashed, normalised numbers, capped batch size, returns which hashes are NovaWallet users. No storage of the contact list.
- `GET /api/v1/qr/me` + a QR resolve endpoint (H01/H02): signed, versioned QR payload with expiry for dynamic amounts; static personal QR carries only the recipient token.
- `GET /api/v1/payouts/name-check?msisdn=&network=` — mobile-money account name before sending (G07). Uses the provider's name-lookup if documented; otherwise returns `NAME_CHECK_UNAVAILABLE` (never a made-up name).
- `GET /api/v1/payment-methods` + add/remove linked mobile-money numbers (O06, F01): ownership verified by OTP to that number; masked in responses.

Do:
1. Anti-enumeration: strict per-user rate limits, uniform timing class and response shape for found/not found where the design allows, audit bursts, block after abuse.
2. Users can opt out of being discoverable by phone (setting; default per owner decision — record as a DEC).
3. Recent recipients and favourites endpoints if G01 shows them (check the BUILD.md).
4. QR signatures use a server key from env; verify on resolve; reject expired/forged.

Tests: masking, opt-out, enumeration limits, QR forgery/expiry, contact batch caps, linked-number ownership.
