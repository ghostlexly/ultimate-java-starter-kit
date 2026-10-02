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
    usecase/       # Business logic (one class = one action) — declares its Query/Command/Result records
    dto/           # Web-layer payloads: request bodies, endpoint response records, shared payloads
    event/         # Domain events + listeners
core/
  dto/             # Shared DTOs (ErrorResponse, Violation, PaginatedResponse)
  exception/       # BusinessRuleException, GlobalExceptionHandler
  pagination/      # SortWhitelist (static helper: sort key whitelist resolution for web Pageables)
  security/        # JWT filter, provider, UserPrincipal, @PublicEndpoint + scanner
  ratelimit/       # @RateLimit annotation + interceptor
config/            # SecurityConfig, JpaConfig, WebConfig, JwtProperties
shared/
  entity/          # BaseEntity
  repository/      # Shared repositories
```

## Naming Conventions

- **Packages**: singular lowercase (`usecase`, `repository`, `controller`)
- **Use cases**: `[Verb][Entity]UseCase`, declaring its `[Verb][Entity]Query` (read) or `[Verb][Entity]Command`
  (write) parameter record and its `[Verb][Entity]Result` return record — see **Use Case Pattern**
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

- One class per business action, `@Service @RequiredArgsConstructor`, single public method `execute(...)`
- **CQRS naming**: each use case declares its own records **inside its class**, prefixed with the use case name
  (`GetBookingQuery`, not `Query`). No separate record files, never `Input` / `Output`. Callers import the record
  (`import ….GetBookingUseCase.GetBookingQuery;`) and write `new GetBookingQuery(...)`.

  | Kind                    | Parameter (`query` / `command`) | Returns                                                         | Transaction                       |
  |-------------------------|---------------------------------|-----------------------------------------------------------------|-----------------------------------|
  | Query — reads only      | `GetBookingQuery`               | `GetBookingResult` (or `List<…>` / `PaginatedResponse<…>` of it) | `@Transactional(readOnly = true)` |
  | Command — changes state | `CancelBookingCommand`          | `CancelBookingResult`, or `void`                                | `@Transactional`                  |

- **The `Query` / `Command` is mandatory, even with 0 or 1 parameter** (`record GetStatsQuery() {}`). Never
  `execute(UUID id)` or `execute()`: call sites stay uniform, arguments are named, and adding a field never changes
  the signature.
- It is a flat record of domain fields. Never pass a controller `XxxRequest` into a use case: the controller maps it,
  so admin and customer endpoints can share one use case.
- **Never return a JPA entity**, always a `Result` the use case owns.
- **Simple writes (insert / update / delete) return only the id**: `record UpdateBookingResult(UUID id) {}`. Return
  more only when the action itself produces data the caller needs (e.g. tokens).
- The same shape returned by 2+ use cases → one shared `XxxView` record in `dto/`, not duplicated `Result`s.
- Extract helper logic into private methods (e.g. `checkCooldown`, `buildSpecs`)

```java
@Service
@RequiredArgsConstructor
public class GetTownsUseCase {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "population");

    // API sort key -> entity property paths. Anything else falls back to DEFAULT_SORT.
    private static final Map<String, List<String>> SORTABLE_PROPERTIES = Map.of("population", List.of("population"));

    private final TownRepository townRepository;

    public record GetTownsQuery(Pageable pageable, String id, String search) {}

    public record GetTownsResult(UUID id, String inseeCode, String name, int population) {}

    @Transactional(readOnly = true)
    public PaginatedResponse<GetTownsResult> execute(GetTownsQuery query) {
        Specification<Town> spec = buildSpecs(query);
        Pageable pageable = SortWhitelist.apply(query.pageable(), SORTABLE_PROPERTIES, DEFAULT_SORT);

        Page<GetTownsResult> page = townRepository.findAll(spec, pageable).map(this::toResult);

        return PaginatedResponse.from(page);
    }

    private GetTownsResult toResult(Town town) {

        return new GetTownsResult(town.getId(), town.getInseeCode(), town.getName(), town.getPopulation());
    }

    // One guarded block per filter; Specification.allOf (empty list -> match all).
    private Specification<Town> buildSpecs(GetTownsQuery query) {
        List<Specification<Town>> specs = new ArrayList<>();

        if (StringUtils.hasText(query.search())) {
            specs.add(TownSpecification.matchesSearch(query.search()));
        }

        return Specification.allOf(specs);
    }
}
```

### Mapping

- Entity → `Result` / `XxxView` mapping is **always an injectable instance method, never a `static from(Entity)`** on
  the record: a static can't reach injected dependencies (repositories, presigned URLs, …).
    - `Result` → `private XxxResult toResult(Entity e)` on the use case, referenced with `this::toResult`
    - shared `XxxView` → a `@Component XxxViewMapper` with `toView(Entity e)`, injected into each use case
- Naming: `to<Target>(source)` on a mapper; `from(source)` only for a `static` factory on the target type itself.
  Never `mapper.from(...)`.
- **Web-layer reshaping**: when an endpoint needs a wire shape different from the `Result` / `XxxView`, declare a
  record in `dto/` named after the endpoint (`GetBookingsResponse`, not `BookingResponse`) with a
  `static from(result)` factory. A `static from` is allowed there (and in `PaginatedResponse.from(Page)`): it is a
  pure record → record reshape. Otherwise the controller returns the `Result` as-is.

## Controller Pattern

- Return `ResponseEntity<T>` always, typed with the use case's result: `ResponseEntity<GetTownsResult>`,
  `ResponseEntity<PaginatedResponse<GetTownsResult>>`.
- Controller builds the `Query` / `Command` and calls `execute(...)`; it holds no business logic. Locals are named
  `query` / `command` and `result`.
- `@Valid @RequestBody` for body validation
- `@Validated` on class for `@RequestParam` / `@PathVariable` validation (add `@Size`/etc. to params)
- `@AuthenticationPrincipal UserPrincipal principal` for authenticated user
- Pagination params are `page` (1-based), `size` and `sort`, received as a single `@PageableDefault(size = …) Pageable
  pageable` argument and passed as-is into the use case `Query` — see **Pagination**

```java

