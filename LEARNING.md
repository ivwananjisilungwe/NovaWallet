# Learn Spring Boot — From This Project

> **A senior engineer walks you through the codebase. No fluff. Just what you need to know.**

---

## Part 1: The Wide View

Before we touch a single file, you need the mental map. Without it, every file looks like random code. With it, you'll see patterns everywhere.

---

### Chapter 1: What Spring Boot Actually Is

**Forget what the tutorials say. Here's the truth:**

Spring Boot is a **pre-built application engine**. You provide the business logic. It provides everything else.

Think of it like buying a restaurant vs building one from scratch:

| Building from scratch (plain Java) | Using Spring Boot |
|---|---|
| Build the building | Find a ready-made restaurant space |
| Install the kitchen | Kitchen is already there |
| Hire the staff | Staff is already trained |
| Set up the menu system | Menu system is ready to use |
| Open for business | **You just cook the food** |

**What Spring Boot gives you for free:**

- **A web server** (Tomcat) — listens for HTTP requests
- **JSON converter** (Jackson) — turns Java objects into JSON and back
- **Database connector** (JPA/Hibernate) — talks to PostgreSQL for you
- **Security system** (Spring Security) — handles authentication and authorization
- **Validation engine** — checks that input data is correct
- **Error handler** — catches and formats errors consistently
- **Testing framework** — makes it easy to test everything

**Your job is only:**
1. Create the database tables (migrations)
2. Write the business logic (services)
3. Define the API endpoints (controllers)
4. Set up the configuration (application.yml)

**This is why Spring Boot became the standard for Java web apps.** You go from idea to working API in days, not months.

---

### Chapter 2: The Project Map — Where Everything Lives

Let me walk you through the folder structure like I'm showing you around a new office.

```
NovaWallet/
├── novawallet-api/                   ← THE WHOLE APP LIVES HERE
│   ├── pom.xml                       ← Recipe file (what ingredients to use)
│   ├── src/main/java/com/novawallet/novawallet_api/
│   │   ├── NovawalletApiApplication.java    ← The front door (main method)
│   │   │
│   │   ├── auth/                     ← AUTHENTICATION MODULE
│   │   │   ├── controller/           ← Receives login/register requests
│   │   │   ├── service/              ← The login/register logic
│   │   │   ├── dto/request/          ← What the client sends (register form data)
│   │   │   └── dto/response/         ← What the server sends back (tokens + user info)
│   │   │
│   │   ├── user/                     ← USER MANAGEMENT MODULE
│   │   │   ├── controller/           ← Profile update requests
│   │   │   ├── service/              ← Profile update logic
│   │   │   └── dto/                  ← Profile data that goes in/out
│   │   │
│   │   ├── wallet/                   ← WALLET MODULE
│   │   │   ├── controller/           ← Balance check requests
│   │   │   ├── service/              ← Wallet creation logic
│   │   │   └── dto/                  ← Wallet data that goes in/out
│   │   │
│   │   ├── admin/                    ← ADMIN MODULE
│   │   │   ├── controller/           ← Admin-only requests
│   │   │   ├── service/              ← Admin logic (user management)
│   │   │   └── dto/                  ← Admin-specific data
│   │   │
│   │   ├── common/                   ← SHARED STUFF
│   │   │   ├── config/              ← Security, password, JPA config
│   │   │   ├── exception/           ← Custom exceptions and global handler
│   │   │   └── dto/                 ← ApiResponse (wraps ALL responses)
│   │   │
│   │   ├── entity/                   ← DATABASE TABLE MAPS
│   │   │   ├── User.java            ← maps to "users" table
│   │   │   ├── Wallet.java           ← maps to "wallets" table
│   │   │   └── RefreshToken.java     ← maps to "refresh_tokens" table
│   │   │
│   │   ├── repository/               ← DATABASE QUERIES
│   │   │   ├── UserRepository.java
│   │   │   ├── WalletRepository.java
│   │   │   └── RefreshTokenRepository.java
│   │   │
│   │   ├── security/                 ← JWT + AUTH FILTERS
│   │   │   ├── JwtUtil.java          ← Creates/validates JWT tokens
│   │   │   ├── JwtAuthFilter.java    ← Checks JWT on every request
│   │   │   ├── CustomUserDetailsService.java  ← Loads user from DB
│   │   │   └── SecurityConfig.java   ← Ties security together
│   │   │
│   │   └── resources/                ← CONFIGURATION FILES
│   │       ├── application.yml       ← Main config (always loaded)
│   │       ├── application-dev.yml   ← Dev database settings
│   │       └── db/migration/         ← SQL files that build the database
│   │           ├── V1__create_refresh_tokens.sql
│   │           ├── V2__create_users_table.sql
│   │           ├── V3__create_wallets_table.sql
│   │           └── V4__add_pin_and_reset_fields.sql
│   │
│   └── src/test/                     ← TESTS
│       └── java/.../
│           ├── AuthServiceTest.java  ← Unit tests (fast, no database)
│           └── AuthControllerIntegrationTest.java  ← Full app test
│
├── docs/                             ← REPORTS + GUIDES
├── .handoffs/HANDOFF.md              ← Context for next developer
└── README.md                         ← What this project is
```

**The golden rule of this structure:**

> Each module (`auth/`, `user/`, `wallet/`, `admin/`) is self-contained. They follow the same pattern: `controller/` → `service/` → `dto/`. You learn one module, you know them all.

**Why this structure instead of putting everything in one package?**

When we had one package (`auth/` with everything), it was:
- Hard to find things (20+ files in one folder)
- Confusing what belonged where
- Impossible to work on multiple features without stepping on each other

The modular structure means:
- **AuthController** only talks about authentication
- **UserController** only talks about user profiles
- **WalletController** only talks about wallets
- If you need to change login behavior, you know exactly which folder to open

