package com.bitetogether.chat_service.configuration.webclient;

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

/**
 * WebFilter to add ServerWebExchange and Authorization header to the reactive context.
 * This allows downstream components (like WebClient filters) to access the incoming request.
 * Must run early in the filter chain.
 */
@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ServerWebExchangeContextFilter implements WebFilter {

    public static final String EXCHANGE_CONTEXT_KEY = "SERVER_WEB_EXCHANGE";
    public static final String AUTH_HEADER_KEY = "AUTH_HEADER";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        log.info("🔍 Filter intercepted request: {} {} with Auth: {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                authHeader != null ? "Bearer ***" : "NONE");

        // Store both the exchange and the auth header directly in context
        Context context = Context.of(EXCHANGE_CONTEXT_KEY, exchange)
                .put(ServerWebExchange.class, exchange);

        if (authHeader != null) {
            context = context.put(AUTH_HEADER_KEY, authHeader);
            log.info("✅ Stored Authorization header in context");
        } else {
            log.warn("⚠️ No Authorization header found in incoming request");
        }

        return chain.filter(exchange)
                .contextWrite(context);
    }
}

