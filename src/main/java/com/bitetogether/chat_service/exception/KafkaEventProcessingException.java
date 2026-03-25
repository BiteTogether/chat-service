package com.bitetogether.chat_service.exception;

public class KafkaEventProcessingException extends RuntimeException {

  public KafkaEventProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
