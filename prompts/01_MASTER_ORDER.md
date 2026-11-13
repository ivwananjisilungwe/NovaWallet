# Master Order — run top to bottom, nothing in parallel unless marked

**The sequence is: housekeeping → finish the backend completely → connect staging → build the app screen by screen (against the finished backend) → admin console → hardening → launch.**

Legend: 🔒 run `templates/T1_independent_review.md` (then T2) before committing. ⛔ blocked until the named decision/partner item exists; prove with the simulator and move on only as the prompt says. A step starts only when everything in **Needs** is ticked. Prompt paths are relative to `prompts/`.

## PHASE 0 — Housekeeping (week 1)
| ✓ | # | Prompt | Needs |
|---|---|---|---|
| ☐ | 1 | `stage-00_housekeeping/S0-01_commit_batch1_and_baseline.md` | — |
| ☐ | 2 | `stage-00_housekeeping/S0-02_workspace_check.md` | 1 |
| ☐ | 3 | `stage-00_housekeeping/S0-03_decisions_and_contract_target.md` | 2 |

## PHASE 1 — Backend A: finish Phase 1C security
| ✓ | # | Prompt | Needs |
|---|---|---|---|
| ☐ | 4 | `stage-01_backend-security/BE-1C-2_authorization_ownership.md` 🔒 | 3 |
| ☐ | 5 | `stage-01_backend-security/BE-1C-3_financial_concurrency.md` 🔒 | 4 |
| ☐ | 6 | `stage-01_backend-security/BE-1C-4_database_integrity.md` 🔒 | 5 |
| ☐ | 7 | `stage-01_backend-security/BE-1C-6_kyc_pii_documents.md` 🔒 | 6 |
| ☐ | 8 | `stage-01_backend-security/BE-1C-7_rate_limiting.md` 🔒 | 7 |
| ☐ | 9 | `stage-01_backend-security/BE-1C-8_audit_logging_errors.md` 🔒 | 8 |
| ☐ | 10 | `stage-01_backend-security/BE-1C-9_final_regression.md` 🔒 | 9 |

## PHASE 2 — Backend B: everything the app and admin need (Phase 1D)
| ✓ | # | Prompt | Needs |
|---|---|---|---|
| ☐ | 11 | `stage-02_backend-app-api/BE-1D-01_app_config_notices.md` | 10 |
| ☐ | 12 | `stage-02_backend-app-api/BE-1D-02_identity_otp_devices.md` 🔒 | 11 |
| ☐ | 13 | `stage-02_backend-app-api/BE-1D-03_pin_lifecycle.md` 🔒 | 12 |
| ☐ | 14 | `stage-02_backend-app-api/BE-1D-04_kyc_reference_data.md` | 13 |
| ☐ | 15 | `stage-02_backend-app-api/BE-1D-05_lookup_contacts_qr_methods.md` 🔒 | 14 |
| ☐ | 16 | `stage-02_backend-app-api/BE-1D-06_notifications_push.md` | 15 |
| ☐ | 17 | `stage-02_backend-app-api/BE-1D-07_admin_rbac_staff.md` 🔒 | 16 |
| ☐ | 18 | `stage-02_backend-app-api/BE-1D-08_account_lifecycle_profile.md` | 17 |

## PHASE 3 — Backend C: double-entry ledger
| ✓ | # | Prompt | Needs |
|---|---|---|---|
| ☐ | 19 | `stage-03_ledger/LED-01_ledger_design.md` (plan only; owner approves) | 18 |
| ☐ | 20 | `stage-03_ledger/LED-02_schema_posting_engine.md` 🔒 | 19 approved |
| ☐ | 21 | `stage-03_ledger/LED-03_route_flows_through_ledger.md` 🔒 | 20 |
| ☐ | 22 | `stage-03_ledger/LED-04_reconciliation_daily_proof.md` 🔒 | 21 |

## PHASE 4 — Backend D: payments, fees, money features
| ✓ | # | Prompt | Needs |
|---|---|---|---|
| ☐ | 23 | `stage-04_payments/PAY-01_orchestration_core.md` 🔒 | 22 |
| ☐ | 24 | `stage-04_payments/PAY-02_lipila_disbursements_on_orchestration.md` 🔒 | 23 |
| ☐ | 25 | `stage-01_backend-security/BE-1C-5_webhook_security.md` 🔒 ⛔ (stops at BLOCKED if no Lipila contract) | 24 |
| ☐ | 26 | `stage-04_payments/PAY-03_lipila_collections_topup.md` 🔒 ⛔ | 25 |
| ☐ | 27 | `stage-04_payments/PAY-04_fees_limits_engine.md` 🔒 | 26 |
| ☐ | 28 | `stage-02_backend-app-api/BE-1D-09_requests_splits_merchant_pay.md` 🔒 | 27 |
| ☐ | 29 | `stage-02_backend-app-api/BE-1D-10_budgets_goals_roundups_chilimba.md` 🔒 | 28 |
| ☐ | 30 | `stage-02_backend-app-api/BE-1D-11_activity_categories_insights.md` | 29 |
| ☐ | 31 | `stage-02_backend-app-api/BE-1D-12_receipts_support_help.md` | 30 |
| ☐ | 32 | `stage-04_payments/PAY-05_bills_airtime_data_store.md` 🔒 ⛔ | 31 |
| ☐ | 33 | `stage-04_payments/PAY-06_cards_waitlist_and_module.md` 🔒 ⛔ (Part A only without a partner) | 32 |

