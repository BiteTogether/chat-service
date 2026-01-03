package com.bitetogether.chat_service.repository.httpclient;

import com.bitetogether.chat_service.dto.UserDTO;
import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.ConnectException;

/**
 * Reactive HTTP client for User Service using WebClient.
 * Replaces the blocking OpenFeign client with a fully reactive implementation.
 * JWT token is automatically propagated via WebClient filter.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserClient {

    private final WebClient userServiceWebClient;

    /**
     * Fetches user information by ID in a reactive, non-blocking manner.
     * Accepts an explicit Authorization header for token propagation.
     *
     * @param id The user ID
     * @param authorization The Authorization header (e.g., "Bearer <token>")
     * @return Mono containing the UserDTO, or empty Mono if user not found or error occurs (graceful degradation)
     */
    public Mono<UserDTO> getUserById(Long id, String authorization) {
        return buildRequest(id, authorization)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                    response -> handleClientError(id, response))
                .onStatus(HttpStatusCode::is5xxServerError,
                    response -> handleServerError(id, response))
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<UserDTO>>() {})
                .flatMap(this::extractUserData)
                .timeout(java.time.Duration.ofSeconds(5))
                .onErrorResume(throwable -> handleError(id, throwable));
    }

    /**
     * Builds the WebClient request with optional authorization header
     */
    private WebClient.RequestHeadersSpec<?> buildRequest(Long id, String authorization) {
        WebClient.RequestHeadersSpec<?> spec = userServiceWebClient
                .get()
                .uri(Constants.PREFIX_REQUEST_MAPPING_USER + "/{id}", id);

        if (authorization != null && !authorization.isEmpty()) {
            spec = spec.header(HttpHeaders.AUTHORIZATION, authorization);
            log.debug("Adding Authorization header to user-service request for user {}", id);
        } else {
            log.debug("No Authorization header provided for user {} request", id);
        }

        return spec;
    }

    /**
     * Handles 4xx client errors from user-service
     */
    private Mono<Throwable> handleClientError(Long id, org.springframework.web.reactive.function.client.ClientResponse response) {
        HttpStatus status = HttpStatus.resolve(response.statusCode().value());

        if (response.statusCode() == HttpStatus.NOT_FOUND) {
            log.warn("User not found: {}", id);
            return Mono.error(new UserNotFoundException("User not found with id: " + id));
        }

        if (response.statusCode() == HttpStatus.UNAUTHORIZED) {
            log.warn("Unauthorized access to user-service for user: {}", id);
            return Mono.error(new UnauthorizedException("Unauthorized access for user id: " + id));
        }

        log.warn("Client error fetching user {}: {}", id, status);
        return Mono.error(new UserServiceException("Client error: " + status));
    }

    /**
     * Handles 5xx server errors from user-service
     */
    private Mono<Throwable> handleServerError(Long id, org.springframework.web.reactive.function.client.ClientResponse response) {
        HttpStatus status = HttpStatus.resolve(response.statusCode().value());
        log.error("Server error from user-service fetching user {}: {}", id, status);
        return Mono.error(new UserServiceException("User service error: " + status));
    }

    /**
     * Extracts UserDTO from ApiResponse wrapper
     */
    private Mono<UserDTO> extractUserData(ApiResponse<UserDTO> apiResponse) {
        if (apiResponse.getStatus() == ApiResponseStatus.SUCCESS.getCode()
                && apiResponse.getData() != null) {
            return Mono.just(apiResponse.getData());
        }

        log.warn("Failed to fetch user info: {}", apiResponse.getMessage());
        return Mono.error(new UserServiceException("Failed to fetch user info: " + apiResponse.getMessage()));
    }

    /**
     * Centralized error handling with graceful degradation.
     * Returns empty Mono for recoverable errors to allow message sending without user info.
     */
    private Mono<UserDTO> handleError(Long id, Throwable throwable) {
        String errorType = throwable.getClass().getSimpleName();

        // Network/Connection errors - service unavailable (recoverable)
        if (isNetworkError(throwable)) {
            log.warn("Cannot reach user-service for user {}: {} - continuing without user info",
                    id, errorType);
            return Mono.empty();
        }

        // Timeout errors (recoverable)
        if (isTimeoutError(throwable)) {
            log.warn("Timeout fetching user {}: {} - continuing without user info",
                    id, errorType);
            return Mono.empty();
        }

        // Business errors (recoverable)
        if (throwable instanceof UserNotFoundException) {
            log.warn("User {} not found - continuing without user info", id);
            return Mono.empty();
        }

        if (throwable instanceof UnauthorizedException) {
            log.warn("Unauthorized access for user {} - continuing without user info", id);
            return Mono.empty();
        }

        if (throwable instanceof UserServiceException) {
            log.warn("User service error for user {}: {} - continuing without user info",
                    id, throwable.getMessage());
            return Mono.empty();
        }

        // Unexpected errors - log with full stack trace and degrade gracefully
        log.error("Unexpected error fetching user {}: {}", id, throwable.getMessage(), throwable);
        return Mono.empty();
    }

    /**
     * Checks if the error is a network/connection related error
     */
    private boolean isNetworkError(Throwable throwable) {
        return throwable instanceof ConnectException
                || throwable instanceof org.springframework.web.reactive.function.client.WebClientRequestException
                || throwable.getClass().getName().contains("ConnectTimeout");
    }

    /**
     * Checks if the error is a timeout related error
     */
    private boolean isTimeoutError(Throwable throwable) {
        String className = throwable.getClass().getName();
        return className.contains("Timeout") || className.contains("timeout");
    }

    /**
     * Custom exception for user not found scenarios
     */
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Custom exception for user service errors
     */
    public static class UserServiceException extends RuntimeException {
        public UserServiceException(String message) {
            super(message);
        }
    }

    /**
     * Custom exception for unauthorized access
     */
    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }
}

