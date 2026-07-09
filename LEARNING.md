# Learn Spring Boot — From This Project

**Author**: Senior Java Engineer  
**Project**: NovaWallet API

This guide teaches Spring Boot from the ground up using the NovaWallet API codebase. Every concept links to a real file in this project — you learn by reading real production code, not toy examples.

---

## Chapter 1: What Is Spring Boot?

### The Problem Spring Boot Solves

Imagine building a Java application from scratch. You'd need to:

- Set up a web server (Tomcat, Jetty...)
- Configure a database connection
- Create a system for receiving HTTP requests
- Build a way to convert JSON to Java objects and back
- Handle security, logging, error handling
- Manage thousands of objects and their dependencies

That's months of work.

**Spring Boot is a pre-built engine** that does all of this for you. You just write the business logic — the engine handles the plumbing.

### What Happens When You Run This Project

Execute `./mvnw spring-boot:run` and Spring Boot does this, IN ORDER:

```
1. Reads application.yml          ← learns database URL, port, JWT secret, etc.
2. Starts embedded Tomcat server  ← listens on port 8080
3. Scans all @Configuration       ← finds SecurityConfig, PasswordConfig, etc.
4. Creates all @Service beans     ← creates ONE instance of AuthService, etc.
5. Creates all @Repository beans  ← creates proxy for UserRepository
6. Sets up JPA/Hibernate          ← connects to PostgreSQL
7. Runs Flyway migrations         ← executes V1__.sql, V2__.sql, etc.
8. Starts the Security filter chain ← installs JwtAuthFilter
9. Registers all @RestController  ← maps /v1/auth/register to AuthController.register()
10. Ready to accept requests      ← Tomcat logs "Started NovawalletApiApplication"
```

**Key insight**: You never wrote code for steps 1-9. Spring Boot does it all automatically based on annotations and configuration files. You only wrote files in the `com.novawallet.novawallet_api/` package.

---

## Chapter 2: The Big Picture — Request/Response Flow

Here's what happens when a mobile app sends a request to `POST /v1/auth/register`:

```
MOBILE APP
    |
    | HTTP POST /v1/auth/register  { "email": "...", "password": "..." }
    v
1.  TOMCAT (embedded web server)
    - Receives raw TCP connection
    - Parses HTTP request (method, path, headers, body)
    |
    v
2.  SPRING SECURITY FILTER CHAIN  [SecurityConfig.java]
    - Each filter checks something:
      a. CorsFilter           → allows/restricts cross-origin requests
      b. JwtAuthFilter        → extracts Bearer token (for protected endpoints)
      c. ExceptionFilter      → catches security errors
    - This endpoint is PUBLIC (no JWT needed), so filter chain passes through
    |
    v
3.  DISPATCHER SERVLET  (Spring MVC core)
    - Looks at the path: "/v1/auth/register"
    - Asks: "Who handles this?"
    - Finds: AuthController.register()  [because @PostMapping("/register")]
    - Note: The base @RequestMapping("/v1/auth") on the class + "/register" = "/v1/auth/register"
    |
    v
4.  AUTH CONTROLLER  [AuthController.java]
    - Receives the request
    - Sees @Valid @RequestBody RegisterRequest request
    - Spring automatically:
      a. Reads the JSON body from the HTTP request
      b. Converts it to a RegisterRequest Java object (JSON → Java)
      c. Runs validation (checking @NotBlank, @Email, @Size)
      d. If validation fails → returns 400 Bad Request
    - If valid → calls authService.register(request)
    |
    v
5.  AUTH SERVICE  [AuthService.java]
    - Contains the business logic rules:
      a. "Is this email already taken?" → check database
      b. "Is this phone already taken?" → check database
      c. Hash the password with BCrypt
      d. Create User object
      e. Save User to database
      f. Generate wallet for user
      g. Create refresh token
      h. Generate JWT access token
      i. Return AuthResponse
    - @Transactional means: if anything fails, ALL database changes roll back
    |
    v
6.  USER / WALLET REPOSITORY  [UserRepository.java / WalletRepository.java]
    - Spring Data JPA translates method names into SQL:
      - findByEmail(email) → SELECT * FROM users WHERE email = ?
      - save(user) → INSERT INTO users (...)
    - You NEVER write SQL — Spring generates it from method names
    |
    v
7.  POSTGRESQL DATABASE
    - Executes the SQL
    - Returns results (or insert confirmation)
    |
    (Now data flows back up)
    |
    v
6.  REPOSITORY returns saved User object
5.  AUTH SERVICE builds AuthResponse, returns to Controller
4.  AUTH CONTROLLER wraps in ApiResponse.success(), returns to DispatcherServlet
3.  DISPATCHER SERVLET converts response to JSON (Java → JSON)
2.  SPRING SECURITY adds response headers
1.  TOMCAT sends HTTP response back to mobile app
    |
    v
MOBILE APP receives: {"success":true,"data":{"accessToken":"...","user":{...}}}
```

### The Response Path in Detail

```java
// In AuthController.java — Step 4
@PostMapping("/register")                              // "I handle POST /v1/auth/register"
public ResponseEntity<ApiResponse<AuthResponse>> register(
        @Valid @RequestBody RegisterRequest request    // Spring converts JSON → Java object
) {
    AuthResponse response = authService.register(request);  // Step 5: delegate to service
    return ResponseEntity                               // Step 4 (return path)
            .status(HttpStatus.CREATED)                 // HTTP status 201
            .body(ApiResponse.success(response, "Registration successful"));  // wrap in standard format
}
// Spring takes the ResponseEntity, converts it to JSON, sends it as HTTP response
```

