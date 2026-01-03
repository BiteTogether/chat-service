package com.bitetogether.chat_service.client.feign;

import com.bitetogether.chat_service.dto.UserDTO;
import com.bitetogether.chat_service.util.FeignClientExtension;
import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
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
    return userServiceWebClient
        .get()
        .uri(Constants.PREFIX_REQUEST_MAPPING_USER + "/{id}", id)
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            response -> FeignClientExtension.handleClientError(id, response, ENTITY_TYPE))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            response -> FeignClientExtension.handleServerError(id, response, ENTITY_TYPE))
        .bodyToMono(new ParameterizedTypeReference<ApiResponseDTO<UserDTO>>() {})
        .flatMap(apiResponse -> FeignClientExtension.extractData(apiResponse, ENTITY_TYPE))
        .timeout(java.time.Duration.ofSeconds(TIMEOUT_SECONDS))
        .onErrorResume(throwable -> FeignClientExtension.handleError(id, throwable, ENTITY_TYPE));
  }
}
