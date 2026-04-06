package com.lunisoft.ultimatejavastarterkit.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunisoft.ultimatejavastarterkit.core.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Returns a 401 JSON response when an unauthenticated user hits a protected endpoint.
 *
 * <p>Invoked by Spring Security's {@code ExceptionTranslationFilter} for failures that happen
 * <b>inside the security filter chain</b>, before the request ever reaches the {@code
 * DispatcherServlet} (e.g. {@code .anyRequest().authenticated()} rejecting a request with no/invalid
 * JWT). Because these failures never reach a controller, {@code @RestControllerAdvice} cannot catch
 * them — that's why this handler is needed in addition to {@code GlobalExceptionHandler}.
 *
 * <p>Method-level {@code @PreAuthorize} failures, on the other hand, are thrown from the controller
 * call site and are handled by {@code GlobalExceptionHandler#handleAccessDenied}.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final ErrorResponse BODY =
      new ErrorResponse("UnauthorizedException", "Authentication required", "UNAUTHORIZED", null);

  @Override
  public void commence(
          @NonNull HttpServletRequest request,
          HttpServletResponse response,
          @NonNull AuthenticationException authException)
      throws IOException {
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    OBJECT_MAPPER.writeValue(response.getWriter(), BODY);
  }
}
