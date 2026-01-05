package com.bitetogether.chat_service.configuration.mongodb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component("mongoAuditorProvider")
@Slf4j
public class MongoAuditorAwareImpl implements ReactiveAuditorAware<String> {

  @Override
  public Mono<String> getCurrentAuditor() {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .filter(Authentication::isAuthenticated)
        .map(Authentication::getName)
        .map(username -> "USER_" + username)
        .defaultIfEmpty("SYSTEM")
        .doOnError(ex -> log.warn("Could not get current user ID for auditing", ex))
        .onErrorReturn("SYSTEM");
  }
}
