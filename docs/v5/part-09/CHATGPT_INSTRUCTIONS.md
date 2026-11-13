# ChatGPT instructions: Part 09 extra flows (Q01–Q13)

Paste this into your Flutter build chat after Batch 4 (step 4.13) of `CHATGPT_BUILD_GUIDE_BATCHES_1-4.md`. Some steps can be done earlier; the "Needs" column says when.

```text
Next I want the 13 extra flow screens from Part 09 (Q01–Q13). I'm uploading the Part 09 README and each BUILD.md as we go.
Use the same rules as before (one prompt per step, design PNGs, shared components only, fake repository when an endpoint is marked NEW, tests, flutter analyze/test, list changed files).
For every NEW endpoint, first write the backend prompt for novawallet-api (Flyway migration, tests, risk IDs if any), then the Flutter prompt.
Give me prompt Q-1 only.
```

| Step | Screens | Needs | Backend first? |
|---|---|---|---|
| Q-1 | Q12 Terms and privacy (markdown from server, versioned acceptance) | Use in B03 sign-up | Yes: `/legal/*` |
| Q-2 | Q13 Find friends + contact hashing | G01 | Yes: `/contacts/match` |
| Q-3 | Q11 Verify email (+ deep link handling) | O02 | Uses existing `/email/verify` (fix SEC-03/SEC-07 first) |
| Q-4 | Q01–Q03 Reset PIN (code → selfie → new PIN) | C02, C07 | Yes: PIN reset + selfie match |
| Q-5 | Q10 Change phone number | O02 | Yes: phone change flow |
| Q-6 | Q04 Data bundles tab | K04 | Yes: bundles endpoint (bill aggregator) |
| Q-7 | Q05 Incoming money request (from push + E06) | G08 | Yes: payment requests pay/decline |
| Q-8 | Q08/Q09 Goal add / take out sheets | M05 | Yes: goal deposit/withdraw (ledger sub-accounts) |
| Q-9 | Q06/Q07 Create Chilimba (tracker) | M09 | Yes: chilimba groups |

Done when every Q screen matches its light/dark PNG, every state in its BUILD.md is reachable with the fake repository, and the real endpoint works in sandbox.
