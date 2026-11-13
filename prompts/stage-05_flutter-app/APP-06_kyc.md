# APP-06 — KYC (D01–D09) 🔒

**Area:** novawallet-app · **Needs:** APP-05 · **Screens:** D01, D02, D03, D04, D05, D06, D07, D08, D09

---
Follow `prompts/stage-05_flutter-app/_COMMON_SCREEN_RULES.md` exactly (design fidelity protocol, states, fidelity gate, report).

Screens to build (open each folder via `docs/v5/SCREEN_INDEX.md`): D01 · D02 · D03 · D04 · D05 · D06 · D07 · D08 · D09.

Endpoints: GET /kyc/tiers, GET /kyc/status, POST /kyc/documents/upload, POST /kyc/submit.

Specifics:
- D02 account levels from the API (never hardcoded); D03 ID type choice; D04 NRC capture with camera frame guide + gallery fallback, permission rationale and denied states; D05 selfie with oval guide; client-side compression below 10 MB; upload progress, cancel, retry, per-document redo.
- D06 check and submit; D07 in review; D08 verified; D09 action needed showing the server rejection reason and the document to redo.
- Images never persisted after upload; FLAG_SECURE on D04–D06; quality hints use the backend codes from AI-04.

Integration: seeded user at level 0 → upload both documents against sandbox → in review → admin approval (sandbox seed toggle) → verified.

Acceptance: every listed screen passes the fidelity compare in both themes (table in the report), all states built, gates green, flow verified on an emulator against the sandbox backend.
