package com.bitetogether.chat_service.client.webclient;

import com.bitetogether.chat_service.dto.user.ListUserDetailDTO;
import com.bitetogether.chat_service.dto.user.UserDTO;
import com.bitetogether.chat_service.util.WebClientExtension;
import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.util.Constants;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserClient {

  private static final String ENTITY_TYPE = "User";
  private static final int TIMEOUT_SECONDS = 5;

  private final WebClient userServiceWebClient;

  public Mono<UserDTO> getUserById(Long id) {
    return WebClientExtension.executeRequest(
        userServiceWebClient.get().uri(Constants.PREFIX_REQUEST_MAPPING_USER + "/{id}", id),
        new ParameterizedTypeReference<ApiResponseDTO<UserDTO>>() {},
        id,
        ENTITY_TYPE,
        TIMEOUT_SECONDS);
  }

  public Mono<ListUserDetailDTO> getListUsersByIds(List<Long> userIds) {
    return WebClientExtension.executeRequest(
        userServiceWebClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path(Constants.PREFIX_REQUEST_MAPPING_USER)
                        .queryParam("userIds", userIds)
                        .build()),
        new ParameterizedTypeReference<ApiResponseDTO<ListUserDetailDTO>>() {},
        null,
        ENTITY_TYPE,
        TIMEOUT_SECONDS);
  }
}
