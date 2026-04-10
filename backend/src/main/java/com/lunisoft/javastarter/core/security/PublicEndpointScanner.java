package com.lunisoft.javastarter.core.security;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Scans all registered request mappings for {@link PublicEndpoint} (on class or method) and builds
 * a {@link RequestMatcher} that permits those routes without authentication.
 */
@Component
public class PublicEndpointScanner {

  private static final Logger log = LoggerFactory.getLogger(PublicEndpointScanner.class);

  private final RequestMatcher publicEndpointsMatcher;

  public PublicEndpointScanner(RequestMappingHandlerMapping handlerMapping) {
    List<RequestMatcher> matchers =
        handlerMapping.getHandlerMethods().entrySet().stream()
            .filter(entry -> isPublicEndpoint(entry.getValue()))
            .flatMap(entry -> toMatchers(entry.getKey()).stream())
            .toList();

    this.publicEndpointsMatcher = matchers.isEmpty() ? _ -> false : new OrRequestMatcher(matchers);

    log.info("Registered {} public endpoint(s)", matchers.size());
  }

  /** Returns a RequestMatcher that matches all @PublicEndpoint routes. */
  public RequestMatcher getRequestMatcher() {
    return publicEndpointsMatcher;
  }

  /** Checks if the handler method or its declaring class is annotated with @PublicEndpoint. */
  private boolean isPublicEndpoint(HandlerMethod handlerMethod) {
    return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), PublicEndpoint.class)
        || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), PublicEndpoint.class);
  }

  /** Converts a RequestMappingInfo into PathPatternRequestMatchers (one per method + pattern). */
  private List<RequestMatcher> toMatchers(RequestMappingInfo mappingInfo) {
    var methods = mappingInfo.getMethodsCondition().getMethods();
    var pathPatternsCondition = mappingInfo.getPathPatternsCondition();

    if (pathPatternsCondition == null) {
      return List.of();
    }

    var patterns = pathPatternsCondition.getPatterns();

    return patterns.stream()
        .flatMap(
            pattern -> {
              String path = pattern.getPatternString();

              // If no HTTP method constraint, match all methods for this path
              if (methods.isEmpty()) {
                log.debug("Public endpoint: * {}", path);

                return java.util.stream.Stream.of(PathPatternRequestMatcher.pathPattern(path));
              }

              return methods.stream()
                  .map(
                      method -> {
                        var httpMethod = org.springframework.http.HttpMethod.valueOf(method.name());
                        log.debug("Public endpoint: {} {}", httpMethod, path);

                        return PathPatternRequestMatcher.pathPattern(httpMethod, path);
                      });
            })
        .map(RequestMatcher.class::cast)
        .toList();
  }
}
