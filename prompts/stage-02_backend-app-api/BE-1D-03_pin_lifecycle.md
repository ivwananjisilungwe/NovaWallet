# BE-1D-03 — PIN lifecycle 🔒

**Area:** novawallet-api · **Needs:** BE-1D-02 · **Screens:** B04, B05, C02, C07, G04, G05, O05, Q01–Q03

---
Follow `_COMMON_FEATURE_RULES.md`.

Endpoints: existing `POST /pin`, `POST /pin/change`; new `POST /api/v1/pin/verify` (C02 unlock / step-up), PIN reset flow for Q01 (code) → Q02 (selfie) → Q03 (new PIN).

Do:
1. `pin/verify`: returns success/failure only; durable attempt counter; lockout after N failures with increasing durations (C07 shows the lock state and remaining time — return `lockedUntil`); never reveals whether a PIN is set to unauthenticated callers.
2. One shared `PinVerifier` used by verify and by every money endpoint so counters and lockouts are consistent. Wrong PIN on a money request returns a specific code with attempts remaining (G05).
3. Reject weak PINs server-side (repeated, sequential, birth-year patterns if DOB is known).
4. **Reset PIN:** OTP (purpose pin-reset) → selfie step. The selfie check is a manual/admin or automated comparison against the KYC selfie: implement the request + review state machine and an admin decision endpoint; automated face match is `BLOCKED` unless a provider is chosen. After approval the user sets a new PIN; all sessions except the current are revoked; a cooling-off limit on outgoing money for 24 h is configurable.
5. PIN hashes use a slow password hash (same encoder as passwords); never log; never return.
6. Audit every PIN event without the PIN.

Tests: lockout timing with a fake clock, counter shared across endpoints, reset state machine, cooling-off enforcement, concurrency on attempts counter.