---

### Chapter 3: How The Folders Talk To Each Other

This is the most important concept to understand. Let me show you the communication lines.

```
                   ┌──────────────────┐
                   │   CONTROLLER      │
                   │  (AuthController) │
                   └────────┬─────────┘
                            │ CALLS
                            ▼
                   ┌──────────────────┐
                   │   SERVICE         │
                   │  (AuthService)    │
                   └──┬───────┬───────┘
                      │       │
            CALLS     │       │  CALLS
                      ▼       ▼
           ┌────────────┐  ┌────────────┐
           │ REPOSITORY  │  │ OTHER      │
           │ (UserRepo)  │  │ SERVICES   │
           └──────┬─────┘  │ (TokenSrvc)│
                  │         └────────────┘
                  ▼
           ┌────────────┐
           │ DATABASE    │
           │ (PostgreSQL)│
           └────────────┘
```

**Rules of communication (NEVER break these):**

```
✅ Controller → Service     (controller calls service)
✅ Service → Repository     (service calls repository)
✅ Service → Service        (services can call each other)
❌ Controller → Repository  (controller should NOT call repository directly)
❌ Controller → Controller  (controllers don't call each other)
❌ Repository → Service     (repositories don't call services)
```

**Why can't the Controller talk directly to the Repository?**

Because that would skip the business logic. Imagine:

```java
// WRONG — controller bypasses service
@GetMapping("/users/{id}")
public User getUser(@PathVariable UUID id) {
    return userRepository.findById(id).orElse(null);
    // No business logic! No logging! No authorization check!
    // Anyone can look up any user by ID!
}

// RIGHT — controller delegates to service
@GetMapping("/users/{id}")
public ResponseEntity<ApiResponse<UserProfileResponse>> getUser(@PathVariable UUID id) {
    UserProfileResponse user = userService.getUserById(id);
    // Service checks: does the requester have permission?
    // Service checks: is the user deleted?
    // Service logs: "Profile retrieved for user {id}"
    return ResponseEntity.ok(ApiResponse.success(user, "User found"));
}
```

**The dependency direction matters:**

```
HIGH-LEVEL (abstract, changes often)        ← You write this
    Controller  → depends on Service
        Service  → depends on Repository
            Repository  → depends on Entity
LOW-LEVEL (concrete, changes rarely)        ← Spring provides this
    Entity      → maps to Database
```

Controllers change when the API changes (new features, new fields).
Entities rarely change (database structure is stable).

---

### Chapter 4: The Request's Journey — Full Trace

Let me trace what happens when you hit **POST /v1/auth/login** with `{"email": "john@email.com", "password": "mypass123"}`.

I'll walk through every single step so you see how all the pieces connect.

---

**Step 1: The Network**

Your mobile app opens a TCP connection to `server:8080` and sends an HTTP request.

```
POST /v1/auth/login HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{"email": "john@email.com", "password": "mypass123"}
```

This is just text sent over the internet. Nothing special yet.

---

**Step 2: Tomcat (The Web Server)**

Tomcat is embedded inside your app (you never installed it separately). It:
1. Accepts the TCP connection
2. Reads the bytes until the HTTP request is complete
3. Passes the request to Spring's DispatcherServlet

**Key insight:** Tomcat is literally inside your JAR file. When you run `./mvnw spring-boot:run`, it starts Tomcat as part of your application process. No separate server installation needed.

---

**Step 3: The Security Filter Chain (Spring Security)**

Before your code sees the request, it goes through a series of security filters. Think of this like airport security before you reach the gate:

```
Request arrives
    │
    ▼
1. CorsFilter               ← "Is this request from an allowed origin?"
    │                         (Checks if the mobile app's domain is allowed)
    ▼
2. JwtAuthFilter            ← "Does the request have a JWT token?"
    │                         (For login: no JWT needed, this is public)
    │                         (JwtAuthFilter sees no token → does nothing → passes through)
    ▼
3. Request proceeds to controller
```

For public endpoints (register, login), the filter chain does nothing — the request passes through.

For protected endpoints (get profile, check balance), JwtAuthFilter:
1. Extracts the `Authorization: Bearer eyJhbGci...` header
2. Validates the JWT signature (using the secret key)
3. Loads the user from the database
4. Stores the user in the SecurityContext (like a sticky note saying "This is user X")
5. Passes the request through

---

**Step 4: The DispatcherServlet**

This is Spring MVC's brain. It:
1. Looks at the path `/v1/auth/login`
2. Checks its registry: "Who handles this path?"
3. Finds: `AuthController.login()` method (because of `@PostMapping("/login")` on the class with `@RequestMapping("/v1/auth")`)
4. Passes the request to that method

---

**Step 5: AuthController (The Receptionist)**

```java
@PostMapping("/login")
public ResponseEntity<ApiResponse<AuthResponse>> login(
        @Valid @RequestBody LoginRequest request
) {
    // Spring has already:
    // 1. Read the JSON body
    // 2. Converted it to a LoginRequest Java object
    // 3. Validated it (checked @NotBlank, @Email)
    // 4. If validation failed, returned 400 without running this code
    
    AuthResponse response = authService.login(request);
    return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
}
```

The controller does NOTHING except:
- Receive the validated request
- Call the service
- Return the response

No if-statements. No calculations. No database calls. Just delegation.

---

**Step 6: AuthService (The Brain)**

```java
public AuthResponse login(LoginRequest request) {
    // 1. Find the user by email
    User user = userRepository.findByEmail(request.email().toLowerCase().trim())
            .orElseThrow(() -> new BadRequestException("Invalid email or password"));
    // Note: Same error message for "email not found" and "wrong password"
    // This prevents attackers from knowing which emails are registered
    
    // 2. Check password
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
        log.warn("Failed login attempt for email: {}", request.email());
        throw new BadRequestException("Invalid email or password");
    }
    
    // 3. Generate tokens
    String accessToken = jwtUtil.generateToken(user.getId().toString());
    String refreshToken = tokenService.createRefreshToken(user);
    
    // 4. Return response
    return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(jwtUtil.getExpirationMs())
            .user(buildUserInfo(user))
            .build();
}
```

