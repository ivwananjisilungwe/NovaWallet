# NovaWallet

### A Modern Digital Wallet and Financial Management Platform

NovaWallet is a Zambia-focused digital wallet and personal finance platform designed to make everyday financial services more accessible, convenient and manageable.

The platform is being engineered to support individuals, students, professionals, entrepreneurs, freelancers, small businesses and families through a secure, scalable financial technology ecosystem.

NovaWallet aims to bring together digital wallets, money movement, financial management, payments and intelligent financial insights in one integrated platform.

---

## Project Status

**Active Development**

NovaWallet is currently under active backend engineering and security hardening.

The current implementation focuses on establishing reliable authentication, wallet operations, transaction safeguards, payout processing, authorization and database integrity before expanding into the complete product.

This project is not yet production-ready. Live financial services, external provider integrations and regulatory readiness remain subject to their respective technical, operational and legal requirements.

## Vision

To build a trusted financial technology platform that empowers individuals and businesses to manage, move and understand their money through accessible digital financial services.

## Core Product Areas

| Area | Description |
|---|---|
| Digital Wallets | Secure personal wallet accounts and balance management. |
| Money Transfers | Account-to-account transfers and money movement. |
| Deposits and Withdrawals | Wallet funding and cash-out capabilities. |
| Payments | Payment requests, QR payments and merchant payment capabilities. |
| Bills and Airtime | Utility payments, airtime and mobile data services. |
| Transaction Management | Transaction history, receipts and financial activity tracking. |
| Savings and Budgets | Savings goals, budgeting and personal financial management. |
| Cards | Future virtual and physical card programme. |
| NovaAI | AI-powered financial insights, spending categorisation and recommendations. |
| Administration | Operational management, transaction monitoring, KYC review and support. |

These represent the intended product capabilities; they are not all currently implemented.

---

## Technology Stack

### Backend

| Technology | Purpose |
|---|---|
| Java | Primary backend language |
| Spring Boot | Backend application framework |
| Spring Security | Authentication and authorization |
| Spring Data JPA | Persistence and data access |
| PostgreSQL | Relational database |
| Flyway | Database schema versioning |
| JWT | Access-token authentication |
| BCrypt | Password hashing |
| Maven | Build and dependency management |
| Springdoc OpenAPI | API documentation |
| Lombok | Boilerplate reduction |

### Planned Client Applications

**Mobile application**
- Flutter
- Dart
- Riverpod
- GoRouter
- Dio
- Drift
- Firebase Cloud Messaging

**Administration dashboard**
- React
- TypeScript
- Vite
- Tailwind CSS
- TanStack Query
- TanStack Table

The mobile and administration applications are separate development tracks.

---

## System Architecture

NovaWallet follows a domain-oriented architecture with a deliberate evolution path towards independently deployable microservices.

### Architectural Evolution

```text
Stage 1: Modular Monolith
          |
          v
Stage 2: Event-Driven Modular Monolith
          |
          v
Stage 3: Selective Microservices
```

The modular monolith establishes strong domain boundaries before services are independently deployed.

The event-driven stage introduces reliable asynchronous communication, transactional outbox patterns and message processing.

The long-term architecture will support independently deployable services where operational scale and domain ownership justify extraction.

### Target Service Boundaries

| Service | Responsibility |
|---|---|
| Identity & KYC | Authentication, identity and verification |
| Wallet & Ledger | Wallet accounts, balances and financial records |
| Payments | Payment orchestration and external provider integrations |
| Notifications | Email, push and transactional notifications |

Service boundaries will evolve as the platform matures.

### Financial Integrity Principle

The Wallet and Ledger domains own financial truth.

Payments coordinates payment execution and external provider interactions. It must not independently modify wallet balances.

Financial operations must be validated and recorded through controlled backend processes.

---

## Current Backend Implementation

The backend has progressed through its initial engineering foundations and financial safety work.

### Implemented foundations

- User registration and authentication.
- Password hashing and credential security.
- JWT-based authentication.
- Role-based endpoint protection using the existing administrator role.
- Wallet creation during user registration.
- Wallet account number generation.
- Wallet balance retrieval.
- Transaction and database foundations.
- PostgreSQL integration.
- Flyway database migrations.
- API documentation foundations.

### Financial processing and security

The engineering work has also covered:

- Financial transaction safeguards.
- Payout processing and reconciliation foundations.
- Payout idempotency and controlled retry handling.
- Provider callback containment.
- Database-backed access-token revocation.
- JWT issuer, audience and signature validation.
- Logout and refresh-token revocation.
- Email-verification enforcement at financial operation boundaries.
- Sensitive token and notification-data redaction.

These capabilities are being expanded through the ongoing security and database hardening programme.

### Current limitations

- A complete double-entry accounting ledger has not yet been implemented.
- Full payment orchestration and external provider integrations remain future development work.
- Collections remain safely contained pending verified provider security requirements.
- The administration and mobile applications are separate development tracks.
- Production infrastructure and live financial operations are not yet enabled.