## PHASE 5 — Backend E: AI, architecture, completion gate
| ✓ | # | Prompt | Needs |
|---|---|---|---|
| ☐ | 34 | `stage-07_ai/AI-01_gateway_categorization_tips.md` | 33 |
| ☐ | 35 | `stage-07_ai/AI-02_fraud_rules_anomaly.md` 🔒 | 34 |
| ☐ | 36 | `stage-07_ai/AI-03_nova_assistant_backend.md` 🔒 | 35 |
| ☐ | 37 | `stage-07_ai/AI-04_kyc_ocr_prefill.md` 🔒 | 36 |
| ☐ | 38 | `stage-08_architecture/ARC-01_modular_monolith.md` 🔒 | 37 |
| ☐ | 39 | `stage-08_architecture/ARC-02_transactional_outbox.md` 🔒 | 38 |
| ☐ | 40 | **`stage-08_architecture/BE-99_backend_complete_gate.md`** 🔒 — contract freeze, sandbox seed, OpenAPI snapshot | 39 |

**★ Backend is complete here. The app is built only from the frozen contract (`docs/api/openapi.json`).**

## PHASE 6 — Environments the app will connect to
| ✓ | # | Prompt | Needs |
|---|---|---|---|
| ☐ | 41 | `stage-09_infra-deploy/OPS-01_containers_environments.md` | 40 |
| ☐ | 42 | `stage-09_infra-deploy/OPS-02_server_caddy_tls.md` 🔒 (needs VPS + domain) | 41 |
| ☐ | 43 | `stage-09_infra-deploy/OPS-03_cicd_pipelines.md` 🔒 (staging auto-deploy) | 42 |

## PHASE 7 — Flutter app, exactly as designed (screen groups in design order)
| ✓ | # | Prompt | Screens | Needs |
|---|---|---|---|---|
| ☐ | 44 | `stage-05_flutter-app/APP-00_bootstrap.md` | — | 41 |
| ☐ | 45 | `stage-05_flutter-app/APP-01_design_system_and_fidelity_tool.md` | components, tokens | 44 |
| ☐ | 46 | `stage-05_flutter-app/APP-02_core_network_session.md` 🔒 | client, session, router | 45 |
| ☐ | 47 | `stage-05_flutter-app/APP-03_launch_onboarding.md` | A01–A07 | 46 |
| ☐ | 48 | `stage-05_flutter-app/APP-04_create_account.md` 🔒 | B01–B07 | 47 |
| ☐ | 49 | `stage-05_flutter-app/APP-05_sign_in_unlock_recovery.md` 🔒 | C01–C07 | 48 |
| ☐ | 50 | `stage-05_flutter-app/APP-06_kyc.md` 🔒 | D01–D09 | 49 |
| ☐ | 51 | `stage-05_flutter-app/APP-07_home_notifications.md` | E01–E07 | 50 |
| ☐ | 52 | `stage-05_flutter-app/APP-08_topup.md` 🔒 | F01–F08 | 51 |
| ☐ | 53 | `stage-05_flutter-app/APP-09_send_request_split.md` 🔒 | G01–G09 | 52 |
| ☐ | 54 | `stage-05_flutter-app/APP-10_receive_scan_withdraw.md` 🔒 | H01–H03, I01–I03 | 53 |
| ☐ | 55 | `stage-05_flutter-app/APP-11_activity_receipts_statements.md` | J01–J07 | 54 |
| ☐ | 56 | `stage-05_flutter-app/APP-12_bills_airtime_store.md` 🔒 | K00–K06, Q04 | 55 |
| ☐ | 57 | `stage-05_flutter-app/APP-13_cards.md` 🔒 | L01–L07 | 56 |
| ☐ | 58 | `stage-05_flutter-app/APP-14_insights_budgets_goals_chilimba.md` 🔒 | M01–M09, Q06–Q09 | 57 |
| ☐ | 59 | `stage-05_flutter-app/APP-15_nova_ai.md` | N01–N02 | 58 |
| ☐ | 60 | `stage-05_flutter-app/APP-16_me_settings_security_support.md` 🔒 | O01–O13 | 59 |
| ☐ | 61 | `stage-05_flutter-app/APP-17_offline_system_extra_flows.md` 🔒 | P01–P09, Q01–03, Q05, Q10–Q13 | 60 |
| ☐ | 62 | `stage-05_flutter-app/APP-18_push_deep_links.md` 🔒 | — | 61 |
| ☐ | 63 | `stage-05_flutter-app/APP-19_motion_animations.md` | animations | 62 |
| ☐ | 64 | `stage-05_flutter-app/APP-20_resilience_a11y_i18n_perf.md` | all | 63 |
| ☐ | 65 | `stage-05_flutter-app/APP-21_test_hardening.md` | all | 64 |
| ☐ | 66 | `stage-05_flutter-app/APP-22_design_audit_and_fix.md` (audit → fix → audit until clean) | all | 65 |

