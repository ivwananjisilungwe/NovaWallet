# Coding Conventions

**Analysis Date:** 2026-08-06

Monorepo with two codebases, each with independent conventions:

| Codebase | Language/Stack | Root |
|----------|----------------|------|
| Backend | Java 17, Spring Boot 3.5.3, Maven | `novawallet-api/` |
| Frontend | Dart 3.12+, Flutter (Riverpod, GoRouter, Dio) | `novawallet-app/` |

Conventions below are grouped per codebase. File examples use actual in-repo code.

---

## Backend (novawallet-api)

### Naming Patterns

**Files:**
- One top-level type per file; file name equals class/record name. PascalCase, e.g. `AuthController.java`, `GlobalExceptionHandler.java`, `RegisterRequest.java`.
- Flyway migrations `V<n>__snake_case.sql` under `novawallet-api/src/main/resources/db/migration/` (currently `V1__init_schema.sql` … `V10__create_notifications_tables.sql`).

**Packages:**
- Root package `com.novawallet.novawallet_api` — note the **underscore** in `novawallet_api` (derived from the Maven artifactId `novawallet-api`; do not rename).
- Domain packages: `auth/`, `user/`, `wallet/`, `transaction/`, `fee/`, `kyc/`, `admin/`, `token/`, `notification/`, `audit/`, `idempotency/` (e.g. `novawallet-api/src/main/java/com/novawallet/novawallet_api/auth/`).
- Inside each domain, a fixed sub-layer split: `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `enums/`. DTOs split further into `dto/request/` and `dto/response/`.
- Cross-cutting packages at root level: `config/`, `exception/`, `common/dto/`, `security/`, `filter/`, `bootstrap/`, `audit/` (annotation + aspect).

**Classes:**
- PascalCase; **records** for all DTOs (`RegisterRequest`, `ApiResponse`, `ErrorResponse`, `AuthResponse`), **classes** for entities, services, controllers.
- Test classes: `XxxServiceTest` (unit), `XxxIntegrationTest` (Spring Boot / MockMvc), shared bases `BaseAuthIntegrationTest` and `EndpointTestSupport`.

**Methods/fields:**
- camelCase. `private static final Logger log = LoggerFactory.getLogger(X.class)` is the universal logger declaration (see `novawallet-api/src/main/java/com/novawallet/novawallet_api/auth/service/AuthService.java:40`).
- `static final` constants are SCREAMING_SNAKE_CASE: `MAX_PIN_ATTEMPTS`, `PIN_LOCK_MINUTES` (`AuthService.java:41-43`).

### Code Style

- **4-space indentation, no tabs** (verified: zero tab characters in `src/main`).
- Long lines tolerated (some exceed 130 cols, e.g. `AuthController.java:119`).
- Javadoc on classes and public methods with `<p>` paragraphs and `@see` references — see `AuthController.java:22-30`, `User.java:14-22`. DTOs use short one-liners: `/** DTO: user registration. */` (`RegisterRequest.java`).
- Section banner comments separate method groups: `// ==================== Register ====================` (`AuthService.java:68`, used consistently in services and test classes).
- Swagger annotations on controllers (`@Operation`, `@ApiResponses`, `@Tag`) and `@Schema(description=…, example=…)` on every DTO component (see `RegisterRequest.java`).

### Import Organization

- Standard Java convention: `com.novawallet.*` imports first, then `org.*`/`io.*`, then `java.*`. No wildcard imports except `javax.persistence.*`, `lombok.*`, `org.springframework.web.bind.annotation.*`, and `jakarta.validation.constraints.*` on entities/DTOs; `com.novawallet.novawallet_api.auth.dto.request.*` appears once in `AuthService.java:3` (avoid introducing new wildcards).

### Layering & Dependency Rules

- **Controller → Service → Repository**: controllers stay thin (request validation via `@Valid`, envelope wrapping); business logic in services; data access in Spring Data JPA interfaces in `repository/`.
- **Constructor injection everywhere** — no `@Autowired` fields, no field injection. Example: `AuthController(AuthService, LoginRateLimiter)` (`AuthController.java:39-42`), `SecurityConfig(JwtAuthFilter, IdempotencyFilter)` (`config/SecurityConfig.java:35-38`). `@Value("${app.cors.origins:}")` used for property injection with default.
- Services annotated `@Service`; transactions declared at class level `@Transactional` with `readOnly = true` on queries and `REQUIRES_NEW` for audit/idempotency writes (see `admin/service/AdminService.java:37-116`, `audit/service/AuditService.java:35`, `idempotency/service/IdempotencyService.java:67`).

### Entities

