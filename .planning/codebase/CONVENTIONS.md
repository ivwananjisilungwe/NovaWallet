# Coding Conventions & Practices

## General Principles (ECC common rules)
- **Immutability** is enforced where possible – data classes are immutable, fields are `final` (Java) or `final` (Dart).
- **KISS** and **DRY** are followed; business logic lives in service classes, controllers are thin.
- **Error handling**: custom exception hierarchy (`BadRequestException`, `UnauthorizedException`, etc.) with a global `@ControllerAdvice` (`GlobalExceptionHandler`).
- **Input validation**: Spring `@Valid` on DTOs, Java Bean Validation annotations, and explicit checks for PIN, amounts, etc.
- **Security**: JWT authentication, rate‑limiting (`LoginRateLimiter`), CSRF protection not needed for stateless API.
- **Logging**: Structured logging via Logstash Logback encoder; no sensitive data printed.

## Java / Spring Specifics
- **Package‑by‑feature** layout – each domain (auth, kyc, transaction, wallet, admin) has its own sub‑package containing controller, service, DTOs, and repository.
- **Lombok** is used for boilerplate reduction (`@Getter`, `@Setter`, `@Builder`).
- **Spring Data JPA** for repository pattern; repositories are interfaces extending `JpaRepository`.
- **Flyway** migrations are the single source of truth for DB schema.
- **AOP Audit** – `@Audited` annotation triggers `AuditedAspect` to write audit logs.
- **Idempotency** – `IdempotencyKey` entity + filter ensures safe retries for payment‑related endpoints.
- **Configuration** – All secrets are externalised via `${ENV_VAR}` placeholders; defaults are deliberately failing fast (`${APP_ADMIN_PASSWORD}` without default).

## Dart / Flutter Specifics
- **State Management**: Riverpod (`flutter_riverpod`) – providers are defined in the `providers` folder (implicit). UI widgets read state via `ref.watch` / `ref.read`.
- **Networking**: `dio` with interceptors for adding Authorization header and handling errors.
- **Routing**: `go_router` declarative routes; deep linking enabled via `uri` parsing.
- **Secure Storage**: `flutter_secure_storage` stores JWT and PIN hashes on device.
- **Formatting**: `dart format` enforced via pre‑commit hook; line length 80.
- **Testing**: Default widget test present; additional tests are encouraged but not yet added.

## Documentation & Linting
- **Java**: Checkstyle & SpotBugs via Maven plugins (not shown in `pom.xml` but recommended).
- **Dart**: `analysis_options.yaml` references `flutter_lints` for style enforcement.
- **README/Spec**: Comprehensive `SPECIFICATION.md`, `USER-FLOW.md`, and UI hand‑offs are kept in the repo root.
