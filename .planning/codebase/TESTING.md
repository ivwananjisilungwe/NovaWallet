# Testing Patterns

**Analysis Date:** 2026-08-06

Two independent test setups — one per codebase. The backend (`novawallet-api/`) has a mature suite (124 `@Test` methods across 25 files); the frontend (`novawallet-app/`) has a single smoke test and is effectively untested.

---

## Backend (novawallet-api)

### Test Framework

- **JUnit 5** + **AssertJ** (fluent assertions) + **Mockito** (mocking).
- **Spring Boot Test** (`@SpringBootTest` + `@AutoConfigureMockMvc`) for integration tests, with **H2 in PostgreSQL compatibility mode** and Flyway migrations applied.
- Config: `novawallet-api/src/test/resources/application-test.yml` — `jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL`, `ddl-auto: none`, `show-sql: true`, Flyway `locations: classpath:db/migration`, and `app.kyc.upload-dir: target/test-uploads/kyc`. Activated by `@ActiveProfiles("test")` on every test class.
- Run commands (from `novawallet-api/`):
  ```bash
  ./mvnw test        # run all unit + integration tests
  ./mvnw test -Dtest=AuthServiceTest   # single test class
  ```
- **Coverage:** no JaCoCo plugin configured in `novawallet-api/pom.xml` — no enforced coverage gate.

### Test File Organization

- Mirrors the `src/main/java` package structure under `src/test/java/com/novawallet/novawallet_api/`:
  ```
  src/test/java/com/novawallet/novawallet_api/
  ├── auth/
  │   ├── controller/   # @SpringBootTest + MockMvc integration tests
  │   │   ├── AuthControllerIntegrationTest.java
  │   │   ├── BaseAuthIntegrationTest.java        (abstract shared base)
  │   │   ├── EmailVerificationIntegrationTest.java
  │   │   ├── LoginRateLimitingIntegrationTest.java
  │   │   ├── PinValidationIntegrationTest.java
  │   │   ├── RefreshTokenRotationIntegrationTest.java
  │   │   └── ...
  │   └── service/
  │       └── AuthServiceTest.java                (Mockito unit test)
  ├── audit/service/AuditServiceTest.java
  ├── fee/service/FeeEngineServiceTest.java
  ├── notification/NotificationServiceIntegrationTest.java
  ├── endpoint/          # cross-feature, security-sensitive endpoint suites
  │   ├── EndpointTestSupport.java                (shared base + helpers)
  │   ├── IdempotencyEndpointIntegrationTest.java
  │   ├── KycAdminEndpointIntegrationTest.java
  │   ├── ProfileFeeEndpointIntegrationTest.java
  │   └── TransactionEdgeCaseEndpointIntegrationTest.java
  ├── security/RateLimitFilterTest.java
  ├── transaction/
  │   ├── controller/ConcurrentTransferIntegrationTest.java
  │   ├── controller/TransactionFlowIntegrationTest.java
  │   └── service/TransactionHistoryServiceTest.java
  └── config/TestConfig.java                      (@TestConfiguration)
  ```
- Naming: `XxxTest` for units, `XxxIntegrationTest` for `@SpringBootTest` suites, `IntegrationTest` suffix also used for e.g. `NotificationServiceIntegrationTest`.

### Unit Test Pattern (Mockito)

Canonical pattern in `src/test/java/com/novawallet/novawallet_api/fee/service/FeeEngineServiceTest.java`:

```java
@ExtendWith(MockitoExtension.class)
class FeeEngineServiceTest {

    @Mock
    private FeeConfigurationRepository feeConfigurationRepository;

    private FeeEngineService feeEngineService;

    @BeforeEach
    void setUp() {
        feeEngineService = new FeeEngineService(feeConfigurationRepository);
    }

    @Nested
    class CalculateFee {

        @Test
        void shouldCalculateFlatFeeOnly() {
            FeeConfiguration config = FeeConfiguration.builder()
                    .transactionType(FeeType.WITHDRAWAL)
                    .flatFee(new BigDecimal("1.50"))
                    .active(true)
                    .build();
            when(feeConfigurationRepository
                    .findByTransactionTypeAndActiveTrue(FeeType.WITHDRAWAL))
                    .thenReturn(Optional.of(config));

            BigDecimal fee = feeEngineService.calculateFee(FeeType.WITHDRAWAL, new BigDecimal("100.00"));

            assertThat(fee).isEqualByComparingTo("1.50");
        }
    }
}
```