---

## Chapter 3: The Four Layers (The Heart of Spring Boot)

Every Spring Boot app has exactly 4 layers. Understanding this is understanding Spring Boot.

```
┌─────────────────────────────────────────────────────┐
│                  1. CONTROLLER LAYER                 │
│              (*Controller.java)                       │
│  Job: Talk to the outside world (HTTP, JSON)         │
│  Rules: No business logic. No database calls.        │
│         Just receive, delegate, respond.             │
├─────────────────────────────────────────────────────┤
│                  2. SERVICE LAYER                     │
│              (*Service.java)                          │
│  Job: All business rules live here                   │
│  Rules: Pure Java logic. Orchestrates repositories.  │
│         This is the "brain" of the app.              │
├─────────────────────────────────────────────────────┤
│                  3. REPOSITORY LAYER                  │
│              (*Repository.java)                       │
│  Job: Talk to the database                           │
│  Rules: One method per query. No business logic.     │
│         Spring Data JPA generates the SQL for you.   │
├─────────────────────────────────────────────────────┤
│                  4. ENTITY LAYER                      │
│              (*Entity.java)                           │
│  Job: Represent a database table as a Java class     │
│  Rules: One entity = one table. One field = one column.│
└─────────────────────────────────────────────────────┘
```

### The Golden Rule

**Data flows DOWN through the layers, and results flow UP.**

```
Controller  →  Service  →  Repository  →  Database
   ↑             ↑             ↑
Response      Business      Raw data from DB
formatted     result        (Entity objects)
as JSON
```

### Why Separate Layers?

Without layers, you'd write everything in one file:

```java
// BAD: Everything mixed together
@RestController
public class MessyController {
    @PostMapping("/register")
    public void register(...) {
        // 1. Parse JSON (controller's job)
        // 2. Check if email exists (service's job)
        // 3. Write SQL query (repository's job)
        // 4. Build response (controller's job)
        // ALL IN ONE METHOD — impossible to test, impossible to change
    }
}
```

With layers, each piece is independent:

```java
// GOOD: Each layer has ONE responsibility
// Want to change from PostgreSQL to MongoDB? → Only change Repository layer
// Want to add email verification? → Only change Service layer
// Want to change JSON format? → Only change Controller layer
// Want to add CAPTCHA? → Add it in Service without touching Controller or Repository
```

---

## Chapter 4: Deep Dive — Each Layer Explained

### 4.1 Controllers — The Receptionist

Controllers are like a hotel receptionist. They:

