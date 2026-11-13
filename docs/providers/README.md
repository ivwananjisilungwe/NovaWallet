# Provider documents

The owner saves official provider API documents here. **Agents build only against contracts documented in this folder.** If a document is missing, the prompt that needs it stops with `BLOCKED: <document>` and the feature runs on the built-in sandbox simulator.

| Folder | Provider | Needed by | Status |
|---|---|---|---|
| `lipila/` | Lipila: disbursements, collections, webhook signing spec | PAY-02, BE-1C-5, PAY-03 | collections signing spec **missing** (BLK-01) |
| `sms/` | SMS / OTP gateway | BE-1D-02 | missing |
| `bills/` | Bills, airtime and data aggregator | PAY-05 | missing |
| `cards/` | Card-issuing partner | PAY-06 | missing |

Rules:
- Save PDFs or exported pages unchanged, with the version and date in the filename.
- **Never put keys, secrets or credentials here.** Sandbox keys live in env files outside git.
