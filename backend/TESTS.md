# Testing Guide

## Stack

- **JUnit 5** — test framework
- **Mockito 5** — mocking
- **AssertJ** — fluent assertions

All dependencies come from `spring-boot-starter-webmvc-test`.

---

## Test Structure

Tests mirror the main source tree:

```
src/test/java/com/lunisoft/javastarter/
  module/
    auth/usecase/        → SendCodeUseCaseTest.java
    customer/usecase/    → CreateProfileUseCaseTest.java
    admin/usecase/       → GetStatsUseCaseTest.java
    demo/usecase/        → DemoSearchCustomerUseCaseTest.java
```

---

## Naming Convention

Method names follow the pattern: `method_scenario_expectedResult`

```java
execute_validCode_returnsAuthResponse()

execute_accountNotFound_throwsBusinessRuleException()

execute_cooldownNotExpired_throwsBusinessRuleException()
```

---

## Writing a Use Case Test

### 1. Class Setup

Every test class uses `@ExtendWith(MockitoExtension.class)` — no Spring context needed.

```java

@ExtendWith(MockitoExtension.class)
class MyUseCaseTest {

    // Mock all dependencies
    @Mock
    private SomeRepository someRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    // The class under test — NOT mocked
    private MyUseCase myUseCase;

    @BeforeEach
    void setUp() {
        // Instantiate with mocked dependencies
        myUseCase = new MyUseCase(someRepository, eventPublisher);
    }
}
```

### 2. Happy Path Test

```java

@Test
void execute_validInput_returnsExpectedResponse() {
    // Arrange — create test data via TestFactory and mock behavior
    var accountId = UUID.randomUUID();
    var account = createAccount(accountId, "test@example.com");

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

    // Act — call the method under test
    var result = myUseCase.execute(accountId);

    // Assert — verify the result
    assertThat(result.email()).isEqualTo("test@example.com");
    assertThat(result.role()).isEqualTo("CUSTOMER");
}
```

### 3. Error Path Test

Use cases throw `BusinessRuleException` for domain violations:

```java

@Test
void execute_entityNotFound_throwsBusinessRuleException() {
    var accountId = UUID.randomUUID();
    when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> myUseCase.execute(accountId))
            .isInstanceOf(BusinessRuleException.class)
            .satisfies(
                    ex -> {
                        var bre = (BusinessRuleException) ex;
                        assertThat(bre.getCode()).isEqualTo("NOT_FOUND");
                        assertThat(bre.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
}
```

### 4. Verifying Side Effects

Use `verify()` to check that repositories or event publishers were called:

```java

@Test
void execute_validInput_savesEntityAndPublishesEvent() {
    // ... arrange and act ...

    // Verify save was called
    verify(customerRepository).save(any(Customer.class));

    // Verify event was published
    verify(eventPublisher).publishEvent(any(CustomerEmailUpdatedEvent.class));
}
```

Use `never()` to verify something was NOT called:

```java
verify(customerRepository, never()).

save(any());

verify(eventPublisher, never()).

publishEvent(any());
```

### 5. Capturing Arguments

Use `ArgumentCaptor` to inspect what was passed to a mock:

```java

@Test
void execute_validInput_savesEntityWithCorrectFields() {
    // ... arrange and act ...

    var captor = ArgumentCaptor.forClass(Customer.class);
    verify(customerRepository).save(captor.capture());

    var savedCustomer = captor.getValue();
    assertThat(savedCustomer.getCountryCode()).isEqualTo("US");
    assertThat(savedCustomer.getAccount()).isEqualTo(account);
}
```

### 6. Mocking save() That Returns the Saved Entity

When the use case reads the ID from the saved entity:

```java
when(customerRepository.save(any(Customer.class)))
        .

thenAnswer(
        invocation ->{
var customer = invocation.getArgument(0, Customer.class);
          customer.

setId(UUID.randomUUID());

        return customer;
        });
```

---

## Common Patterns

### Testing with Instant / Time

Create tokens or entities with specific timestamps:

```java
// Recent — within cooldown
var recentToken = new VerificationToken();
recentToken.

setCreatedAt(Instant.now().

minusSeconds(10));

// Old — past cooldown
var oldToken = new VerificationToken();
oldToken.

setCreatedAt(Instant.now().

minusSeconds(61));

// Future expiration
        token.

setExpiresAt(Instant.now().

plus(15,ChronoUnit.MINUTES));
```

### Testing Paginated Results

Use `PageImpl` to mock Spring Data `Page` results:

```java
var pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
var page = new PageImpl<>(List.of(customer1, customer2), pageable, 2);

when(repository.findAll(any(Specification.class),eq(pageable))).

thenReturn(page);
```

### Testing with HttpServletRequest

Mock the servlet request for use cases that need it:

```java

@Mock
private HttpServletRequest request;

// In test method:
when(request.getRemoteAddr()).

thenReturn("127.0.0.1");

when(request.getHeader("User-Agent")).

thenReturn("TestAgent/1.0");
```

### TestFactory — Shared Entity Builders

All test entities are created via `TestFactory` — a shared utility class that avoids
duplicating helper methods across test files.

**Location**: `src/test/java/.../shared/TestFactory.java`

**Import with static imports**:

```java
import static com.lunisoft.javastarter.shared.TestFactory.*;
```

**Available methods**:

```java
// Account
createAccount("test@example.com")                  // default CUSTOMER role

createAccount("test@example.com",Role.ADMIN)       // specific role

createAccount(accountId, "test@example.com")        // specific UUID

// Customer
createCustomer(account, "FR")                       // from existing account

createCustomer("test@example.com","FR",Role.CUSTOMER) // creates account too

// Session
createSession(account)                              // random ID, 7-day expiry

createSession(sessionId, account)                   // specific UUID

// VerificationToken
createVerificationToken(account, "1234",0)         // code + attempt count
```

**Adding a new factory method**: if your test needs a new entity, add it to `TestFactory`
so other tests can reuse it.

---

## Common Imports

```java
// Test entity builders

import static com.lunisoft.javastarter.shared.TestFactory.*;

// Assertions
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Mockito
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// JUnit 5
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
```

---

## Checklist for a New Use Case Test

1. Create test class in the matching test package
2. Add `@ExtendWith(MockitoExtension.class)`
3. `@Mock` all dependencies, instantiate the use case in `@BeforeEach`
4. Write a happy path test (valid input → expected output)
5. Write error path tests (not found, already exists, expired, etc.)
6. Verify side effects (saves, deletes, events published)
7. Run: `./mvnw test -Dtest="com.lunisoft.javastarter.module.**.*UseCaseTest"`

---

## Running Tests

```bash
# All use case tests
./mvnw test -Dtest="com.lunisoft.javastarter.module.**.*UseCaseTest"

# Single test class
./mvnw test -Dtest="SendCodeUseCaseTest"

# Single test method
./mvnw test -Dtest="SendCodeUseCaseTest#execute_existingAccount_sendsCode"

# All tests
./mvnw test
```
