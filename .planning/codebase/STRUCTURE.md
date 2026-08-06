# Project Structure Overview

## Backend (Java Spring Boot)
```
novawallet-api/
├── src/main/java/com/novawallet/novawallet_api/
│   ├── admin/            # Admin controllers & services
│   ├── audit/            # Audit log entity, repository, service
│   ├── auth/             # Authentication controllers, DTOs, service
│   ├── config/           # Spring config (security, swagger, caching, etc.)
│   ├── fee/              # Fee calculation engine and related entities
│   ├── filter/           # Servlet filters (request tracing, idempotency)
│   ├── idempotency/      # Idempotency key handling
│   ├── kyc/              # KYC workflow, document handling
│   ├── notification/    # Email/SMS notification service & scheduling
│   ├── security/         # JWT utilities, rate‑limiting, custom auth details
│   ├── token/            # Refresh token entity & cleanup job
│   ├── transaction/      # Transaction service, controller, specs, repository
│   ├── user/             # User entity, controller, service, DTOs
│   ├── wallet/           # Wallet entity, service, account number generation
│   └── ...               # Other shared utilities (exception handling, etc.)
├── src/main/resources/
│   ├── application.yml               # Base configuration (dev defaults)
│   ├── application-dev.yml           # Dev profile overrides
│   ├── application-prod.yml          # Production profile (env‑var driven)
│   └── db/migration/                 # Flyway SQL migration scripts V1‑V10
├── src/test/java/com/novawallet/...   # Integration tests (H2 DB) covering auth, kyc, fees, transactions, notifications, security
└── pom.xml                            # Maven build, dependencies, plugins
```

## Frontend (Flutter)
```
novawallet-app/
├── lib/main.dart                # App entry point, root widget
├── lib/... (additional feature folders not fully listed)  # UI screens, Riverpod providers, routing (go_router)
├── test/widget_test.dart        # Minimal widget test (default generated)
├── pubspec.yaml                 # Flutter dependencies (riverpod, dio, go_router, etc.)
└── android / ios / web          # Platform specific build files
```

## Supporting Assets
- `stitch_duplicate_of_novawallet/` – UI design mock‑ups and HTML snippets used for documentation and hand‑offs.
- `.planning/` – Generated planning artefacts (codebase maps, hand‑offs, etc.).

The repository is a **brownfield** codebase: a fully‑featured backend is already in place, while the mobile client is a thin Flutter wrapper awaiting further feature implementation.