- **Receive** requests (guests arriving)
- **Validate** the request format (do you have a reservation?)
- **Pass** to the right service (let me call housekeeping)
- **Respond** with the result (here's your room key)

**File**: `AuthController.java`

```java
@RestController                              // Tells Spring: "I'm a controller"
@RequestMapping("/v1/auth")                  // All endpoints start with /v1/auth
public class AuthController {

    // Dependency Injection (see Chapter 5)
    private final AuthService authService;    // Controller holds a reference to Service

    // Constructor — Spring calls this to give AuthController its dependencies
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")                // Handles POST /v1/auth/register
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request  // Takes JSON body, validates it
    ) {
        // 1. DELEGATE to service (no logic here!)
        AuthResponse response = authService.register(request);

        // 2. RETURN response wrapped in standard format
        return ResponseEntity
                .status(HttpStatus.CREATED)               // HTTP 201
                .body(ApiResponse.success(response, "Registration successful"));
    }
}
```

**What `@Valid @RequestBody` does**:

When a mobile app sends:
```json
{"firstName": "", "email": "bad-email", "password": "ab"}
```

Spring automatically:
1. Reads the JSON → creates a `RegisterRequest` object
2. Checks the validation annotations on `RegisterRequest.java`:

```java
public record RegisterRequest(
    @NotBlank(message = "First name is required")     // ← fails: empty string
    @Size(min = 1, max = 100) String firstName,

    @Email(message = "Email must be valid")            // ← fails: "bad-email"
    String email,

    @Size(min = 8, max = 128) String password          // ← fails: only 2 chars
) {}
```

3. Detects 3 validation failures
4. Automatically returns `400 Bad Request` with details — AuthController.register() NEVER RUNS

This is called **declarative validation** — you declare the rules, Spring enforces them.

### 4.2 Services — The Brain

Services contain ALL business logic. If you're debating where to put code, put it in a Service.

**File**: `AuthService.java`

```java
@Service                                          // Tells Spring: "I'm a Service"
@Transactional                                    // All methods are database transactions
public class AuthService {

    // Services hold references to Repositories and other Services
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AccountNumberGenerator accountNumberGenerator;
    private final TokenService tokenService;

    // Constructor injection — Spring provides all dependencies
    public AuthService(
            UserRepository userRepository,
            WalletRepository walletRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            AccountNumberGenerator accountNumberGenerator,
            TokenService tokenService
    ) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        // ... store all references
    }

    public AuthResponse register(RegisterRequest request) {
        // STEP 1: VALIDATE BUSINESS RULES
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered");
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new DuplicateResourceException("Phone already registered");
        }

        // STEP 2: CREATE USER ENTITY
        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email().toLowerCase().trim())
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.password()))  // Hash password!
                .verificationToken(tokenService.generateSecureToken())    // For email verification
                .build();

        user = userRepository.save(user);        // Save to database

        // STEP 3: CREATE WALLET
        Wallet wallet = Wallet.builder()
                .userId(user.getId())
                .accountNumber(accountNumberGenerator.generate())
                .build();
        walletRepository.save(wallet);

        // STEP 4: GENERATE TOKENS
        String refreshToken = tokenService.createRefreshToken(user);

        // STEP 5: BUILD RESPONSE
        return buildAuthResponse(user, refreshToken);
    }
}
```

**What `@Transactional` means**:

If the wallet creation fails (step 3), the user creation (step 2) is automatically **undone**. The database stays consistent. Without `@Transactional`, you'd have a user with no wallet — a bug.

Think of it like a bank transfer:
```
transferMoney() {
    withdraw(fromAccount, 100);    // Step 1
    deposit(toAccount, 100);       // Step 2 — if this fails, step 1 must undo
}
```

`@Transactional` ensures: ALL steps succeed, OR the database looks like nothing happened.

### 4.3 Repositories — The Database Translator

Repositories translate between Java objects and database tables. You define an interface, Spring writes the implementation.

**File**: `UserRepository.java`

```java
@Repository                                        // Tells Spring: "I'm a Repository"
public interface UserRepository extends JpaRepository<User, UUID> {
    // Spring Data JPA reads method names and generates SQL:

    Optional<User> findByEmail(String email);
    // → SELECT * FROM users WHERE email = ?

    Optional<User> findByPhone(String phone);
    // → SELECT * FROM users WHERE phone = ?

    boolean existsByEmail(String email);
    // → SELECT COUNT(*) FROM users WHERE email = ?  (returns true if > 0)

    @Query(value = "SELECT * FROM users WHERE id = :id", nativeQuery = true)
    Optional<User> findByIdIncludingDeleted(@Param("id") UUID id);
    // → Runs raw SQL. Needed because @SQLRestriction hides deleted users
}
```

**How method name parsing works**:

| Method Name | Generated SQL |
|---|---|
| `findByEmail(String email)` | `WHERE email = ?` |
| `findByEmailAndPhone(String email, String phone)` | `WHERE email = ? AND phone = ?` |
| `findByEmailOrPhone(String email, String phone)` | `WHERE email = ? OR phone = ?` |
| `findByCreatedAtAfter(LocalDateTime date)` | `WHERE created_at > ?` |
| `findByRoleOrderByEmailAsc(Role role)` | `WHERE role = ? ORDER BY email ASC` |
| `countByRole(Role role)` | `SELECT COUNT(*) ... WHERE role = ?` |
| `deleteByEmail(String email)` | `DELETE ... WHERE email = ?` |

**Methods you get for FREE** (from `JpaRepository<User, UUID>`):

| Method | What it does |
|---|---|
| `findAll()` | `SELECT * FROM users` |
| `findById(id)` | `SELECT * FROM users WHERE id = ?` |
| `save(user)` | If id=null → INSERT, if id exists → UPDATE |
| `deleteById(id)` | `DELETE FROM users WHERE id = ?` |
| `count()` | `SELECT COUNT(*) FROM users` |
| `existsById(id)` | `SELECT COUNT(*) FROM users WHERE id = ?` |

**Key insight**: You wrote an INTERFACE (just method signatures), not an IMPLEMENTATION. Spring Data JPA generates the implementation class at runtime. This is why Spring Boot can build apps with so little code.

### 4.4 Entities — The Table Map

An Entity is a Java class where each field represents a database column.

**File**: `User.java`

```java
@Entity                                              // Tells Spring: "This is a database table"
@Table(name = "users")                               // The table name in PostgreSQL
@EntityListeners(AuditingEntityListener.class)        // Enables auto-timestamps
@SQLRestriction("deleted = false")                   // Every query adds WHERE deleted = false
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)  // Auto-generate UUID
    private UUID id;                                 // → Column: id UUID PRIMARY KEY

    @Column(nullable = false, unique = true, length = 255)
    private String email;                            // → Column: email VARCHAR(255) NOT NULL UNIQUE

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;                     // → Column: password_hash VARCHAR(255) NOT NULL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;                   // → Column: role VARCHAR(20) NOT NULL DEFAULT 'USER'

    @Column(name = "email_verified")
    @Builder.Default
    private boolean emailVerified = false;           // → Column: email_verified BOOLEAN DEFAULT FALSE

    @Version
    private Integer version;                         // → Column: version INTEGER — for locking

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;                 // → Column: created_at TIMESTAMP

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;                 // → Column: updated_at TIMESTAMP
}
```

**Compare this to the actual SQL** (V2__create_users_table.sql):

```sql
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    email_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**See the pattern?** Every `@Column` in Java = one line in SQL. Spring Boot keeps them in sync (when `ddl-auto: validate` — it checks they match).

### The @SQLRestriction Trick

```java
@SQLRestriction("deleted = false")
```

This adds `AND deleted = false` to EVERY SQL query Spring Data JPA generates. When a user is soft-deleted (deleted = true), they simply disappear from all queries. The app behaves as if the user was deleted, but the data is still in the database.

**Problem**: The admin can't find deleted users to restore them.  
**Solution**: A native query that bypasses the restriction:

```java
@Query(value = "SELECT * FROM users WHERE id = :id", nativeQuery = true)
Optional<User> findByIdIncludingDeleted(@Param("id") UUID id);
```

Native queries don't go through Hibernate's filter system — they run the exact SQL you write.

---

## Chapter 5: Dependency Injection (The Superpower)

### The Problem

Service A needs Service B to work. Service B needs Repository C. Repository C needs Database D.

```java
// Without Spring — you manage everything
public class AuthService {
    private UserRepository repo;
    
    public AuthService() {
        // You create everything manually
        this.repo = new UserRepository(Database.getConnection());
        // What if you change the database? Edit every class?
    }
}
```

This is called **tight coupling** — every class knows how to build its dependencies. Change one thing, break everything.

### The Solution

```java
// With Spring — you just declare what you need
public class AuthService {
    private final UserRepository userRepository;

    // "I don't care how this is created. Just give it to me."
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

Spring looks at this constructor and says: *"AuthService needs a UserRepository. Let me check if I have one..."*

1. Spring looks at `UserRepository`
2. Sees `@Repository` → "I have one!"
3. Creates a single instance (a "bean")
4. Passes it to `AuthService`'s constructor
5. Also creates `PasswordEncoder`, `JwtUtil`, `TokenService` — all the things `AuthService` needs
6. Passes all of them to the constructor

This is called **Inversion of Control** — instead of your code reaching out to get dependencies, Spring injects them into your code.

### How Spring Knows What to Create

Spring scans for these annotations at startup:

| Annotation | Creates | Scope |
|---|---|---|
| `@Component` | Generic bean | 1 instance for the whole app |
| `@Service` | Service bean | 1 instance for the whole app |
| `@Repository` | Repository bean | 1 instance for the whole app |
| `@Bean` (in `@Configuration`) | Custom bean | 1 instance for the whole app |

**Important**: All beans are SINGLETONS by default — only ONE instance exists. When you inject `UserRepository` into 10 different services, they all get the SAME instance. This is safe because services don't store user data as fields (they're stateless).

### The Constructor Injection Pattern

```java
// CORRECT — field is final, dependencies are clear, testable
public class AuthService {
    private final UserRepository userRepository;  // final = cannot change

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

**Why this pattern is used everywhere in this project:**

1. **Final fields** — once set, never change. Thread-safe.
2. **Explicit dependencies** — you can see everything a class needs by looking at its constructor
3. **Testable** — in tests, you can pass mock objects:

```java
// In AuthServiceTest.java
@Mock
private UserRepository userRepository;  // Mockito creates a fake repository

@BeforeEach
void setUp() {
    // Pass the mock to AuthService — no database needed!
    authService = new AuthService(
            userRepository,        // ← mock
            walletRepository,      // ← mock
            passwordEncoder,       // ← real (BCrypt)
            jwtUtil,               // ← mock
            accountNumberGenerator,// ← real
            tokenService           // ← spy (partial mock)
    );
}
```

---

## Chapter 6: How Spring Boot Starts Up

### Step-by-step from `./mvnw spring-boot:run`

```
1. Maven compiles all .java files to .class
2. Maven packages the JAR with embedded Tomcat
3. Java starts the JAR
4. Spring Boot finds application.yml   ← Where port, database, etc. are configured
5. Tomcat starts on port 8080           ← Server is now listening
6. Spring scans the package             ← Finds EVERY annotation in com.novawallet...
   a. Finds @Configuration classes      ← SecurityConfig, PasswordConfig, JpaAuditingConfig
   b. Runs their @Bean methods          ← Creates PasswordEncoder, SecurityFilterChain
   c. Finds @RestController classes     ← AuthController, UserController, AdminController, WalletController
   d. Finds @Service classes            ← AuthService, UserService, WalletService, AdminService, TokenService
   e. Finds @Repository interfaces      ← UserRepository, WalletRepository, RefreshTokenRepository
   f. Finds @Entity classes             ← User, Wallet, RefreshToken
7. Spring resolves dependencies:
   a. Sees AuthController needs AuthService     → creates AuthService first
   b. AuthService needs UserRepository, etc.    → creates them first
   c. Builds the dependency tree from bottom up → creates all beans
8. Spring Data JPA processes repositories:
   a. UserRepository extends JpaRepository
   b. Spring generates implementation with all the SQL methods
   c. Connects to PostgreSQL
   d. Validates entities match database (ddl-auto: validate)
9. Flyway runs:
   a. Checks flyway_schema_history table
   b. Runs any NEW migration files
   c. Marks them as complete
10. Spring Security configures:
    a. Sets up the filter chain from SecurityConfig
    b. Creates JwtAuthFilter instance
11. DispatcherServlet registers all endpoints:
    a. /v1/auth/register → AuthController.register()
    b. /v1/auth/login    → AuthController.login()
    c. /v1/users/me      → UserController.getProfile()
    d. ... etc for all 15+ endpoints
12. Application is READY
    Tomcat logs: "Started NovawalletApiApplication in 8.6 seconds"
```

### The Main Method

```java
// NovawalletApiApplication.java
@SpringBootApplication                          // This ONE annotation does ALL of the above
public class NovawalletApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(NovawalletApiApplication.class, args);
        // This single line starts the ENTIRE application
        // 1. Starts Tomcat
        // 2. Scans for beans
        // 3. Sets up JPA
        // 4. Configures security
        // 5. Runs migrations
        // 6. Registers endpoints
        // 7. Waits for requests
    }
}
```

`@SpringBootApplication` is actually 3 annotations in one:

| Annotation | What it does |
|---|---|
| `@SpringBootConfiguration` | Marks this as the main config class |
| `@EnableAutoConfiguration` | Spring guesses what you need (has JPA? → configure JPA. Has web? → configure Tomcat) |
| `@ComponentScan` | Scan this package and ALL sub-packages for @Component, @Service, @Repository, etc. |

This is why EVERY class is under `com.novawallet.novawallet_api/` — `@ComponentScan` starts at the main class's package and scans everything inside it.

---

## Chapter 7: Configuration Management

### The 3 Configuration Files

| File | Profile | Database | When to Use |
|---|---|---|---|
| `application.yml` | Default | No DB config | Common settings for ALL environments |
| `application-dev.yml` | `dev` | PostgreSQL (local) | Development on your machine |
| `application-prod.yml` | `prod` | Production | Live server |

### How Profiles Work

```yaml
# application.yml  ← ALWAYS loaded
spring:
  profiles:
    active: dev     ← Also load application-dev.yml
