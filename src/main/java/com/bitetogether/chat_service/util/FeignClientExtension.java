package com.bitetogether.chat_service.util;

import com.bitetogether.chat_service.exception.ErrorCode;
import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.exception.AppException;
import java.net.ConnectException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

@Slf4j
public final class FeignClientExtension {

  private FeignClientExtension() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static Mono<Throwable> handleClientError(
      Long id, ClientResponse response, String entityType) {
    HttpStatus status = HttpStatus.resolve(response.statusCode().value());

    if (response.statusCode() == HttpStatus.NOT_FOUND) {
      log.warn("{} not found: {}", entityType, id);
      return Mono.error(new AppException(ErrorCode.USER_NOT_FOUND));
    }

    if (response.statusCode() == HttpStatus.UNAUTHORIZED) {
      log.warn(
          "Unauthorized access to {}-service for {}: {}",
          entityType.toLowerCase(),
          entityType.toLowerCase(),
          id);
      return Mono.error(new AppException(ErrorCode.USER_SERVICE_UNAUTHORIZED));
    }

    log.warn("Client error fetching {} {}: {}", entityType.toLowerCase(), id, status);
    return Mono.error(new AppException(ErrorCode.USER_SERVICE_ERROR));
  }

  public static Mono<Throwable> handleServerError(
      Long id, ClientResponse response, String entityType) {
    HttpStatus status = HttpStatus.resolve(response.statusCode().value());
    log.error(
        "Server error from {}-service fetching {} {}: {}",
        entityType.toLowerCase(),
        entityType.toLowerCase(),
        id,
        status);
    return Mono.error(new AppException(ErrorCode.USER_SERVICE_ERROR));
  }

  public static <T> Mono<T> extractData(ApiResponseDTO<T> apiResponse, String entityType) {
    if (apiResponse.getStatus() == ApiResponseStatus.SUCCESS.getCode()
        && apiResponse.getData() != null) {
      return Mono.just(apiResponse.getData());
    }

    log.warn("Failed to fetch {} info: {}", entityType.toLowerCase(), apiResponse.getMessage());
    return Mono.error(new AppException(ErrorCode.USER_SERVICE_ERROR));
  }

  public static <T> Mono<T> handleError(Long id, Throwable throwable, String entityType) {
    String errorType = throwable.getClass().getSimpleName();

    if (isNetworkError(throwable)) {
      log.warn(
          "Cannot reach {}-service for {} {}: {} - continuing without {} info",
          entityType.toLowerCase(),
          entityType.toLowerCase(),
          id,
          errorType,
          entityType.toLowerCase());
      return Mono.empty();
    }

    if (isTimeoutError(throwable)) {
      log.warn(
          "Timeout fetching {} {}: {} - continuing without {} info",
          entityType.toLowerCase(),
          id,
          errorType,
          entityType.toLowerCase());
      return Mono.empty();
    }

    if (throwable instanceof AppException appException) {
      if (appException.getErrorCode() == ErrorCode.USER_NOT_FOUND) {
        log.warn(
            "{} {} not found - continuing without {} info",
            entityType,
            id,
            entityType.toLowerCase());
        return Mono.empty();
      }
      if (appException.getErrorCode() == ErrorCode.USER_SERVICE_UNAUTHORIZED) {
        log.warn(
            "Unauthorized access for {} {} - continuing without {} info",
            entityType.toLowerCase(),
            id,
            entityType.toLowerCase());
        return Mono.empty();
      }
      if (appException.getErrorCode() == ErrorCode.USER_SERVICE_ERROR) {
        log.warn(
            "{} service error for {} {}: {} - continuing without {} info",
            entityType,
            entityType.toLowerCase(),
            id,
            throwable.getMessage(),
            entityType.toLowerCase());
        return Mono.empty();
      }
    }

    log.error(
        "Unexpected error fetching {} {}: {}",
        entityType.toLowerCase(),
        id,
        throwable.getMessage(),
        throwable);
    return Mono.empty();
  }

  public static boolean isNetworkError(Throwable throwable) {
    return throwable instanceof ConnectException
        || throwable
            instanceof org.springframework.web.reactive.function.client.WebClientRequestException
        || throwable.getClass().getName().contains("ConnectTimeout");
  }

  public static boolean isTimeoutError(Throwable throwable) {
    String className = throwable.getClass().getName();
    return className.contains("Timeout") || className.contains("timeout");
  }
}