## PHASE 8 — Admin console, exactly as designed
| ✓ | # | Prompt | Screens | Needs |
|---|---|---|---|---|
| ☐ | 67 | `stage-06_admin-console/ADM-00_bootstrap_design_system.md` | shell | 40 |
| ☐ | 68 | `stage-06_admin-console/ADM-01_auth_totp_rbac.md` 🔒 | X01 | 67 |
| ☐ | 69 | `stage-06_admin-console/ADM-02_dashboard_users.md` | X02–X04 | 68 |
| ☐ | 70 | `stage-06_admin-console/ADM-03_kyc_review.md` 🔒 | X05–X06 | 69 |
| ☐ | 71 | `stage-06_admin-console/ADM-04_transactions_wallets.md` 🔒 | X07–X08 | 70 |
| ☐ | 72 | `stage-06_admin-console/ADM-05_reconciliation_providers.md` 🔒 | X09–X10 | 71 |
| ☐ | 73 | `stage-06_admin-console/ADM-06_fees_limits_notices_config.md` 🔒 | X11–X12 | 72 |
| ☐ | 74 | `stage-06_admin-console/ADM-07_support_audit_staff.md` | X13–X15 | 73 |

(Phase 7 and Phase 8 can alternate if you wish; they only share the finished backend.)

## PHASE 9 — Production readiness
| ✓ | # | Prompt | Needs |
|---|---|---|---|
| ☐ | 75 | `stage-09_infra-deploy/OPS-04_backups_restore.md` 🔒 | 43 |
| ☐ | 76 | `stage-09_infra-deploy/OPS-05_observability_alerting.md` | 75 |
| ☐ | 77 | `stage-09_infra-deploy/OPS-06_secrets_keys.md` 🔒 | 76 |
| ☐ | 78 | `stage-09_infra-deploy/OPS-07_security_testing.md` 🔒 (fix criticals/highs with T2/T3) | 77, 66, 74 |
| ☐ | 79 | `stage-09_infra-deploy/OPS-08_load_resilience_testing.md` | 78 |
| ☐ | 80 | `stage-09_infra-deploy/OPS-09_runbooks_incident_response.md` | 79 |
| ☐ | 81 | `stage-05_flutter-app/APP-23_release_engineering.md` 🔒 | 66, 78 |

## PHASE 10 — Launch
| ✓ | # | Prompt | Needs |
|---|---|---|---|
| ☐ | 82 | `stage-10_launch/LAUNCH-01_compliance_pack.md` ⛔ | 80 |
| ☐ | 83 | `stage-10_launch/LAUNCH-02_provider_go_live.md` 🔒 ⛔ | 82, 81 |
| ☐ | 84 | `stage-10_launch/LAUNCH-03_closed_beta.md` | 83 |
| ☐ | 85 | `stage-10_launch/LAUNCH-04_production_rollout.md` 🔒 | 84 |
| ☐ | 86 | `stage-10_launch/LAUNCH-05_post_launch_operations.md` (recurring) | 85 |

## OPTIONAL / LATER (only when the trigger fires)
| Prompt | Trigger |
|---|---|
| `stage-07_ai/AI-05_forecasting_credit_readiness.md` | ≥ 3 months of pilot data |
| `stage-04_payments/PAY-07_direct_mno_integration.md` ⛔ | Volume justifies direct MTN/Airtel and contract signed |
| `stage-06_admin-console/ADM-08_card_programme.md` | Card partner module live (DEC-18) |
| `stage-08_architecture/ARC-03_service_extraction_gate.md` | Measured scaling/team triggers |

## Picture
```
S0 → BE-1C → BE-1D(1-8) → LED → PAY(1-4)+BE-1D(9-12)+PAY(5-6) → AI → ARC → BE-99 ★ contract frozen
        ★ → OPS(1-3 staging) → APP(0-22) ┐
        ★ ────────────────→ ADM(0-7) ───┤→ OPS(4-9) + APP-23 → LAUNCH(1-5)
```