@PublicEndpoint
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/towns")
public class TownController {

    private final GetTownsUseCase getTownsUseCase;

    @GetMapping
    public ResponseEntity<PaginatedResponse<GetTownsResult>> getTowns(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) @Size(max = 191) String search) {

        var query = new GetTownsQuery(pageable, null, search);
        var result = getTownsUseCase.execute(query);

        return ResponseEntity.ok(result);
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

- `dto/` holds the web-layer records: request bodies, endpoint response records (only when the wire shape differs
  from the use case's `Result`, see **Mapping**) and shared `XxxView`s. Never a Response class that merely mirrors a
  `Result`.
- Always Java records (immutable), validation annotations directly on the fields.

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
- End user-facing `BusinessRuleException` messages with a period (they are surfaced to the frontend via
  `ErrorResponse`). Omit the period only when the message ends in an interpolated value (e.g.
  `"Failed to load resource: %s"`) or is a pure internal/diagnostic string (e.g. `INTERNAL_SERVER_ERROR`). A `%s`
  mid-string still gets a period if the message ends in static text (e.g. `"... maximum allowed size of %s MB."`).

## Rate Limiting

```java
@RateLimit(requests = 5, periodSeconds = 60)
@GetMapping("/endpoint")
public ResponseEntity<...>

method() { ...}
```

## Code Style

- Never write `if` on a single line
- Extract complex lambda logic into private methods
- Use `@NonNull` from `org.jspecify.annotations` when overriding `@NullMarked` methods
- Constants as `private static final` (e.g. `SecureRandom`, `Duration`)
- Comments on non-obvious code, Javadoc on public methods
- Always null-check return values that can be null — never assume a method returns non-null unless documented
- Use `HttpMethod` enum (not `String`) for HTTP method references
- Prefer streams and functional style over imperative loops when it improves readability
- Avoid duplicated overloaded methods — find a generic approach instead
- Use `"text %s".formatted(var)` instead of string concatenation (`+`) for building strings with variables

## Spring Boot 4 / Spring Security 7 / Java 25

### Removed APIs (do NOT use)

- ❌ `AntPathRequestMatcher` — removed in Spring Security 7
- ✅ Use `PathPatternRequestMatcher.pathPattern(HttpMethod, String)` or
  `PathPatternRequestMatcher.pathPattern(String)`
  instead

### Prefer Spring built-in infrastructure over manual reflection

- ❌ Manual reflection to scan `@GetMapping`, `@PostMapping`, etc. on controller methods
- ✅ Use `RequestMappingHandlerMapping.getHandlerMethods()` — Spring already knows all registered routes, HTTP methods,
  and path patterns
- ✅ Use `AnnotatedElementUtils.hasAnnotation()` for annotation detection — handles meta-annotations and works on both
  classes and methods

### Java 25 features to use

- Always prefer the modern, idiomatic Java 25 way of doing things — use the standard recommendations and best practices
  for Java 25 APIs and language features
- Unnamed variables: `_ -> false` instead of `request -> false`
- Pattern matching for `instanceof` and `switch`
- Records for DTOs and events
- `var` for local variables when the type is obvious from the right-hand side

### Custom annotations

- When creating annotations that work like `@PreAuthorize`, always support both `ElementType.TYPE` (class-level) and
  `ElementType.METHOD` (method-level)
- Check both the method and its declaring class when scanning:
  `AnnotatedElementUtils.hasAnnotation(method, ...) || AnnotatedElementUtils.hasAnnotation(beanType, ...)`

## Pagination

- Query params are `page` (1-based, `?page=1`), `size` (items per page) and the optional `sort`, in Spring's
  format: `?sort=<key>,<asc|desc>` (repeatable). Never `first`, never a separate `order` param.
- Controllers receive them as a web-resolved `Pageable`: `@PageableDefault(size = 10) Pageable pageable`, passed as-is
  into the use case `Query` (`Pageable pageable` is its first field). Do not declare `page`/`size`/`sort`
  `@RequestParam`s by hand. The 1-based index and the `100` items cap come from `spring.data.web.pageable.*` in
  `application.yaml`. Don't put a `sort` in `@PageableDefault` — the default sort belongs to the use case.
- **Never pass the received `Pageable` to a repository as-is**: its sort is client-controlled. Declare a
  `SORTABLE_PROPERTIES` whitelist (`Map<String, List<String>>` of API sort key → entity property paths) and a
  `DEFAULT_SORT` on the use case, then build the final pageable with the static `core/pagination/SortWhitelist`:
  `SortWhitelist.apply(query.pageable(), SORTABLE_PROPERTIES, DEFAULT_SORT)` — unknown keys fall back to the default
  sort, so clients can never sort on arbitrary columns. When the sort must be decided conditionally (e.g. unsorted
  for a relevance-ordered search), use `SortWhitelist.resolve(requestedSort, SORTABLE_PROPERTIES, DEFAULT_SORT)` and
  rebuild the `PageRequest` yourself (see `PaginateAnalysesUseCase.resolvePageable`).
- List endpoints return `core/dto/PaginatedResponse<XxxResult>` built via
  `PaginatedResponse.from(page)`. Shape: `{ content, totalItems, totalPages, isFirst, isLast }`. The frontend reads
  `data.content`.

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
- Listener with `@EventListener` (no `@Async`) for synchronous execution within the same transaction (rollback on
  failure)
- Publish via `ApplicationEventPublisher.publishEvent(...)`

## Database

- Flyway migrations in `src/main/resources/db/migration/`
- Naming: `V{version}__{description}.sql`
- `spring.jpa.hibernate.ddl-auto=validate` (Flyway handles schema)
- JPA auditing enabled via `@EnableJpaAuditing`

## Testing

Two tiers, both run together by **Surefire** in `mvn test`. They live in separate root packages under
`src/test/java/com/lunisoft/javastarter/`, each mirroring the `src/main` package layout:

```
src/test/java/com/lunisoft/javastarter/
├── unit/                     # Pure Mockito, no Spring context
│   ├── support/TestFactory   # Detached entity builders
│   ├── core/...
│   └── module/[feature]/usecase/[Verb][Entity]UseCaseTest
└── integration/              # Full Spring context + Testcontainers
    ├── support/              # AbstractIntegrationTest, IntegrationTestFixtures
    └── module/[feature]/controller/[Entity]ControllerIntegrationTest
```

- **Unit tests** — package `unit.*`, suffix `*Test` (use cases are `[Verb][Entity]UseCaseTest`). Pure Mockito
  (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`), no Spring context. Build entities with
  `TestFactory` (detached, no DB).
- **Integration tests** — package `integration.*`, suffix `*IntegrationTest`. Boot the full context against
  Testcontainers Postgres + Redis by extending `AbstractIntegrationTest`; drive endpoints through `MockMvcTester`.
  Persist state with the `fixtures` (`givenX(...)`) helpers, not `TestFactory`. Docker must be running.

Run a single tier with `./mvnw test -Dtest='com.lunisoft.javastarter.unit.**'` (or `integration.**`).

### Integration test conventions

- **One class per controller**, named `[Entity]ControllerIntegrationTest`, in
  `integration.module.[feature].controller`.
- **One `@Nested` class per endpoint**, named after the action (`SendCode`, `VerifyCode`), each with a
  `/** HTTP_METHOD /api/path */` Javadoc and a `private static final String URL` constant.
- Shared `@Autowired` repositories and the `MockMvcTester` live on the outer class; nested classes reference them.
- **Test names**: `returns_<status>_when_<condition>` (snake_case), declared `throws Exception`.
- **Each endpoint covers at least** the happy path, the wrong-role case (`403`) and the unauthenticated case (`401`).
- **Arrange**: persist state with `fixtures.givenX(...)`, then build the body by serializing the endpoint's real
  request record: `jsonMapper.writeValueAsString(new XxxRequest(...))`. Never hand-write JSON strings.
- **Act**: always `MockMvcTester` (AssertJ), ending with `.exchange()` into a `result` variable. Authenticate with
  `.header(HttpHeaders.AUTHORIZATION, bearer(account))`. Do not use `mockMvc.perform(...)` / `andExpect(...)` /
  `andReturn()` in new tests.
- **Assert the status** with `assertThat(result).hasStatus(HttpStatus.XXX)` — the `HttpStatus` enum, not
  `hasStatusOk()` or a raw int.
- **Read the JSON body into an object** with
  `assertThat(result).bodyJson().convertTo(XxxResult.class).actual()` (the use case's `Result`/`View`, or the
  endpoint's response record). No `getContentAsString()` + `jsonMapper.readValue(...)`.
- **Assert the persisted DB state, always inside `assertPersistedState(() -> { ... })`** — every repository read and
  every assertion on an entity goes in the block, even when it only reads an id or a scalar column and no lazy
  association is traversed. Never assert on an entity outside of it. Reload the entity from its repository with the
  id returned by the endpoint (`repository.findById(response.id()).orElseThrow()`), then assert every field the
  request sets or the use case computes (relations by id, scalars, status).

Reference: `CustomerCleaningRequestControllerIntegrationTest`. New integration tests follow this shape exactly:

```java
public class CustomerCleaningRequestControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CleaningRequestRepository cleaningRequestRepository;

    @Autowired
    private MockMvcTester mockMvcTester;

    /** POST /api/customer/cleaning-requests */
    @Nested
    class CreateCleaningRequest {

        private static final String URL = "/api/customer/cleaning-requests";

        @Test
        void returns_200_when_creates_cleaning_request() throws Exception {
            var customer = fixtures.givenCustomer("customer@example.com");
            var account = customer.getAccount();
            var housekeeperServiceType = fixtures.givenServiceType("HOUSE");
            var interventionAddress = fixtures.givenInterventionAddress(customer);

            var body = jsonMapper.writeValueAsString(new CleaningRequestCreateRequest(
                    180.00, interventionAddress.getId(), housekeeperServiceType, "test commentaire"));

            var result = mockMvcTester
                    .post()
                    .uri(URL)
                    .header(HttpHeaders.AUTHORIZATION, bearer(account))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.OK);

            var response = assertThat(result)
                    .bodyJson()
                    .convertTo(CreateCleaningRequestResult.class)
                    .actual();

            assertPersistedState(() -> {
                var cleaningRequest =
                        cleaningRequestRepository.findById(response.id()).orElseThrow();

                assertThat(cleaningRequest.getCustomer().getId()).isEqualTo(customer.getId());
                assertThat(cleaningRequest.getServiceType().getKey()).isEqualTo(housekeeperServiceType.getKey());
                assertThat(cleaningRequest.getInterventionAddress().getId()).isEqualTo(interventionAddress.getId());
                assertThat(cleaningRequest.getDesiredDuration()).isEqualTo(180.00);
                assertThat(cleaningRequest.getComments()).isEqualTo("test commentaire");
                assertThat(cleaningRequest.getStatus()).isEqualTo(CleaningRequestStatus.PENDING);
            });
        }

        @Test
        void returns_403_when_user_role_is_not_customer() throws Exception {
            var housekeeper = fixtures.givenHousekeeper("housekeeper@example.com");
            var account = housekeeper.getAccount();
            var housekeeperServiceType = fixtures.givenServiceType("HOUSE");
            var body = jsonMapper.writeValueAsString(
                    new CleaningRequestCreateRequest(180.00, UUID.randomUUID(), housekeeperServiceType, null));

            var result = mockMvcTester
                    .post()
                    .uri(URL)
                    .header(HttpHeaders.AUTHORIZATION, bearer(account))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        void returns_401_when_user_not_authenticated() throws Exception {
            var body = jsonMapper.writeValueAsString(
                    new CleaningRequestCreateRequest(180.00, UUID.randomUUID(), null, null));

            var result = mockMvcTester
                    .post()
                    .uri(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }
}
```
