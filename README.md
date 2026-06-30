# NovaWallet

A full-stack digital wallet mobile app: **Flutter frontend** + **Spring Boot backend API**.

Manages users, wallet balances, deposits, withdrawals, and peer-to-peer transfers with JWT authentication, transaction PIN security, configurable fee engine, idempotency safeguards, audit logging, and scheduled job processing. Designed for students and small businesses in Zambia.

## Structure

```
NovaWallet/
├── novawallet-api/       Spring Boot backend (Java 17, Maven)
└── frontend/             Flutter mobile app (coming soon)
```

## Backend Stack

- **Java 17** + **Spring Boot 3**
- **PostgreSQL** (production) / **H2** (development)
- **Flyway** for database migrations
- **JWT** authentication (jjwt)
- **OpenAPI/Swagger** for API docs
- **Testcontainers** for integration testing

## Getting Started

```bash
cd novawallet-api
mvn clean compile
mvn spring-boot:run
```

Health check: `GET http://localhost:8080/api/v1/health`
