# APP-22 — Full design audit & fix loop (run until clean)

**Area:** novawallet-app · **Needs:** APP-21 · Run in two sessions: **22a audit (read-only)**, then **22b fix** with T2, then audit again.

---
## 22a — Audit (do not edit source)
Output `docs/app/DESIGN_AUDIT_<n>.md`. For **every** app screen code in `docs/v5/SCREEN_INDEX.md`:
1. Run the app on an emulator (sandbox seed) and capture a screenshot of each state/theme (or use the fidelity PNGs where equivalent). Compare with the design PNG/SVG visually **and** with the compare tool.
2. Check: layout, spacing, radii, colours, type sizes/weights, icons, illustrations, copy (exact string match against SVG text), status bar/nav behaviour, state coverage, motion, navigation per BUILD.md table, data shown matches the design's fields, light and dark.
3. Check cross-cutting: text scale 2.0, 360/412 widths, small height 640, TalkBack order, offline/error states, performance feel, money-flow safety (idempotency, pending wording), trust cues.
4. Table of findings: ID · screen · severity (blocker / major / minor) · what differs · design reference (file) · fix size S/M/L. End with a fix list grouped into prompts of ≤ 1 day each.
Honesty rule: only report what you verified by running or by reading code; list what you could not check.

## 22b — Fix
Use `templates/T2_fix_findings.md` per group. After fixes, rerun 22a. **Done when there are no blockers or majors and `FIDELITY_LOG.md` deviations are all owner-approved.**
