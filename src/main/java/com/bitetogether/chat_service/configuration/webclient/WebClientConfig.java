package com.bitetogether.chat_service.configuration.webclient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class WebClientConfig {

    @Value("${webclient.user-service.url:http://localhost:8081}")
    private String userServiceUrl;

    @Bean
    public WebClient userServiceWebClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(5))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS)));

        return builder
                .baseUrl(userServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(jwtTokenPropagationFilter())
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    /**
     * Filter to automatically propagate JWT token from incoming request to outgoing WebClient request.
     * Checks multiple context keys to find the Authorization header.
     */
    private ExchangeFilterFunction jwtTokenPropagationFilter() {
        return (clientRequest, next) ->
            Mono.deferContextual(contextView -> {
                log.info("🔍 JWT Filter - Checking for auth header in context");
                log.info("Context keys available: {}", contextView);

                String authHeader = null;

                // Strategy 1: Check for AUTH_HEADER directly (simplest)
                if (contextView.hasKey(ServerWebExchangeContextFilter.AUTH_HEADER_KEY)) {
                    authHeader = contextView.get(ServerWebExchangeContextFilter.AUTH_HEADER_KEY);
                    log.info("✅ Found auth header directly from AUTH_HEADER_KEY");
                }
                // Strategy 2: Extract from ServerWebExchange (class key)
                else if (contextView.hasKey(ServerWebExchange.class)) {
                    ServerWebExchange exchange = contextView.get(ServerWebExchange.class);
                    authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                    log.info("✅ Found auth header from ServerWebExchange (class key)");
                }
                // Strategy 3: Extract from ServerWebExchange (custom key)
                else if (contextView.hasKey(ServerWebExchangeContextFilter.EXCHANGE_CONTEXT_KEY)) {
                    ServerWebExchange exchange = contextView.get(ServerWebExchangeContextFilter.EXCHANGE_CONTEXT_KEY);
                    authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                    log.info("✅ Found auth header from ServerWebExchange (custom key)");
                }
                else {
                    log.error("❌ No ServerWebExchange or AUTH_HEADER found in context!");
                }

                // If we have an auth header, propagate it
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    log.info("✅ Propagating JWT token to user-service: Bearer ***");
                    ClientRequest modifiedRequest = ClientRequest.from(clientRequest)
                            .header(HttpHeaders.AUTHORIZATION, authHeader)
                            .build();
                    return next.exchange(modifiedRequest);
                } else {
                    log.warn("❌ No valid Authorization header found (header: {})",
                            authHeader != null ? authHeader.substring(0, Math.min(10, authHeader.length())) + "..." : "NULL");
                }

                log.warn("⚠️ Proceeding without auth header to: {}", clientRequest.url());
                return next.exchange(clientRequest);
            });
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.debug("WebClient Request: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers().forEach((name, values) ->
                    values.forEach(value -> {
                        if (name.equalsIgnoreCase("Authorization")) {
                            log.debug("Header: {}=Bearer ***", name);
                        } else {
                            log.debug("Header: {}={}", name, value);
                        }
                    }));
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.debug("WebClient Response: Status {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }
}