---

## Engineering Roadmap

NovaWallet development is organized into major engineering phases.

| Phase | Description | Status |
|---|---|---|
| Phase 0 | Architecture assessment and engineering foundations | Complete |
| Phase 1A | Financial safeguards | Complete |
| Phase 1B | Payout processing and reconciliation | Complete |
| Phase 1C | Security and database hardening | In progress |
| Phase 1D | Backend product and API completion | Planned |
| Phase 2 | Double-entry ledger and accounting infrastructure | Planned |
| Phase 3 | Payment orchestration and provider integrations | Planned |
| Phase 4 | Modular monolith boundary enforcement | Planned |
| Phase 5 | Event-driven architecture | Planned |
| Phase 6 | Notifications microservice extraction | Planned |
| Phase 7 | Payments microservice extraction | Planned |
| Phase 8 | Identity and KYC microservice extraction | Planned |
| Phase 9 | Production engineering and launch readiness | Planned |

### Application Development

In parallel with backend development:

- **NovaWallet Mobile:** Flutter-based customer application.
- **NovaWallet Admin:** Web-based operational administration console.

The applications will integrate with the backend through documented and versioned APIs.

---

## Security and Financial Engineering Principles

Security and financial integrity are foundational requirements.

NovaWallet follows these engineering principles:

1. **Authentication security** — Secure credential handling, token validation and controlled session revocation.
2. **Authorization** — Ownership verification and role-based access controls.
3. **Financial integrity** — Transaction boundaries, idempotency, concurrency controls and controlled balance updates.
4. **Database integrity** — Constraints, migrations and transactional consistency.
5. **Data protection** — Sensitive data minimisation and protected handling of customer information.
6. **Auditability** — Traceable administrative and financial operations.
7. **Provider security** — Verified callbacks, signature validation and reconciliation.
8. **Operational resilience** — Monitoring, backups, recovery and controlled failure handling.

Security testing and independent review remain part of the development lifecycle.

---

## NovaAI

NovaAI is the planned financial intelligence component of NovaWallet.

Its intended capabilities include:

- Automatic spending categorisation.
- Income and expenditure summaries.
- Spending pattern analysis.
- Budget recommendations.
- Savings insights.
- Personal financial guidance.

NovaAI is intended to assist users in understanding their finances.

It will not autonomously transfer money, execute financial transactions or change account settings.

---

## Financial Services and Regulatory Readiness

NovaWallet is being developed with the Zambian financial services environment in mind.

Before enabling live financial operations, the project must establish the appropriate legal, regulatory, security and commercial arrangements.

This includes assessment of:

- Applicable Bank of Zambia requirements.
- Financial Intelligence Centre obligations.
- Anti-money laundering and counter-terrorist financing controls.
- Know Your Customer requirements.
- Data protection and privacy obligations.
- Payment provider and banking partnerships.
- Consumer protection and complaints handling.

The project does not claim regulatory approval, financial institution status or authorization to provide regulated services.

---

## Repository Structure

The backend repository is organized around the Spring Boot application.

```text
novawallet-api/
├── docs/
│   ├── architecture/
│   └── phase-1c/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/novawallet/
│   │   └── resources/
│   │       └── db/
│   │           └── migration/
│   └── test/
├── .env.example
├── docker-compose.yml
├── mvnw
├── pom.xml
└── README.md
```

The exact package and documentation structure may evolve as engineering work progresses.

---

## Development Setup

### Prerequisites

- Java Development Kit.
- PostgreSQL.
- Git.
- Docker and Docker Compose (for the repository's containerized development environment).

### Clone the repository

```bash
git clone <YOUR_REPOSITORY_URL>
cd novawallet-api
```

### Configure the environment

Inspect `.env.example` and configure the required local environment variables.

Never commit real credentials, database passwords, signing secrets or API keys.

### Start the application

Configure PostgreSQL using the repository's supported local configuration.

Then run:

```bash
./mvnw spring-boot:run
```

### Run tests

```bash
./mvnw clean test
```

Database-specific integration tests should use the repository's PostgreSQL testing configuration.

### API Documentation

Open the configured Springdoc OpenAPI / Swagger UI endpoint while the application is running.

Consult the application configuration for the exact documentation URL.

---

## Development Philosophy

NovaWallet is developed with an emphasis on:

- Security before feature expansion.
- Financial correctness before convenience.
- Clear domain ownership.
- Maintainable software architecture.
- Automated testing and regression protection.
- Reliable data management.
- Incremental and evidence-driven engineering.
- Long-term scalability.

The objective is not simply to build a wallet application, but to establish the engineering foundations for a broader financial technology ecosystem.

---

## Project Direction

NovaWallet is being developed as a long-term financial technology platform with an initial focus on Zambia and the potential to expand into additional markets as the platform, partnerships and regulatory capabilities mature.

**Build securely. Move money reliably. Empower financial decisions.**

---

**NovaWallet**  
*Financial technology for a more connected financial future.*
