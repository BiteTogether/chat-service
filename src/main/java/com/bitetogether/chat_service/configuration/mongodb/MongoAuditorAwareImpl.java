package com.bitetogether.chat_service.configuration.mongodb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.bitetogether.common.util.UserContextUtils.getCurrentUserId;

@Component("mongoAuditorProvider")
@Slf4j
public class MongoAuditorAwareImpl implements AuditorAware<String> {

  @Override
  public Optional<String> getCurrentAuditor() {
    try {
      Long currentUserId = getCurrentUserId();

      if (currentUserId != null) {
        return Optional.of("USER_" + currentUserId);
      }

    } catch (Exception ex) {
      log.warn("Could not get current user ID for auditing", ex);
    }

    return Optional.of("SYSTEM");
  }
}