- Lombok on every entity: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`, plus `@Entity @Table(name = "users")` (`user/entity/User.java:23-32`).
- UUID primary keys: `@GeneratedValue(strategy = GenerationType.UUID) private UUID id;`.
- Audit timestamps: `@CreatedDate`/`@LastModifiedDate` + `@EntityListeners(AuditingEntityListener.class)` plus `@PrePersist`/`@PreUpdate` fallbacks (`User.java:119-136`).
- Optimistic locking via `@Version private Integer version;` (`User.java:116-117`).
- Soft delete via Hibernate `@SQLRestriction("deleted = false")` (`User.java:26`).
- Enums stored as strings: `@Enumerated(EnumType.STRING)` (`User.java:63`).
- `@Builder.Default` for fields with non-null defaults (`pinAttempts`, `role`, `kycStatus` — `User.java:57-86`).
- Money fields are `BigDecimal` scaled to 2 (`EndpointTestSupport.java:82`).

### API Response & Error Handling

- Success envelope: `ApiResponse<T>` record `{success, data, message, timestamp}` in `common/dto/ApiResponse.java` with static factories `success(data, message)` and `error(message)`. Controllers return `ResponseEntity<ApiResponse<T>>` (`AuthController.java:63-65`).
- Errors are **not** returned through `ApiResponse`; they are thrown as domain exceptions and mapped by `@RestControllerAdvice GlobalExceptionHandler` (`exception/GlobalExceptionHandler.java`) to the `ErrorResponse` record `{status, code, message, timestamp, path, errors}` (`exception/ErrorResponse.java`).
- Stable error codes: `RESOURCE_NOT_FOUND`, `DUPLICATE_RESOURCE`, `BAD_REQUEST`, `UNAUTHORIZED`, `RATE_LIMITED`, `FORBIDDEN`, `VALIDATION_ERROR`, `INTERNAL_SERVER_ERROR`.
- Domain exceptions in `exception/` all extend `RuntimeException`: `BadRequestException`, `ResourceNotFoundException`, `DuplicateResourceException`, `UnauthorizedException`, `ForbiddenException`, `RateLimitException`.
- Include context in exception messages: `"Email already registered: " + request.email()` (`AuthService.java:72`).
- Never leak stack traces: catch-all `handleGeneralException` logs `log.error("Unhandled exception", exception)` and returns a generic 500 (`GlobalExceptionHandler.java:140-158`).
- Bean Validation on request records: `@NotBlank`, `@Size`, `@Email`, `@Pattern` with human-readable `message =` on each component (`auth/dto/request/RegisterRequest.java`).

### Logging

- SLF4J via the `log` constant (above). Levels: `log.info` for lifecycle events (`AuthService.java:89`), `log.warn` for failed attempts (`AuthService.java:108`), `log.error` for unhandled exceptions.
- Structured config in `novawallet-api/src/main/resources/logback-spring.xml`: console pattern embeds `%X{requestId}` (MDC populated by `filter/RequestTracingFilter.java`); prod profile switches to `LogstashEncoder` JSON + rolling file `/var/log/novawallet/api-%d{yyyy-MM-dd}.log.gz`.
- Config split: `application.yml` (base) + `application-dev.yml` + `application-prod.yml`; secrets/config externalized via env vars with dev defaults, e.g. `jwt.secret: ${JWT_SECRET:...}`, `app.admin.password: ${APP_ADMIN_PASSWORD:Admin@123}` (`application.yml`).

### Cross-Cutting Features (conventions to preserve)

- Security: single `SecurityFilterChain` in `config/SecurityConfig.java` — stateless JWT, explicit `permitAll()` list for auth/swagger/actuator endpoints, security headers (HSTS, CSP), CORS from `app.cors.origins`.
- Filter chain order: `JwtAuthFilter` (before UsernamePasswordAuthenticationFilter) → `IdempotencyFilter` (after) → `RateLimitFilter` → `RequestTracingFilter` (see `SecurityConfig.java:86-87`).
- Audit: custom annotation `@Audited(action = "WALLET_ACTIVATE", entityType = "Wallet")` on service methods, intercepted by `@Aspect AuditedAspect` (`audit/aspect/AuditedAspect.java`) which writes via `AuditService` in a `REQUIRES_NEW` transaction.
- Money-moving operations use pessimistic locking with **consistent lock order** to prevent deadlocks (`transaction/service/TransactionService.java:234-239`, `findActiveWalletWithLock` at line 371).

---

## Frontend (novawallet-app)

### Naming Patterns

**Files/directories:**
- `snake_case.dart`. Feature-first layout: `lib/features/<feature>/{data,models,providers,screens,routes}/` (e.g. `lib/features/auth/data/auth_repository.dart`, `lib/features/auth/screens/login_screen.dart`).
- Shared code under `lib/core/{config,network,storage,theme,utils,widgets}/`; app wiring under `lib/app/` (`app.dart`, `router.dart`, `main_shell.dart`).

**Classes:**
- PascalCase. Widgets end in `Screen` (`LoginScreen`, `DashboardScreen`); repository classes `XxxRepository` (`AuthRepository`, `WalletRepository`, `TransactionRepository`, `KycRepository`, `UserRepository`); notifiers `AuthNotifier`; providers `authProvider`, `apiClientProvider` (camelCase + `Provider` suffix).
- Private state classes and members prefixed `_` (`_LoginScreenState`, `_email`, `_submit`, `_toast` — `login_screen.dart:18-54`).

**Variables/constants:**
- camelCase locals; `static const` colors are camelCase in `NovaColors` (`primary`, `primaryContainer` — `core/theme/nova_colors.dart`); private constants use `k`-prefix camelCase in `TokenStorage` (`_kAccessToken`, `_kRefreshToken` — `core/storage/token_storage.dart:8-11`); `static const _anonymousPaths` set in `core/network/api_client.dart:254`.

### Code Style

- `dart format` standard: 2-space indent, 80-col line length, trailing commas on multi-line argument/parameter lists (visible throughout, e.g. `api_client.dart:35-40`).
- `analysis_options.yaml` includes `package:flutter_lints/flutter.yaml` with no custom rules enabled (defaults apply: `avoid_print`, etc.).
- Extensive `///` doc comments on classes, fields, and methods — often documenting endpoint contracts (`auth_repository.dart:8-15`) or design-system provenance (`nova_colors.dart:1-5`).

