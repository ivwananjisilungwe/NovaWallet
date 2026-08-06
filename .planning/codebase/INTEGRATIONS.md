# Integrations

## External Services
- **Mobile Money (mock)**: Simulated Airtel and MTN mobile‑money integration via admin‑controlled pending requests. Currently stubbed; real integration planned for Phase 2.
- **Email**: Spring Mail (`MailService`) using a configurable SMTP server for verification and notifications.
- **Push Notifications**: Placeholder `NotificationService` with `flutter_secure_storage` for device tokens; real push provider (e.g., Firebase Cloud Messaging) to be added later.

## Internal Modules (Java)
- `auth` – authentication, JWT handling, rate‑limiting.
- `kyc` – document upload, verification workflow.
- `wallet` – wallet entity, balance logic, fee deductions.
- `transaction` – deposit, withdraw, transfer, ledger entries.
- `fee` – fee configuration and calculation.
- `notification` – email/SMS notification scheduling.
- `admin` – admin panel endpoints for confirming deposits/withdrawals, user management.
- `audit` – AOP‑based audit logging of critical actions.
- `idempotency` – idempotency key handling for safe retries.
- `security` – custom filters, rate‑limit, JWT utilities.

## Internal Modules (Flutter)
- `auth_ui` – registration, login, PIN setup screens.
- `home` – wallet dashboard, quick actions.
- `send` – transfer UI.
- `cards` – virtual card creation and management.
- `kyc_ui` – KYC upload flow.
- `profile` – user settings and admin actions.
- `admin_ui` – admin dashboard (web, not mobile).

## Future/Planned Integrations
- **Flutterwave**: real payment gateway for deposits/withdrawals.
- **Union54**: card issuance provider.
- **Firebase Cloud Messaging**: push notifications.
- **Analytics**: Mixpanel or Amplitude for user behavior.