**Conventions observed in unit tests:**
- `@ExtendWith(MockitoExtension.class)` + `@Mock` fields; some suites relax strictness with `@MockitoSettings(strictness = Strictness.LENIENT)` (`auth/service/AuthServiceTest.java:43`).
- Services built **manually via constructor** in `setUp()` — never `@InjectMocks` (see `AuthServiceTest.java:71-97` where the real `BCryptPasswordEncoder`, a real `TokenService` wrapped with `Mockito.spy(...)`, and a manually wired `NotificationService` are composed).
- `@Captor ArgumentCaptor<User>` for save-argument verification (`AuthServiceTest.java:66-69`).
- `@Nested` inner classes group tests per method: `Register`, `Login`, `SetPin`, etc. (`AuthServiceTest.java:101-102`).
- Assertions via AssertJ: `assertThat(...).isEqualByComparingTo("2.00")`, `assertThatThrownBy(() -> ...).isInstanceOf(UnauthorizedException.class)`.
- Test method names: `shouldRegisterUserSuccessfully()` (camelCase). Section banner comments: `// ==================== Register ====================`.
- No `@ParameterizedTest` / `@DisplayName` in unit suites; `@DisplayName` is used on integration/endpoint tests.

### Integration Test Pattern (Spring Boot + MockMvc)

Adapted from `src/test/java/com/novawallet/novawallet_api/auth/controller/AuthControllerIntegrationTest.java`:

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_shouldReturn201AndTokens() throws Exception {
        RegisterRequest request = new RegisterRequest("John", "Doe",
                "john.integration@example.com", "+260971234567", "SecurePass@123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.user.emailVerified").value(false));
    }
}
```

**Conventions:**
- `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`; `@Autowired` `MockMvc` and `ObjectMapper`.
- Method names: `feature_shouldExpectedOutcome` with underscores (`register_shouldReturn409WhenEmailExists`). `@DisplayName("human readable")` used on endpoint-level tests (`auth/controller/EmailVerificationIntegrationTest.java:33`).
- Assertions on the `ApiResponse` envelope via `jsonPath`: `$.success`, `$.data.*`, `$.code` (error code from `ErrorResponse`, e.g. `$.code` = `DUPLICATE_RESOURCE`, `AuthControllerIntegrationTest.java:69`).
- Inline JSON bodies via **Java text blocks** (`login_json` block, `AuthControllerIntegrationTest.java:102-107`).

### Shared Test Bases (important reuse pattern)

- `auth/controller/BaseAuthIntegrationTest.java` — abstract; `@Autowired` `MockMvc`, `ObjectMapper`, `UserRepository`; `@BeforeEach` generates unique `testEmail`/`testPhone` via a static counter so tests share a single app context without unique-constraint collisions.
- `endpoint/EndpointTestSupport.java` — extends `BaseAuthIntegrationTest`; provides lifecycle helpers: `registerEndpointUser(label)` (returns a `RegisteredUser` **record** with `userId/email/phone/token`), `adminToken()` (JWT for the seeded admin from `bootstrap/AdminDataInitializer.java`), `createWalletFor(user)`, `uploadKycDocument(user)` (MockMultipartFile).
- `config/TestConfig.java` — `@TestConfiguration` with a `@Primary` bean that returns a deterministic `AccountNumberGenerator` backed by a mocked `WalletRepository`, so account numbers stay predictable across context starts.
- Endpoint suites register their own unique users via `registerEndpointUser`, avoiding reliance on DB cleanup between tests.

### Concurrency & Scheduler Testing

- `transaction/controller/ConcurrentTransferIntegrationTest.java` exercises pessimistic-lock behavior through MockMvc with parallel requests.
- `endpoint/IdempotencyEndpointIntegrationTest.java` and `transaction/controller/SchedulerIdempotencyIntegrationTest.java` verify `Idempotency-Key` dedupe and scheduler-driven retry semantics.
- Rate limiting verified by `security/RateLimitFilterTest.java` and `auth/controller/LoginRateLimitingIntegrationTest.java`.

### Mocking

- **Mockito** for unit tests (`@Mock`, `@Captor`, `spy`). No other mocking libs.
- Spring beans are overridden in tests via `@TestConfiguration` + `@Primary` (test account-number generator) rather than `@MockBean`; auth integration tests exercise the real filters/controllers, not mocked controllers.
- **What to mock:** repositories, `JwtUtil`, `MailService`, scheduler dependencies in unit tests.
- **What NOT to mock:** the `PasswordEncoder` (real `BCryptPasswordEncoder` used in `AuthServiceTest`), the HTTP layer in integration tests (real `MockMvc` pipeline with real `JwtAuthFilter`, `RateLimitFilter`, `IdempotencyFilter`).

### Fixtures

- Entities built with Lombok builders inline (`User.builder()...`, `FeeConfiguration.builder()...`).
- Test profile config (H2 + Flyway) in `src/test/resources/application-test.yml`; Flyway `V1..V10` migrations provide the schema.
- No dedicated fixture/factory classes — builders + `EndpointTestSupport` helpers cover it.

---

## Frontend (novawallet-app)

### Test Framework

- **flutter_test** (bundled with the SDK, declared in `novawallet-app/pubspec.yaml` dev_dependencies); no mockito/mocktail, no `integration_test` package.
- Run commands (from `novawallet-app/`):
  ```bash
  flutter test               # run all tests
  flutter test --coverage    # produce lcov.info (no threshold enforced)
  flutter analyze            # static analysis gate (flutter_lints)
  dart format --set-exit-if-changed .   # formatting gate
  ```
- Coverage reporting is available but **no target/threshold is configured** and no CI exists in the repo (`.github/` absent).

### Test File Organization

- Single flat file: `novawallet-app/test/widget_test.dart` (12 lines).

### Existing Test Content

```dart
// Core widget smoke test — replaced with themed app-trees once features land.
import 'package:flutter_test/flutter_test.dart';
import 'package:novawallet_app/core/theme/app_theme.dart';

