package com.bitetogether.chat_service.configuration.websocket;

import static com.bitetogether.common.util.Constants.HEADER_USERNAME;
import static com.bitetogether.common.util.Constants.HEADER_USER_EMAIL;
import static com.bitetogether.common.util.Constants.HEADER_USER_ID;
import static com.bitetogether.common.util.Constants.HEADER_USER_ROLE;

import com.bitetogether.common.dto.UserContext;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Service to extract user context from WebSocket handshake. Supports both query parameters (for
 * browser clients) and headers (for API Gateway).
 */
@Slf4j
@Component
public class WebSocketAuthService {

  private static final String USER_ID_PARAM = "userId";

  /** Extract UserContext from WebSocket handshake info. Priority: Headers > Query Parameters */
  public Optional<UserContext> extractUserContext(URI uri, HttpHeaders headers) {
    // Try headers first (from API Gateway)
    Optional<UserContext> fromHeaders = extractFromHeaders(headers);
    if (fromHeaders.isPresent()) {
      log.debug("Extracted UserContext from headers");
      return fromHeaders;
    }

    // Fallback to query parameters (for direct WebSocket connections)
    Optional<UserContext> fromQuery = extractFromQueryParams(uri);
    if (fromQuery.isPresent()) {
      log.debug("Extracted UserContext from query parameters");
      return fromQuery;
    }

    log.warn("Could not extract UserContext from WebSocket handshake");
    return Optional.empty();
  }

  private Optional<UserContext> extractFromHeaders(HttpHeaders headers) {
    String userIdHeader = headers.getFirst(HEADER_USER_ID);
    if (userIdHeader == null) {
      return Optional.empty();
    }

    try {
      Long userId = Long.parseLong(userIdHeader);
      String role = Optional.ofNullable(headers.getFirst(HEADER_USER_ROLE)).orElse("USER");
      String email = Optional.ofNullable(headers.getFirst(HEADER_USER_EMAIL)).orElse("");
      String username =
          Optional.ofNullable(headers.getFirst(HEADER_USERNAME)).orElse("user_" + userId);

      return Optional.of(new UserContext(userId, role, email, username));
    } catch (NumberFormatException e) {
      log.warn("Invalid userId format in header: {}", userIdHeader);
      return Optional.empty();
    }
  }

  private Optional<UserContext> extractFromQueryParams(URI uri) {
    String query = uri.getQuery();
    if (query == null || query.isEmpty()) {
      return Optional.empty();
    }

    Map<String, String> params = parseQueryParams(query);
    String userIdParam = params.get(USER_ID_PARAM);
    if (userIdParam == null) {
      return Optional.empty();
    }

    try {
      Long userId = Long.parseLong(userIdParam);
      String role = params.getOrDefault("role", "USER");
      String email = params.getOrDefault("email", "");
      String username = params.getOrDefault("username", "user_" + userId);

      return Optional.of(new UserContext(userId, role, email, username));
    } catch (NumberFormatException e) {
      log.warn("Invalid userId format in query: {}", userIdParam);
      return Optional.empty();
    }
  }

  private Map<String, String> parseQueryParams(String query) {
    return Arrays.stream(query.split("&"))
        .map(param -> param.split("=", 2))
        .filter(parts -> parts.length == 2)
        .collect(
            java.util.stream.Collectors.toMap(
                parts -> parts[0], parts -> parts[1], (v1, v2) -> v1));
  }
}
