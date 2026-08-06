# NovaWallet API — Phase 0 Report

## Foundation & Learning Reference

> **Project**: NovaWallet — Digital wallet backend API
> **Stack**: Java 17, Spring Boot 3.5.3, Maven, PostgreSQL, Flyway
> **Date**: July 2026

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Project Structure](#2-project-structure)
3. [Dependencies (pom.xml)](#3-dependencies-pomxml)
4. [Multi-Profile Configuration](#4-multi-profile-configuration)
5. [Core Annotations Reference](#5-core-annotations-reference)
6. [Component Deep Dive](#6-component-deep-dive)
   - [6.1 Main Application Class](#61-main-application-class)
   - [6.2 Global Exception Handler](#62-global-exception-handler)
   - [6.3 Standard API Response](#63-standard-api-response)
   - [6.4 Security Configuration](#64-security-configuration)
   - [6.5 Request Tracing Filter](#65-request-tracing-filter)
   - [6.6 Test Controller](#66-test-controller)
7. [Testing the API](#7-testing-the-api)
8. [Key Concepts Learned](#8-key-concepts-learned)

---

## 1. Architecture Overview

### Request Flow

```
HTTP Request
      |
      v
RequestTracingFilter (MDC + X-Request-ID)
      |
      v
SecurityFilterChain (headers, CORS, CSRF)
      |
      v
Controller (receives request)
      |
      v
Service (business logic) — Phase 1+
      |
      v
Repository (database) — Phase 1+
```

### Response Flow (Error)

```
Controller throws Exception
      |
      v
GlobalExceptionHandler (@RestControllerAdvice)
      |
      v
ErrorResponse record (uniform JSON)
      |
      v
HTTP Response with correct status code
```

---

## 2. Project Structure

```
src/main/java/com/novawallet/novawallet_api/
│
├── NovawalletApiApplication.java    # Entry point
│
├── config/
│   ├── SecurityConfig.java          # HTTP security + headers
│   └── PasswordConfig.java          # BCrypt password encoder
│
├── controller/
│   └── TestController.java          # Test endpoints
│
├── dto/
│   └── ApiResponse.java             # Standard response wrapper
│
├── exception/
│   ├── ErrorResponse.java           # Error response record
│   ├── GlobalExceptionHandler.java  # Central error handler
│   └── ResourceNotFoundException.java  # Custom exception
│
├── filter/
│   └── RequestTracingFilter.java    # MDC request tracing
│
├── audit/      (empty — Phase 4)
├── entity/     (empty — Phase 1)
├── repository/ (empty — Phase 1)
├── scheduler/  (empty — Phase 6)
├── security/   (empty — Phase 1)
└── service/    (empty — Phase 1)
```

```
src/main/resources/
├── application.yml         # Base config (shared across profiles)
├── application-dev.yml     # Development: local PostgreSQL
├── application-prod.yml    # Production: env-var PostgreSQL
├── application-test.yml    # Test config
└── db/migration/           # Flyway SQL migrations
```

---

## 3. Dependencies (pom.xml)

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST API — Tomcat, REST controllers, JSON serialization |
| `spring-boot-starter-data-jpa` | Database access via JPA/Hibernate |
| `spring-boot-starter-validation` | `@Valid`, `@NotBlank`, `@Positive` etc. |
| `spring-boot-starter-security` | Authentication, authorization, security headers |
| `postgresql` | PostgreSQL JDBC driver (runtime only) |
| `flyway-core` + `flyway-database-postgresql` | Database migration management |
| `spring-boot-starter-actuator` | Health checks, metrics, monitoring |
| `spring-boot-starter-cache` | Caching abstraction |
| `caffeine` | High-performance in-memory cache |
| `spring-boot-starter-mail` | Email sending |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI + OpenAPI docs |
| `jjwt-api/impl/jackson` | JWT creation and validation |
| `lombok` | Boilerplate reduction (`@Data`, `@Builder`, etc.) |
| `spring-boot-starter-test` | JUnit 5, Mockito, integration test support |
| `h2` | In-memory database for tests |

### Build Plugins

| Plugin | Purpose |
|---|---|
| `spring-boot-maven-plugin` | Package executable JAR, `spring-boot:run` goal |
| `maven-compiler-plugin` | Java 17 compilation, Lombok annotation processing |

---

## 4. Multi-Profile Configuration

### How Spring Profiles Work

Spring profiles allow different configurations for different environments without changing code.

```
application.yml          ← shared by ALL profiles
application-dev.yml      ← loaded when spring.profiles.active=dev
application-prod.yml     ← loaded when spring.profiles.active=prod
application-test.yml     ← loaded when spring.profiles.active=test
```

**Override rule**: Profile-specific files override values in `application.yml`.

### Base: `application.yml`

```yaml
spring:
  application:
    name: novawallet-api

  profiles:
    active: dev              # <-- Default profile

server:
  servlet:
    context-path: /api       # <-- All endpoints prefixed with /api

management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - info            # <-- Only expose health + info

logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%X{requestId}] %-5level %logger{36} - %msg%n"
```

### Dev: `application-dev.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/novawallet
    username: postgres
    password: 8407

  jpa:
    hibernate:
      ddl-auto: validate     # Validate schema, do not modify DB
    show-sql: true            # Log all SQL queries
```

### Prod: `application-prod.yml`

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}          # Environment variables
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false               # No SQL logging in production
```

---

## 5. Core Annotations Reference

### 5.1 Spring Boot / Application

| Annotation | Target | What It Does |
|---|---|---|
| `@SpringBootApplication` | Class | Combines `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`. Marks the main class. |
| `@Configuration` | Class | Declares the class as a source of bean definitions (`@Bean` methods). |
| `@Component` | Class | Generic Spring-managed bean — auto-detected via component scanning. |
| `@Bean` | Method | Declares a single bean to be managed by the Spring IoC container. |

### 5.2 REST Controllers

| Annotation | Target | What It Does |
|---|---|---|
| `@RestController` | Class | Combines `@Controller` + `@ResponseBody`. Every method returns JSON (not a view). |
| `@RequestMapping` | Class/Method | Maps HTTP requests to handler methods. Can set path, method, headers. |
| `@GetMapping` | Method | Shorthand for `@RequestMapping(method = GET)`. |
| `@PostMapping` | Method | Shorthand for `@RequestMapping(method = POST)`. |
| `@RequestParam` | Parameter | Binds a query parameter to a method parameter. |
| `@PathVariable` | Parameter | Binds a URI template variable to a method parameter. |
| `@RequestBody` | Parameter | Binds the HTTP request body to a method parameter (JSON deserialization). |
| `@Valid` | Parameter | Triggers validation on a `@RequestBody` using Bean Validation (JSR-380). |

### 5.3 Exception Handling

| Annotation | Target | What It Does |
|---|---|---|
| `@RestControllerAdvice` | Class | Global exception handler for `@RestController` classes. Combines `@ControllerAdvice` + `@ResponseBody`. |
| `@ExceptionHandler` | Method | Declares a method handles a specific exception type thrown by any controller. |
| `@ResponseStatus` | Method/Exception | Sets the HTTP response status code for an exception or handler method. |

### 5.4 Security

| Annotation | Target | What It Does |
|---|---|---|
| `@EnableWebSecurity` | Class | Enables Spring Security web security support (often implicit with Spring Boot auto-config). |
| `@EnableMethodSecurity` | Class | Enables `@PreAuthorize`, `@PostAuthorize` annotations on methods. |
| `@PreAuthorize` | Method | Method-level security — checks a SpEL expression before execution. |

### 5.5 JPA / Database

| Annotation | Target | What It Does |
|---|---|---|
| `@Entity` | Class | Marks a class as a JPA entity (maps to a database table). |
| `@Table(name = "...")` | Class | Specifies the database table name for an entity. |
| `@Id` | Field | Marks the primary key field. |
| `@GeneratedValue` | Field | Specifies primary key generation strategy. |
| `@Column` | Field | Configures column name, nullable, unique, length. |
| `@Version` | Field | Optimistic locking — increments on every update, prevents lost updates. |
| `@Enumerated` | Field | Maps a Java enum to a database column. |
| `@Transactional` | Method/Class | Declares a transaction boundary. All DB operations within succeed or roll back together. |
| `@Repository` | Class | Marks a DAO class — Spring Data JPA auto-implementation. Translates persistence exceptions. |

### 5.6 Filters

| Annotation | Target | What It Does |
|---|---|---|
| `@Component` | Class | Registers this filter as a Spring bean so it's auto-discovered. |
| `@Order` | Class | Defines the execution order of filters (lower number = higher priority). |
| `@WebFilter` | Class | Servlet-native filter declaration (alternative to `@Component`). |

### 5.7 Configuration Properties

| Annotation | Target | What It Does |
|---|---|---|
| `@Value("${property.name}")` | Field | Injects a value from `application.yml` / environment variables. |
| `@ConfigurationProperties(prefix = "...")` | Class | Binds a group of properties to a Java object (type-safe configuration). |
| `@PropertySource` | Class | Specifies a custom properties file to load. |

### 5.8 Testing

| Annotation | Target | What It Does |
|---|---|---|
| `@SpringBootTest` | Class | Boots the full Spring context for integration testing. |
| `@WebMvcTest` | Class | Loads only web layer (controllers, filters, etc.) — faster than full boot. |
| `@DataJpaTest` | Class | Loads only JPA layer (entities, repositories). |
| `@MockBean` | Field | Creates a Mockito mock and adds it to the Spring context. |
| `@Test` | Method | Marks a JUnit 5 test method. |

### 5.9 Other Useful Annotations

| Annotation | Target | What It Does |
|---|---|---|
| `@Async` | Method | Executes the method in a separate thread (requires `@EnableAsync`). |
| `@Scheduled` | Method | Executes the method on a cron/fixed-delay schedule (requires `@EnableScheduling`). |
| `@Cacheable` | Method | Caches the method's return value. |
| `@CacheEvict` | Method | Removes entries from the cache. |
| `@Slf4j` | Class | Lombok — generates an SLF4J `log` field. |
| `@Data` | Class | Lombok — generates getters, setters, toString, equals, hashCode. |
| `@Builder` | Class | Lombok — implements the Builder pattern for the class. |
| `@Record` | Keyword (Java 16+) | Immutable data carrier with constructor, getters, equals, hashCode, toString built-in. |

---

## 6. Component Deep Dive

### 6.1 Main Application Class

**File**: `NovawalletApiApplication.java`

```java
package com.novawallet.novawallet_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NovawalletApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(NovawalletApiApplication.class, args);
    }
}
```

#### `@SpringBootApplication` — The Most Important Annotation

This single annotation combines three annotations:

| Component | Purpose |
|---|---|
| `@Configuration` | Marks this class as a source of bean definitions |
| `@EnableAutoConfiguration` | Tells Spring Boot to auto-configure based on dependencies on the classpath |
| `@ComponentScan` | Scans the current package and sub-packages for `@Component`, `@Service`, `@Repository`, `@Controller` |

**What happens when you call `SpringApplication.run()`**:

```
1. Load application.yml
2. Determine active profile(s)
3. Auto-configure beans (Tomcat, DataSource, JPA, Security, etc.)
4. Scan for components
5. Register all beans in the IoC container
6. Start embedded Tomcat server
7. Application is ready to accept HTTP requests
```

---

### 6.2 Global Exception Handler

**Files**: `GlobalExceptionHandler.java`, `ErrorResponse.java`, `ResourceNotFoundException.java`

#### `@RestControllerAdvice`

This annotation allows a single class to handle exceptions thrown by ANY controller in the application.

**Without `@RestControllerAdvice`**: Every controller would need try-catch blocks — duplicated, messy code.

**With `@RestControllerAdvice`**: One class catches everything. Clean, consistent error responses.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),     // 404
                "RESOURCE_NOT_FOUND",
                exception.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception exception,
            HttpServletRequest request
    ) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),  // 500
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",             // Generic message (security)
                LocalDateTime.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

#### Why two handlers?

| Handler | Catches | Status | Message |
|---|---|---|---|
| `handleResourceNotFound` | `ResourceNotFoundException` | 404 | Specific (developer-friendly) |
| `handleGeneralException` | All other exceptions | 500 | Generic (security — don't leak internals) |

#### Java `record` — ErrorResponse

```java
public record ErrorResponse(
        int status,
        String code,
        String message,
        LocalDateTime timestamp,
        String path
) {}
```

**Why `record`?** Java 16+ feature. One line gives you:
- Immutable object
- Constructor with all fields
- Getters (`.status()`, `.code()`, etc.)
- `equals()`, `hashCode()`, `toString()`

Equivalent code without `record`:

```java
// ~50 lines of boilerplate
public class ErrorResponse {
    private int status;
    private String code;
    // ... getters, setters, constructor, equals, hashCode, toString
}
```

#### Output format

Every error response follows the same structure:

```json
{
    "status": 404,
    "code": "RESOURCE_NOT_FOUND",
    "message": "Testing error handler",
    "timestamp": "2026-07-07T21:30:00",
    "path": "/api/test/error"
}
```

**Why consistent format?** Frontend/mobile apps can write ONE error handler instead of guessing what each endpoint returns.

---

### 6.3 Standard API Response

**File**: `ApiResponse.java`

```java
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, LocalDateTime.now());
    }
}
```

#### Generic Type `<T>`

`ApiResponse<T>` is a **generic** type — the `data` field can be any type:

```java
ApiResponse<String>        // data is String
ApiResponse<User>          // data is User object
ApiResponse<List<Wallet>>  // data is a list of wallets
ApiResponse<Map<String, Object>> // data is a map
```

#### Why a standard envelope?

| Benefit | Explanation |
|---|---|
| **Consistency** | Every response has the same shape — frontend writes one parser |
| **Predictability** | `success` boolean tells the client if the request worked |
| **Generics** | Strong typing without losing flexibility |
| **Static factories** | `ApiResponse.success(data, msg)` is cleaner than `new ApiResponse<>(true, ...)` |

Output:

```json
{
    "success": true,
    "data": "NovaWallet API is running",
    "message": "Request successful",
    "timestamp": "2026-07-07T21:30:00"
}
```

---

### 6.4 Security Configuration

**File**: `SecurityConfig.java`

```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .contentTypeOptions(contentType -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().permitAll()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
```

#### `@Configuration` + `@Bean` — The Factory Pattern

| Concept | Explanation |
|---|---|
| `@Configuration` | Tells Spring "this class has bean definitions" |
| `@Bean` | Tells Spring "call this method, use the return value as a bean" |

Without `@Bean`:

```java
// You would have to create SecurityFilterChain manually everywhere
SecurityFilterChain filterChain = new SecurityConfig().securityFilterChain(http);
```

With `@Bean`:

```java
// Spring creates it once, manages it, injects it where needed
@Autowired
private SecurityFilterChain securityFilterChain;
```

#### Security headers explained

| Header | What It Prevents |
|---|---|
| `X-Frame-Options: SAMEORIGIN` | Clickjacking — your site cannot be embedded in an `<iframe>` on another domain |
| `X-Content-Type-Options: nosniff` | MIME sniffing — browser won't guess content type |
| `Strict-Transport-Security: max-age=31536000; includeSubDomains` | Forces HTTPS for 1 year |
| `X-XSS-Protection: 0` | Disables outdated XSS filter (modern browsers use CSP instead) |

#### Philosophy: Permit all in Phase 0

```java
.authorizeHttpRequests(auth -> auth
    .anyRequest().permitAll()
)
```

**Why?** Phase 0 is about infrastructure. Authentication comes in Phase 1 with JWT. This keeps testing simple during setup.

---

### 6.5 Request Tracing Filter

**File**: `RequestTracingFilter.java`

```java
@Component
public class RequestTracingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestId = request.getHeader("X-Request-ID");

        // If client didn't send one, generate it
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        // Store in MDC (thread-local) for logging
        MDC.put(REQUEST_ID, requestId);

        // Echo back to client
        response.setHeader("X-Request-ID", requestId);

        try {
            filterChain.doFilter(request, response);  // Continue to controller
        } finally {
            MDC.remove(REQUEST_ID);  // Clean up — prevent memory leaks
        }
    }
}
```

#### Why `OncePerRequestFilter` instead of `Filter`?

| `javax.servlet.Filter` | `OncePerRequestFilter` |
|---|---|
| May execute multiple times per request | Guarantees execution ONCE per request |
| Manual URL pattern matching | Built-in `shouldNotFilter()` method |
| No MDC cleanup guarantee | Clean `try/finally` structure |

#### What is MDC (Mapped Diagnostic Context)?

MDC is thread-local storage for logging. Think of it like a backpack:

```
Thread A                                 Thread B
├── MDC ["requestId" = "abc"]           ├── MDC ["requestId" = "xyz"]
├── "User logged in" → "abc"            ├── "Payment processed" → "xyz"
└── "Database query" → "abc"            └── "Email sent" → "xyz"
```

**Without MDC**: Logs are a wall of text — you can't tell which log lines belong to which request.

**With MDC**: Every log line includes the request ID:

```
2026-07-07 21:30:12 [abc] INFO  User profile requested
2026-07-07 21:30:13 [abc] INFO  Database query completed
2026-07-07 21:30:14 [xyz] INFO  Payment initiated
```

#### Logging pattern explained

```
%date  [%X{requestId}]  %level  %logger  -  %message%n
  │         │              │        │           │
  │         │              │        │           └── The actual log message
  │         │              │        │
  │         │              │        └── Class name (abbreviated)
  │         │              │
  │         │              └── ERROR/WARN/INFO/DEBUG
  │         │
  │         └── MDC value for key "requestId"
  │
  └── Timestamp
```

---

### 6.6 Test Controller

**File**: `TestController.java`

```java
@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/error")
    public String testError() {
        throw new ResourceNotFoundException("Testing error handler");
    }

    @GetMapping("/success")
    public ApiResponse<String> testSuccess() {
        return ApiResponse.success(
                "NovaWallet API is running",
                "Request successful"
        );
    }
}
```

#### Why `@RestController` instead of `@Controller`?

| `@Controller` | `@RestController` |
|---|---|
| Returns view names (JSP/Thymeleaf) | Returns data (JSON/XML) |
| Needs `@ResponseBody` on every method | Assumes `@ResponseBody` on all methods |
| Used for server-side rendering | Used for REST APIs |

#### Endpoints available in Phase 0

| Method | URL | Response |
|---|---|---|
| `GET` | `/api/test/success` | `{"success":true, "data":"NovaWallet API is running", ...}` |
| `GET` | `/api/test/error` | `{"status":404, "code":"RESOURCE_NOT_FOUND", ...}` |
| `GET` | `/api/actuator/health` | `{"status":"UP"}` |
| `GET` | `/api/actuator/info` | `{"app":{"name":"NovaWallet API", ...}}` |

---

## 7. Testing the API

### 7.1 Health Check

```bash
curl http://localhost:8080/api/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

### 7.2 App Info

```bash
curl http://localhost:8080/api/actuator/info
```

Expected response:
```json
{
  "app": {
    "name": "NovaWallet API",
    "description": "Digital wallet backend system",
    "version": "0.0.1"
  }
}
```

### 7.3 Success Endpoint

```bash
curl http://localhost:8080/api/test/success
```

Expected response:
```json
{
  "success": true,
  "data": "NovaWallet API is running",
  "message": "Request successful",
  "timestamp": "2026-07-07T21:30:00"
}
```

### 7.4 Error Endpoint

```bash
curl http://localhost:8080/api/test/error
```

Expected response:
```json
{
  "status": 404,
  "code": "RESOURCE_NOT_FOUND",
  "message": "Testing error handler",
  "timestamp": "2026-07-07T21:30:00",
  "path": "/api/test/error"
}
```

### 7.5 Request Tracing — Auto-generated ID

```bash
curl -i http://localhost:8080/api/test/success | grep X-Request-ID
```

Response header:
```
X-Request-ID: 4ba75aef-816b-4d37-b909-94b584d9ef6a
```

### 7.6 Request Tracing — Custom ID

```bash
curl -i -H "X-Request-ID: nova-wallet-test-001" http://localhost:8080/api/test/success | grep X-Request-ID
```

Response header:
```
X-Request-ID: nova-wallet-test-001
```

---

## 8. Key Concepts Learned

### 8.1 IoC (Inversion of Control) Container

Spring manages objects (beans) for you. Instead of:

```java
// Without Spring — you manage everything
UserService service = new UserService(new UserRepository(new DataSource()));
```

Spring does:

```java
// With Spring — declare what you need
@Service
public class UserService {
    public UserService(UserRepository repository) { ... }
}

// Spring automatically creates UserRepository and injects it
```

### 8.2 Dependency Injection

Spring injects dependencies in three ways:

| Method | Annotation | When to Use |
|---|---|---|
| Constructor injection | Pass in constructor params | ✅ **Best practice** — immutable, testable |
| Setter injection | `@Autowired` on setter | When dependency is optional |
| Field injection | `@Autowired` on field | ❌ Avoid — hard to test, hides dependencies |

### 8.3 Auto-Configuration

Spring Boot's `@EnableAutoConfiguration` looks at the dependencies on your classpath and configures them automatically:

| Dependency found | What Spring Boot configures |
|---|---|
| `tomcat-embed-core` | Starts embedded Tomcat on port 8080 |
| `spring-boot-starter-data-jpa` | Creates `DataSource`, `EntityManagerFactory`, `TransactionManager` |
| `spring-boot-starter-security` | Sets up security filter chain, generates default password |
| `flyway-core` | Runs database migrations on startup |
| `springdoc-openapi` | Exposes `/swagger-ui.html` and `/api-docs` |

### 8.4 The Filter Chain

Every HTTP request passes through a chain of filters before reaching your controller:

```
Request → Filter1 → Filter2 → Filter3 → ... → DispatcherServlet → Controller
                ↓
          Security headers, logging, authentication, rate limiting, etc.
```

Filters are powerful because they run **before** your controller code, so cross-cutting concerns (security, logging, tracing) don't clutter your business logic.

### 8.5 Spring Bean Lifecycle

```
1. @ComponentScan finds classes with @Component, @Service, etc.
2. Spring instantiates the class
3. Spring injects dependencies (constructor/setter/field)
4. @PostConstruct method runs (if any)
5. Bean is ready for use
6. On shutdown: @PreDestroy method runs (if any)
```

### 8.6 Why Interfaces Matter

Spring uses interfaces for JDK dynamic proxies. Key Spring features REQUIRE interfaces:

| Feature | Why Interface Needed |
|---|---|
| `@Transactional` | Spring creates a proxy that wraps your method in a transaction |
| `@Cacheable` | Proxy checks cache before calling your method |
| `@Async` | Proxy runs your method in a separate thread |
| `@PreAuthorize` | Proxy checks permissions before calling your method |

**Pattern**: Always code to interfaces:

```java
// ✅ Good — interface
public interface UserService { ... }
public class UserServiceImpl implements UserService { ... }

// ❌ Not recommended — no interface
public class UserService { ... }
```

---

## Summary: Phase 0 Checklist

| # | Task | Files | Status |
|---|---|---|---|
| 0.1 | Dependencies | `pom.xml` | ✅ |
| 0.2 | Multi-profile config | `application.yml`, `-dev`, `-prod`, `-test` | ✅ |
| 0.3 | Package structure | All packages created | ✅ |
| 0.4 | Global exception handler | `GlobalExceptionHandler.java`, `ErrorResponse.java`, `ResourceNotFoundException.java` | ✅ |
| 0.5 | API response wrapper | `ApiResponse.java` | ✅ |
| 0.6 | Flyway migration | `db/migration/*.sql` | ✅ |
| 0.7 | Security headers | `SecurityConfig.java` | ✅ |
| 0.8 | Health check | Actuator + `application.yml` | ✅ |
| 0.9 | Swagger/OpenAPI | `springdoc` dependency + config | ✅ |
| 0.10 | MDC tracing | `RequestTracingFilter.java` + log pattern | ✅ |

---

> **Next Phase**: Phase 1 — User & Authentication Core
> Topics: JWT, Spring Security with `UserDetailsService`, `@Entity`, `@Repository`,
> `@Service`, Flyway migrations for users/wallets, BCrypt password hashing
