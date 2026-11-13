# NovaWallet v5: start here

| Part | Zip | What's inside | ChatGPT instructions |
|---|---|---|---|
| 01 | `NovaWallet-v5-Part-01-Launch-SignUp-SignIn-KYC.zip` | 30 screens: splash → onboarding → sign up → sign in → ID verification | `docs/CHATGPT_BUILD_GUIDE_BATCHES_1-4.md` (inside Parts 1–4) |
| 02 | `…Part-02-Home-TopUp-Send-Receive-Withdraw.zip` | 30 screens: Home, notifications, top up, send, request, split, QR, withdraw | same guide |
| 03 | `…Part-03-Activity-Bills-Store-Cards.zip` | 21 screens: Payments tab, activity, receipts, statements, bills, airtime, store, cards | same guide |
| 04 | `…Part-04-Insights-NovaAI-Settings-Offline.zip` | 33 screens: Insights, budgets, goals, Chilimba, Nova AI, Me/settings, offline and loading | same guide |
| 05 | `…Part-05-Animations.zip` | 19 Lottie files + GIF previews + MOTION.md | `CHATGPT_INSTRUCTIONS.md` |
| 06 | `…Part-06-Brand-Icons-Illustrations.zip` | Logos, app icons, splash, 125 icons, 22 category tiles, 26 illustrations, fonts, tokens.json, DESIGN_SYSTEM.md | `CHATGPT_INSTRUCTIONS.md` |
| 07 | `…Part-07-Admin-Console.zip` | 16 admin web screens (light/dark) + ADMIN_GUIDE.md | inside `ADMIN_GUIDE.md` §5 |
| 08 | `…Part-08-Architecture-Backend-Payments-Cards-AI.zip` | Architecture, backend completion plan, mobile money, virtual cards, AI features, compliance | `07_CHATGPT_INSTRUCTIONS_BACKEND.md` |
| 09 | `…Part-09-Extra-Flows.zip` | 13 screens: reset PIN, data bundles, incoming request, create Chilimba, goal sheets, change number, verify email, terms, find friends | `CHATGPT_INSTRUCTIONS.md` |

**Totals:** 127 app screens + 16 admin screens, each in light and dark with its own BUILD.md.

## Order of work

1. Create the new Flutter repo and copy the Part 1–4 and Part 9 `screens/` folders into `design/`, the brand pack into `design/brand/` and the animations into `design/animations/`.
2. **Flutter chat:** paste the master instruction from the build guide, then do Phase 0 (it uses Part 6 brand + Part 5 motion).
3. **Backend chat:** paste `07_CHATGPT_INSTRUCTIONS_BACKEND.md` and do Block 1 (Batch 3 + security) while the Flutter foundation is built.
4. Build Batches 1–4 (Flutter), running each backend step in the backend chat just before the screen that needs it.
5. Part 9 extra flows.
6. **Admin chat:** Part 7, once payments and KYC work in sandbox.
7. Ledger, orchestration and AI blocks, then the pilot (after the compliance checklist in Part 8 doc 06).
