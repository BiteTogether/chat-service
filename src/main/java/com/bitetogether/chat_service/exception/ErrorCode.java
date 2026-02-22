package com.bitetogether.chat_service.exception;

import static com.bitetogether.common.enums.ApiResponseStatus.getDefaultMessage;

import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.exception.BaseErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode implements BaseErrorCode {
  // Message errors
  MESSAGE_NOT_FOUND(ApiResponseStatus.NOT_FOUND, "Message not found"),
  MESSAGE_UPDATE_UNAUTHORIZED(
      ApiResponseStatus.FORBIDDEN, "You are not authorized to update this message"),
  MESSAGE_DELETE_UNAUTHORIZED(
      ApiResponseStatus.FORBIDDEN, "You are not authorized to delete this message"),

  // Conversation errors
  CONVERSATION_NOT_FOUND(ApiResponseStatus.NOT_FOUND, "Conversation not found"),
  CONVERSATION_UPDATE_UNAUTHORIZED(
      ApiResponseStatus.FORBIDDEN, "You are not authorized to update this conversation"),
  CONVERSATION_DELETE_UNAUTHORIZED(
      ApiResponseStatus.FORBIDDEN, "You are not authorized to delete this conversation"),
  DIRECT_CONVERSATION_EXISTS(
      ApiResponseStatus.BAD_REQUEST, "Direct conversation already exists between these users"),
  DIRECT_CONVERSATION_REQUIRES_TWO_PARTICIPANTS(
      ApiResponseStatus.BAD_REQUEST, "Direct conversation requires exactly 2 participants"),
  GROUP_CONVERSATION_REQUIRES_NAME(
      ApiResponseStatus.BAD_REQUEST, "Group conversation requires a name"),

  // Participant errors
  NOT_A_PARTICIPANT(ApiResponseStatus.FORBIDDEN, "You are not a participant in this conversation"),
  PARTICIPANT_NOT_FOUND(ApiResponseStatus.NOT_FOUND, "Participant not found"),
  CANNOT_REMOVE_LAST_ADMIN(ApiResponseStatus.BAD_REQUEST, "Cannot remove the last admin from the conversation"),
  ALREADY_A_PARTICIPANT(ApiResponseStatus.BAD_REQUEST, "User is already a participant"),

  // User errors
  USER_NOT_FOUND(ApiResponseStatus.NOT_FOUND, "User not found"),
  USER_SERVICE_UNAUTHORIZED(ApiResponseStatus.UNAUTHORIZED, "Unauthorized access to user service"),
  USER_SERVICE_ERROR(ApiResponseStatus.INTERNAL_SERVER_ERROR, "User service error"),
  ;

  ApiResponseDTO<Void> response;

  ErrorCode(ApiResponseStatus status, String message) {
    this.response =
        ApiResponseDTO.<Void>builder().status(status.getCode()).message(message).data(null).build();
  }

  public String getMessage() {
    String defaultMessage = getDefaultMessage(response.getStatus());
    String message = response.getMessage();
    return message.isEmpty() ? defaultMessage : message;
  }
}
