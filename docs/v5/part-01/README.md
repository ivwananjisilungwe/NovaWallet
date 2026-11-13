# NovaWallet v5 · Part-01-Launch-SignUp-SignIn-KYC

Each screen has its own folder with light and dark designs (PNG 3× and SVG) and a `BUILD.md` with full build instructions and a ready prompt for opencode. Build `docs/COMPONENTS.md` first, then the screens in the order below (this is the order a user meets them).

## Launch & onboarding
| ID | Screen | Route | Folder |
|---|---|---|---|
| A01 | Splash | `/splash` | [screens/A-launch-onboarding/A01_splash](screens/A-launch-onboarding/A01_splash/BUILD.md) |
| A02 | Onboarding 1 – Track | `/onboarding?page=0` | [screens/A-launch-onboarding/A02_onboarding_track](screens/A-launch-onboarding/A02_onboarding_track/BUILD.md) |
| A03 | Onboarding 2 – Budget | `/onboarding?page=1` | [screens/A-launch-onboarding/A03_onboarding_budget](screens/A-launch-onboarding/A03_onboarding_budget/BUILD.md) |
| A04 | Onboarding 3 – Save | `/onboarding?page=2` | [screens/A-launch-onboarding/A04_onboarding_save](screens/A-launch-onboarding/A04_onboarding_save/BUILD.md) |
| A05 | Welcome | `/welcome` | [screens/A-launch-onboarding/A05_welcome](screens/A-launch-onboarding/A05_welcome/BUILD.md) |
| A06 | Choose language | `/language` | [screens/A-launch-onboarding/A06_choose_language](screens/A-launch-onboarding/A06_choose_language/BUILD.md) |
| A07 | Allow notifications | `/permissions/notifications` | [screens/A-launch-onboarding/A07_allow_notifications](screens/A-launch-onboarding/A07_allow_notifications/BUILD.md) |

## Create account
| ID | Screen | Route | Folder |
|---|---|---|---|
| B01 | Phone number | `/signup/phone` | [screens/B-create-account/B01_phone_number](screens/B-create-account/B01_phone_number/BUILD.md) |
| B02 | Verify code | `/signup/otp` | [screens/B-create-account/B02_verify_code](screens/B-create-account/B02_verify_code/BUILD.md) |
| B03 | Your details | `/signup/details` | [screens/B-create-account/B03_your_details](screens/B-create-account/B03_your_details/BUILD.md) |
| B04 | Create PIN | `/signup/pin` | [screens/B-create-account/B04_create_pin](screens/B-create-account/B04_create_pin/BUILD.md) |
| B05 | Confirm PIN – mismatch | `/signup/pin?step=confirm` | [screens/B-create-account/B05_confirm_pin](screens/B-create-account/B05_confirm_pin/BUILD.md) |
| B06 | Enable fingerprint | `/signup/biometrics` | [screens/B-create-account/B06_enable_biometrics](screens/B-create-account/B06_enable_biometrics/BUILD.md) |
| B07 | Account ready | `/signup/done` | [screens/B-create-account/B07_account_ready](screens/B-create-account/B07_account_ready/BUILD.md) |

## Sign in
| ID | Screen | Route | Folder |
|---|---|---|---|
| C01 | Sign in | `/signin` | [screens/C-sign-in/C01_sign_in](screens/C-sign-in/C01_sign_in/BUILD.md) |
| C02 | Unlock with PIN | `/unlock` | [screens/C-sign-in/C02_unlock_pin](screens/C-sign-in/C02_unlock_pin/BUILD.md) |
| C03 | Forgot password | `/password/forgot` | [screens/C-sign-in/C03_forgot_password](screens/C-sign-in/C03_forgot_password/BUILD.md) |
| C04 | New password | `/password/new` | [screens/C-sign-in/C04_new_password](screens/C-sign-in/C04_new_password/BUILD.md) |
| C05 | New phone detected | `/signin/new-device` | [screens/C-sign-in/C05_new_device_check](screens/C-sign-in/C05_new_device_check/BUILD.md) |
| C06 | Session expired | `/session-expired` | [screens/C-sign-in/C06_session_expired](screens/C-sign-in/C06_session_expired/BUILD.md) |
| C07 | PIN locked | `/pin-locked` | [screens/C-sign-in/C07_pin_locked](screens/C-sign-in/C07_pin_locked/BUILD.md) |

## Verify ID (KYC)
| ID | Screen | Route | Folder |
|---|---|---|---|
| D01 | KYC intro | `/kyc` | [screens/D-verify-id-kyc/D01_kyc_intro](screens/D-verify-id-kyc/D01_kyc_intro/BUILD.md) |
| D02 | Account levels | `/kyc/levels` | [screens/D-verify-id-kyc/D02_kyc_levels](screens/D-verify-id-kyc/D02_kyc_levels/BUILD.md) |
| D03 | Choose your ID | `/kyc/document` | [screens/D-verify-id-kyc/D03_kyc_choose_document](screens/D-verify-id-kyc/D03_kyc_choose_document/BUILD.md) |
| D04 | Capture NRC | `/kyc/capture?side=front` | [screens/D-verify-id-kyc/D04_kyc_capture_nrc](screens/D-verify-id-kyc/D04_kyc_capture_nrc/BUILD.md) |
| D05 | Selfie | `/kyc/selfie` | [screens/D-verify-id-kyc/D05_kyc_selfie](screens/D-verify-id-kyc/D05_kyc_selfie/BUILD.md) |
| D06 | Check and submit | `/kyc/review` | [screens/D-verify-id-kyc/D06_kyc_check_submit](screens/D-verify-id-kyc/D06_kyc_check_submit/BUILD.md) |
| D07 | ID in review | `/kyc/status` | [screens/D-verify-id-kyc/D07_kyc_in_review](screens/D-verify-id-kyc/D07_kyc_in_review/BUILD.md) |
| D08 | Verified | `/kyc/approved` | [screens/D-verify-id-kyc/D08_kyc_approved](screens/D-verify-id-kyc/D08_kyc_approved/BUILD.md) |
| D09 | Action needed | `/kyc/action-needed` | [screens/D-verify-id-kyc/D09_kyc_action_needed](screens/D-verify-id-kyc/D09_kyc_action_needed/BUILD.md) |