Notice what the service handles:
- **Defensive email handling**: `.toLowerCase().trim()` — prevents "John@Email.com" from causing issues
- **Secure error messages**: Same message for "wrong email" and "wrong password" — prevents email harvesting
- **Logging**: Failed attempts are logged (but successful ones might not be, depends on requirements)
- **Token generation**: Creates both access and refresh tokens

---

**Step 7: UserRepository (The Database Bridge)**

When the service calls `userRepository.findByEmail(email)`:

```java
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}
```

Spring Data JPA reads this method and automatically generates:

```sql
SELECT * FROM users WHERE email = ? AND deleted = false
```

Note: The `AND deleted = false` comes from `@SQLRestriction("deleted = false")` on the User entity. Every query automatically excludes soft-deleted users.

**You never wrote that SQL. You never even configured it.**
Spring generates it from the method name + entity annotations.

---

**Step 8: PostgreSQL (The Database)**

PostgreSQL executes the query and returns the user row (or empty result).

The database doesn't know or care about your Java code. It just executes SQL and returns data.

---

**Step 9: Response Flows Back**

```
PostgreSQL returns data
    → Repository returns User entity
        → Service builds AuthResponse
            → Controller wraps in ApiResponse.success()
                → Spring converts to JSON (Jackson library)
                    → Security filter chain adds response headers
                        → Tomcat sends HTTP response
```

The mobile app receives:

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "expiresIn": 900000,
    "user": {
      "id": "70c3c6bc-4c90-4c18-a36e-4e6b8a43540d",
      "firstName": "John",
      "lastName": "Doe",
      "email": "john@email.com",
      "role": "USER",
      "emailVerified": false,
      "pinSet": false
    }
  },
  "message": "Login successful",
  "timestamp": "2026-07-10T12:00:00"
}
```

**Every response in this project looks the same.** The mobile app can always expect `{success, data, message, timestamp}`.

---

## Part 2: Deep Into Each Layer

Now that you have the big picture, let's drill into each layer.

---

### Chapter 5: Controllers — The Reception Desk

**Analogy:** A controller is like a hotel receptionist.
- A guest arrives (HTTP request)
- Receptionist checks: did you bring what we need? (validation)
- Receptionist calls the right department (service)
- Receptionist gives you the result (response)

**What a controller MUST do:**
1. Accept the request
2. Validate input (via `@Valid`)
3. Call the service
4. Return the result

**What a controller MUST NOT do:**
- No business logic (no if-else for business rules)
- No database calls (no repository calls)
- No complex calculations

**Spot the violation:**

```java
// This is WRONG — controller doing service work
@PostMapping("/transfer")
public ResponseEntity<?> transfer(@RequestBody TransferRequest req) {
    if (req.amount() <= 0) {           // ← Business logic in controller!
        return ResponseEntity.badRequest().body("Amount must be positive");
    }
    String result = walletRepo.findById(req.fromWallet())  // ← Controller calling repository!
            .map(w -> doTransfer(w, req))                  // ← Business logic!
            .orElse("Wallet not found");
    return ResponseEntity.ok(result);
}

// This is RIGHT — controller just delegates
@PostMapping("/transfer")
public ResponseEntity<ApiResponse<TransferResponse>> transfer(
        @Valid @RequestBody TransferRequest req
) {
    TransferResponse result = walletService.transfer(req);
    return ResponseEntity.ok(ApiResponse.success(result, "Transfer completed"));
}
```

**The `@Valid` annotation is doing a LOT of work:**

When you write `@Valid @RequestBody RegisterRequest request`, Spring:
1. Reads the JSON body from the HTTP request
2. Creates a `RegisterRequest` Java object (using Jackson)
3. For every field with a validation annotation (`@NotBlank`, `@Email`, `@Size`...):
   - Checks the value
   - If any field fails, collects all error messages
4. If there are errors:
   - Throws a `MethodArgumentNotValidException`
   - This is caught by... `GlobalExceptionHandler`!
   - Returns HTTP 400 with all the validation errors
5. If no errors:
   - Passes the validated object to your controller method

**Think of it like airport security:** Passengers go through the scanner (validation). If they have prohibited items, they're stopped before boarding. Clean passengers board the plane (reach your controller method).

---

### Chapter 6: Services — Where Business Logic Lives

**Analogy:** A service is like the kitchen in a restaurant. The waiter (controller) brings the order, the kitchen (service) does the actual cooking.

**What goes in a service:**
- Business rules ("email must be unique")
- Calculations (fee calculation, interest)
- Orchestration (do A, then B, then C — all or nothing)
- Security checks ("can this user do this?")
- External service calls (send email, call payment API)

**The `@Service` annotation is just a label.** It marks the class so Spring knows to create it and inject it where needed. The actual "service-ness" comes from what you put inside.

**`@Transactional` — The Safety Net:**

```java
@Service
@Transactional  // ← Every public method is a database transaction
public class TransferService {
    
