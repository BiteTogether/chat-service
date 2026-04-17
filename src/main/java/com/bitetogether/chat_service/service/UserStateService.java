package com.bitetogether.chat_service.service;

import com.bitetogether.chat_service.enums.websocket.UserState;
import reactor.core.publisher.Mono;

public interface UserStateService {

  Mono<Void> setUserState(Long userId, UserState state);

  Mono<Void> markOffline(Long userId);

  Mono<Void> markBackground(Long userId);
}
