# APP-20 — Resilience, accessibility, i18n, performance, size

**Area:** novawallet-app · **Needs:** APP-19

---
Hardening across everything built in APP-02…19. Fix, do not just report. Do not change the visual design while fixing.

1. **Resilience:** every route has loading/empty/error/offline; interrupted uploads resume or restart cleanly; process death during a money flow restores to the safe in-progress/status screen (never re-submits); clock changes; low memory; rotation locked or handled per design; keyboard overlap.
2. **Accessibility:** TalkBack walkthrough of every flow; Semantics labels (amounts as words, status chips, icon buttons), focus order, 48dp targets, contrast AA test helper in both themes, text scale 1.3/2.0 and bold text without clipping (scroll), reduce-motion, colour-independent status. Add `meetsGuideline` tests per screen.
3. **i18n:** all strings in ARB (`app_en.arb`), ICU plurals, no concatenated sentences; pseudo-locale test to catch hardcoded strings/overflow; `app_bem.arb` and `app_nya.arb` created (owner supplies translations; English fallback), language switch (A06/O08) applies instantly. Exact design copy must remain identical in English.
4. **Performance** on a low-end Android profile: cold start first frame < 2 s; scrolling 60 fps on 500-row lists; memory stable over 10 minutes; image/Lottie cache sizes; no jank in hero/chart screens. Before/after numbers.
5. **Size:** `--analyze-size`, tree-shake icons, remove unused assets/fonts weights, deferred loading for heavy assets; target release AAB < 25 MB (report actual).
6. **Low bandwidth:** throttled 2G/3G tests — timeouts, retries, no request storms, payload sizes of the main screens.
7. Security pass: no sensitive data in logs/screenshots/backups; clipboard handling for tokens/OTP; FLAG_SECURE verified on every sensitive screen.

Acceptance: before/after tables, a11y findings fixed vs remaining, pseudo-locale result, size report, gates + all fidelity tests still green.
