package com.bebo.common.exception;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;

public record ApiError(
    Instant timestamp,
    int status,
    String code,
    String message,
    String path,
    Map<String, String> fieldErrors) {

  public ApiError {
    fieldErrors = fieldErrors == null ? Map.of() : Map.copyOf(fieldErrors);
  }

  public static ApiError create(HttpStatus status, String code, String message, String path) {
    return create(status, code, message, path, Map.of());
  }

  public static ApiError create(
      HttpStatus status,
      String code,
      String message,
      String path,
      Map<String, String> fieldErrors) {
    return new ApiError(Instant.now(), status.value(), code, message, path, fieldErrors);
  }
}
