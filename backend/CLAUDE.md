# CLAUDE.md - Java Spring Boot Conventions

## Project Setup

- Java 25, Spring Boot 4.x, Spring Security 7.x, Maven
- PostgreSQL + Flyway migrations
- Bucket4j, JJWT (RSA256)
- Stateless REST API with JWT authentication
- No Lombok — use manual getters/setters and constructors
- Use Java 25 features: unnamed variables (`_`), pattern matching, records, etc.

## Package Structure

```
module/
  [feature]/
    controller/    # REST endpoints
    entity/        # JPA entities
    repository/    # Spring Data repositories + Specifications
    usecase/       # Business logic (one class = one action)
    dto/           # Request/Response records
    event/         # Domain events + listeners
core/
  dto/             # Shared DTOs (ErrorResponse, Violation)
  exception/       # BusinessRuleException, GlobalExceptionHandler
  security/        # JWT filter, provider, UserPrincipal, @PublicEndpoint + scanner
  ratelimit/       # @RateLimit annotation + interceptor
config/            # SecurityConfig, JpaConfig, WebConfig, JwtProperties
shared/
  entity/          # BaseEntity
  repository/      # Shared repositories
```

## Naming Conventions

- **Packages**: singular lowercase (`usecase`, `repository`, `controller`)
- **Use cases**: `[Verb][Entity]UseCase` with single `execute()` method
- **Controllers**: `[Entity]Controller`
- **Repositories**: `[Entity]Repository`
- **Specifications**: `[Entity]Specification` (static methods, private constructor)
- **DTOs**: Java records, named `[Purpose][Entity][Request|Response]`
- **Entities**: singular PascalCase, extend `BaseEntity`
- **Events**: `[Name]Event` / `[Name]Listener`
- **Constants**: `[Feature]Constants` with `UPPER_SNAKE_CASE` fields
- **Prefix with module name** when a class name could conflict across modules (e.g. `DemoCustomerRepository`)

## Annotation Ordering

### Controllers

```java
@PublicEndpoint                    // if public (class or method level)
@RestController
@RequestMapping("/api/[feature]")
@PreAuthorize("hasRole('ROLE')")   // if role-restricted (class or method level)
```

### Use Cases / Services

```java
@Service
```

### Entities

```java
@Entity
@Table(name = "table_name")
```

### Config

```java
@Configuration
@EnableWebSecurity        // if security config
@EnableMethodSecurity     // if security config
```

## Constructor Injection

- All dependencies are `private final` fields
- Logging via `private final Logger log = LoggerFactory.getLogger(ClassName.class)`

```java

@Service
public class CreateProfileUseCase {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;

    public CreateProfileUseCase(
            CustomerRepository customerRepository,
            AccountRepository accountRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
    }
}
```

## Use Case Pattern

- One class per business action
- Single public method: `execute(...)`
- `@Transactional` on writes, `@Transactional(readOnly = true)` on reads
- Extract helper logic into private methods (e.g. `checkCooldown`, `buildSpec`)

## Controller Pattern

- Return `ResponseEntity<T>` always
- `@Valid @RequestBody` for body validation
- `@Validated` on class for `@RequestParam` / `@PathVariable` validation
- `@AuthenticationPrincipal UserPrincipal principal` for authenticated user
- Blank line before every `return`

```java
@PostMapping("/profile")
public ResponseEntity<CustomerResponse> createProfile(
    @AuthenticationPrincipal UserPrincipal principal,
    @Valid @RequestBody RegisterCustomerRequest request) {

  CustomerResponse response = createProfileUseCase.execute(principal.accountId(), request);

  return ResponseEntity.ok(response);
}
```

## Entity Pattern

- Always extend `BaseEntity` (provides `id`, `createdAt`, `updatedAt`)
- Use `FetchType.LAZY` for all relationships
- Use `@Enumerated(EnumType.STRING)` for enums
- Use `CascadeType.ALL` + `orphanRemoval = true` on parent collections

## Repository Pattern

- Extend `JpaRepository<Entity, UUID>`
- Add `JpaSpecificationExecutor<Entity>` when dynamic filtering is needed
- Return `Optional<T>` for single results, `List<T>` for collections
- Use `JOIN FETCH` in `@Query` to avoid lazy loading issues
- Use Specifications for complex/dynamic queries (not long `@Query` with `IS NULL OR`)

## Specification Pattern

```java
public final class CustomerSpecification {

  private CustomerSpecification() {}

  public static Specification<Customer> fetchAccount() {
    return (root, query, cb) -> {
      if (query.getResultType() != Long.class && query.getResultType() != long.class) {
        root.fetch("account");
      }

      return cb.conjunction();
    };
  }

  public static Specification<Customer> hasEmail(String email) {
    return (root, query, cb) ->
        cb.like(cb.lower(root.join("account").get("email")), "%" + email.toLowerCase() + "%");
  }
}
```

