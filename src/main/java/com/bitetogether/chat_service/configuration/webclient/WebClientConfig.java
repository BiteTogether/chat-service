package com.bitetogether.chat_service.configuration.webclient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Configuration
@Slf4j
public class WebClientConfig {

  @Value("${webclient.user-service.url:http://localhost:8081}")
  private String userServiceUrl;

  @Bean
  public WebClient userServiceWebClient(WebClient.Builder builder) {
    HttpClient httpClient =
        HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofSeconds(5))
            .doOnConnected(
                conn ->
                    conn.addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS)));

    return builder
        .baseUrl(userServiceUrl)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .filter(forwardBearerToken())
        .filter(logRequest())
        .filter(logResponse())
        .build();
  }

  private ExchangeFilterFunction forwardBearerToken() {
    return (request, next) ->
        Mono.deferContextual(
            contextView -> {
              // Try to get authorization header from Reactor context
              if (contextView.hasKey("authorizationHeader")) {
                String authHeader = contextView.get("authorizationHeader");
                log.debug(
                    "Found Authorization header in context, forwarding to: {}", request.url());

                return next.exchange(
                    org.springframework.web.reactive.function.client.ClientRequest.from(request)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .build());
              }

              log.warn("No Authorization header in context for request to: {}", request.url());
              return next.exchange(request);
            });
  }

  private ExchangeFilterFunction logRequest() {
    return ExchangeFilterFunction.ofRequestProcessor(
        clientRequest -> {
          log.debug("WebClient Request: {} {}", clientRequest.method(), clientRequest.url());
          clientRequest
              .headers()
              .forEach(
                  (name, values) ->
                      values.forEach(
                          value -> {
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
    return ExchangeFilterFunction.ofResponseProcessor(
        clientResponse -> {
          log.debug("WebClient Response: Status {}", clientResponse.statusCode());
          return Mono.just(clientResponse);
        });
  }
}
