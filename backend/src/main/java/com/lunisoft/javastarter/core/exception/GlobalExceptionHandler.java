package com.lunisoft.javastarter.core.exception;

import com.lunisoft.javastarter.core.dto.ErrorResponse;
import com.lunisoft.javastarter.core.dto.Violation;
import com.lunisoft.javastarter.module.telegram.service.TelegramService;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.io.EOFException;
import java.util.Arrays;
import java.util.List;

/**
 * Global exception handler that produces consistent JSON error responses.
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final TelegramService telegramService;

    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * Handles custom business rule violations thrown from services/use cases.
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        ErrorResponse response = new ErrorResponse("BusinessRuleException", ex.getMessage(), ex.getCode(), null);

        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    /**
     * Handles uploads exceeding the configured multipart size limit (413).
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("Upload rejected: file too large", ex);

        ErrorResponse response =
                new ErrorResponse("PayloadTooLargeException", "The file is too large.", "CONTENT_TOO_LARGE", null);

        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(response);
    }

    /**
     * Handles a multipart request that could not be parsed. The usual case is a client that
     * disconnects mid-upload (user leaving the screen, mobile network drop, request larger than the
     * reverse proxy limit): nothing is wrong server-side and the response goes nowhere since the
     * connection is already closed, so it is logged as a one-line warning rather than an ERROR with
     * a full stack trace. A genuinely malformed multipart request is still logged with its stack.
     *
     * <p>Note: {@link MaxUploadSizeExceededException} extends {@link MultipartException} but is
     * handled by its own, more specific handler above.
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipart(MultipartException ex) {
        boolean isClientDisconnected =
                findCause(ex, ClientAbortException.class) != null || findCause(ex, EOFException.class) != null;

        if (isClientDisconnected) {
            log.warn("Upload aborted: the client closed the connection before sending the whole request body");
        } else {
            log.error("Failed to parse multipart request", ex);
        }

        ErrorResponse response = new ErrorResponse(
                "MultipartException", "The file upload could not be completed.", "UPLOAD_INCOMPLETE", null);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles errors when we send a wrong request type. Example: POST to a PATCH endpoint.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ErrorResponse response =
                new ErrorResponse("MethodNotSupportedException", ex.getMessage(), "METHOD_NOT_SUPPORTED", null);

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * Handles errors when we try to access a ressource that doesn't exist (404).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        ErrorResponse response = new ErrorResponse("NoResourceFoundException", ex.getMessage(), "NOT_FOUND", null);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles errors when we send a wrong Content-Type. Example: Expecting Content-Type
     * multipart/form-data but received application/json.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        ErrorResponse response = new ErrorResponse(
                "MediaTypeNotSupportedException",
                "Expected Content-Type %s but received Content-Type %s"
                        .formatted(ex.getSupportedMediaTypes(), ex.getContentType()),
                "CONTENT_TYPE_NOT_SUPPORTED",
                null);

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * Handles authorization failures thrown by method-level security (e.g. {@code @PreAuthorize}).
     * Returns 401 if the current request has no authenticated user, otherwise 403.
     *
     * <p>Method-level {@code @PreAuthorize} runs as an AOP interceptor around the controller call,
     * so
     * the {@code AccessDeniedException} it throws bubbles up through the {@code DispatcherServlet}
     * and reaches {@code @RestControllerAdvice} — unlike URL-level rules in {@code SecurityConfig},
     * which fail inside the security filter chain and are handled by
     * {@code RestAuthenticationEntryPoint} / {@code RestAccessDeniedHandler} instead.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken;

        if (isAnonymous) {
            ErrorResponse response =
                    new ErrorResponse("UnauthorizedException", "Authentication required", "UNAUTHORIZED", null);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        ErrorResponse response = new ErrorResponse("ForbiddenException", "Access denied", "FORBIDDEN", null);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handles unreadable request bodies — missing body, malformed JSON, or a field that cannot be
     * deserialized (e.g. {@code "regionId": "qsdsqdds"} for a UUID field).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {

        InvalidFormatException ife = findCause(ex, InvalidFormatException.class);

        if (ife != null && !ife.getPath().isEmpty()) {
            String field = ife.getPath().getLast().getPropertyName();
            String type = ife.getTargetType().getSimpleName();
            String message =
                    "Invalid value '%s' for field '%s'. Expected type: %s.".formatted(ife.getValue(), field, type);
            ErrorResponse response = new ErrorResponse(
                    "ValidationException",
                    message,
                    "VALIDATION_ERROR",
                    List.of(new Violation("InvalidFormat", message, field)));

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        ErrorResponse response = new ErrorResponse(
                "MessageNotReadableException",
                "Required request body is missing or malformed",
                "MESSAGE_NOT_READABLE",
                null);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles a response body that could not be written. The usual case is a client that closed the
     * connection before the whole body was sent (user navigating away or reloading, front-end
     * aborting the request, reverse proxy timeout): the request itself succeeded and nothing can be
     * returned since the socket is already gone, so it is logged as a one-line warning rather than
     * an ERROR with a full stack trace. Returning null tells Spring there is no response to write.
     *
     * <p>A genuine serialization failure (a getter that throws, an unmappable type) is still logged
     * with its stack and answered with a 500.
     */
    @ExceptionHandler({HttpMessageNotWritableException.class, AsyncRequestNotUsableException.class})
    public ResponseEntity<ErrorResponse> handleMessageNotWritable(Exception ex) {
        boolean isClientDisconnected = findCause(ex, ClientAbortException.class) != null
                || findCause(ex, AsyncRequestNotUsableException.class) != null;

        if (isClientDisconnected) {
            log.warn("Response aborted: the client closed the connection before the body was fully written");

            return null;
        }

        log.error("Failed to serialize the response body", ex);

        ErrorResponse response =
                new ErrorResponse("InternalServerError", "Internal server error", "INTERNAL_ERROR", null);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handles @Valid/@RequestBody validation failures (Bean Validation on DTOs). Example: POST
     * /api/auth/send-code with { "email": "" } triggers @NotBlank on SendCodeRequest.email
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<Violation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new Violation(error.getCode(), error.getDefaultMessage(), error.getField()))
                .toList();

        String message = violations.isEmpty()
                ? "Validation failed"
                : violations.getFirst().message();
        ErrorResponse response = new ErrorResponse("ValidationException", message, "VALIDATION_ERROR", violations);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles @Validated constraint violations on @RequestParam/@PathVariable. Example: GET
     * /api/demo/customers/paginated?page=0 (@Min(1) throws an error to enforce minimum value)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<Violation> violations = ex.getConstraintViolations().stream()
                .map(cv -> {
                    // Extract the parameter name from the property path (e.g. "searchCustomers.role"
                    // -> "role")
                    String path = cv.getPropertyPath().toString();
                    String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;

                    return new Violation("Invalid", cv.getMessage(), field);
                })
                .toList();

        String message = violations.isEmpty()
                ? "Validation failed"
                : violations.getFirst().message();
        ErrorResponse response = new ErrorResponse("ValidationException", message, "VALIDATION_ERROR", violations);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles missing required @RequestParam when no default value is set. Example: GET
     * /api/demo/customers (missing role query parameter)
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        ErrorResponse response = new ErrorResponse(
                "ValidationException",
                "Required parameter '%s' is missing.".formatted(ex.getParameterName()),
                "MISSING_PARAMETER",
                null);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles type conversion failures on @RequestParam/@PathVariable (e.g. invalid enum values).
     * Example: GET /api/demo/customers?role=test ("test" is not a valid Role enum value)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message;

        // Build a descriptive message for enum type mismatches
        Class<?> requiredType = ex.getRequiredType();
        if (requiredType != null && requiredType.isEnum()) {
            String allowedValues = String.join(
                    ", ",
                    Arrays.stream(requiredType.getEnumConstants())
                            .map(Object::toString)
                            .toArray(String[]::new));
            message = "Invalid value '%s' for parameter '%s'. Allowed values: %s"
                    .formatted(ex.getValue(), ex.getName(), allowedValues);
        } else {
            message = "Invalid value '%s' for parameter '%s'.".formatted(ex.getValue(), ex.getName());
        }

        ErrorResponse response = new ErrorResponse("ValidationException", message, "INVALID_PARAMETER", null);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Catch-all for any unhandled exception. Returns a generic 500 to avoid leaking internal details.
     * Example: database connection failure, NullPointerException, or any unexpected runtime error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);

        telegramService.sendMessage("[%s] Unhandled exception: %s".formatted(applicationName, ex.getMessage()));

        ErrorResponse response =
                new ErrorResponse("InternalServerError", "Internal server error", "INTERNAL_ERROR", null);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Walks the cause chain of {@code ex} and returns the first one matching {@code type}, or null.
     */
    @SuppressWarnings("unchecked")
    private <T extends Throwable> T findCause(Throwable ex, Class<T> type) {
        Throwable current = ex;
        while (current != null) {
            if (type.isInstance(current)) {
                return (T) current;
            }
            current = current.getCause() == current ? null : current.getCause();
        }

        return null;
    }
}
