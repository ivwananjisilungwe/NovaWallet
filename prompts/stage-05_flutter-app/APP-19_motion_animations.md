# APP-19 — Motion & animations exactly as designed

**Area:** novawallet-app · **Needs:** APP-18 · **Design sources:** `docs/v5/part-05/MOTION.md`, `docs/v5/part-05/lottie/*.json` (light/dark variants), `ALL_ANIMATIONS_frames.png`, `preview/`

---
Read `MOTION.md` fully and list each animation, its trigger, duration, curve, loop behaviour and the screens it belongs to in `docs/app/MOTION_MAP.md`.

Do:
1. Wire every Lottie file to its screen/state (e.g. success check on F06/G06, failed on F07, confetti on goal reached/B07, card freeze on L06, loaders) choosing the light/dark file by theme. Use `NwLottie` with reduce-motion fallback to the static frame.
2. Implement the non-Lottie motion described in MOTION.md (splash progress slide, balance count-up, hero card transitions, list item entrance, sheet spring, PIN dot fill and shake, button press feedback, skeleton shimmer, tab pill slide) with the exact durations and curves; central `NwMotion` constants.
3. Haptics where the design specifies (PIN key, success, error).
4. Performance: 60 fps on a low-end emulator profile; no animation blocks input; animations paused when the app is backgrounded; Lottie caches; total Lottie bytes reported.
5. Tests: motion-constant tests, reduce-motion tests, golden of final frames; a short screen recording or frame sequence captured for each key animation for owner review.

Acceptance: owner reviews each animation against `preview/`; deviations in `FIDELITY_LOG.md`.
