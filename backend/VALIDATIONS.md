# Validation Reference

This document lists all available Bean Validation and Hibernate Validator annotations that can be used in this project, with examples.

> Dependency: `spring-boot-starter-validation` (Jakarta Bean Validation + Hibernate Validator)

---

## String Constraints

### @NotBlank

Rejects `null`, empty strings `""`, and whitespace-only strings `"   "`.

```java
public record CreateUserRequest(
    @NotBlank String name // "  " -> invalid, "John" -> valid
) {}
```

### @NotEmpty

Rejects `null` and empty strings `""`, but allows whitespace `"   "`.

```java
public record TagRequest(
    @NotEmpty String tag // "" -> invalid, "  " -> valid, "java" -> valid
) {}
```

### @Email

Validates that the string follows an email format.

```java
public record ContactRequest(
    @NotBlank @Email String email // "test" -> invalid, "test@mail.com" -> valid
) {}
```

### @Pattern

Validates that the string matches a regex pattern.

```java
public record PhoneRequest(
    @Pattern(regexp = "^\\+[0-9]{10,15}$", message = "Invalid phone number")
    String phone // "hello" -> invalid, "+33612345678" -> valid
) {}
```

---

## Size Constraints

### @Size

Validates the size of a String, Collection, Map, or Array.

```java
public record VerifyCodeRequest(
    @Size(min = 4, max = 4) String code // "12" -> invalid, "1234" -> valid
) {}
```

### @Length (Hibernate)

Same as `@Size` but specific to Strings. From `org.hibernate.validator.constraints`.

```java
@GetMapping("/customers")
public ResponseEntity<List<Customer>> search(
    @Length(min = 2, max = 2) @RequestParam String countryCode // "FRA" -> invalid, "FR" -> valid
) {}
```

---

## Number Constraints

### @Min / @Max

Validates that a number is greater/less than or equal to the specified value.

```java
public record PaginationRequest(
    @Min(1) int page,          // 0 -> invalid, 1 -> valid
    @Min(1) @Max(100) int size // 200 -> invalid, 50 -> valid
) {}
```

### @Positive / @PositiveOrZero

Validates that a number is strictly positive, or positive including zero.

```java
public record ProductRequest(
    @Positive BigDecimal price,      // -1 -> invalid, 0 -> invalid, 10.5 -> valid
    @PositiveOrZero int stock        // -1 -> invalid, 0 -> valid, 5 -> valid
) {}
```

### @Negative / @NegativeOrZero

Validates that a number is strictly negative, or negative including zero.

```java
public record AdjustmentRequest(
    @Negative int discount,          // 1 -> invalid, -5 -> valid
    @NegativeOrZero int correction   // 1 -> invalid, 0 -> valid, -3 -> valid
) {}
```

### @DecimalMin / @DecimalMax

Like `@Min`/`@Max` but for decimal values with string-based precision.

```java
public record PriceRequest(
    @DecimalMin("0.01") @DecimalMax("9999.99") BigDecimal amount // 0 -> invalid, 50.00 -> valid
) {}
```

### @Digits

Validates the number of integer and fraction digits.

```java
public record InvoiceRequest(
    @Digits(integer = 6, fraction = 2) BigDecimal total // 1234567.89 -> invalid, 1234.56 -> valid
) {}
```

---

## Null Constraints

### @NotNull

Rejects `null` values. Allows empty strings and zero.

```java
@GetMapping("/customers")
public ResponseEntity<List<Customer>> search(
    @NotNull @RequestParam Role role // missing param -> invalid, "CUSTOMER" -> valid
) {}
```

### @Null

Validates that the value is `null`. Useful for create vs update DTOs.

```java
public record CreateRequest(
    @Null UUID id // id must not be provided on creation
) {}
```

---

## Date/Time Constraints

### @Past / @PastOrPresent

Validates that a date is in the past.

```java
public record ProfileRequest(
    @Past LocalDate birthDate // tomorrow -> invalid, 2000-01-01 -> valid
) {}
```

### @Future / @FutureOrPresent

Validates that a date is in the future.

```java
public record EventRequest(
    @Future Instant startsAt // yesterday -> invalid, next week -> valid
) {}
```

---

## Boolean Constraints

### @AssertTrue / @AssertFalse

Validates that a boolean is true or false.

```java
public record RegisterRequest(
    @AssertTrue(message = "You must accept the terms")
    boolean termsAccepted // false -> invalid, true -> valid
) {}
```

---

## Collection Constraints

### @Size (on collections)

Validates the size of a Collection, Map, or Array.

```java
public record BulkRequest(
    @Size(min = 1, max = 10) List<UUID> ids // [] -> invalid, [id1, id2] -> valid
) {}
```

---

## Triggering Validation

### @Valid (on @RequestBody)

Triggers validation on DTO fields. Used on controller method parameters.

```java
@PostMapping("/users")
public ResponseEntity<User> create(@Valid @RequestBody CreateUserRequest request) {}
```

Validation errors throw `MethodArgumentNotValidException` -> **400 Bad Request**.

### @Validated (on controller class)

Enables validation on `@RequestParam` and `@PathVariable`. Applied at class level.

```java
@Validated
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    @GetMapping("/customers")
    public ResponseEntity<List<Customer>> search(
        @Length(min = 2, max = 2) @RequestParam String countryCode,
        @RequestParam Role role
    ) {}
}
```

Validation errors throw `ConstraintViolationException` -> **400 Bad Request**.

---

## Error Handling

All validation errors are caught by `GlobalExceptionHandler` and return a consistent JSON response:

```json
{
  "type": "ValidationException",
  "message": "must not be blank",
  "code": "VALIDATION_ERROR",
  "violations": [
    {
      "code": "NotBlank",
      "message": "must not be blank",
      "path": "email"
    }
  ]
}
```