void main() {
  test('light theme exposes the brand palette', () {
    final theme = AppTheme.light;
    // Design system anchor: indigo primary.
    expect(theme.colorScheme.primary.toARGB32(), 0xFF3525CD);
  });
}
```

### Mocking

- No mocking framework configured (no `mockito`/`mocktail` in `pubspec.yaml`). The intended seam for tests is Riverpod provider overrides: `core/providers.dart:10-12` explicitly documents that tests should override `apiClientProvider` with a mock or an `ApiClient` pointing at a fake backend (no such test exists yet).

---

## Coverage Gaps & Risks

| Area | Status | Location | Priority |
|------|--------|----------|----------|
| Backend service unit tests (auth, fee, audit, transaction history) | Covered | `auth/service/`, `fee/service/`, `audit/service/`, `transaction/service/` | — |
| Backend controller/endpoint integration | Covered (auth, KYC, idempotency, fees, admin security, rate limiting, concurrency) | `auth/controller/`, `endpoint/`, `transaction/controller/` | — |
| Java coverage threshold | **Not enforced** (no JaCoCo) | `novawallet-api/pom.xml` | Medium |
| Flutter repositories/providers (`AuthRepository`, `ApiClient` refresh single-flight, `AuthNotifier`, `_AuthInterceptor` retry logic, `TransactionRepository`) | **Untested** — the highest-risk Dart code has zero tests | `lib/core/network/api_client.dart`, `lib/features/*/data/*.dart`, `lib/features/*/providers/*.dart` | High |
| Flutter widget tests (screens, `PinPad`, `BalanceCard`, router redirect gates) | **Untested** — only one theme smoke assertion | `lib/features/*/screens/`, `lib/core/widgets/`, `lib/app/router.dart` | High |
| Flutter E2E / integration tests (login → PIN → dashboard flows) | **Not present** (no `integration_test/` directory) | — | Medium |
| Flutter goldens / golden tests | **Not used** | — | Low |

Recommended next steps when adding Flutter tests: add `mocktail` (no codegen) or hand-written fakes per repo-level guidance, use `ProviderContainer(overrides: [apiClientProvider.overrideWithValue(...)])` to unit-test repositories/providers against a fabricated ApiClient, and use `testWidgets` + `pumpWidget` with `ProviderScope` for widget tests.

---

*Testing analysis: 2026-08-06*