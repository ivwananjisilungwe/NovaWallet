# APP-05 — Sign in, unlock, recovery (C01–C07) 🔒

**Area:** novawallet-app · **Needs:** APP-04 · **Screens:** C01, C02, C03, C04, C05, C06, C07

---
Follow `prompts/stage-05_flutter-app/_COMMON_SCREEN_RULES.md` exactly (design fidelity protocol, states, fidelity gate, report).

Screens to build (open each folder via `docs/v5/SCREEN_INDEX.md`): C01 · C02 · C03 · C04 · C05 · C06 · C07.

Endpoints: POST /auth/login, POST /pin/verify, /password/forgot, /password/reset, /auth/device/verify, GET /devices.

Specifics:
- C01 sign in; C02 unlock with PIN + biometric prompt, wrong PIN counter display, switch to full sign-in; C07 PIN locked with the server-provided lockedUntil countdown.
- C03/C04 forgot and new password; C05 new phone detected → OTP device verification; C06 session expired (reached from refresh failure or revoked token).
- Auto-lock timing from AGENTS.md §6; FLAG_SECURE on C02/C07.

Integration: sign in → lock (background) → unlock with PIN → wrong PIN ×N → locked → session expired path.

Acceptance: every listed screen passes the fidelity compare in both themes (table in the report), all states built, gates green, flow verified on an emulator against the sandbox backend.
