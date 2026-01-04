package com.bitetogether.chat_service.configuration.reactive;

import static com.bitetogether.common.util.Constants.HEADER_USERNAME;
import static com.bitetogether.common.util.Constants.HEADER_USER_EMAIL;
import static com.bitetogether.common.util.Constants.HEADER_USER_ID;
import static com.bitetogether.common.util.Constants.HEADER_USER_ROLE;
import static com.bitetogether.common.util.Constants.USER_CONTEXT_KEY;

import com.bitetogether.common.dto.UserContext;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Slf4j
public class JwtHeaderWebFluxFilter implements WebFilter {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String userIdHeader = exchange.getRequest().getHeaders().getFirst(HEADER_USER_ID);
    String role = exchange.getRequest().getHeaders().getFirst(HEADER_USER_ROLE);
    String email = exchange.getRequest().getHeaders().getFirst(HEADER_USER_EMAIL);
    String username = exchange.getRequest().getHeaders().getFirst(HEADER_USERNAME);

    if (Objects.nonNull(userIdHeader)) {
      try {
        Long userId = Long.parseLong(userIdHeader);

        // Use default values if headers are missing
        String finalRole = Objects.nonNull(role) ? role : "USER";
        String finalEmail = Objects.nonNull(email) ? email : "unknown@example.com";
        String finalUsername = Objects.nonNull(username) ? username : "user_" + userId;

        UserContext context = new UserContext(userId, finalRole, finalEmail, finalUsername);

        log.debug("Storing UserContext for userId={}", userId);

        return chain.filter(exchange).contextWrite(ctx -> ctx.put(USER_CONTEXT_KEY, context));
      } catch (NumberFormatException e) {
        log.warn("Invalid userId format: {}", userIdHeader);
        return chain.filter(exchange);
      }
    }

    log.debug("Missing userId header, continuing without UserContext");
    return chain.filter(exchange);
  }
}
