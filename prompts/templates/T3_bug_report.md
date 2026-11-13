# T3 — Fix a bug you found while testing

---
Read root `AGENTS.md` and `<AREA>/AGENTS.md`.

**Bug:** <one sentence>
**Where:** <screen / endpoint / job>
**Steps to reproduce:**
1. …
**Expected:** …
**Actual:** … (paste error text, logs with secrets removed, screenshot path)
**Environment:** local / staging, app flavor, backend commit.

Do:
1. Reproduce it first with a failing automated test (unit, integration, widget, or e2e). If you cannot reproduce, stop and report what you tried.
2. Find the root cause (explain it in 2–4 sentences). Do not patch symptoms.
3. Fix minimally. Make the test pass. Check for the same bug pattern elsewhere and list (don't fix) other occurrences.
4. Run the full gates. Stop and report using the root report format, plus "Root cause" and "Other occurrences".
