# v5 Screen Index (generated from every BUILD.md)

Each screen folder holds `BUILD.md` (spec), `light.png`, `dark.png` (3×, 1170 × 2532) and SVGs. The PNGs are the visual truth.

| Code | Screen | Route | Folder | Endpoints named in the spec |
|---|---|---|---|---|
| A01 | Splash | `/splash` | `docs/v5/part-01/screens/A-launch-onboarding/A01_splash` | GET /api/v1/app/config |
| A02 | Onboarding 1 – Track | `/onboarding?page=0` | `docs/v5/part-01/screens/A-launch-onboarding/A02_onboarding_track` |  |
| A03 | Onboarding 2 – Budget | `/onboarding?page=1` | `docs/v5/part-01/screens/A-launch-onboarding/A03_onboarding_budget` |  |
| A04 | Onboarding 3 – Save | `/onboarding?page=2` | `docs/v5/part-01/screens/A-launch-onboarding/A04_onboarding_save` |  |
| A05 | Welcome | `/welcome` | `docs/v5/part-01/screens/A-launch-onboarding/A05_welcome` |  |
| A06 | Choose language | `/language` | `docs/v5/part-01/screens/A-launch-onboarding/A06_choose_language` | PUT /api/v1/users/me |
| A07 | Allow notifications | `/permissions/notifications` | `docs/v5/part-01/screens/A-launch-onboarding/A07_allow_notifications` |  |
| B01 | Phone number | `/signup/phone` | `docs/v5/part-01/screens/B-create-account/B01_phone_number` | POST /api/v1/auth/otp/send |
| B02 | Verify code | `/signup/otp` | `docs/v5/part-01/screens/B-create-account/B02_verify_code` | POST /api/v1/auth/otp/verify |
| B03 | Your details | `/signup/details` | `docs/v5/part-01/screens/B-create-account/B03_your_details` | POST /api/v1/auth/register |
| B04 | Create PIN | `/signup/pin` | `docs/v5/part-01/screens/B-create-account/B04_create_pin` |  |
| B05 | Confirm PIN – mismatch | `/signup/pin?step=confirm` | `docs/v5/part-01/screens/B-create-account/B05_confirm_pin` | POST /api/v1/auth/register |
| B06 | Enable fingerprint | `/signup/biometrics` | `docs/v5/part-01/screens/B-create-account/B06_enable_biometrics` |  |
| B07 | Account ready | `/signup/done` | `docs/v5/part-01/screens/B-create-account/B07_account_ready` |  |
| C01 | Sign in | `/signin` | `docs/v5/part-01/screens/C-sign-in/C01_sign_in` | POST /api/v1/auth/login |
| C02 | Unlock with PIN | `/unlock` | `docs/v5/part-01/screens/C-sign-in/C02_unlock_pin` | POST /api/v1/auth/refresh<br>POST /api/v1/pin/verify |
| C03 | Forgot password | `/password/forgot` | `docs/v5/part-01/screens/C-sign-in/C03_forgot_password` | POST /api/v1/password/forgot |
| C04 | New password | `/password/new` | `docs/v5/part-01/screens/C-sign-in/C04_new_password` | POST /api/v1/password/reset |
| C05 | New phone detected | `/signin/new-device` | `docs/v5/part-01/screens/C-sign-in/C05_new_device_check` | POST /api/v1/auth/device/verify |
| C06 | Session expired | `/session-expired` | `docs/v5/part-01/screens/C-sign-in/C06_session_expired` |  |
| C07 | PIN locked | `/pin-locked` | `docs/v5/part-01/screens/C-sign-in/C07_pin_locked` |  |
| D01 | KYC intro | `/kyc` | `docs/v5/part-01/screens/D-verify-id-kyc/D01_kyc_intro` | GET /api/v1/kyc/status |
| D02 | Account levels | `/kyc/levels` | `docs/v5/part-01/screens/D-verify-id-kyc/D02_kyc_levels` | GET /api/v1/kyc/tiers |
| D03 | Choose your ID | `/kyc/document` | `docs/v5/part-01/screens/D-verify-id-kyc/D03_kyc_choose_document` |  |
| D04 | Capture NRC | `/kyc/capture?side=front` | `docs/v5/part-01/screens/D-verify-id-kyc/D04_kyc_capture_nrc` | POST /api/v1/kyc/documents/upload |
| D05 | Selfie | `/kyc/selfie` | `docs/v5/part-01/screens/D-verify-id-kyc/D05_kyc_selfie` |  |
| D06 | Check and submit | `/kyc/review` | `docs/v5/part-01/screens/D-verify-id-kyc/D06_kyc_check_submit` | POST /api/v1/kyc/submit |
| D07 | ID in review | `/kyc/status` | `docs/v5/part-01/screens/D-verify-id-kyc/D07_kyc_in_review` | GET /api/v1/kyc/status |
| D08 | Verified | `/kyc/approved` | `docs/v5/part-01/screens/D-verify-id-kyc/D08_kyc_approved` | GET /api/v1/kyc/status<br>GET /api/v1/wallets/me |
| D09 | Action needed | `/kyc/action-needed` | `docs/v5/part-01/screens/D-verify-id-kyc/D09_kyc_action_needed` | GET /api/v1/kyc/status |
| E01 | Home – ID in review | `/home` | `docs/v5/part-02/screens/E-home/E01_home_pending_id` | GET /api/v1/kyc/status<br>GET /api/v1/notifications/unread-count<br>GET /api/v1/users/me |
| E02 | Home | `/home` | `docs/v5/part-02/screens/E-home/E02_home` | GET /api/v1/insights/summary<br>GET /api/v1/wallets/me<br>GET /api/v1/wallets/{walletId}/transactions?size=5 |
| E03 | Home – balance hidden | `/home` | `docs/v5/part-02/screens/E-home/E03_home_balance_hidden` |  |
| E04 | Home – new user | `/home` | `docs/v5/part-02/screens/E-home/E04_home_new_user` |  |
| E05 | Home – service notice | `/home` | `docs/v5/part-02/screens/E-home/E05_home_service_notice` | GET /api/v1/app/notices |
| E06 | Notifications | `/notifications` | `docs/v5/part-02/screens/E-home/E06_notifications` | GET /api/v1/notifications?category=<br>PUT /api/v1/notifications/read-all |
| E07 | Notifications – empty | `/notifications?category=security` | `docs/v5/part-02/screens/E-home/E07_notifications_empty` |  |
| F01 | Add money – method | `/topup` | `docs/v5/part-02/screens/F-top-up/F01_topup_method` | GET /api/v1/payment-methods |
| F02 | Top up – amount | `/topup/amount?provider=AIRTEL` | `docs/v5/part-02/screens/F-top-up/F02_topup_amount` | GET /api/v1/fees/estimate?type=DEPOSIT&amount= |
| F03 | Top up – over limit | `/topup/amount` | `docs/v5/part-02/screens/F-top-up/F03_topup_over_limit` | GET /api/v1/wallets/me |
| F04 | Top up – review | `/topup/review` | `docs/v5/part-02/screens/F-top-up/F04_topup_review` | POST /api/v1/wallets/{walletId}/collections |
| F05 | Approve on your phone | `/topup/waiting` | `docs/v5/part-02/screens/F-top-up/F05_topup_approve_on_phone` | GET /api/v1/transactions/{reference}<br>POST /api/v1/webhooks/lipila/collections |
| F06 | Top up – success | `/topup/result` | `docs/v5/part-02/screens/F-top-up/F06_topup_success` |  |
| F07 | Top up – failed | `/topup/result` | `docs/v5/part-02/screens/F-top-up/F07_topup_failed` |  |
| F08 | Top up – pending | `/topup/result` | `docs/v5/part-02/screens/F-top-up/F08_topup_pending` |  |
| G01 | Send – choose recipient | `/send` | `docs/v5/part-02/screens/G-send-money/G01_send_recipient` | GET /api/v1/users/lookup?phone=<br>POST /api/v1/contacts/match |
| G02 | Send – amount | `/send/amount` | `docs/v5/part-02/screens/G-send-money/G02_send_amount` | GET /api/v1/fees/estimate?type=TRANSFER |
| G03 | Send – review | `/send/review` | `docs/v5/part-02/screens/G-send-money/G03_send_review` |  |
| G04 | Send – enter PIN | `/send/review (sheet)` | `docs/v5/part-02/screens/G-send-money/G04_send_pin` | POST /api/v1/transfers |
| G05 | Send – wrong PIN | `/send/review (sheet)` | `docs/v5/part-02/screens/G-send-money/G05_send_wrong_pin` |  |
| G06 | Send – success | `/send/result` | `docs/v5/part-02/screens/G-send-money/G06_send_success` |  |
| G07 | Send to mobile money | `/send/mobile-money` | `docs/v5/part-02/screens/G-send-money/G07_send_mobile_money` | GET /api/v1/payouts/name-check?msisdn=&network=<br>POST /api/v1/wallets/{walletId}/payouts |
| G08 | Request money | `/request` | `docs/v5/part-02/screens/G-send-money/G08_request_money` | POST /api/v1/payment-requests |
| G09 | Split a bill | `/split` | `docs/v5/part-02/screens/G-send-money/G09_split_bill` | POST /api/v1/splits |
| H01 | Receive – my QR | `/receive` | `docs/v5/part-02/screens/H-receive-and-pay/H01_receive_qr` | GET /api/v1/qr/me |
| H02 | Scan QR | `/scan` | `docs/v5/part-02/screens/H-receive-and-pay/H02_scan_qr` |  |
| H03 | Pay a shop | `/pay/merchant` | `docs/v5/part-02/screens/H-receive-and-pay/H03_pay_merchant` | POST /api/v1/merchant-payments |
| I01 | Withdraw | `/withdraw` | `docs/v5/part-02/screens/I-withdraw/I01_withdraw` | GET /api/v1/fees/estimate?type=WITHDRAWAL |
| I02 | Withdraw – review | `/withdraw/review` | `docs/v5/part-02/screens/I-withdraw/I02_withdraw_review` | POST /api/v1/wallets/{walletId}/payouts |
| I03 | Withdraw – processing | `/withdraw/status` | `docs/v5/part-02/screens/I-withdraw/I03_withdraw_processing` | GET /api/v1/transactions/{reference} |
| J01 | Activity | `/activity` | `docs/v5/part-03/screens/J-activity/J01_activity` | GET /api/v1/insights/summary?month=<br>GET /api/v1/wallets/{walletId}/transactions?page=&size=&type=&from=&to=&q= |
| J02 | Activity filters | `/activity (sheet)` | `docs/v5/part-03/screens/J-activity/J02_activity_filters` |  |
| J03 | Transaction detail | `/activity/:reference` | `docs/v5/part-03/screens/J-activity/J03_transaction_detail` | GET /api/v1/transactions/{reference}<br>PATCH /api/v1/transactions/{reference}/category |
| J04 | Receipt | `/activity/:reference/receipt` | `docs/v5/part-03/screens/J-activity/J04_receipt` | GET /api/v1/transactions/{reference}/receipt.pdf |
| J05 | Statements | `/statements` | `docs/v5/part-03/screens/J-activity/J05_statements` | GET /api/v1/wallets/{walletId}/statements |
| J06 | Report a problem | `/activity/:reference/report` | `docs/v5/part-03/screens/J-activity/J06_report_problem` | POST /api/v1/support/cases |
| J07 | Activity – no results | `/activity?q=` | `docs/v5/part-03/screens/J-activity/J07_activity_no_results` |  |
| K01 | Pay bills | `/bills` | `docs/v5/part-03/screens/K-bills-airtime-and-store/K01_bills` | GET /api/v1/billers |
| K02 | ZESCO prepaid | `/bills/zesco` | `docs/v5/part-03/screens/K-bills-airtime-and-store/K02_zesco_token` | POST /api/v1/bills/pay<br>POST /api/v1/bills/validate |
| K03 | Token ready | `/bills/zesco/result` | `docs/v5/part-03/screens/K-bills-airtime-and-store/K03_zesco_token_ready` |  |
| K04 | Airtime and data | `/airtime` | `docs/v5/part-03/screens/K-bills-airtime-and-store/K04_airtime` |  |
| K05 | Digital store | `/store` | `docs/v5/part-03/screens/K-bills-airtime-and-store/K05_digital_store` | GET /api/v1/store/products |
| K06 | Store product | `/store/:id` | `docs/v5/part-03/screens/K-bills-airtime-and-store/K06_store_product` | POST /api/v1/store/orders |
| K00 | Payments hub | `/payments (tab)` | `docs/v5/part-03/screens/K-payments-tab/K00_payments_hub` | GET /api/v1/billers/saved |
| L01 | Cards – waitlist | `/cards (tab)` | `docs/v5/part-03/screens/L-cards/L01_cards_waitlist` | POST /api/v1/cards/waitlist |
| L02 | Design your card | `/cards/new` | `docs/v5/part-03/screens/L-cards/L02_card_choose_design` | POST /api/v1/cards |
| L03 | Card home | `/cards (tab)` | `docs/v5/part-03/screens/L-cards/L03_card_home` | GET /api/v1/cards<br>GET /api/v1/cards/{id}/transactions |
| L04 | Card details | `/cards/:id/details` | `docs/v5/part-03/screens/L-cards/L04_card_details` |  |
| L05 | Card controls | `/cards/:id/controls` | `docs/v5/part-03/screens/L-cards/L05_card_controls` | PATCH /api/v1/cards/{id}/controls |
| L06 | Card frozen | `/cards (tab)` | `docs/v5/part-03/screens/L-cards/L06_card_frozen` | POST /api/v1/cards/{id}/freeze |
| L07 | Add money to card | `/cards/:id/fund` | `docs/v5/part-03/screens/L-cards/L07_card_add_money` | POST /api/v1/cards/{id}/fund |
| M01 | Insights | `/insights (tab)` | `docs/v5/part-04/screens/M-insights-budgets-and-goals/M01_insights` | GET /api/v1/insights/summary?month= |
| M02 | Budgets | `/budgets` | `docs/v5/part-04/screens/M-insights-budgets-and-goals/M02_budgets` | GET /api/v1/budgets?month= |
| M03 | New budget | `/budgets/new` | `docs/v5/part-04/screens/M-insights-budgets-and-goals/M03_create_budget` | POST /api/v1/budgets |
| M04 | Savings goals | `/goals` | `docs/v5/part-04/screens/M-insights-budgets-and-goals/M04_goals` | GET /api/v1/goals |
| M05 | Goal detail | `/goals/:id` | `docs/v5/part-04/screens/M-insights-budgets-and-goals/M05_goal_detail` | POST /api/v1/goals/{id}/deposit |
| M06 | New goal | `/goals/new` | `docs/v5/part-04/screens/M-insights-budgets-and-goals/M06_create_goal` | POST /api/v1/goals |
| M07 | Round-ups | `/round-ups` | `docs/v5/part-04/screens/M-insights-budgets-and-goals/M07_round_ups` |  |
| M08 | Add cash | `/cash/new` | `docs/v5/part-04/screens/M-insights-budgets-and-goals/M08_add_cash_entry` | POST /api/v1/cash-entries |
| M09 | Chilimba | `/chilimba/:id` | `docs/v5/part-04/screens/M-insights-budgets-and-goals/M09_chilimba` |  |
| N01 | Nova AI chat | `/nova` | `docs/v5/part-04/screens/N-nova-ai/N01_nova_ai_chat` | POST /api/v1/ai/chat |
| N02 | Nova tips | `/nova/tips` | `docs/v5/part-04/screens/N-nova-ai/N02_nova_tips` | GET /api/v1/ai/tips |
| O01 | Me | `/me (tab)` | `docs/v5/part-04/screens/O-me-and-settings/O01_me` | GET /api/v1/users/me |
| O02 | Personal details | `/me/details` | `docs/v5/part-04/screens/O-me-and-settings/O02_personal_details` | POST /api/v1/email/verify |
| O03 | Security centre | `/me/security` | `docs/v5/part-04/screens/O-me-and-settings/O03_security_centre` | GET /api/v1/security/overview<br>POST /api/v1/security/lock |
| O04 | Devices | `/me/devices` | `docs/v5/part-04/screens/O-me-and-settings/O04_devices` | DELETE /api/v1/devices/{id}<br>GET /api/v1/devices |
| O05 | Change PIN | `/me/security/pin` | `docs/v5/part-04/screens/O-me-and-settings/O05_change_pin` | POST /api/v1/pin/change |
| O06 | Linked mobile money | `/me/linked` | `docs/v5/part-04/screens/O-me-and-settings/O06_linked_accounts` |  |
| O07 | Notification settings | `/me/notifications` | `docs/v5/part-04/screens/O-me-and-settings/O07_notification_settings` |  |
| O08 | Appearance | `/me/appearance` | `docs/v5/part-04/screens/O-me-and-settings/O08_appearance` |  |
| O09 | Invite friends | `/me/invite` | `docs/v5/part-04/screens/O-me-and-settings/O09_invite_friends` | GET /api/v1/referrals/me |
| O10 | Help centre | `/help` | `docs/v5/part-04/screens/O-me-and-settings/O10_help_centre` | GET /api/v1/help/articles |
| O11 | Support chat | `/help/chat/:caseId` | `docs/v5/part-04/screens/O-me-and-settings/O11_support_chat` |  |
| O12 | Privacy and data | `/me/privacy` | `docs/v5/part-04/screens/O-me-and-settings/O12_privacy_data` | POST /api/v1/privacy/export |
| O13 | Close account | `/me/close` | `docs/v5/part-04/screens/O-me-and-settings/O13_close_account` | POST /api/v1/users/me/close |
| P01 | No connection | `(full-screen state)` | `docs/v5/part-04/screens/P-offline-and-system/P01_no_connection` |  |
| P02 | Home – offline | `/home` | `docs/v5/part-04/screens/P-offline-and-system/P02_home_offline` |  |
| P03 | Payment status unknown | `/send/result` | `docs/v5/part-04/screens/P-offline-and-system/P03_payment_unknown` | GET /api/v1/transactions/{reference} |
| P04 | Server error | `(full-screen state)` | `docs/v5/part-04/screens/P-offline-and-system/P04_server_error` |  |
| P05 | Maintenance | `(full-screen state)` | `docs/v5/part-04/screens/P-offline-and-system/P05_maintenance` | GET /api/v1/app/config |
| P06 | Update required | `(full-screen state)` | `docs/v5/part-04/screens/P-offline-and-system/P06_update_required` |  |
| P07 | Home – loading | `/home` | `docs/v5/part-04/screens/P-offline-and-system/P07_home_loading` |  |
| P08 | List – loading | `/activity` | `docs/v5/part-04/screens/P-offline-and-system/P08_list_loading` |  |
| P09 | Back online toast | `/home` | `docs/v5/part-04/screens/P-offline-and-system/P09_back_online_toast` |  |
| Q01 | Reset PIN – code | `/pin/reset` | `docs/v5/part-09/screens/Q-extra-flows/Q01_reset_pin_code` |  |
| Q02 | Reset PIN – selfie | `/pin/reset/selfie` | `docs/v5/part-09/screens/Q-extra-flows/Q02_reset_pin_selfie` |  |
| Q03 | Reset PIN – new PIN | `/pin/reset/new` | `docs/v5/part-09/screens/Q-extra-flows/Q03_reset_pin_new` |  |
| Q04 | Data bundles | `/airtime?tab=data` | `docs/v5/part-09/screens/Q-extra-flows/Q04_data_bundles` |  |
| Q05 | Incoming money request | `/requests/:id` | `docs/v5/part-09/screens/Q-extra-flows/Q05_incoming_request` |  |
| Q06 | New Chilimba – details | `/chilimba/new` | `docs/v5/part-09/screens/Q-extra-flows/Q06_chilimba_create` |  |
| Q07 | New Chilimba – members | `/chilimba/new/members` | `docs/v5/part-09/screens/Q-extra-flows/Q07_chilimba_members` |  |
| Q08 | Goal – add money | `/goals/:id (sheet)` | `docs/v5/part-09/screens/Q-extra-flows/Q08_goal_add_money` |  |
| Q09 | Goal – take money out | `/goals/:id (sheet)` | `docs/v5/part-09/screens/Q-extra-flows/Q09_goal_take_out` |  |
| Q10 | Change phone number | `/me/details/phone` | `docs/v5/part-09/screens/Q-extra-flows/Q10_change_phone` |  |
| Q11 | Verify email | `/me/details/email/verify` | `docs/v5/part-09/screens/Q-extra-flows/Q11_verify_email` |  |
| Q12 | Terms and privacy | `/legal` | `docs/v5/part-09/screens/Q-extra-flows/Q12_terms_privacy` |  |
| Q13 | Find friends (contacts permission) | `/permissions/contacts` | `docs/v5/part-09/screens/Q-extra-flows/Q13_contacts_permission` |  |
| X01 | Staff sign in (admin console) | `/login` | `docs/v5/part-07/screens/X01_admin_sign_in` | POST /api/v1/admin/auth/login |
| X02 | Dashboard (admin console) | `/` | `docs/v5/part-07/screens/X02_admin_dashboard` | GET /api/v1/admin/alerts<br>GET /api/v1/admin/metrics/overview |
| X03 | Users (admin console) | `/users` | `docs/v5/part-07/screens/X03_admin_users` |  |
| X04 | User detail (admin console) | `/users/:id` | `docs/v5/part-07/screens/X04_admin_user_detail` |  |
| X05 | KYC review queue (admin console) | `/kyc` | `docs/v5/part-07/screens/X05_admin_kyc_queue` |  |
| X06 | KYC review detail (admin console) | `/kyc/:id` | `docs/v5/part-07/screens/X06_admin_kyc_review` |  |
| X07 | Transactions (admin console) | `/transactions` | `docs/v5/part-07/screens/X07_admin_transactions` | GET /api/v1/admin/transactions |
| X08 | Transaction detail (admin console) | `/transactions/:ref` | `docs/v5/part-07/screens/X08_admin_transaction_detail` | GET /api/v1/admin/transactions/{ref} |
| X09 | Reconciliation (admin console) | `/reconciliation` | `docs/v5/part-07/screens/X09_admin_reconciliation` |  |
| X10 | Providers (kill switch) (admin console) | `/providers` | `docs/v5/part-07/screens/X10_admin_providers` |  |
| X11 | Limits and fees (admin console) | `/settings/limits` | `docs/v5/part-07/screens/X11_admin_limits_fees` |  |
| X12 | Notices (admin console) | `/notices` | `docs/v5/part-07/screens/X12_admin_notices` |  |
| X13 | Support cases (admin console) | `/support` | `docs/v5/part-07/screens/X13_admin_support` |  |
| X14 | Audit log (admin console) | `/audit` | `docs/v5/part-07/screens/X14_admin_audit_log` |  |
| X15 | Staff and roles (admin console) | `/staff` | `docs/v5/part-07/screens/X15_admin_staff_roles` |  |
| X16 | Card programme (admin console) | `/cards` | `docs/v5/part-07/screens/X16_admin_card_programme` |  |
