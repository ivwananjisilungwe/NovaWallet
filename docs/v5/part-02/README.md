# NovaWallet v5 · Part-02-Home-TopUp-Send-Receive-Withdraw

Each screen has its own folder with light and dark designs (PNG 3× and SVG) and a `BUILD.md` with full build instructions and a ready prompt for opencode. Build `docs/COMPONENTS.md` first, then the screens in the order below (this is the order a user meets them).

## Home
| ID | Screen | Route | Folder |
|---|---|---|---|
| E01 | Home – ID in review | `/home` | [screens/E-home/E01_home_pending_id](screens/E-home/E01_home_pending_id/BUILD.md) |
| E02 | Home | `/home` | [screens/E-home/E02_home](screens/E-home/E02_home/BUILD.md) |
| E03 | Home – balance hidden | `/home` | [screens/E-home/E03_home_balance_hidden](screens/E-home/E03_home_balance_hidden/BUILD.md) |
| E04 | Home – new user | `/home` | [screens/E-home/E04_home_new_user](screens/E-home/E04_home_new_user/BUILD.md) |
| E05 | Home – service notice | `/home` | [screens/E-home/E05_home_service_notice](screens/E-home/E05_home_service_notice/BUILD.md) |
| E06 | Notifications | `/notifications` | [screens/E-home/E06_notifications](screens/E-home/E06_notifications/BUILD.md) |
| E07 | Notifications – empty | `/notifications?category=security` | [screens/E-home/E07_notifications_empty](screens/E-home/E07_notifications_empty/BUILD.md) |

## Top up
| ID | Screen | Route | Folder |
|---|---|---|---|
| F01 | Add money – method | `/topup` | [screens/F-top-up/F01_topup_method](screens/F-top-up/F01_topup_method/BUILD.md) |
| F02 | Top up – amount | `/topup/amount?provider=AIRTEL` | [screens/F-top-up/F02_topup_amount](screens/F-top-up/F02_topup_amount/BUILD.md) |
| F03 | Top up – over limit | `/topup/amount` | [screens/F-top-up/F03_topup_over_limit](screens/F-top-up/F03_topup_over_limit/BUILD.md) |
| F04 | Top up – review | `/topup/review` | [screens/F-top-up/F04_topup_review](screens/F-top-up/F04_topup_review/BUILD.md) |
| F05 | Approve on your phone | `/topup/waiting` | [screens/F-top-up/F05_topup_approve_on_phone](screens/F-top-up/F05_topup_approve_on_phone/BUILD.md) |
| F06 | Top up – success | `/topup/result` | [screens/F-top-up/F06_topup_success](screens/F-top-up/F06_topup_success/BUILD.md) |
| F07 | Top up – failed | `/topup/result` | [screens/F-top-up/F07_topup_failed](screens/F-top-up/F07_topup_failed/BUILD.md) |
| F08 | Top up – pending | `/topup/result` | [screens/F-top-up/F08_topup_pending](screens/F-top-up/F08_topup_pending/BUILD.md) |

## Send money
| ID | Screen | Route | Folder |
|---|---|---|---|
| G01 | Send – choose recipient | `/send` | [screens/G-send-money/G01_send_recipient](screens/G-send-money/G01_send_recipient/BUILD.md) |
| G02 | Send – amount | `/send/amount` | [screens/G-send-money/G02_send_amount](screens/G-send-money/G02_send_amount/BUILD.md) |
| G03 | Send – review | `/send/review` | [screens/G-send-money/G03_send_review](screens/G-send-money/G03_send_review/BUILD.md) |
| G04 | Send – enter PIN | `/send/review (sheet)` | [screens/G-send-money/G04_send_pin](screens/G-send-money/G04_send_pin/BUILD.md) |
| G05 | Send – wrong PIN | `/send/review (sheet)` | [screens/G-send-money/G05_send_wrong_pin](screens/G-send-money/G05_send_wrong_pin/BUILD.md) |
| G06 | Send – success | `/send/result` | [screens/G-send-money/G06_send_success](screens/G-send-money/G06_send_success/BUILD.md) |
| G07 | Send to mobile money | `/send/mobile-money` | [screens/G-send-money/G07_send_mobile_money](screens/G-send-money/G07_send_mobile_money/BUILD.md) |
| G08 | Request money | `/request` | [screens/G-send-money/G08_request_money](screens/G-send-money/G08_request_money/BUILD.md) |
| G09 | Split a bill | `/split` | [screens/G-send-money/G09_split_bill](screens/G-send-money/G09_split_bill/BUILD.md) |

## Receive and pay
| ID | Screen | Route | Folder |
|---|---|---|---|
| H01 | Receive – my QR | `/receive` | [screens/H-receive-and-pay/H01_receive_qr](screens/H-receive-and-pay/H01_receive_qr/BUILD.md) |
| H02 | Scan QR | `/scan` | [screens/H-receive-and-pay/H02_scan_qr](screens/H-receive-and-pay/H02_scan_qr/BUILD.md) |
| H03 | Pay a shop | `/pay/merchant` | [screens/H-receive-and-pay/H03_pay_merchant](screens/H-receive-and-pay/H03_pay_merchant/BUILD.md) |

## Withdraw
| ID | Screen | Route | Folder |
|---|---|---|---|
| I01 | Withdraw | `/withdraw` | [screens/I-withdraw/I01_withdraw](screens/I-withdraw/I01_withdraw/BUILD.md) |
| I02 | Withdraw – review | `/withdraw/review` | [screens/I-withdraw/I02_withdraw_review](screens/I-withdraw/I02_withdraw_review/BUILD.md) |
| I03 | Withdraw – processing | `/withdraw/status` | [screens/I-withdraw/I03_withdraw_processing](screens/I-withdraw/I03_withdraw_processing/BUILD.md) |
