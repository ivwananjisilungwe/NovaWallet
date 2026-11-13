# AI-04 — KYC assist: capture quality checks & OCR pre-fill 🔒

**Area:** novawallet-api · **Needs:** AI-03 · **Decisions:** DEC-17, DEC-08 · **Screens:** D04–D06, X06

---
Follow `_COMMON_FEATURE_RULES.md`. Read `docs/AI-STRATEGY.md` Phase C.

Do:
1. **Quality checks on upload** (no external service): resolution, blur, glare/brightness, document edges in frame, file type — returned as actionable codes so D04/D05 can say "too dark, retake".
2. **OCR pre-fill** behind an `OcrProvider` interface: extract name, ID number, date of birth from the NRC/passport to pre-fill D06 "Check and submit"; the user confirms/edits; extracted values and confidence stored for the reviewer; mismatches between typed and extracted values flagged on X06.
3. Provider choice: on-device/in-house open-source OCR vs external API. KYC images are highly sensitive: sending them to a third party needs DEC-17 + counsel approval. Default to in-house or `BLOCKED`.
4. **Face match (selfie vs ID photo)** and liveness: interface + reviewer-assist score only if a provider is approved; never auto-approve KYC; humans decide (X06).
5. Duplicate detection: same ID number on another account → flag for review.
6. Evaluation: test set of synthetic/consented sample documents (never real customer documents in the repo); accuracy report per field.

Tests: quality-check thresholds, prefill never bypasses user confirmation, flags on mismatch/duplicate, no images in logs, provider off → manual flow still works.
