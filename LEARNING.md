# Learn Spring Boot from This Project

This guide maps the NovaWallet API codebase to Spring Boot concepts. Each section links concepts to specific files you can open and study.

---

## 1. Understand the Request Flow

Every HTTP request follows the same path. Trace one endpoint to understand the full stack.

### Example: `POST /v1/auth/register`

```
Browser/Swagger
    ↓ HTTP POST /v1/auth/register
SecurityConfig      ← Lets the request through (public endpoint)
    ↓
AuthController      ← Receives request, calls service, returns response
    ↓
AuthService         ← Business logic: validate, create user, create wallet
    ↓
UserRepository      ← Spring Data JPA: saves to database
WalletRepository
    ↓
User / Wallet       ← JPA entities mapped to PostgreSQL tables
```

### Try it yourself — trace a different endpoint:

| Endpoint | Controller | Service | Repository | Entity |
|---|---|---|---|---|
| POST /v1/auth/register | AuthController.java | AuthService.java | UserRepository, WalletRepository | User, Wallet |
| POST /v1/auth/login | AuthController.java | AuthService.java | UserRepository | User |
| GET /v1/users/me | UserController.java | UserService.java | UserRepository | User |
| GET /v1/wallets/me | WalletController.java | WalletService.java | WalletRepository | Wallet |
| GET /v1/admin/users | AdminController.java | AdminService.java | UserRepository | User |
| PATCH /v1/admin/users/{id}/toggle-delete | AdminController.java | AdminService.java | UserRepository | User |

---

## 2. Spring Boot Annotations — What Each One Does

### Layer 1: Controllers (`*Controller.java`)

| Annotation | What it does | See in |
|---|---|---|
| `@RestController` | Marks class as HTTP API handler | All controllers |
| `@RequestMapping("/v1/auth")` | Base path for all endpoints in this class | AuthController |
| `@GetMapping("/me")` | Handles GET requests | UserController |
| `@PostMapping("/register")` | Handles POST requests | AuthController |
| `@PutMapping("/me")` | Handles PUT requests | UserController |
| `@PatchMapping("/users/{id}/toggle-delete")` | Handles PATCH requests | AdminController |
| `@PathVariable UUID userId` | Extracts value from URL path | AdminController |
| `@RequestParam("token")` | Extracts query parameter | AuthController (verify email) |
| `@RequestBody` | Converts JSON body to Java object | AuthController |
| `@Valid` | Auto-validates request body using DTO annotations | AuthController |
| `@AuthenticationPrincipal` | Gets the authenticated user | UserController/WalletController |
| `ResponseEntity<T>` | Full control over HTTP response (status, headers, body) | All controllers |

### Layer 2: Services (`*Service.java`)

| Annotation | What it does | See in |
|---|---|---|
| `@Service` | Marks class as business logic (Spring creates a singleton bean) | All services |
| `@Transactional` | Wraps method in a database transaction (all or nothing) | AuthService, TokenService |
| `@Transactional(readOnly = true)` | Read-only transaction (optimization hint) | UserService.getProfile |

### Layer 3: Data Access (`*Repository.java`)

| Feature | What it does | See in |
|---|---|---|
| `extends JpaRepository<User, UUID>` | Gives you findAll, findById, save, delete for free | UserRepository |
| `Optional<User> findByEmail(String email)` | Spring Data parses method name → generates query | UserRepository |
| `boolean existsByEmail(String email)` | Generates existence check query | UserRepository |
| `@Query(value = "...", nativeQuery = true)` | Custom SQL that bypasses Hibernate filters | UserRepository |
| `@Modifying @Query("UPDATE ...")` | Custom update/delete query | RefreshTokenRepository |

### Layer 4: Entities (`*Entity.java` / `@Entity`)

| Annotation | What it does | See in |
|---|---|---|
| `@Entity` | Maps this class to a database table | User, Wallet, RefreshToken |
| `@Table(name = "users")` | Specifies table name | User |
| `@Id @GeneratedValue(strategy = UUID)` | Auto-generated UUID primary key | User |
| `@Column(nullable = false, unique = true)` | Column constraints | User |
| `@Enumerated(EnumType.STRING)` | Stores Java enum as string in DB | User (role) |
| `@Version` | Optimistic locking (prevents concurrent overwrites) | User |
| `@CreatedDate` | Auto-set on insert (needs @EnableJpaAuditing) | User |
| `@LastModifiedDate` | Auto-set on every update | User |
| `@PrePersist` | Runs before insert (custom logic) | User |
| `@SQLRestriction("deleted = false")` | Hides soft-deleted rows from ALL queries | User |
| `@EntityListeners(AuditingEntityListener.class)` | Enables @CreatedDate / @LastModifiedDate | User |

### Configuration Layer

| Annotation | What it does | See in |
|---|---|---|
| `@Configuration` | Marks class as source of bean definitions | SecurityConfig |
| `@Bean` | Declares a component Spring should manage | PasswordConfig |
| `@Value("${jwt.secret}")` | Injects value from application.yml | JwtUtil |
| `@SpringBootApplication` | Main entry point (combo of 3 annotations) | NovawalletApiApplication |

---

## 3. Study Path — 7 Days

