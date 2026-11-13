# APP-03 — Launch & onboarding (A01–A07)

**Area:** novawallet-app · **Needs:** APP-02 · **Screens:** A01, A02, A03, A04, A05, A06, A07

---
Follow `prompts/stage-05_flutter-app/_COMMON_SCREEN_RULES.md` exactly (design fidelity protocol, states, fidelity gate, report).

Screens to build (open each folder via `docs/v5/SCREEN_INDEX.md`): A01 · A02 · A03 · A04 · A05 · A06 · A07.

Endpoints: GET /app/config; push permission only requests the OS permission here (token registration is wired in APP-17).

Specifics:
- A01 splash restores session and routes per its Navigation table (first install → A02, onboarded no account → A05, signed in before → C02, version too low → P06, maintenance → P05); never blocks longer than 3 s.
- A02–A04 carousel exactly as drawn (illustrations from assets, page indicator, Skip/Next, onboarded flag persisted).
- A06 language choice persisted (English active; other languages shown only if the design shows them enabled — strings via ARB).
- A07 notification permission rationale then OS prompt; both Allow and Not now paths reach A05/B01 per BUILD.md.

Integration: fresh install → A01 → A02–A04 → A05; second launch skips onboarding.

Acceptance: every listed screen passes the fidelity compare in both themes (table in the report), all states built, gates green, flow verified on an emulator against the sandbox backend.