    public void transfer(UUID fromWalletId, UUID toWalletId, BigDecimal amount) {
        Wallet from = walletRepository.findById(fromWalletId)
                .orElseThrow(() -> new ResourceNotFoundException("Source wallet not found"));
        Wallet to = walletRepository.findById(toWalletId)
                .orElseThrow(() -> new ResourceNotFoundException("Target wallet not found"));
        
        from.setBalance(from.getBalance().subtract(amount));  // Deduct
        to.setBalance(to.getBalance().add(amount));            // Add
        
        walletRepository.save(from);
        walletRepository.save(to);
    }
}
```

What happens if `walletRepository.save(to)` fails (database error)?

With `@Transactional`:
- The `save(from)` change is **automatically rolled back**
- The database is exactly as it was before `transfer()` ran
- No lost money

Without `@Transactional`:
- `save(from)` already committed the deduction
- `save(to)` failed
- Money disappeared from the system

**This is why `@Transactional` exists.** It wraps your method in: BEGIN TRANSACTION → do work → COMMIT (if no error) / ROLLBACK (if error).

**The Service is also where you handle cross-cutting concerns:**

```java
public AuthResponse register(RegisterRequest request) {
    // Validation
    if (userRepository.findByEmail(email).isPresent()) {
        throw new BadRequestException("Email already taken");
    }
    
    // Creation
    User user = User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(password))
            .build();
    
    // Side effects
    accountNumberGenerator.generateNewAccount(user);  // Create wallet
    emailService.sendVerificationEmail(user);          // Send email
    auditLogger.log("User registered: " + email);      // Log
    
    // Return
    return buildAuthResponse(user);
}
```

The controller doesn't know about wallet creation, email sending, or audit logging. That's all the service's job.

---

### Chapter 7: Repositories — The Database Bridge

**Analogy:** A repository is an interpreter. Your Java code speaks methods, the database speaks SQL. The repository translates.

**What makes Spring Data JPA magical:**

You write an INTERFACE, not an implementation:

```java
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}
```

Spring generates a class at runtime that:
1. Parses `findByEmail` → "SELECT * FROM users WHERE email = ?"
2. Executes the query
3. Converts the result set to a `User` object
4. Returns it wrapped in `Optional`

**Naming convention = SQL:**

| Method | Generated SQL |
|---|---|
| `findByEmail(String email)` | `WHERE email = ?` |
| `findByEmailAndPhone(e, p)` | `WHERE email = ? AND phone = ?` |
| `findByEmailOrPhone(e, p)` | `WHERE email = ? OR phone = ?` |
| `findByCreatedAtAfter(date)` | `WHERE created_at > ?` |
| `findByRoleOrderByEmailAsc(role)` | `WHERE role = ? ORDER BY email ASC` |
| `findByFirstNameContainingIgnoreCase(name)` | `WHERE UPPER(first_name) LIKE ?` |
| `deleteByEmail(email)` | `DELETE FROM users WHERE email = ?` |

**The methods you get from JpaRepository without writing anything:**

```
save(entity)         → INSERT (if new) or UPDATE (if exists)
findById(id)         → SELECT with primary key
findAll()            → SELECT all rows
findAll(pageable)    → SELECT with pagination (LIMIT/OFFSET)
count()              → SELECT COUNT(*)
delete(entity)       → DELETE
deleteById(id)       → DELETE with primary key
existsById(id)       → SELECT COUNT(*) and return boolean
```

**Native queries — when the naming convention isn't enough:**

```java
@Query(value = "SELECT * FROM users WHERE email = :email AND deleted = true", nativeQuery = true)
Optional<User> findDeletedByEmail(@Param("email") String email);
```

Use `nativeQuery = true` when:
- You need database-specific features
- The query is too complex for the naming convention
- You need to bypass Hibernate filters (like `@SQLRestriction`)

---

### Chapter 8: Entities — The Database Mirror

**Analogy:** An Entity is a photograph of a database row, taken at a specific moment.

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
}
```

Each field maps to a column. Each instance represents one row.

**The important annotations:**

| Annotation | Purpose |
|---|---|
| `@Entity` | "This class maps to a database table" |
| `@Table(name = "users")` | "The table name is 'users'" |
| `@Id` | "This is the primary key" |
| `@GeneratedValue` | "Let the database generate this value" |
| `@Column(name = "col_name")` | "Override the column name" |
| `@Column(nullable = false)` | "This column can't be null" |
| `@Column(unique = true)` | "No duplicate values allowed" |
| `@Enumerated(EnumType.STRING)` | "Store enum as a string, not a number" |
| `@CreatedDate` | "Set this when the row is first created" |
| `@LastModifiedDate` | "Update this every time the row changes" |
| `@Version` | "Track for optimistic locking (prevents concurrent edits)" |
| `@SQLRestriction` | "Add a WHERE clause to EVERY query for this entity" |

**The `@SQLRestriction` trick deserves special attention:**

```java
@SQLRestriction("deleted = false")
public class User { ... }
```

This means: Every time you call `findById()`, `findAll()`, `findByEmail()` — Spring adds `AND deleted = false` to the SQL.

So `userRepository.findAll()` actually runs: `SELECT * FROM users WHERE deleted = false`

- Soft-deleted users simply don't exist as far as the app is concerned
- The admin can restore them (using a native query that bypasses the restriction)
- When you query "how many users", deleted accounts don't count

**Entity lifecycle hooks:**

```java
@Entity
@EntityListeners(AuditingEntityListener.class)
public class User {
    @CreatedDate
    private LocalDateTime createdAt;     // Auto-set on first save
    
    @LastModifiedDate
    private LocalDateTime updatedAt;     // Auto-set on every save
    
    @PrePersist
    public void beforeSave() {
        this.email = this.email.toLowerCase().trim();  // Auto-format before saving
    }
}
```

- `@PrePersist`: Runs BEFORE the first save
- `@PreUpdate`: Runs BEFORE every update
- `@PostLoad`: Runs AFTER reading from database
- `@PostPersist`: Runs AFTER the first save

---

### Chapter 9: DTOs — The Data Bodyguards

**Analogy:** A DTO is like a bodyguard. The entity (VIP) stays safely inside. The DTO goes out to meet the public.

**Why not return the entity directly?** 

