# CLAUDE.md - Java Spring Boot Conventions

## Project Setup

- Java 25, Spring Boot 4.x, Spring Framework 7.x, Spring Security 7.x, Maven
- PostgreSQL + Flyway migrations
- Bucket4j, JJWT (RSA256)
- Stateless REST API with JWT authentication
- You can only use these Lombok annotations: `@Getter`, `@Setter`, `@RequiredArgsConstructor`
- Use Java 25 features: unnamed variables (`_`), pattern matching, records, etc.

## Package Structure

```
module/
  [feature]/
    controller/    # REST endpoints
    entity/        # JPA entities
    repository/    # Spring Data repositories + Specifications
    usecase/       # Business logic (one class = one action) — owns its Input/Output records
    dto/           # Only for payloads shared across use cases / standalone request bodies
    event/         # Domain events + listeners
core/
  dto/             # Shared DTOs (ErrorResponse, Violation, PaginatedResponse, PageQuery)
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
- **Prefix with module name** when a class name could conflict across modules (e.g.
  `DemoCustomerRepository`)

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

- One class per business action, `@Service @RequiredArgsConstructor`
- Single public method: `execute(Input input)`
- `@Transactional` on writes, `@Transactional(readOnly = true)` on reads
- Extract helper logic into private methods (e.g. `checkCooldown`, `buildSpec`)

### Input / Output records (canonical shape)

Each use case declares its own **nested `Input` and `Output` records** — do NOT create separate
`dto/` Response classes for them. Reads return `Output` (or `List<Output>` /
`PaginatedResponse<Output>`);
**writes return only the id** of the affected element (e.g. `record Output(UUID id) {}`), never the
full object.

**Entity → `Output` mapping is ALWAYS a `private` instance method on the use case** (e.g.
`private Output toOutput(Entity e)`), referenced with `this::toOutput` — never a `static
from(Entity)` factory on the record. This is the single, uniform rule: the DTO records stay pure
data carriers, and the mapper has every injected dependency (repositories, services, presigned-URL
generation, …) available. A `static from` can only see the entity, so the day a "pure" mapping needs
a dependency it forces a refactor; the instance method never does. Nested sub-view records map the
same way — one `private toXxx(...)` helper per view. Only when the **same** mapping is reused by 3+
use cases, extract a `@Component XxxMapper` (constructor-injected deps) and call it from each —
still
an instance method, never a static factory.

The `Input` is a **flat, role-agnostic record of domain fields** — do NOT pass a controller request
DTO (e.g. `XxxRequest`) into the use case. Each controller validates its own `@RequestBody` request
record and maps it into the use case `Input`. This keeps admin and customer endpoints independent:
they can expose different request fields (e.g. admin updates more than the customer) while sharing
the same use case.

```java

@Service
@RequiredArgsConstructor
public class GetTownsUseCase {

  private static final Sort SORT = Sort.by(Sort.Direction.DESC, "population");

  private final TownRepository townRepository;

  public record Input(
      String id, String postalCode, String city, String search, Integer page, Integer size) {

  }

  public record Output(UUID id, String inseeCode, String name, int population) {

  }

  @Transactional(readOnly = true)
  public PaginatedResponse<Output> execute(Input input) {
    Specification<Town> spec = buildSpec(input);
    Pageable pageable = new PageQuery(input.page(), input.size()).toPageable(SORT);

    Page<Output> page = townRepository.findAll(spec, pageable).map(this::toOutput);

    return PaginatedResponse.from(page);
  }

  // Mapping lives on the use case (never a static Output.from), so injected deps are available.
  private Output toOutput(Town town) {

    return new Output(town.getId(), town.getInseeCode(), town.getName(), town.getPopulation());
  }

