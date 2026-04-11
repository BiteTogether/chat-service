package com.bitetogether.chat_service.configuration.websocket;

import static com.bitetogether.common.util.Constants.HEADER_USERNAME;
import static com.bitetogether.common.util.Constants.HEADER_USER_EMAIL;
import static com.bitetogether.common.util.Constants.HEADER_USER_ID;
import static com.bitetogether.common.util.Constants.HEADER_USER_ROLE;

import com.bitetogether.common.dto.UserContext;
import java.net.URI;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebSocketAuthService {

  public Optional<UserContext> extractUserContext(URI uri, HttpHeaders headers) {
    Optional<UserContext> userContext = extractFromHeaders(headers);

    if (userContext.isEmpty()) {
      log.warn(
          "WebSocket authentication failed - missing or invalid headers. "
              + "Ensure connection goes through API Gateway. Path: {}",
          uri.getPath());
    } else {
      log.debug(
          "Successfully extracted UserContext from Gateway headers for userId={}",
          userContext.get().getUserId());
    }

    return userContext;
  }

  private Optional<UserContext> extractFromHeaders(HttpHeaders headers) {
    String userIdHeader = headers.getFirst(HEADER_USER_ID);

    if (userIdHeader == null || userIdHeader.isEmpty()) {
      log.warn(
          "Missing required header: {}. WebSocket must be accessed through API Gateway.",
          HEADER_USER_ID);
      return Optional.empty();
    }

    try {
      Long userId = Long.parseLong(userIdHeader);

      // Extract other user information from headers (with defaults)
      String role =
          Optional.ofNullable(headers.getFirst(HEADER_USER_ROLE))
              .filter(r -> !r.isEmpty())
              .orElse("USER");

      String email = Optional.ofNullable(headers.getFirst(HEADER_USER_EMAIL)).orElse("");

      String username =
          Optional.ofNullable(headers.getFirst(HEADER_USERNAME))
              .filter(u -> !u.isEmpty())
              .orElse("user_" + userId);

      UserContext context = new UserContext(userId, role, email, username);

      log.debug("Extracted UserContext: userId={}, role={}, username={}", userId, role, username);

      return Optional.of(context);

    } catch (NumberFormatException e) {
      log.error(
          "Invalid {} header value: '{}'. Must be a valid Long.", HEADER_USER_ID, userIdHeader);
      return Optional.empty();
    }
  }
}