```java
// What if you returned the User entity directly from the API?
{
  "id": "70c3c6bc-...",
  "email": "john@email.com",
  "passwordHash": "$2a$10$...",      // ← PASSWORD HASH! 
  "pinHash": "$2a$10$...",            // ← PIN HASH!
  "deleted": false,                    // ← INTERNAL STATE
  "version": 1,                        // ← LOCKING FIELD
  "verificationToken": "abc123",       // ← VERIFICATION TOKEN!
  "resetToken": null,
  "pinAttempts": 0,
  "pinLockedUntil": null
}
```

You just leaked:
- Password hash (could be cracked offline)
- PIN hash
- Email verification token (anyone could verify any email)
- Internal implementation details

DTOs solve this by defining EXACTLY what to expose:

```java
// DTO — only contains what the client needs
public record UserProfileResponse(
    String id,
    String firstName,
    String lastName,
    String email,
    String role,
    boolean emailVerified,
    boolean pinSet          // ← Just "yes or no", not the hash!
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId().toString(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name(),
                user.isEmailVerified(),
                user.getPinHash() != null  // ← Just checking if it exists
        );
    }
}
```

**The `from()` pattern is used everywhere:**

```java
// In any service:
return UserProfileResponse.from(user);
// One line converts Entity → DTO safely
```

**Records vs Classes for DTOs:**

```java
// OLD WAY - 60 lines of boilerplate
public class RegisterRequest {
    private String email;
    private String password;
    public String getEmail() { return email; }
    public void setEmail(String e) { this.email = e; }
    // 20 more lines for password, toString, equals, hashCode
}

// NEW WAY (Java 16+) - 1 line
public record RegisterRequest(
    String email,
    String password
) {}
// Java auto-creates: constructor, getters (email(), password()), 
//                    toString(), equals(), hashCode()
```

Records are perfect for DTOs because:
- They're **immutable** — once created, fields can't change
- They automatically get `toString()`, `equals()`, `hashCode()`
- They're concise — the data shape is immediately visible
- They support validation annotations

---

### Chapter 10: Configuration — How the App Knows What to Do

**Analogy:** Configuration is like a restaurant's opening instructions. "Open at 8am. Close at 10pm. Suppliers deliver on Tuesdays. Fire alarm code is 1234."

**The configuration files:**

```yaml
# application.yml (always loaded)
server:
  port: 8080              # Listen on port 8080

spring:
  profiles:
    active: dev           # Also load application-dev.yml

jwt:
  secret: base64-secret-key-here
  expiration: 900000      # 15 minutes in milliseconds
```

```yaml
# application-dev.yml (loaded when "dev" profile is active)
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/novawallet
    username: postgres
    password: 8407
```

**How configuration reaches your code:**

```
application.yml  ──→  @Value("${jwt.secret}")  ──→  JwtUtil.java
                      @Value reads: "ZGV2LXNlY3JldC1rZXkt..."
```

```java
// In JwtUtil.java constructor:
public JwtUtil(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.expiration}") long expirationMs
) {
    this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    this.expirationMs = expirationMs;
}
```

**The `@ConfigurationProperties` approach (cleaner for many values):**

We don't use this yet, but it's good to know:

```yaml
app:
  jwt:
    secret: abc123
    expiration: 900000
  verification:
    expiry-hours: 24
```

```java
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    String secret,
    long expiration
) {}
```

Spring automatically maps `app.jwt.secret` → `JwtProperties.secret()`.

**Profiles let you have different configs for different environments:**

| Profile | Database | Debug | Used When |
|---|---|---|---|
| `dev` | local PostgreSQL | Full logging | Development |
| `test` | H2 in-memory | Minimal | Testing |
| `prod` | Production DB | Warnings+errors | Live server |

When running tests, Spring reads: `application.yml` + `application-test.yml` (overrides)
When running in prod, Spring reads: `application.yml` + `application-prod.yml` (overrides)

Each profile only overrides what's different. Common settings stay in `application.yml`.

---

### Chapter 11: Security — The Bouncer System

**Analogy:** Security is like a nightclub bouncer system.

```
Door policy: Anyone can enter the PUBLIC areas  (register, login)
             Only members can enter the VIP areas  (profile, dashboard)
             Only managers can enter the BACK OFFICE  (admin endpoints)
```

**How Spring Security implements this:**

```java
// SecurityConfig.java
.authorizeHttpRequests(auth -> auth
    // PUBLIC — no ID required
    .requestMatchers("/v1/auth/register", "/v1/auth/login").permitAll()
    
    // ADMIN ONLY — must have ADMIN role
    .requestMatchers("/v1/admin/**").hasRole("ADMIN")
    
    // AUTHENTICATED — must have valid JWT
    .anyRequest().authenticated()
)
```

**The JWT flow step by step:**

```
1. User logs in → server validates password → server creates JWT
2. Server returns JWT to client: "Here's your pass. It's good for 15 minutes."
3. Client stores JWT (in mobile app storage)
4. Client sends every request with: "Authorization: Bearer <JWT>"
5. Server reads JWT → validates signature → extracts userId
6. Server loads user from database
7. Server processes request as "this user"
```

**Why JWT and not sessions?**

Sessions: Server stores login state. "User X is logged in." Memory usage grows with users. Doesn't scale horizontally well.

JWT: Client proves identity on every request. Server doesn't store anything. "Show me your token, and I'll verify the signature." Scales linearly. No server-side memory.