```

When you set `spring.profiles.active: dev`, Spring loads `application.yml` first, then overlays `application-dev.yml` on top. Settings in `dev` override settings in the base file.

**The dev profile has:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/novawallet
    username: postgres
    password: 8407
```

**The prod profile would have:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
```

Notice: production uses `${}` environment variables — NEVER hardcode production credentials.

### How configuration values get into Java code

1. You define a value in YAML:
```yaml
jwt:
  secret: ZGV2LXNlY3JldC1rZXk...
  expiration: 900000
```

2. You inject it in Java:
```java
public JwtUtil(
        @Value("${jwt.secret}") String secret,           // ← from YAML
        @Value("${jwt.expiration}") long expirationMs     // ← from YAML
) {
    this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    this.expirationMs = expirationMs;
}
```

3. The `${}` syntax also supports defaults:
```yaml
jwt:
  secret: ${JWT_SECRET:ZGV2LXNlY3JldC1rZXk...}
  #         ↑ env variable    ↑ default value
```

This means: use the `JWT_SECRET` environment variable if set, otherwise use the hardcoded default.

---

## Chapter 8: Spring Security Explained

### What SecurityConfig.java Does

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Disable CSRF (we use JWT, not cookies)
            .csrf(csrf -> csrf.disable())
            
            // 2. No sessions (every request is independent)
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 3. Define access rules
            .authorizeHttpRequests(auth -> auth
                // These paths are PUBLIC — anyone can call them
                .requestMatchers(
                    "/v1/auth/register",
                    "/v1/auth/login",
                    "/v1/auth/verify",
                    "/v1/auth/forgot-password",
                    "/v1/auth/reset-password",
                    "/v1/auth/refresh"
                ).permitAll()
                
                // Swagger docs are PUBLIC
                .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                
                // EVERYTHING ELSE requires authentication
                .anyRequest().authenticated()
            )
            
            // 4. Tell Spring to use JWT, not login forms
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            
            // 5. Insert JwtAuthFilter BEFORE Spring's UsernamePassword filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

### How JwtAuthFilter Works

```
REQUEST COMES IN (no session cookie)
    │
    ▼