## DTOs

- Always Java records (immutable)
- Validation annotations directly on record fields
- Nested records for paginated responses

```java
public record SendCodeRequest(
    @NotBlank @Email String email
) {}
```

## Error Handling

- Throw `BusinessRuleException(message, code, HttpStatus)` from use cases
- `GlobalExceptionHandler` catches all exceptions and returns `ErrorResponse`
- Error codes are `UPPER_SNAKE_CASE` strings
- All error responses follow the same structure: `{ type, message, code, violations }`

## Rate Limiting

```java
@RateLimit(requests = 5, periodSeconds = 60)
@GetMapping("/endpoint")
public ResponseEntity<...> method() { ... }
```

## Code Style

- Blank line before every `return` statement
- Never write `if` on a single line
- Extract complex lambda logic into private methods
- Use `Optional.ifPresent(this::methodRef)` over `if (opt.isPresent()) { opt.get()... }`
- Use `@NonNull` from `org.jspecify.annotations` when overriding `@NullMarked` methods
- Constants as `private static final` (e.g. `SecureRandom`, `Duration`)
- Comments on non-obvious code, Javadoc on public methods
- Always null-check return values that can be null — never assume a method returns non-null unless documented
- Use `HttpMethod` enum (not `String`) for HTTP method references
- Prefer streams and functional style over imperative loops when it improves readability
- Avoid duplicated overloaded methods — find a generic approach instead

## Spring Boot 4 / Spring Security 7 / Java 25

### Removed APIs (do NOT use)
- ❌ `AntPathRequestMatcher` — removed in Spring Security 7
- ✅ Use `PathPatternRequestMatcher.pathPattern(HttpMethod, String)` or `PathPatternRequestMatcher.pathPattern(String)` instead

### Prefer Spring built-in infrastructure over manual reflection
- ❌ Manual reflection to scan `@GetMapping`, `@PostMapping`, etc. on controller methods
- ✅ Use `RequestMappingHandlerMapping.getHandlerMethods()` — Spring already knows all registered routes, HTTP methods, and path patterns
- ✅ Use `AnnotatedElementUtils.hasAnnotation()` for annotation detection — handles meta-annotations and works on both classes and methods

### Java 25 features to use
- Unnamed variables: `_ -> false` instead of `request -> false`
- Pattern matching for `instanceof` and `switch`
- Records for DTOs and events
- `var` for local variables when the type is obvious from the right-hand side

### Custom annotations
- When creating annotations that work like `@PreAuthorize`, always support both `ElementType.TYPE` (class-level) and `ElementType.METHOD` (method-level)
- Check both the method and its declaring class when scanning: `AnnotatedElementUtils.hasAnnotation(method, ...) || AnnotatedElementUtils.hasAnnotation(beanType, ...)`

## Pagination

- 1-based pages in API (`?page=1`), converted to 0-based internally (`page - 1`)
- Response DTO includes: `content`, `totalItems`, `totalPages`, `isFirst`, `isLast`
- Use `PageRequest.of(page, size, Sort.by("id").ascending())`

## Validation

- `@Valid` on `@RequestBody` -> `MethodArgumentNotValidException`
- `@Validated` on controller class for `@RequestParam` -> `ConstraintViolationException`
- Missing `@RequestParam` -> `MissingServletRequestParameterException`
- Wrong type (e.g. invalid enum) -> `MethodArgumentTypeMismatchException`
- All handled by `GlobalExceptionHandler` -> 400 Bad Request

## Security

- JWT with RSA256 (private/public key pair)
- Access token (short-lived) + Refresh token (long-lived)
- `UserPrincipal` record implements `UserDetails`
- `@PreAuthorize("hasRole('ROLE')")` for role-based access on class or method level
- `@PublicEndpoint` for public routes (no authentication required), on class or method level
- Public routes are auto-discovered at startup by `PublicEndpointScanner` — never hardcode routes in `SecurityConfig`
- Cookie-based token delivery (`HttpOnly`, `Secure` configurable)

## Events

- Event as record: `public record LoginCodeRequestedEvent(String email, String code) {}`
- Listener with `@Async @EventListener` for non-blocking execution
- Listener with `@EventListener` (no `@Async`) for synchronous execution within the same transaction (rollback on failure)
- Publish via `ApplicationEventPublisher.publishEvent(...)`

## Database

- Flyway migrations in `src/main/resources/db/migration/`
- Naming: `V{version}__{description}.sql`
- `spring.jpa.hibernate.ddl-auto=validate` (Flyway handles schema)
- JPA auditing enabled via `@EnableJpaAuditing`