  // One conditional block per filter; combine with Specification.allOf (empty list -> match all).
  private Specification<Town> buildSpec(Input input) {
    List<Specification<Town>> specs = new ArrayList<>();

    if (StringUtils.hasText(input.id())) {
      specs.add(TownSpecification.hasId(input.id()));
    }

    if (StringUtils.hasText(input.search())) {
      specs.add(TownSpecification.matchesSearch(input.search()));
    }

    return Specification.allOf(specs);
  }
}
```

- Filtering: build a `List<Specification<T>>` in `buildSpec`, guarding each with
  `StringUtils.hasText(...)`, then `Specification.allOf(specs)`.

## Controller Pattern

- Return `ResponseEntity<T>` always, typed with the use case's nested record:
  `ResponseEntity<GetTownsUseCase.Output>`,
  `ResponseEntity<PaginatedResponse<GetTownsUseCase.Output>>`.
- Controller builds the `Input` and calls `execute(input)`; it holds no business logic.
- `@Valid @RequestBody` for body validation
- `@Validated` on class for `@RequestParam` / `@PathVariable` validation (add `@Size`/etc. to
  params)
- `@AuthenticationPrincipal UserPrincipal principal` for authenticated user
- Pagination params are `page` (1-based) and `size`
- Blank line before every `return`

```java

@PublicEndpoint
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/towns")
public class TownController {

  private final GetTownsUseCase getTownsUseCase;

