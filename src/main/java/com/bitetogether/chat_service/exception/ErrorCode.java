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
  MESSAGE_NOT_FOUND(ApiResponseStatus.NOT_FOUND, "Message not found"),
  MESSAGE_UPDATE_UNAUTHORIZED(
      ApiResponseStatus.FORBIDDEN, "You are not authorized to update this message"),
  MESSAGE_DELETE_UNAUTHORIZED(
      ApiResponseStatus.FORBIDDEN, "You are not authorized to delete this message"),

  ROOM_NOT_FOUND(ApiResponseStatus.NOT_FOUND, "Room not found"),
  ROOM_NAME_REQUIRED(ApiResponseStatus.BAD_REQUEST, "Group rooms must have a name"),
  ROOM_DIRECT_TWO_USERS_REQUIRED(
      ApiResponseStatus.BAD_REQUEST, "Direct rooms must have exactly 2 users"),
  ROOM_ADMIN_REQUIRED(ApiResponseStatus.FORBIDDEN, "Only admins can add members to group rooms"),
  ROOM_REMOVE_MEMBER_UNAUTHORIZED(
      ApiResponseStatus.FORBIDDEN, "Only admins can remove other members"),
  ROOM_ONLY_GROUP_HAS_ADMINS(ApiResponseStatus.BAD_REQUEST, "Only group rooms can have admins"),
  ROOM_PROMOTE_ADMIN_UNAUTHORIZED(
      ApiResponseStatus.FORBIDDEN, "Only admins can promote other users"),
  ROOM_USER_NOT_MEMBER(ApiResponseStatus.BAD_REQUEST, "User must be a member of the room"),

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
