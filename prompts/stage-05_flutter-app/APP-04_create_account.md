# APP-04 — Create account: phone, OTP, details, PIN, biometrics (B01–B07) 🔒

**Area:** novawallet-app · **Needs:** APP-03 · **Screens:** B01, B02, B03, B04, B05, B06, B07

---
Follow `prompts/stage-05_flutter-app/_COMMON_SCREEN_RULES.md` exactly (design fidelity protocol, states, fidelity gate, report).

Screens to build (open each folder via `docs/v5/SCREEN_INDEX.md`): B01 · B02 · B03 · B04 · B05 · B06 · B07.

Endpoints: POST /auth/otp/send, /auth/otp/verify, /auth/register (phone-first flow per BE-1D-02), POST /pin, email verify entry points.

Specifics:
- B01 Zambian phone entry with +260 prefix handling and operator detection exactly as drawn; B02 6-digit OTP boxes (NwOtp), resend countdown, wrong/expired code states from BUILD.md, rate-limit messaging.
- B03 details form with the validations in BUILD.md; terms/privacy links open Q12.
- B04/B05 PIN creation + confirm mismatch state (shake, message as drawn); weak-PIN rejection mapped from the backend code; PIN never stored or logged.
- B06 enable biometrics (local_auth) — enrolling only unlocks the app, never authorises money; B07 account ready → routes to D01 (KYC intro) or Home per BUILD.md.
- Delete the temporary debug login from APP-02.

Integration: register a new sandbox user end to end through B07.

Acceptance: every listed screen passes the fidelity compare in both themes (table in the report), all states built, gates green, flow verified on an emulator against the sandbox backend.
