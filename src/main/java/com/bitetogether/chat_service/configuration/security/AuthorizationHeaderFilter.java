package com.bitetogether.chat_service.configuration.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class AuthorizationHeaderFilter implements WebFilter {

  private static final String AUTH_HEADER_KEY = "authorizationHeader";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

    if (authHeader != null && !authHeader.isEmpty()) {
      log.debug("Authorization header captured: Bearer ***");
      return chain.filter(exchange).contextWrite(Context.of(AUTH_HEADER_KEY, authHeader));
    }

    log.warn("No Authorization header found in request to: {}", exchange.getRequest().getPath());
    return chain.filter(exchange);
  }
}
