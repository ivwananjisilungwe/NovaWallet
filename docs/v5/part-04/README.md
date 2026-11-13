# NovaWallet v5 · Part-04-Insights-NovaAI-Settings-Offline

Each screen has its own folder with light and dark designs (PNG 3× and SVG) and a `BUILD.md` with full build instructions and a ready prompt for opencode. Build `docs/COMPONENTS.md` first, then the screens in the order below (this is the order a user meets them).

## Insights, budgets and goals
| ID | Screen | Route | Folder |
|---|---|---|---|
| M01 | Insights | `/insights (tab)` | [screens/M-insights-budgets-and-goals/M01_insights](screens/M-insights-budgets-and-goals/M01_insights/BUILD.md) |
| M02 | Budgets | `/budgets` | [screens/M-insights-budgets-and-goals/M02_budgets](screens/M-insights-budgets-and-goals/M02_budgets/BUILD.md) |
| M03 | New budget | `/budgets/new` | [screens/M-insights-budgets-and-goals/M03_create_budget](screens/M-insights-budgets-and-goals/M03_create_budget/BUILD.md) |
| M04 | Savings goals | `/goals` | [screens/M-insights-budgets-and-goals/M04_goals](screens/M-insights-budgets-and-goals/M04_goals/BUILD.md) |
| M05 | Goal detail | `/goals/:id` | [screens/M-insights-budgets-and-goals/M05_goal_detail](screens/M-insights-budgets-and-goals/M05_goal_detail/BUILD.md) |
| M06 | New goal | `/goals/new` | [screens/M-insights-budgets-and-goals/M06_create_goal](screens/M-insights-budgets-and-goals/M06_create_goal/BUILD.md) |
| M07 | Round-ups | `/round-ups` | [screens/M-insights-budgets-and-goals/M07_round_ups](screens/M-insights-budgets-and-goals/M07_round_ups/BUILD.md) |
| M08 | Add cash | `/cash/new` | [screens/M-insights-budgets-and-goals/M08_add_cash_entry](screens/M-insights-budgets-and-goals/M08_add_cash_entry/BUILD.md) |
| M09 | Chilimba | `/chilimba/:id` | [screens/M-insights-budgets-and-goals/M09_chilimba](screens/M-insights-budgets-and-goals/M09_chilimba/BUILD.md) |

## Nova AI
| ID | Screen | Route | Folder |
|---|---|---|---|
| N01 | Nova AI chat | `/nova` | [screens/N-nova-ai/N01_nova_ai_chat](screens/N-nova-ai/N01_nova_ai_chat/BUILD.md) |
| N02 | Nova tips | `/nova/tips` | [screens/N-nova-ai/N02_nova_tips](screens/N-nova-ai/N02_nova_tips/BUILD.md) |

## Me and settings
| ID | Screen | Route | Folder |
|---|---|---|---|
| O01 | Me | `/me (tab)` | [screens/O-me-and-settings/O01_me](screens/O-me-and-settings/O01_me/BUILD.md) |
| O02 | Personal details | `/me/details` | [screens/O-me-and-settings/O02_personal_details](screens/O-me-and-settings/O02_personal_details/BUILD.md) |
| O03 | Security centre | `/me/security` | [screens/O-me-and-settings/O03_security_centre](screens/O-me-and-settings/O03_security_centre/BUILD.md) |
| O04 | Devices | `/me/devices` | [screens/O-me-and-settings/O04_devices](screens/O-me-and-settings/O04_devices/BUILD.md) |
| O05 | Change PIN | `/me/security/pin` | [screens/O-me-and-settings/O05_change_pin](screens/O-me-and-settings/O05_change_pin/BUILD.md) |
| O06 | Linked mobile money | `/me/linked` | [screens/O-me-and-settings/O06_linked_accounts](screens/O-me-and-settings/O06_linked_accounts/BUILD.md) |
| O07 | Notification settings | `/me/notifications` | [screens/O-me-and-settings/O07_notification_settings](screens/O-me-and-settings/O07_notification_settings/BUILD.md) |
| O08 | Appearance | `/me/appearance` | [screens/O-me-and-settings/O08_appearance](screens/O-me-and-settings/O08_appearance/BUILD.md) |
| O09 | Invite friends | `/me/invite` | [screens/O-me-and-settings/O09_invite_friends](screens/O-me-and-settings/O09_invite_friends/BUILD.md) |
| O10 | Help centre | `/help` | [screens/O-me-and-settings/O10_help_centre](screens/O-me-and-settings/O10_help_centre/BUILD.md) |
| O11 | Support chat | `/help/chat/:caseId` | [screens/O-me-and-settings/O11_support_chat](screens/O-me-and-settings/O11_support_chat/BUILD.md) |
| O12 | Privacy and data | `/me/privacy` | [screens/O-me-and-settings/O12_privacy_data](screens/O-me-and-settings/O12_privacy_data/BUILD.md) |
| O13 | Close account | `/me/close` | [screens/O-me-and-settings/O13_close_account](screens/O-me-and-settings/O13_close_account/BUILD.md) |

## Offline and system
| ID | Screen | Route | Folder |
|---|---|---|---|
| P01 | No connection | `(full-screen state)` | [screens/P-offline-and-system/P01_no_connection](screens/P-offline-and-system/P01_no_connection/BUILD.md) |
| P02 | Home – offline | `/home` | [screens/P-offline-and-system/P02_home_offline](screens/P-offline-and-system/P02_home_offline/BUILD.md) |
| P03 | Payment status unknown | `/send/result` | [screens/P-offline-and-system/P03_payment_unknown](screens/P-offline-and-system/P03_payment_unknown/BUILD.md) |
| P04 | Server error | `(full-screen state)` | [screens/P-offline-and-system/P04_server_error](screens/P-offline-and-system/P04_server_error/BUILD.md) |
| P05 | Maintenance | `(full-screen state)` | [screens/P-offline-and-system/P05_maintenance](screens/P-offline-and-system/P05_maintenance/BUILD.md) |
| P06 | Update required | `(full-screen state)` | [screens/P-offline-and-system/P06_update_required](screens/P-offline-and-system/P06_update_required/BUILD.md) |
| P07 | Home – loading | `/home` | [screens/P-offline-and-system/P07_home_loading](screens/P-offline-and-system/P07_home_loading/BUILD.md) |
| P08 | List – loading | `/activity` | [screens/P-offline-and-system/P08_list_loading](screens/P-offline-and-system/P08_list_loading/BUILD.md) |
| P09 | Back online toast | `/home` | [screens/P-offline-and-system/P09_back_online_toast](screens/P-offline-and-system/P09_back_online_toast/BUILD.md) |