JwtAuthFilter.doFilterInternal()
    │
    ├── Extract token from "Authorization: Bearer <token>"
    │
    ├── Is token present?  → Yes → Validate JWT signature
    │                             │
    │                             ├── Valid? → Extract userId from JWT
    │                             │            Load user from database (loadUserById)
    │                             │            Create Authentication object
    │                             │            Store in SecurityContext
    │                             │            → "User is now logged in for this request"
    │                             │
    │                             └── Invalid? → Log warning, continue as anonymous
    │
    └── Is token missing? → Continue as anonymous user
                            │
                            ├── Public endpoint? → Request proceeds to controller
                            └── Protected endpoint? → Spring returns 403 Forbidden
```

**The key**: Spring Security doesn't use sessions. On every request, the JWT is extracted, validated, and the user is loaded from the database. This is called **stateless authentication**.

### The `@AuthenticationPrincipal` Trick

In controllers, you can get the authenticated user:

```java
@GetMapping("/me")
public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
        @AuthenticationPrincipal UserDetails userDetails  // ← Spring injects the authenticated user
) {
    UUID userId = UUID.fromString(userDetails.getUsername());  // Username = userId (UUID)
    UserProfileResponse profile = userService.getProfile(userId);
    return ResponseEntity.ok(ApiResponse.success(profile, "Profile retrieved"));
}
```

`@AuthenticationPrincipal` looks at the SecurityContext (which JwtAuthFilter set up) and gets the user details. This only works if the request has a valid JWT — otherwise, the filter chain blocks the request before it reaches this method.

---

## Chapter 9: Error Handling — The GlobalExceptionHandler

The `GlobalExceptionHandler.java` is like a safety net — it catches ALL exceptions thrown by any controller and returns a consistent JSON response.

### How it works

```java
@RestControllerAdvice                                  // "I catch exceptions from ALL controllers"
public class GlobalExceptionHandler {