**JWT is like a signed document:**
- Only the server can CREATE valid JWTs (it has the secret key)
- Anyone can READ the JWT (it's base64-encoded, not encrypted)
- If someone modifies the JWT, the signature won't match → rejected
- JWTs expire (typically 15 minutes) — even if stolen, they're short-lived

**The JwtAuthFilter:**

This filter runs on every single request (that reaches protected endpoints):

```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain) {
    
    // Step 1: Extract token from header
    String authHeader = request.getHeader("Authorization");
    // "Bearer eyJhbGciOiJIUzM4NCJ9..."
    
    String token = authHeader.substring(7);  // Remove "Bearer "
    
    // Step 2: Validate token
    String userId = jwtUtil.extractUserId(token);
    
    // Step 3: Load user
    UserDetails userDetails = userDetailsService.loadUserById(userId);
    
    // Step 4: Create authentication
    UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
    
    // Step 5: Store in SecurityContext
    SecurityContextHolder.getContext().setAuthentication(authentication);
    
    // Step 6: Continue the filter chain
    filterChain.doFilter(request, response);
}
```

After this filter runs, every controller method can use `@AuthenticationPrincipal` to get the authenticated user.

---

### Chapter 12: Error Handling — The Safety Net

**Analogy:** Error handling is like a safety net in a circus. If a trapeze artist falls (exception thrown), the net catches them (GlobalExceptionHandler) and prevents injury (ugly error messages shown to user).

**Without a global handler:**

Every controller method would need:
```java
@PostMapping("/register")
public ResponseEntity<?> register(...) {
    try {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    } catch (EmailTakenException e) {
        return ResponseEntity.status(409).body("Email taken");
    } catch (InvalidPasswordException e) {
        return ResponseEntity.status(400).body("Bad password");
    } catch (Exception e) {
        return ResponseEntity.status(500).body("Something broke");
        // ← Stack trace leaks! Inconsistent format!
    }
}
```

Now repeat for EVERY controller method. You'd have 50+ try-catch blocks.

**With GlobalExceptionHandler:**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // ONE handler for ALL BadRequestExceptions across ALL controllers
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "BAD_REQUEST", ex.getMessage(), ...));
    }
    
    // ONE handler for ALL ResourceNotFoundExceptions
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "NOT_FOUND", ex.getMessage(), ...));
    }
    
    // CATCH-ALL — handles anything not specifically handled above
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);  // Log stack trace internally
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "INTERNAL_ERROR", "An unexpected error occurred", ...));
        // ← Safe message: no stack trace, no internal details
    }
}
```

**The flow:**

```
Service throws BadRequestException("Invalid email or password")
    ↓
Exception propagates up through Controller
    ↓
Spring catches it
    ↓
Spring asks: "Who handles BadRequestException?"
    ↓
GlobalExceptionHandler: "I do! @ExceptionHandler(BadRequestException.class)"
    ↓
Returns ErrorResponse with HTTP 400
    ↓
Spring sends this JSON to the client
```

**The custom exceptions:**

```java
// These are simple classes that extend RuntimeException
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

They're just named errors so the handler knows which type to catch and what status code to return.

**Good error message vs Bad error message:**

```
BAD:  "Email null@email.com is already registered, current user id: xyz123"
      ↑ Leaks internal user IDs and implementation details

GOOD: "Email already registered"
      ↑ Tells the user what they need to know, nothing more
```

---

## Part 3: Patterns & Philosophy

---

### Chapter 13: The Patterns We Follow and Why

**Pattern 1: Builder Pattern**

```java
User.builder()
    .firstName("John")
    .lastName("Doe")
    .email("john@email.com")
    .passwordHash(encodedPassword)
    .build();
```

**Why:** Without Builder, you'd either:
- Call 10 setters: error-prone (what if you forget `passwordHash`?)
- Create a constructor with 10 parameters: unreadable (`new User(null, null, "John", "Doe", ...)`)

Builder forces you to name each value and prevents ordering mistakes.

**Pattern 2: Immutable DTOs with Records**

```java
public record AuthResponse(String accessToken, ...) {}
```

**Why:** DTOs should never change after creation. Records enforce this. If something needs to change, create a new record. This prevents bugs where you accidentally modify a response object.

**Pattern 3: Static Factory Methods**

```java
// Instead of:
new ApiResponse<>(true, data, "Success", LocalDateTime.now())

// We write:
ApiResponse.success(data, "Success")
```

**Why:** The method name (`success`) tells you what it does. The constructor call is cryptic (what is that `true`? why `LocalDateTime.now()`?). Named methods are self-documenting.

**Pattern 4: Constructor Injection**

