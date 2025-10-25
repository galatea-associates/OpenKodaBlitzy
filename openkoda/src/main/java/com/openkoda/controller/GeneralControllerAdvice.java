package com.openkoda.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Spring @ControllerAdvice exposing common request attributes to all views.
 * <p>
 * Global controller advice providing @ModelAttribute methods that execute for every request.
 * Exposes currentUri (servlet path) and queryString (raw query) to all Thymeleaf templates.
 * Stateless and side-effect-free.
 * </p>
 * <p>
 * Security-sensitive: query strings can contain secrets/tokens - use carefully in templates.
 * Thread-safety: Stateless advice, thread-safe. Each request gets own method invocation.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see org.springframework.web.bind.annotation.ControllerAdvice
 * @see org.springframework.web.bind.annotation.ModelAttribute
 */
@ControllerAdvice
public class GeneralControllerAdvice {

  /**
   * Provides current request path to all views.
   * <p>
   * Calls {@code request.getServletPath()}, exposes as model attribute for navigation highlighting,
   * breadcrumbs, canonical URLs. Available in all Thymeleaf templates as {@code currentUri}.
   * </p>
   * <p>
   * Example usage in Thymeleaf:
   * <pre>{@code
   * th:classappend="${currentUri == '/admin' ? 'active' : ''}"
   * }</pre>
   * </p>
   *
   * @param request Current HTTP request
   * @return Servlet path (e.g., "/users/123/edit"), never null
   */
  @ModelAttribute("currentUri")
  String getRequestServletPath(HttpServletRequest request) {
    return request.getServletPath();
  }

  /**
   * Provides raw query string to all views.
   * <p>
   * Calls {@code request.getQueryString()}, exposes for URL reconstruction, pagination link building.
   * Available in all Thymeleaf templates as {@code queryString}.
   * </p>
   * <p>
   * <b>Security Warning:</b> Query strings may contain sensitive data (tokens, passwords) - sanitize
   * before rendering in HTML to prevent exposure.
   * </p>
   * <p>
   * Example usage - construct pagination links preserving filters:
   * <pre>{@code
   * ${currentUri}?${queryString}&page=3
   * }</pre>
   * </p>
   *
   * @param request Current HTTP request
   * @return Query string without '?' (e.g., "page=2&sort=name"), or null if no query string
   */
  @ModelAttribute("queryString")
  String getRequestQueryString(HttpServletRequest request) {
    return request.getQueryString();
  }
}

