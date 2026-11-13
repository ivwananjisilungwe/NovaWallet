# ADM-03 — KYC queue and review 🔒

**Area:** novawallet-admin · **Needs:** ADM-02

---
Follow `prompts/stage-06_admin-console/_COMMON_ADMIN_RULES.md` exactly.

Specifics:
- Screens: X05 queue (age, level requested, flags like duplicate ID/OCR mismatch), X06 review detail (document viewer with zoom/rotate, selfie vs ID side-by-side, extracted vs typed data, history).
- Decisions: approve to a level, reject with reason codes (catalogue from BE-1D-04), request re-upload of one document; reason required; four-eyes if configured; keyboard shortcuts for reviewers.
- Documents rendered in-app only via authenticated blob URLs, never cached to disk or put in URLs; watermark overlay with reviewer id; PII reveal rules.

Acceptance: listed screens pass the fidelity compare in light and dark, role-matrix tests green, gates green, flows verified in a browser against the sandbox backend.
