package com.bitetogether.chat_service.util;

public final class Constants {

  private Constants() {
    // Prevent instantiation
  }

  /** API endpoint paths */
  public static final class ApiPaths {
    private ApiPaths() {}

    public static final String API_V1 = "/api/v1";
    public static final String USER_STATE = API_V1 + "/user-state";
  }

  /** Redis cache key prefixes */
  public static final class RedisKeys {
    private RedisKeys() {}

    public static final String USER_STATE_PREFIX = "user:state:";
  }
}
