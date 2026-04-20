package com.lunisoft.javastarter.core.security;

import tools.jackson.databind.ObjectMapper;
import com.lunisoft.javastarter.core.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Returns a 403 JSON response when an authenticated user lacks the required permission.
 *
 * <p>Invoked by Spring Security's {@code ExceptionTranslationFilter} for failures that happen
 * <b>inside the security filter chain</b>, before the request ever reaches the {@code
 * DispatcherServlet} (e.g. an authenticated user blocked by a URL-level rule in {@code
 * SecurityConfig}). Because these failures never reach a controller, {@code @RestControllerAdvice}
 * cannot catch them — that's why this handler is needed in addition to {@code
 * GlobalExceptionHandler}.
 *
 * <p>Method-level {@code @PreAuthorize} failures, on the other hand, are thrown from the controller
 * call site and are handled by {@code GlobalExceptionHandler#handleAccessDenied}.
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

  private static final ErrorResponse BODY =
      new ErrorResponse("ForbiddenException", "Access denied", "FORBIDDEN", null);

  private final ObjectMapper objectMapper;

  @Override
  public void handle(
      @NonNull HttpServletRequest request,
      HttpServletResponse response,
      @NonNull AccessDeniedException accessDeniedException)
      throws IOException {
    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), BODY);
  }
}