```java
@Service
public class AuthService {
    private final UserRepository userRepository;
    
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

**Why:** 
- `final` keyword means the dependency can never be null
- Constructor shows ALL dependencies at a glance
- Tests can pass mocks easily
- No Spring-specific annotations needed (works with plain Java)

**Pattern 5: Single Responsibility per Module**

Each module has ONE job:
- `auth/` : handles login, register, tokens
- `user/` : handles profiles
- `wallet/` : handles wallet data
- `admin/` : handles admin operations

If you need to change login behavior, you know exactly which folder to open.

---

### Chapter 14: The "Why" Behind Every Major Decision

**Why Java Records for DTOs?**

Java 16 introduced Records specifically for data carriers. Before records, a simple DTO required:
- A class declaration
- private fields
- getters for each field
- a constructor
- toString()
- equals() and hashCode()

That's 40+ lines of boilerplate per DTO. Records reduce it to one line. Less code = fewer bugs.

**Why BCrypt for passwords, not SHA-256?**

SHA-256 is a hash function designed for SPEED. It can hash millions of passwords per second. If a hacker steals your database, they can try billions of passwords per day.

BCrypt is designed to be SLOW. It takes ~100ms to hash one password. That's intentional. A hacker can only try ~10 passwords per second per user. BCrypt is also self-salting (each hash includes a random salt).

**Why UUIDs instead of auto-increment IDs?**

Auto-increment IDs (1, 2, 3...) are:
- Predictable (user 1 exists, user 2 exists)
- Guessable (try /api/users/5, /api/users/6...)
- Problematic in distributed systems (two servers might generate ID 5 at the same time)

UUIDs (70c3c6bc-4c90-4c18-a36e-4e6b8a43540d) are:
- Random
- Unguessable
- Unique across all servers
- Can be generated on the client side

**Why Soft Delete instead of Hard Delete?**

Hard delete: `DELETE FROM users WHERE id = ?` — data is gone forever.

Soft delete: `UPDATE users SET deleted = true WHERE id = ?` — data is hidden but recoverable.

Soft delete means:
- You can restore accidentally deleted accounts
- You can audit: "who deleted this and when?"
- You keep referential integrity (wallets referencing deleted users won't break)
- You can query deleted users for analytics

**Why @SQLRestriction instead of WHERE deleted = false everywhere?**

Without `@SQLRestriction`, EVERY repository method would need to add `AND deleted = false`:

```java
Optional<User> findByEmailAndDeletedFalse(String email);
List<User> findAllByDeletedFalse();
```

That's error-prone (one missed method = security hole). `@SQLRestriction` is automatic — you can't forget it.

**Why separate the auth module from the user module?**

Initially, auth and user were one module. Problem:
- Every time you added a user feature (profile update, avatar), auth was involved
- Every time you touched auth (login, security), user tests were affected
- It was unclear what belonged where

Separating them means:
- Auth only handles: login, register, tokens, security
- User only handles: profile, preferences, account management
- You can change how login works without touching user profiles

---

### Chapter 15: How To Think When Adding A New Feature

Here's the mental checklist a senior engineer goes through when adding a feature.

**Example: "Add a feature to change password"**

**Step 1: Which layer does this belong to?**

"Changing password is user-related" → `user/` module. Not `auth/`.
But... it requires verifying the old password, which is auth-related.

Decision: Put the endpoint in `auth/` since it involves authentication credentials.

**Step 2: What does the client send?**

```java
public record ChangePasswordRequest(
    @NotBlank String currentPassword,
    @Size(min = 8, max = 128) String newPassword
) {}
```

Put this in `auth/dto/request/`.

**Step 3: What does the server return?**

```java
public record MessageResponse(String message) {}
```

Or we can use `ApiResponse<Void>` — no data, just success message.

**Step 4: What's the business logic (Service)?**

```java
public void changePassword(UUID userId, ChangePasswordRequest request) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
        throw new BadRequestException("Current password is incorrect");
    }
    
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
    
    // Invalidate all existing tokens (force re-login on other devices)
    refreshTokenRepository.deleteAllByUserId(userId);
}
```

**Step 5: What's the endpoint (Controller)?**

```java
@PutMapping("/password")
public ResponseEntity<ApiResponse<Void>> changePassword(
        @Valid @RequestBody ChangePasswordRequest request,
        @AuthenticationPrincipal UserDetails userDetails
) {
    UUID userId = UUID.fromString(userDetails.getUsername());
    authService.changePassword(userId, request);
    return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully. Please login again."));
}
```

**Step 6: What's the security rule?**

It's a protected endpoint (requires JWT). Users can only change their own password.

The `@AuthenticationPrincipal` ensures the user is authenticated.
The service ensures it's the right user (by using the userId from the JWT, not from the request body).

**Step 7: Add to SecurityConfig?**

```java
.requestMatchers("/v1/auth/password").authenticated()
```

Already covered by `.anyRequest().authenticated()`.

**Step 8: Error cases to handle?**

- Current password wrong → 400 Bad Request
- User not found → 404 Not Found (unlikely since JWT was valid)
- New password same as current → business decision (allow or reject?)
- Database error → 500 handled by GlobalExceptionHandler

**Step 9: Write tests?**

- Unit test: Mock UserRepository, verify password is updated
- Unit test: Verify error when current password is wrong
- Unit test: Verify tokens are invalidated after password change

---

### Chapter 16: Common Mistakes and How to Avoid Them

**Mistake 1: Service calls another Service that calls back (circular dependency)**

```java
// BAD: Circular dependency
@Service public class AuthService {
    private final UserService userService;
    public AuthService(UserService us) { this.userService = us; }
}
@Service public class UserService {
    private final AuthService authService;
    public UserService(AuthService as) { this.authService = as; }
}
// Spring fails: "Cannot create bean — circular dependency!"
```

**Fix:** Extract the common code into a third service, or restructure so A → B → C (one direction).

**Mistake 2: Using field injection instead of constructor injection**

```java
// BAD — field injection
@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;
}

