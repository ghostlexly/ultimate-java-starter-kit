package com.lunisoft.ultimatejavastarterkit.core.exception;

import com.lunisoft.ultimatejavastarterkit.core.dto.ErrorResponse;
import com.lunisoft.ultimatejavastarterkit.core.dto.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/** Global exception handler that produces consistent JSON error responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
    ErrorResponse response =
        new ErrorResponse("BusinessRuleException", ex.getMessage(), ex.getCode(), null);

    return ResponseEntity.status(ex.getStatus()).body(response);
  }

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

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
    log.error("Unhandled exception", ex);

    ErrorResponse response =
        new ErrorResponse("InternalServerError", "Internal server error", "INTERNAL_ERROR", null);

    return ResponseEntity.internalServerError().body(response);
  }
}
