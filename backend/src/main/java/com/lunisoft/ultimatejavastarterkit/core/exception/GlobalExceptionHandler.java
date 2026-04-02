package com.lunisoft.ultimatejavastarterkit.core.exception;

import com.lunisoft.ultimatejavastarterkit.core.dto.ErrorResponse;
import com.lunisoft.ultimatejavastarterkit.core.dto.Violation;
import jakarta.validation.ConstraintViolationException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Global exception handler that produces consistent JSON error responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {
  private final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /** Handles custom business rule violations thrown from services/use cases. */
  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
    ErrorResponse response =
        new ErrorResponse("BusinessRuleException", ex.getMessage(), ex.getCode(), null);

    return ResponseEntity.status(ex.getStatus()).body(response);
  }

  /**
   * Handles body validation failures (POST's or PATCH's body is not set). Example: POST
   * /api/auth/send-code with { } triggers the MessageNotReadableException.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMessageNotReadable(
      HttpMessageNotReadableException ex) {
    ErrorResponse response =
        new ErrorResponse(
            "MessageNotReadableException",
            "Required request body is missing",
            "MESSAGE_NOT_READABLE",
            null);

    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
  }

  /** Handles errors when we send a wrong request type. Example: POST to a PATCH endpoint. */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex) {
    ErrorResponse response =
        new ErrorResponse(
            "MethodNotSupportedException", ex.getMessage(), "METHOD_NOT_SUPPORTED", null);

    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
  }

  /**
   * Handles @Valid/@RequestBody validation failures (Bean Validation on DTOs). Example: POST
   * /api/auth/send-code with { "email": "" } triggers @NotBlank on SendCodeRequest.email
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    List<Violation> violations =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                error ->
                    new Violation(error.getCode(), error.getDefaultMessage(), error.getField()))
            .toList();

    String message = violations.isEmpty() ? "Validation failed" : violations.getFirst().message();
    ErrorResponse response =
        new ErrorResponse("ValidationException", message, "VALIDATION_ERROR", violations);

    return ResponseEntity.badRequest().body(response);
  }

  /**
   * Handles @Validated constraint violations on @RequestParam/@PathVariable. Example: GET
   * /api/demo/customers?countryCode=FRS (@Length(min = 2, max = 2) throws an error to limit the
   * length to 2 characters)
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
    List<Violation> violations =
        ex.getConstraintViolations().stream()
            .map(
                cv -> {
                  // Extract the parameter name from the property path (e.g. "searchCustomers.role"
                  // -> "role")
                  String path = cv.getPropertyPath().toString();
                  String field =
                      path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;

                  return new Violation("Invalid", cv.getMessage(), field);
                })
            .toList();

    String message = violations.isEmpty() ? "Validation failed" : violations.getFirst().message();
    ErrorResponse response =
        new ErrorResponse("ValidationException", message, "VALIDATION_ERROR", violations);

    return ResponseEntity.badRequest().body(response);
  }

  /**
   * Handles missing required @RequestParam when no default value is set. Example: GET
   * /api/demo/customers (missing both countryCode and role query parameters)
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParam(
      MissingServletRequestParameterException ex) {
    ErrorResponse response =
        new ErrorResponse(
            "ValidationException",
            "Required parameter '" + ex.getParameterName() + "' is missing.",
            "MISSING_PARAMETER",
            null);

    return ResponseEntity.badRequest().body(response);
  }

  /**
   * Handles type conversion failures on @RequestParam/@PathVariable (e.g. invalid enum values).
   * Example: GET /api/demo/customers?countryCode=FR&role=test ("test" is not a valid Role enum
   * value)
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    String message;

    // Build a descriptive message for enum type mismatches
    Class<?> requiredType = ex.getRequiredType();
    if (requiredType != null && requiredType.isEnum()) {
      String allowedValues =
          String.join(
              ", ",
              Arrays.stream(requiredType.getEnumConstants())
                  .map(Object::toString)
                  .toArray(String[]::new));
      message =
          "Invalid value '"
              + ex.getValue()
              + "' for parameter '"
              + ex.getName()
              + "'. Allowed values: "
              + allowedValues;
    } else {
      message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'.";
    }

    ErrorResponse response =
        new ErrorResponse("ValidationException", message, "INVALID_PARAMETER", null);

    return ResponseEntity.badRequest().body(response);
  }

  /**
   * Catch-all for any unhandled exception. Returns a generic 500 to avoid leaking internal details.
   * Example: database connection failure, NullPointerException, or any unexpected runtime error
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
    log.error("Unhandled exception", ex);

    ErrorResponse response =
        new ErrorResponse("InternalServerError", "Internal server error", "INTERNAL_ERROR", null);

    return ResponseEntity.internalServerError().body(response);
  }
}