  @GetMapping
  public ResponseEntity<PaginatedResponse<GetTownsUseCase.Output>> getTowns(
      @RequestParam(required = false) @Size(max = 191) String search,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {

    var input = new GetTownsUseCase.Input(null, null, null, search, page, size);

    PaginatedResponse<GetTownsUseCase.Output> response = getTownsUseCase.execute(input);

    return ResponseEntity.ok(response);
  }
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

  private CustomerSpecification() {
  }

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

- Request/response payloads are modelled as the use case's nested `Input` / `Output` records — see
  **Use Case Pattern**. Don't add separate `dto/` Response classes for them.
- A standalone `dto/` record is only for a payload shared across use cases, or a request body you
  want to annotate independently. Always Java records (immutable), validation annotations directly
  on
  the fields.

```java
public record SendCodeRequest(
    @NotBlank @Email String email
) {

}
```

## Error Handling

- Throw `BusinessRuleException(message, code, HttpStatus)` from use cases
- `GlobalExceptionHandler` catches all exceptions and returns `ErrorResponse`
- Error codes are `UPPER_SNAKE_CASE` strings
- All error responses follow the same structure: `{ type, message, code, violations }`
- End user-facing `BusinessRuleException` messages with a period (they are surfaced to the
  frontend via `ErrorResponse`). Omit the period only when the message ends in an interpolated
  value (e.g. `"Failed to load resource: %s"`) or is a pure internal/diagnostic string
  (e.g. `INTERNAL_SERVER_ERROR`). A `%s` mid-string still gets a period if the message ends in
  static text (e.g. `"... maximum allowed size of %s MB."`).

## Rate Limiting

```java
@RateLimit(requests = 5, periodSeconds = 60)
@GetMapping("/endpoint")
public ResponseEntity<...>

method() { ...}
```

## Code Style

- Blank line before every `return` statement
- Never write `if` on a single line
- Extract complex lambda logic into private methods
- Use `Optional.ifPresent(this::methodRef)` over `if (opt.isPresent()) { opt.get()... }`
- Use `@NonNull` from `org.jspecify.annotations` when overriding `@NullMarked` methods
- Constants as `private static final` (e.g. `SecureRandom`, `Duration`)
- Comments on non-obvious code, Javadoc on public methods
- Always null-check return values that can be null — never assume a method returns non-null unless
  documented
- Use `HttpMethod` enum (not `String`) for HTTP method references
- Prefer streams and functional style over imperative loops when it improves readability
- Avoid duplicated overloaded methods — find a generic approach instead
- Use `"text %s".formatted(var)` instead of string concatenation (`+`) for building strings with
  variables

## Spring Boot 4 / Spring Security 7 / Java 25

### Removed APIs (do NOT use)

- ❌ `AntPathRequestMatcher` — removed in Spring Security 7
- ✅ Use `PathPatternRequestMatcher.pathPattern(HttpMethod, String)` or
  `PathPatternRequestMatcher.pathPattern(String)`
  instead

### Prefer Spring built-in infrastructure over manual reflection

- ❌ Manual reflection to scan `@GetMapping`, `@PostMapping`, etc. on controller methods
- ✅ Use `RequestMappingHandlerMapping.getHandlerMethods()` — Spring already knows all registered
  routes, HTTP methods,
  and path patterns
- ✅ Use `AnnotatedElementUtils.hasAnnotation()` for annotation detection — handles meta-annotations
  and works on both
  classes and methods

### Java 25 features to use

- Always prefer the modern, idiomatic Java 25 way of doing things — use the standard recommendations
  and best practices
  for Java 25 APIs and language features
- Unnamed variables: `_ -> false` instead of `request -> false`
- Pattern matching for `instanceof` and `switch`
- Records for DTOs and events
- `var` for local variables when the type is obvious from the right-hand side

### Custom annotations

- When creating annotations that work like `@PreAuthorize`, always support both `ElementType.TYPE` (
  class-level) and
  `ElementType.METHOD` (method-level)
- Check both the method and its declaring class when scanning:
  `AnnotatedElementUtils.hasAnnotation(method, ...) || AnnotatedElementUtils.hasAnnotation(beanType, ...)`

## Pagination

- Query params are `page` (1-based, `?page=1`) and `size` (items per page). Never `first`.
- Build the `Pageable` with `core/dto/PageQuery`: `new PageQuery(page, size).toPageable(sort)` — it
  converts to the 0-based index and applies defaults (`page=1`, `size=50`, capped at `100`).
- List endpoints return `core/dto/PaginatedResponse<Output>` built via
  `PaginatedResponse.from(page)`.
  Shape: `{ content, totalItems, totalPages, isFirst, isLast }`. The frontend reads `data.content`.

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
- Public routes are auto-discovered at startup by `PublicEndpointScanner` — never hardcode routes in
  `SecurityConfig`
- Cookie-based token delivery (`HttpOnly`, `Secure` configurable)

## Events

- Event as record: `public record LoginCodeRequestedEvent(String email, String code) {}`
- Listener with `@Async @EventListener` for non-blocking execution
- Listener with `@EventListener` (no `@Async`) for synchronous execution within the same
  transaction (rollback on
  failure)
- Publish via `ApplicationEventPublisher.publishEvent(...)`

## Database

- Flyway migrations in `src/main/resources/db/migration/`
- Naming: `V{version}__{description}.sql`
- `spring.jpa.hibernate.ddl-auto=validate` (Flyway handles schema)
- JPA auditing enabled via `@EnableJpaAuditing`

## Testing

Two tiers, split by the standard Maven Surefire/Failsafe convention:

- **Unit tests** — suffix `*Test` (use cases are `[Verb][Entity]UseCaseTest`). Pure Mockito
  (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`), no Spring context. Run by
  **Surefire** in `mvn test`. Build entities with `TestFactory` (detached, no DB).
- **Integration tests** — suffix `*IT`, run by **Failsafe** in `mvn verify` (kept out of the fast
  unit-test phase). Boot the full context against Testcontainers Postgres + Redis by extending
  `AbstractIntegrationTest`; drive endpoints through `MockMvc`. Persist state with the `fixtures`
  (`givenX(...)`) helpers, not `TestFactory`.

### Integration test conventions

- **One class per controller**, named `[Entity]ControllerIT`, in the controller's own package
  (`module.[feature].controller`).
- **One `@Nested` class per endpoint**, named after the action (`SendCode`, `VerifyCode`), each
  with a `/** HTTP_METHOD /api/path */` Javadoc and a `private static final String URL` constant.
- Shared `@Autowired` repositories live on the outer class; nested classes reference them.
- Assert the HTTP contract (status, JSON body, cookies) **and** the persisted DB state. Use
  `assertPersistedState(...)` when traversing lazy associations after the request commits.
