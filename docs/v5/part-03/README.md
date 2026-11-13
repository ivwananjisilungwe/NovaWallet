# NovaWallet v5 · Part-03-Activity-Bills-Store-Cards

Each screen has its own folder with light and dark designs (PNG 3× and SVG) and a `BUILD.md` with full build instructions and a ready prompt for opencode. Build `docs/COMPONENTS.md` first, then the screens in the order below (this is the order a user meets them).

## Activity
| ID | Screen | Route | Folder |
|---|---|---|---|
| J01 | Activity | `/activity` | [screens/J-activity/J01_activity](screens/J-activity/J01_activity/BUILD.md) |
| J02 | Activity filters | `/activity (sheet)` | [screens/J-activity/J02_activity_filters](screens/J-activity/J02_activity_filters/BUILD.md) |
| J03 | Transaction detail | `/activity/:reference` | [screens/J-activity/J03_transaction_detail](screens/J-activity/J03_transaction_detail/BUILD.md) |
| J04 | Receipt | `/activity/:reference/receipt` | [screens/J-activity/J04_receipt](screens/J-activity/J04_receipt/BUILD.md) |
| J05 | Statements | `/statements` | [screens/J-activity/J05_statements](screens/J-activity/J05_statements/BUILD.md) |
| J06 | Report a problem | `/activity/:reference/report` | [screens/J-activity/J06_report_problem](screens/J-activity/J06_report_problem/BUILD.md) |
| J07 | Activity – no results | `/activity?q=` | [screens/J-activity/J07_activity_no_results](screens/J-activity/J07_activity_no_results/BUILD.md) |

## Payments tab
| ID | Screen | Route | Folder |
|---|---|---|---|
| K00 | Payments hub | `/payments (tab)` | [screens/K-payments-tab/K00_payments_hub](screens/K-payments-tab/K00_payments_hub/BUILD.md) |

## Bills, airtime and store
| ID | Screen | Route | Folder |
|---|---|---|---|
| K01 | Pay bills | `/bills` | [screens/K-bills-airtime-and-store/K01_bills](screens/K-bills-airtime-and-store/K01_bills/BUILD.md) |
| K02 | ZESCO prepaid | `/bills/zesco` | [screens/K-bills-airtime-and-store/K02_zesco_token](screens/K-bills-airtime-and-store/K02_zesco_token/BUILD.md) |
| K03 | Token ready | `/bills/zesco/result` | [screens/K-bills-airtime-and-store/K03_zesco_token_ready](screens/K-bills-airtime-and-store/K03_zesco_token_ready/BUILD.md) |
| K04 | Airtime and data | `/airtime` | [screens/K-bills-airtime-and-store/K04_airtime](screens/K-bills-airtime-and-store/K04_airtime/BUILD.md) |
| K05 | Digital store | `/store` | [screens/K-bills-airtime-and-store/K05_digital_store](screens/K-bills-airtime-and-store/K05_digital_store/BUILD.md) |
| K06 | Store product | `/store/:id` | [screens/K-bills-airtime-and-store/K06_store_product](screens/K-bills-airtime-and-store/K06_store_product/BUILD.md) |

## Cards
| ID | Screen | Route | Folder |
|---|---|---|---|
| L01 | Cards – waitlist | `/cards (tab)` | [screens/L-cards/L01_cards_waitlist](screens/L-cards/L01_cards_waitlist/BUILD.md) |
| L02 | Design your card | `/cards/new` | [screens/L-cards/L02_card_choose_design](screens/L-cards/L02_card_choose_design/BUILD.md) |
| L03 | Card home | `/cards (tab)` | [screens/L-cards/L03_card_home](screens/L-cards/L03_card_home/BUILD.md) |
| L04 | Card details | `/cards/:id/details` | [screens/L-cards/L04_card_details](screens/L-cards/L04_card_details/BUILD.md) |
| L05 | Card controls | `/cards/:id/controls` | [screens/L-cards/L05_card_controls](screens/L-cards/L05_card_controls/BUILD.md) |
| L06 | Card frozen | `/cards (tab)` | [screens/L-cards/L06_card_frozen](screens/L-cards/L06_card_frozen/BUILD.md) |
| L07 | Add money to card | `/cards/:id/fund` | [screens/L-cards/L07_card_add_money](screens/L-cards/L07_card_add_money/BUILD.md) |