    // When ANY controller throws ResourceNotFoundException:
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage(), request);
    }

    // When ANY controller throws DuplicateResourceException:
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", exception.getMessage(), request);
    }

    // Catch ANY exception not handled above:
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception", exception);  // ← logs the FULL stack trace
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred", request);
    }
}
```

### The Flow

```
AuthService.register():
    if email exists → throw new DuplicateResourceException("Email already registered")
                            ↓
                    Exception is thrown, exits the method
                            ↓
                    Spring catches it
                            ↓
                    Asks GlobalExceptionHandler: "Can you handle this?"
                            ↓
                    GlobalExceptionHandler says: "Yes, I have @ExceptionHandler(DuplicateResourceException.class)"
                            ↓
                    Returns {"status":409,"code":"DUPLICATE_RESOURCE","message":"Email already registered",...}
                            ↓
                    Spring sends this JSON as HTTP response with status 409
```

### Without GlobalExceptionHandler

Every controller method would need try-catch:

```java
// HORRIBLE — repeated in every method
@PostMapping("/register")
public ResponseEntity<?> register(...) {
    try {
        authService.register(request);
        return ResponseEntity.status(201).body(...);
    } catch (DuplicateResourceException e) {
        return ResponseEntity.status(409).body(...);
    } catch (BadRequestException e) {
        return ResponseEntity.status(400).body(...);
    } catch (Exception e) {
        return ResponseEntity.status(500).body(...);
    }
}
```

With `@RestControllerAdvice`, ALL controllers get automatic error handling. You throw exceptions in Services, and they're handled in one place.

---

## Chapter 10: The DTO Pattern (Data Transfer Objects)

### Why Not Send Entities Directly?

You might think: "Why not just return the User object from the controller?"

```java
// BAD: Returning entities exposes internal database structure
@GetMapping("/me")
public User getProfile() {
    return user;  // Returns password_hash, pin_hash, deleted status, version... to the mobile app!
}
```

**DTOs solve this problem**. A DTO is a class that contains ONLY the data you want to expose.

### Request DTOs (What the CLIENT sends)

```java
// RegisterRequest.java — Client sends this JSON
public record RegisterRequest(
    @NotBlank String firstName,    // Validation happens here
    @NotBlank String lastName,
    @Email String email,
    @NotBlank String phone,
    @Size(min = 8) String password
) {}
```

### Response DTOs (What the SERVER sends back)

```java
// AuthResponse.java — Server returns this JSON
public record AuthResponse(
    String accessToken,           // Only what the client needs
    String refreshToken,
    long expiresIn,
    UserInfo user                 // Nested DTO — no password hash!
) {
    public record UserInfo(
        String id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String role,
        boolean emailVerified,
        boolean pinSet              // Just whether PIN is set, not the hash!
    ) {}
}
```

### The ApiResponse Wrapper

Every response is wrapped in `ApiResponse<T>`:

```java
public record ApiResponse<T>(
    boolean success,     // true/false
    T data,              // The actual response data
    String message,      // Human-readable message
    LocalDateTime timestamp  // When the response was generated
) {
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, LocalDateTime.now());
    }
}
```

This means ALL responses have the same format:

```json
// Success
{"success": true, "data": {...}, "message": "Registration successful", "timestamp": "..."}