### Day 1: Request Lifecycle
1. Open AuthController.java and AuthService.java side by side
2. Follow `register()` from controller → service → repository
3. Open User.java — see how fields map to the DB table
4. **Exercise**: Find where the 409 "duplicate email" response comes from

### Day 2: Security & JWT
1. Read SecurityConfig.java — understand the filter chain
2. Read JwtAuthFilter.java — see how tokens are extracted and validated
3. Read JwtUtil.java — see how tokens are created and parsed
4. **Exercise**: Change the JWT expiration from 15min to 1min and test token refresh

### Day 3: Error Handling
1. Read GlobalExceptionHandler.java — see all 7 exception handlers
2. Read each exception class: ResourceNotFoundException, BadRequestException, etc.
3. Trigger errors via Swagger (wrong password, duplicate email, invalid PIN)
4. **Exercise**: Add a new exception `ForbiddenException` and a handler for it

### Day 4: Database & JPA
1. Read V2__create_users_table.sql — see the actual SQL
2. Compare it to User.java — see how annotations map to SQL columns
3. Read `@SQLRestriction("deleted = false")` in User.java
4. Read UserRepository.findByIdIncludingDeleted() — the native query workaround
5. **Exercise**: Add a `status` field to User (enum: ACTIVE, SUSPENDED, BANNED)

### Day 5: Testing
1. Read AuthServiceTest.java — understand the Mockito pattern:
   - `@Mock` — creates fake objects
   - `@InjectMocks` — injects mocks into the tested class
   - `when(...).thenReturn(...)` — defines mock behavior
   - `verify(...)` — checks if a method was called
2. Run tests: `./mvnw test`
3. Read AuthControllerIntegrationTest.java — tests with real HTTP (MockMvc)
4. **Exercise**: Add a test for the "phone already exists" case in AuthServiceTest

### Day 6: Configuration & Profiles
1. Read application.yml — base config
2. Read application-dev.yml — dev profile overrides (PostgreSQL)
3. Read application-prod.yml — production profile (no credentials committed)
4. Read src/test/resources/application-test.yml — test profile (H2 in-memory)
5. **Exercise**: Create an application-staging.yml profile

### Day 7: Build & Deploy
1. Run full test suite: `./mvnw test`
2. Package the app: `./mvnw package -DskipTests`
3. Find the JAR in `target/novawallet-api-0.0.1-SNAPSHOT.jar`
4. Run it: `java -jar target/*.jar`
5. **Exercise**: Create a Dockerfile that builds and runs the app

---

## 4. Practice Exercises (from easy to hard)

### Easy (1 file)

1. **Change the PIN length**: Edit `SetPinRequest.java` — change `@Size(min=4, max=6)` to `@Size(min=5, max=5)`
2. **Rename API path**: In AuthController, change `@RequestMapping("/v1/auth")` to `"/v1/accounts"`
3. **Add a new error code**: In GlobalExceptionHandler, add `@ExceptionHandler(IllegalArgumentException.class)`

### Medium (2-3 files)

4. **Add change password endpoint**: Create ChangePasswordRequest DTO, add method in AuthService, add endpoint in AuthController
5. **Add wallet balance endpoint**: Add `GET /v1/wallets/{accountNumber}/balance` — returns just the balance
6. **Prevent re-registration of deleted accounts**: In AuthService.register(), check if email exists with deleted=true

### Hard (3-5 files)

7. **Add user search**: AdminController GET /v1/admin/users/search?q=... — search by name or email using `@Query`
8. **Add request logging middleware**: Create a filter that logs every request method + path + duration
9. **Add pagination**: Make AdminController GET /v1/admin/users return pageable results using Spring Data's `Pageable`

---

## 5. Common Mistakes and How to Fix Them

| Mistake | Symptom | Fix |
|---|---|---|
| Missing `@Transactional` | LazyInitializationException | Add `@Transactional` to service method |
| Forgetting `@Valid` | Validation annotations ignored | Add `@Valid` before `@RequestBody` |
| Circular dependency | BeanCurrentlyInCreationException | Extract shared logic into a third service |
| Wrong `@SQLRestriction` | Can't find soft-deleted user | Use native query to bypass filter |
| Not importing correct package | Compile error | Check your import statements |
| PostgreSQL running on wrong port | Connection refused | Check application-dev.yml for URL |

---

## 6. Key Commands

```bash
# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest="AuthServiceTest"

# Run a specific test method
./mvnw test -Dtest="AuthServiceTest#shouldRegisterUserSuccessfully"

# Start the app
./mvnw spring-boot:run

# Package the app
./mvnw package -DskipTests

# Run the packaged app
java -jar target/novawallet-api-0.0.1-SNAPSHOT.jar

# Skip tests during build
./mvnw package -DskipTests
```

---

## 7. Resources

| Topic | Link |
|---|---|
| Spring Boot Reference | https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/ |
| Spring Data JPA | https://docs.spring.io/spring-data/jpa/docs/current/reference/html/ |
| Spring Security | https://docs.spring.io/spring-security/reference/ |
| Baeldung Tutorials | https://www.baeldung.com/spring-boot |
| JJWT (JWT library) | https://github.com/jwtk/jjwt |

---

**Pro tip**: Don't try to learn everything at once. Pick one layer (controllers, services, or entities), understand it completely by reading every file, then move to the next.