// GOOD — constructor injection
@Service
public class AuthService {
    private final UserRepository userRepository;
    
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

Field injection hides dependencies (you can't see them in method calls), prevents `final` fields, and makes testing harder.

**Mistake 3: Exposing the entity directly in the API**

```java
// BAD — password hash leaked!
@GetMapping("/me")
public User getProfile() {
    return user;  // Returns the ENTITY with password_hash!
}
```

Always use DTOs. Create a `UserProfileResponse.from(user)` and return that.

**Mistake 4: Forgetting @Transactional on multi-step operations**

```java
// BAD — if withdraw succeeds but deposit fails, money is lost
public void transfer(fromId, toId, amount) {
    walletRepo.deduct(fromId, amount);  // Gets committed immediately
    walletRepo.add(toId, amount);        // If this fails, the deduction is permanent
}

// GOOD — everything succeeds together or nothing happens
@Transactional
public void transfer(fromId, toId, amount) {
    walletRepo.deduct(fromId, amount);
    walletRepo.add(toId, amount);
}
```

**Mistake 5: Writing SQL in controllers**

```java
// BAD — business logic in controller
@GetMapping("/users/{id}")
public User getUser(@PathVariable UUID id) {
    return userRepository.findById(id)  // Controller touching repository!
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    // Also throws Exception class that doesn't match our error format
}
```

Controller → Service → Repository. Never skip a layer.

**Mistake 6: Not using logging properly**

```java
// BAD — no logging
public void login(request) {
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
        throw new BadRequestException("Invalid credentials");
    }
}
// Someone tries to hack into accounts. You have no record of it.

// GOOD — log suspicious activity
public AuthResponse login(request) {
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
        log.warn("Failed login attempt for email: {}", request.email());
        throw new BadRequestException("Invalid credentials");
    }
    log.info("User logged in: {}", user.getId());
}
```

**Mistake 7: Hardcoding configuration values**

```java
// BAD — magic number hidden in code
public static final int JWT_EXPIRATION = 900000;
public static final String JWT_SECRET = "my-secret-key";

// GOOD — configurable from application.yml
@Value("${jwt.expiration}") long expiration;
@Value("${jwt.secret}") String secret;
```

Configuration should be in YAML files, not in Java code. This is how you deploy the same code to dev, staging, and production — just change the YAML.

---

## Part 4: Testing & Operations

---

### Chapter 17: How Testing Works (The Wider View)

**Two types of tests, two different purposes:**

| Aspect | Unit Tests | Integration Tests |
|---|---|---|
| Tests what? | One class in isolation | The whole app together |
| Uses database? | No (mocked) | Yes (H2 in-memory) |
| Speed | Milliseconds | Seconds |
| Number of tests | Many (covers all edge cases) | Few (covers main flows) |
| Catches | Logic bugs | Configuration bugs |
| Runs on every build? | Yes (fast) | Yes (but fewer) |

**Why both? Here's a real example:**

Unit test says: "AuthService.register passes" ✅
Integration test says: "POST /v1/auth/register returns 201" ❌

Problem? The controller was using `AuthResponse.builder()` but the actual `AuthResponse` class was moved to a different package. The unit test tested service in isolation (never built the full response). The integration test caught it because it went through the real controller.

**Unit tests are for logic. Integration tests are for wiring.**

**The test pyramid:**

```
        ╱╲
       ╱  ╲           UI / End-to-End tests (few)
      ╱    ╲
     ╱      ╲
    ╱────────╲
   ╱          ╲        Integration tests (some)
  ╱────────────╲
 ╱              ╲
╱────────────────╲     Unit tests (many — 80% of tests)
```

**Mocking in unit tests:**

```java
// AuthServiceTest.java
@Mock
private UserRepository userRepository;     // ← Fake DB

@InjectMocks
private AuthService authService;            // ← Real service with fake DB

@Test
void register_shouldFail_whenEmailExists() {
    // Arrange: Tell the fake DB to return "email exists"
    when(userRepository.existsByEmail("taken@email.com")).thenReturn(true);
    
    // Act: Try to register
    assertThrows(BadRequestException.class, () -> {
        authService.register(new RegisterRequest(..., "taken@email.com", ...));
    });
    
    // Assert: Verify error was thrown
    verify(userRepository).existsByEmail("taken@email.com");
}
```

This test:
- Runs in 2 milliseconds
- Doesn't need PostgreSQL
- Doesn't need any database setup
- Tests ONLY the duplicate email check logic

**H2 database in integration tests:**

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL
```

H2 is an in-memory database that can emulate PostgreSQL syntax. Every test run starts with a clean database — no data pollution from previous runs.

**The `@ActiveProfiles("test")` annotation:**

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")    // ← Use application-test.yml settings
class AuthControllerIntegrationTest {
    // Tests run with H2 database, not PostgreSQL
}
```

---

### Chapter 18: The Build Pipeline (Code to Running App)

**What happens when you run `mvn clean install`?**

```
clean: Delete target/ directory (remove old compiled files)
  │
compile: Read all .java files, compile to .class files
  │
process-resources: Copy application.yml to target/
  │
process-test-resources: Copy application-test.yml to target/test-classes/
  │
test-compile: Compile test files (*Test.java)
  │
test: Run all @Test methods
  ├── If ANY test fails → BUILD FAILS (stop here)
  │
package: Bundle everything into a .jar file
  │
install: Copy .jar to local Maven repository (for other projects to use)
```

**The pom.xml is the instruction manual:**

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.4</version>
</parent>
```

This means: "Use Spring Boot 3.4.4 as my foundation. I get its default settings, dependency versions, and plugins."

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

This means: "I need to build a web API. Give me Tomcat, Jackson, DispatcherServlet, and everything else."

Each starter is a curated set of dependencies that work together. `spring-boot-starter-web` pulls in 15+ transitive dependencies (Tomcat, Jackson, Spring MVC, etc.) — all version-compatible, all tested together.

**The final JAR contains:**

```
novawallet-api-0.0.1-SNAPSHOT.jar
├── BOOT-INF/
│   ├── classes/            ← Your compiled code + resources
│   │   ├── com/novawallet/.../*.class
│   │   └── application.yml
│   └── lib/                ← All dependencies (Tomcat, Spring, etc.)
│       ├── spring-boot-*.jar
│       ├── tomcat-embed-*.jar
│       ├── postgresql-*.jar
│       └── ... (50+ JAR files)
├── META-INF/
└── org.springframework.boot.loader/  ← Spring Boot loader
```

This is a FAT JAR — it contains everything needed to run. No external Tomcat, no external dependencies. Just `java -jar novawallet-api-0.0.1-SNAPSHOT.jar` and it runs.

---

## What's Next

This project will grow with new phases. Each phase will add:

- **New modules** (transactions, notifications, payments...)
- **New patterns** (caching, messaging, scheduled jobs...)
- **New Spring features** (Redis, RabbitMQ, WebSockets...)

After each phase, this guide gets a new chapter explaining what was built, why it was built that way, and how it connects to everything else.

**The goal:** By Phase 8, you'll have a complete Spring Boot education — from `@RestController` to distributed tracing — all from code you actually built.

---

> **Remember:** Reading about Spring Boot teaches you concepts. Breaking things teaches you understanding. Every bug you fix makes you a better developer. Every feature you add makes the patterns click.