// Error
{"status": 409, "code": "DUPLICATE_RESOURCE", "message": "Email already registered", "path": "/v1/auth/register", "timestamp": "..."}
```

The mobile app always knows the response structure regardless of which endpoint it calls.

---

## Chapter 11: How Testing Works

### Unit Tests (AuthServiceTest.java)

Unit tests test ONE class in isolation. All dependencies are mocked (fake).

```java
@ExtendWith(MockitoExtension.class)         // Enable Mockito
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;   // Create a FAKE repository
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private JwtUtil jwtUtil;

    private AuthService authService;         // The REAL service being tested

    @BeforeEach
    void setUp() {
        // Create the real AuthService with fake dependencies
        authService = new AuthService(
                userRepository,          // ← fake!
                walletRepository,        // ← fake!
                new BCryptPasswordEncoder(),
                jwtUtil,                 // ← fake!
                accountNumberGenerator,  // ← real (but has fake walletRepo inside)
                tokenService             // ← spy
        );
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        // 1. ARRANGE — Tell mocks what to do
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // 2. ACT — Call the real method
        AuthResponse response = authService.register(request);

        // 3. ASSERT — Check the result
        assertThat(response.user().email()).isEqualTo("john@example.com");
        verify(userRepository).save(userCaptor.capture());  // Verify save() was called
    }
}
```

**Why mock?** These tests:
- Run in MILLISECONDS (no database, no HTTP)
- Don't need PostgreSQL installed
- Are 100% reliable (no network issues)
- Test ONLY the business logic, not the infrastructure

### Integration Tests (AuthControllerIntegrationTest.java)

Integration tests test the REAL Spring Boot app with a real (test) database:

```java
@SpringBootTest                               // Start the REAL application
@AutoConfigureMockMvc                          // Enable fake HTTP client
@ActiveProfiles("test")                        // Use H2 database (not PostgreSQL)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;                   // Fake HTTP client

    @Test
    void register_shouldReturn201AndTokens() throws Exception {
        // Perform a REAL HTTP request (but in code, not over network)
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"email":"test@test.com","password":"SecurePass123",...}"""))
                .andExpect(status().isCreated())           // Assert HTTP 201
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString());
    }
}
```

**The difference:**

| Aspect | Unit Test | Integration Test |
|---|---|---|
| What's tested | One class | The whole app |
| Database | Not used (mocked) | H2 in-memory |
| Speed | Milliseconds | Seconds |
| Catches | Logic bugs | Configuration bugs |
| Reliability | 100% | Depends on test DB state |

### The test profile (application-test.yml)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL  # H2 database that speaks "PostgreSQL"
```

H2 is an in-memory database that emulates PostgreSQL. Every test run starts with a FRESH database. No leftover data from previous test runs.

---

## Chapter 12: Walk Through An Entire Feature

Let's trace the PIN verification feature end-to-end to see how everything connects.

### 1. The Request

```
POST /api/v1/auth/pin
Authorization: Bearer eyJhbGciOiJIUzM4NC...
Content-Type: application/json

{"pin": "1234"}
```

### 2. Tomcat receives it

```
Raw TCP data → HTTP request → Spring's DispatcherServlet
```

### 3. Security check (SecurityConfig + JwtAuthFilter)

```
DispatcherServlet asks: "Is this request authenticated?"
JwtAuthFilter says: "Let me check the Authorization header..."
    → Extracts "eyJhbGciOiJIUzM4NC..." from "Bearer eyJhbGciOiJIUzM4NC..."
    → Decodes JWT, verifies signature
    → Extracts userId from the "sub" (subject) field
    → Loads user from database: CustomUserDetailsService.loadUserById(userId)
    → Creates Authentication object
    → Stores in SecurityContext
    → "Request is authenticated as user 70c3c6bc-4c90-4c18-a36e-4e6b8a43540d"
```

### 4. Controller receives it (AuthController)

```java
@PostMapping("/pin")
public ResponseEntity<ApiResponse<Void>> setPin(
        @Valid @RequestBody SetPinRequest request,     // Validates PIN format (4-6 digits)
        @AuthenticationPrincipal UserDetails userDetails  // Gets authenticated user
) {
    UUID userId = UUID.fromString(userDetails.getUsername());  // "70c3c6bc-..."
    authService.setPin(userId, request.pin());
    return ResponseEntity.ok(ApiResponse.success(null, "PIN set successfully"));
}
```

### 5. Service processes it (AuthService)

```java
public void setPin(UUID userId, String pin) {
    // Find user in database
    User user = userRepository.findById(userId)  // SELECT * FROM users WHERE id = ?
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    // Check if PIN is locked
    if (isPinLocked(user)) {
        throw new BadRequestException("PIN is locked until " + user.getPinLockedUntil());
    }

    // Hash the PIN and save
    user.setPinHash(passwordEncoder.encode(pin));  // BCrypt hash — never store raw PIN!
    user.setPinAttempts(0);                         // Reset failed attempts
    user.setPinLockedUntil(null);                   // Clear lockout
    userRepository.save(user);                      // UPDATE users SET pin_hash = ? WHERE id = ?
}
```

### 6. Response flows back

```
AuthService returns void
    → AuthController wraps in ApiResponse.success(null, "PIN set successfully")
        → Spring converts to JSON: {"success":true,"data":null,"message":"PIN set successfully","timestamp":"..."}
            → DispatcherServlet sends HTTP 200 with this JSON
                → Mobile app receives the response
```

### 7. What happens if there's an error?

**Case: PIN is locked**
```
AuthService.setPin() throws BadRequestException("PIN is locked until...")
    → Exception exits the method immediately
        → Spring catches it
            → GlobalExceptionHandler.handleBadRequest() returns:
                {"status":400,"code":"BAD_REQUEST","message":"PIN is locked until 2026-07-10T14:00:00"}
                    → HTTP 400 response sent to mobile app
```

**Case: Database is down**
```
UserRepository.findById() throws DataAccessException
    → Not caught by AuthService
        → Not caught by AuthController
            → Caught by GlobalExceptionHandler.handleGeneralException()
                → Logs full stack trace
                    → Returns: {"status":500,"code":"INTERNAL_SERVER_ERROR","message":"An unexpected error occurred"}
```

---

## Chapter 13: Common Patterns in This Project

### Pattern 1: Builder Pattern (User.java)

```java
User user = User.builder()          // Instead of calling setters 10 times
        .firstName("John")
        .lastName("Doe")
        .email("john@example.com")
        .phone("+260971234567")
        .passwordHash(passwordEncoder.encode("SecurePass123"))
        .build();
```

Without Builder, you'd write:
```java
User user = new User();
user.setFirstName("John");
user.setLastName("Doe");
user.setEmail("john@example.com");
user.setPhone("+260971234567");
user.setPasswordHash(passwordEncoder.encode("SecurePass123"));
```

Builder is cleaner and prevents errors (can't forget to set a field).

### Pattern 2: Static Factory Method (ApiResponse.java)

```java
// Instead of: new ApiResponse<>(true, data, "message", LocalDateTime.now())
ApiResponse.success(data, "message")  // Cleaner, less error-prone
```

### Pattern 3: `from()` Method on DTOs (UserProfileResponse.java)

```java
public record UserProfileResponse(...) {
    public static UserProfileResponse from(User user) {  // Converts Entity → DTO
        return new UserProfileResponse(
                user.getId().toString(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                ...
        );
    }
}
```

Usage:
```java
return UserProfileResponse.from(user);  // Clean one-liner
```

### Pattern 4: Record for DTOs

```java
// Old way (100 lines of boilerplate)
public class RegisterRequest {
    private String email;
    private String password;
    // getters, setters, toString, equals, hashCode — 100 lines
}

// New way (1 line)
public record RegisterRequest(String email, String password) {}
// Java automatically generates: constructor, getters, toString, equals, hashCode
```

### Pattern 5: SLF4J Logging

```java
private static final Logger log = LoggerFactory.getLogger(AuthService.class);

log.info("User registered: {} (id={})", user.getEmail(), user.getId());
log.warn("Failed login attempt for email: {}", request.email());
log.error("Unhandled exception", exception);  // Second arg = stack trace
```

Log levels: `ERROR` (bad), `WARN` (suspicious), `INFO` (normal), `DEBUG` (verbose), `TRACE` (too verbose)

---

## Chapter 14: The `pom.xml` — What Each Dependency Does

```xml
<!-- Web server + REST APIs -->
<artifactId>spring-boot-starter-web</artifactId>
<!-- Gives you: Tomcat, DispatcherServlet, @RestController, @RequestMapping, etc. -->

<!-- Database access -->
<artifactId>spring-boot-starter-data-jpa</artifactId>
<!-- Gives you: JPA, Hibernate, @Entity, JpaRepository, @Transactional -->

<!-- Input validation -->
<artifactId>spring-boot-starter-validation</artifactId>
<!-- Gives you: @Valid, @NotBlank, @Email, @Size, @Pattern -->

<!-- Security -->
<artifactId>spring-boot-starter-security</artifactId>
<!-- Gives you: @EnableWebSecurity, SecurityFilterChain, @AuthenticationPrincipal -->

<!-- Database driver -->
<artifactId>postgresql</artifactId>
<!-- Gives you: ability to connect to PostgreSQL -->

<!-- Database migrations -->
<artifactId>flyway-core</artifactId>
<artifactId>flyway-database-postgresql</artifactId>
<!-- Gives you: V1__.sql, V2__.sql — automatically run on startup -->

<!-- API documentation -->
<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
<!-- Gives you: /swagger-ui.html, /api-docs -->

<!-- JWT tokens -->
<artifactId>jjwt-api</artifactId>
<artifactId>jjwt-impl</artifactId>
<artifactId>jjwt-jackson</artifactId>
<!-- Gives you: Jwts.builder(), Jwts.parser() for JWT creation/validation -->

<!-- Testing -->
<artifactId>spring-boot-starter-test</artifactId>
<artifactId>h2</artifactId>          <!-- ← Added for integration tests -->
<!-- Gives you: @SpringBootTest, MockMvc, @Mock, JUnit 5 -->

<!-- Boilerplate reduction -->
<artifactId>lombok</artifactId>
<!-- Gives you: @Getter, @Setter, @Builder, @NoArgsConstructor — less code -->
```

---

## Chapter 15: The Security Checklist

Before deploying any Spring Boot app, verify:

### 1. Are passwords hashed?
✅ Done — `passwordEncoder.encode(password)` uses BCrypt (PasswordConfig.java)

### 2. Are secrets externalized?
✅ Done — `JWT_SECRET` and `JWT_EXPIRATION` use `${ENV_VAR:default}` pattern

### 3. Is CSRF disabled correctly?
✅ Done — we use JWT (stateless), so CSRF is disabled

### 4. Are validation rules defined?
✅ Done — every DTO has `@NotBlank, @Size, @Email, @Pattern`

### 5. Are error messages safe?
✅ Done — never exposes stack traces, never reveals "email not found" vs "wrong password"

### 6. Is rate limiting implemented?
✅ Done for PIN (3 attempts = 15-min lockout)

### 7. Are deleted users hidden?
✅ Done — `@SQLRestriction("deleted = false")`

### 8. Are tokens revocable?
✅ Done — refresh tokens can be revoked, all tokens revoked on password reset

---

## Chapter 16: Quick Exercises to Test Understanding

### Beginner
1. **Find where the password is hashed** — search for `passwordEncoder.encode(` in AuthService.java
2. **Find where validation fails** — send a bad request via Swagger and trace the error
3. **Find where the JWT secret is configured** — check application.yml and JwtUtil constructor
4. **Change the API to run on port 9090** — edit application.yml (server.port)

### Intermediate
5. **Add a new field to User** — add `@Column String nationality` to User.java AND add `nationality VARCHAR(255)` to V2 migration
6. **Create a new endpoint** `GET /v1/users/me/email` that returns just the email
7. **Add logging to UserService** — log every profile update with old and new values

### Advanced
8. **Add a scheduled job** that deletes expired refresh tokens every hour
9. **Add a custom validator** `@StrongPassword` that requires uppercase, lowercase, number, and special char
10. **Add pagination to admin users list** — use Spring Data's `Pageable`

---

## Final Advice

**You don't learn Spring Boot by reading. You learn by breaking things.**

1. Change something → run the app → see it break → fix it → understand what happened
2. Add a new endpoint from scratch (start with controller, add service, add repository method)
3. Read the errors — Spring Boot gives excellent error messages with suggested fixes

**The learning order that works:**
1. First: REST controllers + services (30% of the code, 70% of what you'll write)
2. Then: JPA/entities (20% of the code, tedious but important)
3. Then: Security (10% of the code, hardest to debug)
4. Then: Configuration, testing, error handling (40% of the code, makes your app production-ready)

Every concept in this guide is visible in this project. Open the files, read them side by side with this guide, and you'll see exactly how each piece works.