### Import Organization

- External `package:` imports first (`package:flutter/material.dart`, `package:flutter_riverpod/flutter_riverpod.dart`, `package:go_router/go_router.dart`, `package:dio/dio.dart`), then internal **relative imports** (`../../../core/network/api_client.dart`, `../providers/auth_provider.dart`) — relative imports are the actual in-repo convention, despite the general guidance to prefer `package:` imports.
- Barrel file for shared widgets: `lib/core/widgets/widgets.dart` declares `library;` and `export 'pill_button.dart';`.

### Immutability & Models

- Prefer `const` constructors and `final` fields; widgets use `const` where possible (`LoginScreen({super.key})`).
- Models are hand-written classes (no freezed/json_serializable codegen in the repo): `const` constructor, `final` nullable fields with defaults, `factory Model.fromJson(Map<String, dynamic>)` and `Map<String, dynamic> toJson()` (`features/auth/models/user.dart:51-90`). Defensive casts: `json['x'] as String?`, `?? 'USER'` defaults.
- State objects are immutable with getters for derived values (`AuthState.isAuthenticated`, `isAdmin` — `features/auth/providers/auth_provider.dart:16-30`).

### State Management

- **Riverpod, hand-written (no codegen):** `ChangeNotifierProvider<AuthNotifier>` for session state (`auth_provider.dart:117-124`); plain `Provider<Dio>` / `Provider<TokenStorage>` in the composition root `core/providers.dart`.
- The composition root throws `UnimplementedError` for `apiClientProvider` until `main()` overrides it with the real client (`core/providers.dart:24-27`, `main.dart:18-24` uses `ProviderContainer(overrides: [...])` + `UncontrolledProviderScope`).
- Router: GoRouter with single redirect gate reading auth state synchronously via `_ref.read(authProvider).state` (`app/router.dart:41-64`); `refreshListenable: _ref.read(authProvider)`.

### Networking & Error Handling

- All HTTP goes through `ApiClient` (`core/network/api_client.dart`): base URL/timeouts from `AppConfig`, Bearer token injection, **single-flight refresh** with one retry (`_AuthInterceptor`), `Idempotency-Key` header on mutating calls, envelope unwrapping in `_unwrap`, and normalization of `DioException` → `ApiException` (`_ErrorInterceptor`).
- Repositories: `const XRepository(this._api)` constructor injection; methods call `_api.get/post/...` with `parser: (json) => Model.fromJson(json as Map<String, dynamic>)`, and throw `const ApiException(message: ...)` when a null payload shouldn't be possible (`transaction_repository.dart:31-32`).
- Screens catch errors as: `on ApiException catch (e) { if (mounted) _toast(e.displayMessage); } catch (_) { ... }` — always check `mounted` after `await` before touching context (`login_screen.dart:45-51`).
- `ApiException` exposes typed helpers: `isNetworkError`, `isUnauthorized` (401), `isRateLimited` (429), `isConflict` (409), and `displayMessage` for SnackBar text (`core/network/api_exception.dart:19-30`).
- Idempotency keys are generated client-side per mutation, e.g. `'send-$receiver-$amount-${DateTime.now().millisecondsSinceEpoch}'` (`features/wallet/screens/send_screen.dart:102-103`).

### UI & Theming

- Material 3 light theme built in `AppTheme.light` (`core/theme/app_theme.dart`); all colors come from `NovaColors` design tokens (indigo primary `0xFF3525CD`, `background` `0xFFF8F9FF` — `core/theme/nova_colors.dart`).
- Screens: `Scaffold` + `SafeArea` + `SingleChildScrollView` with `padding: const EdgeInsets.all(24)`; shared components (`BalanceCard`, `PinPad`, `PillButton`) exported from `core/widgets/widgets.dart`.
- Navigation via `context.push(...)` for pushes (`login_screen.dart:117`); auth gates redirect automatically.

### Comments

- `///` doc comments on public declarations (classes, methods, fields) explaining behavior and rationale — e.g. the single-flight refresh explanation in `api_client.dart:163-169`.
- Section banner comments mirror the backend style: `// ---- Core brand anchors ----` (`nova_colors.dart:9`), `// ---------------------------------------------------------------------------` separators in `core/providers.dart:7-13`.

---

*Convention analysis: 2026-08-06*
