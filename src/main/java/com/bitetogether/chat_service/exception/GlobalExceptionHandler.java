package com.bitetogether.chat_service.exception;

import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.exception.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(AppException.class)
  public ResponseEntity<ApiResponseDTO<Void>> handleAppException(AppException exception) {
    ApiResponseDTO<Void> response = exception.getErrorCode().getResponse();
    return ResponseEntity.status(resolveHttpStatus(response.getStatus())).body(response);
  }

  @ExceptionHandler(WebExchangeBindException.class)
  public ResponseEntity<ApiResponseDTO<Void>> handleValidationException(
      WebExchangeBindException exception) {
    String message =
        exception.getFieldErrors().stream()
            .findFirst()
            .map(FieldError::getDefaultMessage)
            .orElse("Validation failed");

    return ResponseEntity.badRequest()
        .body(
            ApiResponseDTO.<Void>builder()
                .status(ApiResponseStatus.BAD_REQUEST.getCode())
                .message(message)
                .data(null)
                .build());
  }

  @ExceptionHandler(ServerWebInputException.class)
  public ResponseEntity<ApiResponseDTO<Void>> handleWebInputException(
      ServerWebInputException exception) {
    return ResponseEntity.badRequest()
        .body(
            ApiResponseDTO.<Void>builder()
                .status(ApiResponseStatus.BAD_REQUEST.getCode())
                .message("Invalid request payload")
                .data(null)
                .build());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponseDTO<Void>> handleAccessDeniedException(
      AccessDeniedException exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(
            ApiResponseDTO.<Void>builder()
                .status(ApiResponseStatus.FORBIDDEN.getCode())
                .message("Access denied")
                .data(null)
                .build());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponseDTO<Void>> handleUnhandledException(Exception exception) {
    log.error("Unhandled exception", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ApiResponseDTO.<Void>builder()
                .status(ApiResponseStatus.INTERNAL_SERVER_ERROR.getCode())
                .message(ApiResponseStatus.INTERNAL_SERVER_ERROR.getDefaultMessage())
                .data(null)
                .build());
  }

  private HttpStatus resolveHttpStatus(Integer statusCode) {
    HttpStatus resolved = HttpStatus.resolve(statusCode);
    return resolved != null ? resolved : HttpStatus.INTERNAL_SERVER_ERROR;
  }
}
